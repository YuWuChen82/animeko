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

/** Track IDs are file-local. Preferences describe content rather than a numeric track position. */
@Serializable
data class PlayerTrackPreference(
    val disabled: Boolean = false,
    val language: String? = null,
    val name: String? = null,
    val labels: Set<String> = emptySet(),
) {
    fun matchScore(candidate: PlayerTrackPreference): Int {
        if (disabled) return 0
        if (language != null && candidate.language != language) return 0
        val sameName = name != null && name == candidate.name
        val sharedLabels = labels.intersect(candidate.labels).size
        if (language == null && !sameName && sharedLabels == 0) return 0
        return (if (language != null) 100 else 0) + (if (sameName) 20 else 0) + sharedLabels
    }

    companion object {
        fun describe(language: String? = null, name: String? = null, labels: List<String>): PlayerTrackPreference {
            val normalizedName = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            val normalizedLabels = labels.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            val text = (listOfNotNull(language, normalizedName) + normalizedLabels).joinToString(" ").lowercase()
            val normalizedLanguage = when {
                listOf("hans", "simplified", "简体", "简中", "簡體", "chs", "zh-cn", "zh_cn").any { it in text } -> "zh-hans"
                listOf("hant", "traditional", "繁体", "繁體", "繁中", "cht", "zh-tw", "zh_tw").any { it in text } -> "zh-hant"
                Regex("\\b(ja|jpn|japanese)\\b|日语|日語").containsMatchIn(text) -> "ja"
                Regex("\\b(en|eng|english)\\b|英语|英語").containsMatchIn(text) -> "en"
                Regex("\\b(zh|zho|chi|chinese)\\b|中文|汉语|漢語").containsMatchIn(text) -> "zh"
                else -> language?.trim()?.lowercase()?.takeUnless { it.isEmpty() || it == "und" }
            }
            return PlayerTrackPreference(language = normalizedLanguage, name = normalizedName, labels = normalizedLabels)
        }
    }
}

@Serializable
data class SubjectTrackPreferences(
    val audio: PlayerTrackPreference? = null,
    val subtitle: PlayerTrackPreference? = null,
)
