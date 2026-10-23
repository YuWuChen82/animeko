/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.him188.ani.app.data.models.preference.VideoScaffoldConfig
import me.him188.ani.app.data.repository.user.Settings
import org.openani.mediamp.InternalMediampApi
import org.openani.mediamp.InternalForInheritanceMediampApi
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.features.MediaMetadata
import org.openani.mediamp.features.buildPlayerFeatures
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.Chapter
import org.openani.mediamp.metadata.SubtitleTrack
import org.openani.mediamp.metadata.TrackGroup
import org.openani.mediamp.source.UriMediaData
import org.openani.mediamp.test.TestMediampPlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(InternalMediampApi::class, InternalForInheritanceMediampApi::class)
class PlayerTrackMemoryTest {
    private class MemorySettings : Settings<VideoScaffoldConfig> {
        override val flow = MutableStateFlow(VideoScaffoldConfig.Default)
        override suspend fun set(value: VideoScaffoldConfig) { flow.value = value }
    }

    private class Tracks<T> : TrackGroup<T> {
        override val selected = MutableStateFlow<T?>(null)
        override val candidates = MutableStateFlow<List<T>>(emptyList())
        override fun select(track: T?): Boolean {
            if (track != null && track !in candidates.value) return false
            selected.value = track
            return true
        }
    }

    private class Fixture(scope: TestScope, settings: MemorySettings, subject: Int = 1) {
        val audio = Tracks<AudioTrack>()
        val subtitles = Tracks<SubtitleTrack>()
        val base = TestMediampPlayer(StandardTestDispatcher(scope.testScheduler))
        private val player = object : MediampPlayer by base {
            override val features = buildPlayerFeatures {
                add(MediaMetadata, object : MediaMetadata {
                    override val audioTracks = audio
                    override val subtitleTracks = subtitles
                    override val chapters: Flow<List<Chapter>> = MutableStateFlow(emptyList())
                })
            }
        }
        val memory = PlayerTrackMemory(subject, player, settings, scope.backgroundScope)
    }

    @Test
    fun `reopening another episode restores the language with a new track ID`() = runTest {
        val settings = MemorySettings()
        val first = Fixture(this, settings)
        val chosen = AudioTrack("1", "1", "Japanese", emptyList())
        first.audio.candidates.value = listOf(chosen)
        first.memory.selectAudio(chosen)
        val next = Fixture(this, settings)
        val english = AudioTrack("1", "1", "English", emptyList())
        val japanese = AudioTrack("9", "9", "jpn", emptyList())
        next.audio.candidates.value = listOf(english, japanese)
        next.audio.selected.value = english
        runCurrent()
        next.base.setMediaData(UriMediaData("file:///episode02.mkv"), playWhenReady = false)
        runCurrent()
        assertEquals(japanese, next.audio.selected.value)
        first.base.close()
        next.base.close()
    }

    @Test
    fun `subtitle off survives reopening but does not affect another subject`() = runTest {
        val settings = MemorySettings()
        val first = Fixture(this, settings)
        first.memory.selectSubtitle(null)
        first.base.close()
        val next = Fixture(this, settings)
        val subtitle = SubtitleTrack("2", "2", "zh-Hans", emptyList())
        next.subtitles.candidates.value = listOf(subtitle)
        next.subtitles.selected.value = subtitle
        val other = Fixture(this, settings, subject = 2)
        other.subtitles.candidates.value = listOf(subtitle)
        other.subtitles.selected.value = subtitle
        runCurrent()
        next.base.setMediaData(UriMediaData("file:///episode02.mkv"), playWhenReady = false)
        other.base.setMediaData(UriMediaData("file:///other.mkv"), playWhenReady = false)
        runCurrent()
        assertNull(next.subtitles.selected.value)
        assertEquals(subtitle, other.subtitles.selected.value)
        next.base.close()
        other.base.close()
    }
}
