/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.effects

import androidx.compose.ui.input.key.Key

/** Canonical names shared by shortcut capture and command dispatch. */
object PlayerShortcutKeys {
    private val named = mapOf(
        "SPACE" to ComposeKey.Spacebar,
        "LEFT" to ComposeKey.DirectionLeft, "RIGHT" to ComposeKey.DirectionRight,
        "UP" to ComposeKey.DirectionUp, "DOWN" to ComposeKey.DirectionDown,
        "A" to ComposeKey.A, "B" to ComposeKey.B, "C" to ComposeKey.C, "D" to ComposeKey.D,
        "E" to ComposeKey.E, "F" to ComposeKey.F, "G" to ComposeKey.G, "H" to ComposeKey.H,
        "I" to ComposeKey.I, "J" to ComposeKey.J, "K" to ComposeKey.K, "L" to ComposeKey.L,
        "M" to ComposeKey.M, "N" to ComposeKey.N, "O" to ComposeKey.O, "P" to ComposeKey.P,
        "Q" to ComposeKey.Q, "R" to ComposeKey.R, "S" to ComposeKey.S, "T" to ComposeKey.T,
        "U" to ComposeKey.U, "V" to ComposeKey.V, "W" to ComposeKey.W, "X" to ComposeKey.X,
        "Y" to ComposeKey.Y, "Z" to ComposeKey.Z,
        "0" to ComposeKey.Zero, "1" to ComposeKey.One, "2" to ComposeKey.Two,
        "3" to ComposeKey.Three, "4" to ComposeKey.Four, "5" to ComposeKey.Five,
        "6" to ComposeKey.Six, "7" to ComposeKey.Seven, "8" to ComposeKey.Eight, "9" to ComposeKey.Nine,
        "F1" to ComposeKey.F1, "F2" to ComposeKey.F2, "F3" to ComposeKey.F3, "F4" to ComposeKey.F4,
        "F5" to ComposeKey.F5, "F6" to ComposeKey.F6, "F7" to ComposeKey.F7, "F8" to ComposeKey.F8,
        "F9" to ComposeKey.F9, "F10" to ComposeKey.F10, "F11" to ComposeKey.F11, "F12" to ComposeKey.F12,
    )
    private val keypad = listOf(ComposeKey.NumPad0, ComposeKey.NumPad1, ComposeKey.NumPad2, ComposeKey.NumPad3,
        ComposeKey.NumPad4, ComposeKey.NumPad5, ComposeKey.NumPad6, ComposeKey.NumPad7, ComposeKey.NumPad8, ComposeKey.NumPad9)

    fun nameOf(key: Key): String? = named.entries.firstOrNull { it.value == key }?.key
        ?: keypad.indexOf(key).takeIf { it >= 0 }?.toString()

    fun keyOf(name: String): Key? = named[name]
}
