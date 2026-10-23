/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.him188.ani.app.data.models.preference.PlayerTrackPreference
import me.him188.ani.app.data.models.preference.SubjectTrackPreferences
import me.him188.ani.app.data.models.preference.VideoScaffoldConfig
import me.him188.ani.app.data.repository.user.Settings
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.isMediaLoaded
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack
import org.openani.mediamp.metadata.TrackGroup

/** Remembers explicit choices for one subject and restores them after each media load. */
class PlayerTrackMemory(
    private val subjectId: Int,
    private val player: MediampPlayer,
    private val settings: Settings<VideoScaffoldConfig>,
    private val scope: CoroutineScope,
) {
    private var audioRevision = 0L
    private var subtitleRevision = 0L

    init {
        scope.launch {
            player.mediaData.collectLatest { data ->
                if (data == null) return@collectLatest
                val audioBefore = audioRevision
                val subtitleBefore = subtitleRevision
                player.state.first { it.isMediaLoaded }
                val config = settings.flow.first()
                if (!config.rememberTrackSelection) return@collectLatest
                val preference = config.trackPreferences[subjectId.toString()] ?: return@collectLatest
                coroutineScope {
                    launch { restore(player.audioTracks, preference.audio, { audioRevision == audioBefore }, ::describeAudio) }
                    launch { restore(player.subtitleTracks, preference.subtitle, { subtitleRevision == subtitleBefore }, ::describeSubtitle) }
                }
            }
        }
    }

    fun selectAudio(track: AudioTrack?) {
        audioRevision++
        if (player.audioTracks?.select(track) == true) {
            save { copy(audio = track?.let(::describeAudio) ?: PlayerTrackPreference(disabled = true)) }
        }
    }

    fun selectSubtitle(track: SubtitleTrack?) {
        subtitleRevision++
        if (player.subtitleTracks?.select(track) == true) {
            save { copy(subtitle = track?.let(::describeSubtitle) ?: PlayerTrackPreference(disabled = true)) }
        }
    }

    private fun save(change: SubjectTrackPreferences.() -> SubjectTrackPreferences) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            // A completed user selection remains durable when the playback page closes immediately.
            withContext(NonCancellable) {
                settings.update {
                    if (!rememberTrackSelection) return@update this
                    val id = subjectId.toString()
                    val updated = (trackPreferences[id] ?: SubjectTrackPreferences()).change()
                    copy(trackPreferences = (trackPreferences.filterKeys { it != id } + (id to updated))
                        .entries.toList().takeLast(128).associate { it.toPair() })
                }
            }
        }
    }

    private suspend fun <T> restore(
        group: TrackGroup<T>?,
        preference: PlayerTrackPreference?,
        unchanged: () -> Boolean,
        describe: (T) -> PlayerTrackPreference,
    ) {
        if (group == null || preference == null) return
        if (preference.disabled) {
            withTimeoutOrNull(5_000) { group.candidates.first { it.isNotEmpty() } }
            withContext(player.mainDispatcher) { if (unchanged()) group.select(null) }
            return
        }
        val candidate = withTimeoutOrNull(5_000) {
            group.candidates.first { tracks -> tracks.any { preference.matchScore(describe(it)) > 0 } }
        }?.maxByOrNull { preference.matchScore(describe(it)) } ?: return
        withContext(player.mainDispatcher) { if (unchanged()) group.select(candidate) }
    }
}

private fun describeAudio(track: AudioTrack): PlayerTrackPreference =
    PlayerTrackPreference.describe(name = track.name, labels = track.labels.map { it.value })

private fun describeSubtitle(track: SubtitleTrack): PlayerTrackPreference =
    PlayerTrackPreference.describe(language = track.language, labels = track.labels.map { it.value })
