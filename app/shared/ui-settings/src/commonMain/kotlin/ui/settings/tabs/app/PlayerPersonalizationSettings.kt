/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.tabs.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.him188.ani.app.data.models.preference.PlayerShortcutAction
import me.him188.ani.app.data.models.preference.PlayerShortcutConfig
import me.him188.ani.app.data.models.preference.VideoScaffoldConfig
import me.him188.ani.app.ui.lang.*
import me.him188.ani.app.ui.settings.framework.SettingsState
import me.him188.ani.app.ui.settings.framework.components.DropdownItem
import me.him188.ani.app.ui.settings.framework.components.SettingsScope
import me.him188.ani.app.ui.settings.framework.components.SwitchItem
import me.him188.ani.app.ui.settings.framework.components.TextButtonItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsScope.PlayerPersonalizationSettings(state: SettingsState<VideoScaffoldConfig>) {
    val config by state
    Group(title = { Text(stringResource(Lang.settings_player_track_memory)) }) {
        SwitchItem(
            checked = config.rememberTrackSelection,
            onCheckedChange = { state.update(config.copy(rememberTrackSelection = it)) },
            title = { Text(stringResource(Lang.settings_player_track_memory)) },
            description = { Text(stringResource(Lang.settings_player_track_memory_description)) },
        )
        TextButtonItem(onClick = { state.update(config.copy(trackPreferences = emptyMap())) }) {
            Text(stringResource(Lang.settings_player_track_memory_clear))
        }
    }
    var pendingSwap by remember { mutableStateOf<Pair<PlayerShortcutAction, String>?>(null) }
    Group(title = { Text(stringResource(Lang.settings_player_shortcuts)) }) {
        PlayerShortcutAction.entries.forEach { action ->
            DropdownItem(
                selected = { config.keyboardShortcuts.key(action) },
                values = { listOf("") + PlayerShortcutConfig.supportedKeys },
                itemText = { Text(if (it.isEmpty()) stringResource(Lang.settings_player_shortcut_unbound) else it) },
                title = { Text(action.title()) },
                onSelect = { key ->
                    if (config.keyboardShortcuts.conflicts(action, key).isEmpty()) {
                        state.update(config.copy(keyboardShortcuts = config.keyboardShortcuts.bind(action, key)))
                    } else pendingSwap = action to key
                },
            )
        }
        TextButtonItem(onClick = { state.update(config.copy(keyboardShortcuts = PlayerShortcutConfig.Default)) }) {
            Text(stringResource(Lang.settings_player_shortcuts_reset))
        }
    }
    pendingSwap?.let { (action, key) ->
        val other = config.keyboardShortcuts.conflicts(action, key).firstOrNull()
        AlertDialog(
            onDismissRequest = { pendingSwap = null },
            title = { Text(stringResource(Lang.settings_player_shortcut_conflict)) },
            text = { Text(stringResource(Lang.settings_player_shortcut_swap_description, key, other?.title().orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    val latest = state.value
                    state.update(latest.copy(keyboardShortcuts = latest.keyboardShortcuts.bind(action, key, swap = true)))
                    pendingSwap = null
                }) { Text(stringResource(Lang.settings_player_shortcut_swap)) }
            },
            dismissButton = { TextButton(onClick = { pendingSwap = null }) { Text(stringResource(Lang.settings_player_shortcut_cancel)) } },
        )
    }
}

@Composable
private fun PlayerShortcutAction.title(): String = stringResource(when (this) {
    PlayerShortcutAction.TOGGLE_PAUSE -> Lang.settings_player_shortcut_pause
    PlayerShortcutAction.TOGGLE_FULLSCREEN -> Lang.settings_player_shortcut_fullscreen
    PlayerShortcutAction.TOGGLE_DANMAKU -> Lang.settings_player_shortcut_danmaku
    PlayerShortcutAction.TOGGLE_STATS -> Lang.settings_player_shortcut_stats
    PlayerShortcutAction.SEEK_BACKWARD -> Lang.settings_player_shortcut_backward
    PlayerShortcutAction.SEEK_FORWARD -> Lang.settings_player_shortcut_forward
    PlayerShortcutAction.VOLUME_UP -> Lang.settings_player_shortcut_volume_up
    PlayerShortcutAction.VOLUME_DOWN -> Lang.settings_player_shortcut_volume_down
    PlayerShortcutAction.SPEED_DOWN -> Lang.settings_player_shortcut_speed_down
    PlayerShortcutAction.SPEED_UP -> Lang.settings_player_shortcut_speed_up
    PlayerShortcutAction.SPEED_RESET -> Lang.settings_player_shortcut_speed_reset
    PlayerShortcutAction.SPEED_1 -> Lang.settings_player_shortcut_speed_1
    PlayerShortcutAction.SPEED_2 -> Lang.settings_player_shortcut_speed_2
    PlayerShortcutAction.SPEED_3 -> Lang.settings_player_shortcut_speed_3
})
