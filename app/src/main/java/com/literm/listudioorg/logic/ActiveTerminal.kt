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

/** 悬浮球向当前终端注入原始按键序列的通道。 */
interface TerminalInputTarget {
    fun sendRawInput(text: String): Boolean
}

/**
 * 进程内当前活动的终端会话。
 *
 * 终端界面进入时 attach、退出时 detach；悬浮球服务只能通过这里拿到输入目标，
 * 因此同一时刻只保留最后一个 attach 的会话。
 */
object ActiveTerminal {
    @Volatile
    private var target: TerminalInputTarget? = null

    /** 终端进入前台时注册自己。 */
    fun attach(target: TerminalInputTarget) {
        this.target = target
    }

    /** 仅当注册的就是自己时才清空，避免误删新会话。 */
    fun detach(target: TerminalInputTarget) {
        if (this.target === target) {
            this.target = null
        }
    }

    /** 没有活动会话时返回 false，由界面提示用户。 */
    fun sendRawInput(text: String): Boolean = target?.sendRawInput(text) == true
}
