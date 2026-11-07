/*
 * Copyright 2024 li63050a <li63050@foxmail.com>
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

import androidx.annotation.StringRes
import com.literm.listudioorg.R

enum class LinuxInstallStatus {
    NotInstalled,
    Downloading,
    Installed,
}

data class LinuxDistro(
    val id: String,
    val name: String,
    @StringRes val descriptionRes: Int,
    val downloadUrlTemplate: String,
    val downloadVersion: String,
    val arm64Arch: String,
    val x86_64Arch: String,
    val installStatus: LinuxInstallStatus = LinuxInstallStatus.NotInstalled,
    val downloadProgress: Float = 0f,
)

fun defaultLinuxDistros(): List<LinuxDistro> = listOf(
    LinuxDistro(
        id = "alpine",
        name = "Alpine Linux",
        descriptionRes = R.string.distro_alpine_desc,
        downloadUrlTemplate = "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/{arch}/alpine-minirootfs-{version}-{arch}.tar.gz",
        downloadVersion = "3.24.4",
        arm64Arch = "aarch64",
        x86_64Arch = "x86_64",
    ),
    LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu",
        descriptionRes = R.string.distro_ubuntu_desc,
        downloadUrlTemplate = "https://cdimage.ubuntu.com/ubuntu-base/releases/{version}/release/ubuntu-base-{version}-base-{arch}.tar.gz",
        downloadVersion = "26.04",
        arm64Arch = "arm64",
        x86_64Arch = "amd64",
    ),
    // The official Debian, Arch, and Kali rootfs archives are compressed with
    // xz or zstd. UserLAnd publishes equivalent gzip archives for both ABIs.
    LinuxDistro(
        id = "debian",
        name = "Debian",
        descriptionRes = R.string.distro_debian_desc,
        downloadUrlTemplate = "https://github.com/CypherpunkArmory/UserLAnd-Assets-Debian/releases/download/{version}/{arch}-rootfs.tar.gz",
        downloadVersion = "v0.0.10",
        arm64Arch = "arm64",
        x86_64Arch = "x86_64",
    ),
    LinuxDistro(
        id = "arch",
        name = "Arch Linux",
        descriptionRes = R.string.distro_arch_desc,
        downloadUrlTemplate = "https://github.com/CypherpunkArmory/UserLAnd-Assets-Arch/releases/download/{version}/{arch}-rootfs.tar.gz",
        downloadVersion = "v0.0.4",
        arm64Arch = "arm64",
        x86_64Arch = "x86_64",
    ),
    LinuxDistro(
        id = "kali",
        name = "Kali Linux",
        descriptionRes = R.string.distro_kali_desc,
        downloadUrlTemplate = "https://github.com/CypherpunkArmory/UserLAnd-Assets-Kali/releases/download/{version}/{arch}-rootfs.tar.gz",
        downloadVersion = "v0.0.9",
        arm64Arch = "arm64",
        x86_64Arch = "x86_64",
    ),
)
