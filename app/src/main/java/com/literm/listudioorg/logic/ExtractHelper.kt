/*
 * Copyright 2026 li63050a <li63050@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.literm.listudioorg.logic

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAR_BLOCK_SIZE = 512
private const val IO_BUFFER_SIZE = 64 * 1024
private const val MAX_METADATA_SIZE = 16L * 1024L * 1024L
private const val DEFAULT_FILE_MODE = 0x1A4 // 0644
private const val DEFAULT_DIRECTORY_MODE = 0x1ED // 0755

object ExtractHelper {
    suspend fun extract(
        context: Context,
        distroId: String,
        archiveFile: File,
    ): File = withContext(Dispatchers.IO) {
        if (!archiveFile.isFile) {
            throw IOException("Rootfs archive not found: ${archiveFile.absolutePath}")
        }

        val instanceDir = File(context.filesDir, "instances/$distroId")
        val rootfsDir = File(instanceDir, "rootfs")
        deleteNode(rootfsDir)
        if (!rootfsDir.mkdirs()) {
            throw IOException("Unable to create ${rootfsDir.absolutePath}")
        }
        val rootDir = rootfsDir.canonicalFile

        try {
            if (archiveFile.name.endsWith(".zip", ignoreCase = true)) {
                extractZip(archiveFile, rootDir)
            } else {
                extractTarGzip(archiveFile, rootDir)
            }
            writeResolvConf(rootDir)
            rootDir
        } catch (throwable: Throwable) {
            runCatching { deleteNode(rootDir) }
            throw throwable
        }
    }

    private fun extractTarGzip(archiveFile: File, rootDir: File) {
        GZIPInputStream(
            BufferedInputStream(FileInputStream(archiveFile), IO_BUFFER_SIZE),
            IO_BUFFER_SIZE,
        ).use { input ->
            TarExtractor(input, rootDir).extract()
        }
    }

    private fun extractZip(archiveFile: File, rootDir: File) {
        ZipInputStream(
            BufferedInputStream(FileInputStream(archiveFile), IO_BUFFER_SIZE),
        ).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (entryName != null) {
                    val target = resolveSafeTarget(rootDir, entryName)
                    if (target != null) {
                        if (entry.isDirectory) {
                            ensureDirectory(rootDir, target, DEFAULT_DIRECTORY_MODE)
                        } else {
                            ensureParentDirectory(rootDir, target)
                            deleteNode(target)
                            target.outputStream().buffered(IO_BUFFER_SIZE).use { output ->
                                zip.copyTo(output, IO_BUFFER_SIZE)
                            }
                            applyMode(target, DEFAULT_FILE_MODE, DEFAULT_FILE_MODE)
                        }
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun writeResolvConf(rootDir: File) {
        val etcDir = File(rootDir, "etc")
        if (isSymlink(etcDir)) {
            deleteNode(etcDir)
        }
        if (etcDir.exists() && !etcDir.isDirectory) {
            deleteNode(etcDir)
        }
        if (!etcDir.exists() && !etcDir.mkdirs()) {
            throw IOException("Unable to create ${etcDir.absolutePath}")
        }
        ensureInsideRoot(rootDir, etcDir.canonicalFile)

        val resolvConf = File(etcDir, "resolv.conf")
        deleteNode(resolvConf)
        resolvConf.writeText("nameserver 8.8.8.8\n")
        applyMode(resolvConf, DEFAULT_FILE_MODE, DEFAULT_FILE_MODE)
    }

    private class TarExtractor(
        private val input: InputStream,
        rootDir: File,
    ) {
        private val rootDir = rootDir.canonicalFile
        private val header = ByteArray(TAR_BLOCK_SIZE)
        private val ioBuffer = ByteArray(IO_BUFFER_SIZE)
        private var pendingLongName: String? = null
        private var pendingLongLink: String? = null
        private var pendingPax: MutableMap<String, String>? = null

        fun extract() {
            while (true) {
                val read = readFully(input, header)
                if (read == 0) return
                if (read < TAR_BLOCK_SIZE) {
                    throw IOException("Truncated tar header")
                }
                if (header.all { byte -> byte == 0.toByte() }) {
                    return
                }
                extractHeader()
            }
        }

        private fun extractHeader() {
            val headerName = buildHeaderName()
            val headerMode = readOctal(header, 100, 8)
            val headerSize = readOctal(header, 124, 12)
            val typeFlag = header[156].toInt().toChar()
            val rawLinkName = readString(header, 157, 100)

            when (typeFlag) {
                'x' -> {
                    val pax = pendingPax
                        ?: mutableMapOf<String, String>().also { pendingPax = it }
                    parsePax(readMetadata(headerSize), pax)
                    skipPadding(headerSize)
                }

                'g' -> {
                    skipMetadata(headerSize)
                }

                'L' -> {
                    pendingLongName = String(readMetadata(headerSize), Charsets.UTF_8)
                        .trimEnd('\u0000')
                    skipPadding(headerSize)
                }

                'K' -> {
                    pendingLongLink = String(readMetadata(headerSize), Charsets.UTF_8)
                        .trimEnd('\u0000')
                    skipPadding(headerSize)
                }

                else -> extractEntry(
                    headerName = headerName,
                    headerMode = headerMode,
                    headerSize = headerSize,
                    typeFlag = typeFlag,
                    rawLinkName = rawLinkName,
                )
            }
        }

        private fun extractEntry(
            headerName: String,
            headerMode: Long,
            headerSize: Long,
            typeFlag: Char,
            rawLinkName: String,
        ) {
            val pax = pendingPax
            val name = pax?.get("path")?.takeIf { it.isNotEmpty() }
                ?: pendingLongName
                ?: headerName
            val linkName = pax?.get("linkpath")?.takeIf { it.isNotEmpty() }
                ?: pendingLongLink
                ?: rawLinkName
            val size = pax?.get("size")?.toLongOrNull() ?: headerSize
            val mode = pax?.get("mode")?.toLongOrNull(8)?.toInt() ?: headerMode.toInt()

            pendingPax = null
            pendingLongName = null
            pendingLongLink = null

            if (size < 0L) {
                throw IOException("Invalid tar entry size for $name")
            }

            when (typeFlag) {
                '\u0000', '0', ' ', '7' -> extractFile(name, size, mode)
                '1' -> extractHardLink(name, linkName, size)
                '2' -> extractSymlink(name, linkName, size)
                '5' -> extractDirectory(name, size, mode)
                '3', '4', '6' -> skipEntryData(size)
                else -> skipEntryData(size)
            }
        }

        private fun buildHeaderName(): String {
            val name = readString(header, 0, 100)
            val prefix = readString(header, 345, 155)
            return if (prefix.isEmpty()) name else "$prefix/$name"
        }

        private fun extractFile(name: String, size: Long, mode: Int) {
            val target = resolveSafeTarget(rootDir, name)
            if (target == null) {
                skipEntryData(size)
                return
            }

            ensureParentDirectory(rootDir, target)
            deleteNode(target)
            if (!target.createNewFile()) {
                throw IOException("Unable to create ${target.absolutePath}")
            }

            target.outputStream().buffered(IO_BUFFER_SIZE).use { output ->
                var remaining = size
                while (remaining > 0L) {
                    val toRead = minOf(remaining, ioBuffer.size.toLong()).toInt()
                    val read = input.read(ioBuffer, 0, toRead)
                    if (read == -1) {
                        throw IOException("Truncated tar entry: $name")
                    }
                    output.write(ioBuffer, 0, read)
                    remaining -= read
                }
            }

            skipPadding(size)
            applyMode(target, mode, DEFAULT_FILE_MODE)
        }

        private fun extractDirectory(name: String, size: Long, mode: Int) {
            val target = resolveSafeTarget(rootDir, name)
            if (target != null) {
                ensureDirectory(rootDir, target, mode)
            }
            skipEntryData(size)
        }

        private fun extractSymlink(name: String, linkName: String, size: Long) {
            val target = resolveSafeTarget(rootDir, name)
            if (target == null) {
                skipEntryData(size)
                return
            }
            if (linkName.isEmpty()) {
                throw IOException("Symlink entry without a target: $name")
            }

            ensureParentDirectory(rootDir, target)
            deleteNode(target)
            try {
                Os.symlink(linkName, target.path)
            } catch (error: ErrnoException) {
                throw IOException("Unable to create symlink $name -> $linkName", error)
            }
            skipEntryData(size)
        }

        private fun extractHardLink(name: String, linkName: String, size: Long) {
            val target = resolveSafeTarget(rootDir, name)
            val source = resolveSafeTarget(rootDir, linkName)
            if (target == null || source == null) {
                skipEntryData(size)
                return
            }

            ensureParentDirectory(rootDir, target)
            deleteNode(target)

            val sourceFile = source.canonicalFile
            ensureInsideRoot(rootDir, sourceFile)
            if (!sourceFile.isFile) {
                throw IOException("Hard link target is missing: $linkName")
            }

            try {
                Os.link(sourceFile.path, target.path)
            } catch (_: ErrnoException) {
                sourceFile.copyTo(target, overwrite = false, bufferSize = IO_BUFFER_SIZE)
                applyModeFrom(sourceFile, target)
            }
            skipEntryData(size)
        }

        private fun skipEntryData(size: Long) {
            skipFully(input, size)
            skipPadding(size)
        }

        private fun skipPadding(size: Long) {
            val remainder = size % TAR_BLOCK_SIZE.toLong()
            if (remainder != 0L) {
                skipFully(input, TAR_BLOCK_SIZE.toLong() - remainder)
            }
        }

        private fun readMetadata(size: Long): ByteArray {
            if (size < 0L || size > MAX_METADATA_SIZE) {
                throw IOException("Invalid tar metadata size: $size")
            }
            val data = ByteArray(size.toInt())
            val read = readFully(input, data)
            if (read != data.size) {
                throw IOException("Truncated tar metadata")
            }
            return data
        }

        private fun skipMetadata(size: Long) {
            if (size < 0L) {
                throw IOException("Invalid tar metadata size: $size")
            }
            skipFully(input, size)
            skipPadding(size)
        }

        private fun parsePax(data: ByteArray, target: MutableMap<String, String>) {
            var offset = 0
            while (offset < data.size) {
                var space = offset
                while (space < data.size && data[space] != ' '.code.toByte()) {
                    space++
                }
                if (space >= data.size) return

                val recordLength = String(
                    data,
                    offset,
                    space - offset,
                    Charsets.US_ASCII,
                ).toIntOrNull() ?: return
                if (recordLength <= 0 || offset + recordLength > data.size) return

                val recordEnd = offset + recordLength
                var equals = space + 1
                while (equals < recordEnd && data[equals] != '='.code.toByte()) {
                    equals++
                }

                if (equals < recordEnd) {
                    val key = String(data, space + 1, equals - (space + 1), Charsets.UTF_8)
                    var valueEnd = recordEnd
                    while (
                        valueEnd > equals + 1 &&
                        (data[valueEnd - 1] == '\n'.code.toByte() ||
                            data[valueEnd - 1] == 0.toByte())
                    ) {
                        valueEnd--
                    }
                    val value = String(data, equals + 1, valueEnd - (equals + 1), Charsets.UTF_8)
                    if (key.isNotEmpty()) {
                        target[key] = value
                    }
                }

                offset = recordEnd
            }
        }

        private fun readString(bytes: ByteArray, offset: Int, length: Int): String {
            var end = offset
            val limit = minOf(offset + length, bytes.size)
            while (end < limit && bytes[end] != 0.toByte()) {
                end++
            }
            return String(bytes, offset, end - offset, Charsets.UTF_8)
        }

        private fun readOctal(bytes: ByteArray, offset: Int, length: Int): Long {
            var value = 0L
            var foundDigit = false
            val limit = minOf(offset + length, bytes.size)

            for (index in offset until limit) {
                val byte = bytes[index].toInt() and 0xFF
                when {
                    byte == 0 || byte == ' '.code -> {
                        if (foundDigit) break
                    }

                    byte in '0'.code..'7'.code -> {
                        value = value * 8L + (byte - '0'.code)
                        foundDigit = true
                    }

                    else -> return 0L
                }
            }

            return value
        }

        private fun readFully(input: InputStream, buffer: ByteArray): Int {
            var offset = 0
            while (offset < buffer.size) {
                val read = input.read(buffer, offset, buffer.size - offset)
                if (read == -1) break
                if (read == 0) continue
                offset += read
            }
            return offset
        }

        private fun skipFully(input: InputStream, byteCount: Long) {
            var remaining = byteCount
            while (remaining > 0L) {
                val skipped = input.skip(remaining)
                if (skipped > 0L) {
                    remaining -= skipped
                    continue
                }

                val toRead = minOf(remaining, ioBuffer.size.toLong()).toInt()
                val read = input.read(ioBuffer, 0, toRead)
                if (read == -1) {
                    throw IOException("Unexpected end of tar stream")
                }
                if (read > 0) {
                    remaining -= read
                }
            }
        }
    }
}

private fun resolveSafeTarget(rootDir: File, rawName: String): File? {
    val relativePath = normalizeEntryName(rawName) ?: return null
    val target = File(rootDir, relativePath)
    val parent = target.parentFile ?: throw IOException("Invalid entry path: $rawName")
    ensureInsideRoot(rootDir, parent.canonicalFile)
    return target
}

private fun normalizeEntryName(rawName: String): String? {
    if (rawName.indexOf('\u0000') >= 0) {
        throw IOException("Invalid entry name")
    }

    val segments = rawName.split('/')
    val safeSegments = ArrayList<String>(segments.size)
    for (segment in segments) {
        when (segment) {
            "", "." -> Unit
            ".." -> throw IOException("Unsafe entry path: $rawName")
            else -> safeSegments.add(segment)
        }
    }

    return if (safeSegments.isEmpty()) null else safeSegments.joinToString("/")
}

private fun ensureInsideRoot(rootDir: File, file: File) {
    val rootPath = rootDir.path
    val path = file.canonicalPath
    if (path != rootPath && !path.startsWith(rootPath + File.separator)) {
        throw IOException("Blocked path outside rootfs: ${file.path}")
    }
}

private fun ensureParentDirectory(rootDir: File, target: File) {
    val parent = target.parentFile ?: throw IOException("Invalid entry path: ${target.path}")
    ensureInsideRoot(rootDir, parent.canonicalFile)
    if (!parent.exists() && !parent.mkdirs()) {
        throw IOException("Unable to create directory: ${parent.absolutePath}")
    }
    if (!parent.isDirectory) {
        throw IOException("Not a directory: ${parent.absolutePath}")
    }
    ensureInsideRoot(rootDir, parent.canonicalFile)
}

private fun ensureDirectory(rootDir: File, target: File, mode: Int) {
    if (isSymlink(target)) {
        deleteNode(target)
    }
    if (target.exists() && !target.isDirectory) {
        deleteNode(target)
    }
    if (!target.exists() && !target.mkdirs()) {
        throw IOException("Unable to create directory: ${target.absolutePath}")
    }
    ensureInsideRoot(rootDir, target.canonicalFile)
    applyMode(target, mode, DEFAULT_DIRECTORY_MODE)
}

private fun deleteNode(file: File) {
    val stat = lstatOrNull(file) ?: return
    val type = (stat.st_mode and OsConstants.S_IFMT)
    if (type == OsConstants.S_IFDIR) {
        val children = file.listFiles()
        if (children != null) {
            for (child in children) {
                deleteNode(child)
            }
        }
        if (!file.delete()) {
            throw IOException("Unable to delete ${file.absolutePath}")
        }
    } else if (!file.delete()) {
        throw IOException("Unable to delete ${file.absolutePath}")
    }
}

private fun isSymlink(file: File): Boolean {
    val stat = lstatOrNull(file) ?: return false
    return (stat.st_mode and OsConstants.S_IFMT) == OsConstants.S_IFLNK
}

private fun lstatOrNull(file: File): StructStat? =
    try {
        Os.lstat(file.path)
    } catch (_: ErrnoException) {
        null
    }

private fun applyMode(file: File, mode: Int, defaultMode: Int) {
    val permissions = (mode and 0x1FF).takeIf { it != 0 } ?: defaultMode
    try {
        Os.chmod(file.path, permissions)
    } catch (_: ErrnoException) {
        file.setReadable((permissions and 0x100) != 0, true)
        file.setWritable((permissions and 0x80) != 0, true)
        file.setExecutable((permissions and 0x40) != 0, true)
    }
}

private fun applyModeFrom(source: File, target: File) {
    val stat = lstatOrNull(source) ?: return
    applyMode(target, (stat.st_mode and 0x1FF), DEFAULT_FILE_MODE)
}
