/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.videoplayer.ui.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import me.him188.ani.app.data.models.preference.PlayerShortcutAction
import me.him188.ani.app.data.models.preference.PlayerShortcutConfig
import me.him188.ani.app.ui.foundation.effects.PlayerShortcutKeys
import me.him188.ani.app.videoplayer.ui.PlayerKeyboardHandler
import me.him188.ani.app.videoplayer.ui.nextPlaybackSpeed

/** Builds the playback commands dispatched by the player's keyboard scope. */
@Composable
internal fun rememberPlayerKeyboardShortcuts(
    seekerState: SwipeSeekerState,
    fastSkipState: FastSkipState?,
    currentPlaybackSpeed: Float?,
    playbackSpeedRange: ClosedFloatingPointRange<Float>,
    onPlaybackSpeedChanged: (Float) -> Unit,
    volumeEnabled: Boolean,
    onVolumeUp: (fineAdjustment: Boolean) -> Unit,
    onVolumeDown: (fineAdjustment: Boolean) -> Unit,
    onTogglePauseResume: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onTogglePlayerStats: () -> Unit,
    shortcuts: PlayerShortcutConfig = PlayerShortcutConfig.Default,
): PlayerKeyboardHandler {
    val seekHandler = rememberKeyboardSeekHandler(
        onSeekBackward = { seekerState.onSeek(-5) },
        onSeekForward = { seekerState.onSeek(5) },
        fastSkipState = fastSkipState,
        shortcuts = shortcuts,
    )
    val commands by rememberUpdatedState<(KeyEvent) -> Boolean> command@{ event ->
        val name = PlayerShortcutKeys.nameOf(event.key) ?: return@command false
        val action = PlayerShortcutAction.entries.firstOrNull { shortcuts.key(it) == name }
            ?: return@command false
        when (action) {
            PlayerShortcutAction.VOLUME_UP, PlayerShortcutAction.VOLUME_DOWN -> {
                if (!volumeEnabled) return@command false
                if (event.type == KeyEventType.KeyDown) {
                    if (action == PlayerShortcutAction.VOLUME_UP) onVolumeUp(event.isShiftPressed)
                    else onVolumeDown(event.isShiftPressed)
                }
                true
            }
            PlayerShortcutAction.SEEK_BACKWARD, PlayerShortcutAction.SEEK_FORWARD -> false
            else -> {
                val callback = when (action) {
                    PlayerShortcutAction.TOGGLE_PAUSE -> onTogglePauseResume
                    PlayerShortcutAction.TOGGLE_FULLSCREEN -> onToggleFullscreen
                    PlayerShortcutAction.TOGGLE_DANMAKU -> onToggleDanmaku
                    PlayerShortcutAction.TOGGLE_STATS -> onTogglePlayerStats
                    else -> null
                }
                if (callback != null) {
                    if (event.type == KeyEventType.KeyUp) callback()
                    return@command true
                }
                if (currentPlaybackSpeed == null) return@command false
                val speed = when (action) {
                    PlayerShortcutAction.SPEED_DOWN -> nextPlaybackSpeed(currentPlaybackSpeed, playbackSpeedRange, -1)
                    PlayerShortcutAction.SPEED_UP -> nextPlaybackSpeed(currentPlaybackSpeed, playbackSpeedRange, 1)
                    PlayerShortcutAction.SPEED_RESET, PlayerShortcutAction.SPEED_1 -> 1f
                    PlayerShortcutAction.SPEED_2 -> 2f
                    PlayerShortcutAction.SPEED_3 -> 3f
                    else -> return@command false
                }
                if (event.type == KeyEventType.KeyUp) onPlaybackSpeedChanged(speed.coerceIn(playbackSpeedRange))
                true
            }
        }
    }
    return remember(seekHandler) {
        object : PlayerKeyboardHandler {
            override fun invoke(event: KeyEvent): Boolean =
                seekHandler(event) || commands(event)
            override fun cancel() = seekHandler.cancel()
        }
    }
}
