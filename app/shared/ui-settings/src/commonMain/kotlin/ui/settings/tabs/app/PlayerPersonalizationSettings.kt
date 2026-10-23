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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import me.him188.ani.app.data.models.preference.PlayerShortcutAction
import me.him188.ani.app.data.models.preference.PlayerShortcutConfig
import me.him188.ani.app.data.models.preference.VideoScaffoldConfig
import me.him188.ani.app.ui.foundation.effects.PlayerShortcutKeys
import me.him188.ani.app.ui.lang.*
import me.him188.ani.app.ui.settings.framework.SettingsState
import me.him188.ani.app.ui.settings.framework.components.SettingsScope
import me.him188.ani.app.ui.settings.framework.components.SwitchItem
import me.him188.ani.app.ui.settings.framework.components.TextButtonItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsScope.PlayerTrackMemorySettings(state: SettingsState<VideoScaffoldConfig>) {
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
}

@Composable
internal fun SettingsScope.PlayerShortcutsSettings(state: SettingsState<VideoScaffoldConfig>) {
    val config by state
    var capturingAction by remember { mutableStateOf<PlayerShortcutAction?>(null) }
    var pendingSwap by remember { mutableStateOf<Pair<PlayerShortcutAction, String>?>(null) }

    Group(
        title = { Text(stringResource(Lang.settings_player_shortcuts)) },
        description = { Text(stringResource(Lang.settings_player_shortcut_capture_hint)) },
        actions = {
            TextButton(onClick = {
                state.update(config.copy(keyboardShortcuts = PlayerShortcutConfig.Default))
            }) {
                Text(stringResource(Lang.settings_player_shortcuts_reset))
            }
        },
    ) {
        PlayerShortcutAction.entries.forEach { action ->
            PlayerShortcutBindingItem(
                action = action,
                keyName = config.keyboardShortcuts.key(action),
                capturing = capturingAction == action,
                onStartCapture = { capturingAction = action },
                onCancelCapture = { capturingAction = null },
                onCapture = { keyName ->
                    capturingAction = null
                    if (config.keyboardShortcuts.conflicts(action, keyName).isEmpty()) {
                        state.update(config.copy(keyboardShortcuts = config.keyboardShortcuts.bind(action, keyName)))
                    } else {
                        pendingSwap = action to keyName
                    }
                },
            )
        }
    }

    pendingSwap?.let { (action, keyName) ->
        val other = config.keyboardShortcuts.conflicts(action, keyName).firstOrNull()
        AlertDialog(
            onDismissRequest = { pendingSwap = null },
            title = { Text(stringResource(Lang.settings_player_shortcut_conflict)) },
            text = {
                Text(stringResource(Lang.settings_player_shortcut_swap_description, keyName, other?.title().orEmpty()))
            },
            confirmButton = {
                TextButton(onClick = {
                    val latest = state.value
                    state.update(latest.copy(keyboardShortcuts = latest.keyboardShortcuts.bind(action, keyName, swap = true)))
                    pendingSwap = null
                }) {
                    Text(stringResource(Lang.settings_player_shortcut_swap))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwap = null }) {
                    Text(stringResource(Lang.settings_player_shortcut_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsScope.PlayerShortcutBindingItem(
    action: PlayerShortcutAction,
    keyName: String,
    capturing: Boolean,
    onStartCapture: () -> Unit,
    onCancelCapture: () -> Unit,
    onCapture: (String) -> Unit,
) {
    val focusRequester = remember(action) { FocusRequester() }
    LaunchedEffect(capturing) {
        if (capturing) focusRequester.requestFocus()
    }

    Item(
        headlineContent = { Text(action.title()) },
        trailingContent = {
            Button(
                onClick = onStartCapture,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (!capturing) return@onPreviewKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        when {
                            event.key == ComposeKey.Escape -> onCancelCapture()
                            event.isCtrlPressed || event.isAltPressed || event.isMetaPressed -> Unit
                            event.key == ComposeKey.Backspace || event.key == ComposeKey.Delete -> onCapture("")
                            else -> PlayerShortcutKeys.nameOf(event.key)?.let(onCapture)
                        }
                        true
                    },
            ) {
                Text(
                    if (capturing) stringResource(Lang.settings_player_shortcut_press_key)
                    else keyName.ifEmpty { stringResource(Lang.settings_player_shortcut_unbound) },
                )
            }
        },
    )
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
