/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.models.preference

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlayerPersonalizationTest {
    @Test
    fun `danmaku and speed bindings swap without duplicate commands`() {
        val config = PlayerShortcutConfig.Default.bind(PlayerShortcutAction.TOGGLE_DANMAKU, "D", swap = true)
        assertEquals("D", config.key(PlayerShortcutAction.TOGGLE_DANMAKU))
        assertEquals("B", config.key(PlayerShortcutAction.SPEED_UP))
        val keys = PlayerShortcutAction.entries.map(config::key).filter { it.isNotEmpty() }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `a conflicting binding requires confirmation and an action can be disabled`() {
        assertFailsWith<IllegalArgumentException> {
            PlayerShortcutConfig.Default.bind(PlayerShortcutAction.TOGGLE_DANMAKU, "D")
        }
        assertEquals("", PlayerShortcutConfig.Default.bind(PlayerShortcutAction.TOGGLE_DANMAKU, "")
            .key(PlayerShortcutAction.TOGGLE_DANMAKU))
    }

    @Test
    fun `custom bindings and disabled subtitles survive a settings round trip`() {
        val original = VideoScaffoldConfig.Default.copy(
            keyboardShortcuts = PlayerShortcutConfig.Default.bind(PlayerShortcutAction.TOGGLE_DANMAKU, "D", swap = true),
            trackPreferences = mapOf("513878" to SubjectTrackPreferences(subtitle = PlayerTrackPreference(disabled = true))),
        )
        val restored = Json.decodeFromString(VideoScaffoldConfig.serializer(), Json.encodeToString(VideoScaffoldConfig.serializer(), original))
        assertEquals(original, restored)
    }

    @Test
    fun `audio preferences match a language when file local track IDs change`() {
        val saved = PlayerTrackPreference.describe(name = "Japanese", labels = listOf("Original"))
        val next = PlayerTrackPreference.describe(name = "jpn", labels = listOf("5.1"))
        val english = PlayerTrackPreference.describe(name = "English", labels = listOf("Original"))
        assertTrue(saved.matchScore(next) > 0)
        assertEquals(0, saved.matchScore(english))
    }

    @Test
    fun `simplified and traditional subtitles are distinct preferences`() {
        val simplified = PlayerTrackPreference.describe(language = "Chinese (Simplified)", labels = emptyList())
        val traditional = PlayerTrackPreference.describe(language = "Chinese (Traditional)", labels = emptyList())
        assertEquals("zh-hans", simplified.language)
        assertEquals("zh-hant", traditional.language)
        assertEquals(0, simplified.matchScore(traditional))
        assertTrue(simplified.matchScore(simplified) > 0)
    }
}
