/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.models.preference

import kotlinx.serialization.Serializable

@Serializable
enum class PlayerShortcutAction(val defaultKey: String) {
    TOGGLE_PAUSE("SPACE"),
    TOGGLE_FULLSCREEN("F"),
    TOGGLE_DANMAKU("B"),
    TOGGLE_STATS("I"),
    SEEK_BACKWARD("LEFT"),
    SEEK_FORWARD("RIGHT"),
    VOLUME_UP("UP"),
    VOLUME_DOWN("DOWN"),
    SPEED_DOWN("A"),
    SPEED_UP("D"),
    SPEED_RESET("S"),
    SPEED_1("1"),
    SPEED_2("2"),
    SPEED_3("3"),
}

/** Logical key names are portable between desktop platforms. An empty name disables an action. */
@Serializable
data class PlayerShortcutConfig(
    val overrides: Map<PlayerShortcutAction, String> = emptyMap(),
) {
    fun key(action: PlayerShortcutAction): String = overrides[action] ?: action.defaultKey

    fun conflicts(action: PlayerShortcutAction, keyName: String): List<PlayerShortcutAction> =
        if (keyName.isEmpty()) emptyList() else PlayerShortcutAction.entries.filter { it != action && key(it) == keyName }

    /** A confirmed swap transfers the action's previous binding to the conflicting action. */
    fun bind(action: PlayerShortcutAction, keyName: String, swap: Boolean = false): PlayerShortcutConfig {
        require(keyName.isEmpty() || keyName in supportedKeys)
        val conflicts = conflicts(action, keyName)
        require(swap || conflicts.isEmpty()) { "Key is already assigned" }
        val previous = key(action)
        return copy(overrides = overrides.toMutableMap().apply {
            conflicts.forEachIndexed { index, other -> put(other, if (index == 0) previous else "") }
            put(action, keyName)
        })
    }

    companion object {
        val supportedKeys: List<String> = listOf("SPACE", "LEFT", "RIGHT", "UP", "DOWN") +
            ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() } + (1..12).map { "F$it" }
        val Default = PlayerShortcutConfig()
    }
}
