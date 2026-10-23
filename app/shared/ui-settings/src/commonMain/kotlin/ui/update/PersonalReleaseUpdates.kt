/*
 * Copyright (C) 2024-2026 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.him188.ani.utils.platform.Arch
import me.him188.ani.utils.platform.Platform
import me.him188.ani.utils.platform.currentPlatform

/** The personal Windows channel contains only complete, tested releases from this fork. */
object PersonalReleaseUpdates {
    const val REPOSITORY = "YuWuChen82/animeko"
    const val RELEASES_URL = "https://github.com/$REPOSITORY/releases"
    const val PACKAGE_NAME = "Animeko-personal-windows-x64.zip"
    private const val CHECK_TIMEOUT_MILLIS = 15_000L

    private val versionPattern = Regex("""^\d+\.\d+\.\d+-personal\.(\d+)\.(\d+)$""")
    private val json = Json { ignoreUnknownKeys = true }

    internal suspend fun check(
        client: HttpClient,
        currentVersion: String,
        platform: Platform = currentPlatform(),
    ): NewVersion? {
        if (platform !is Platform.Windows || platform.arch != Arch.X86_64) return null
        val response = client.get("https://api.github.com/repos/$REPOSITORY/releases/latest") {
            expectSuccess = false
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            timeout { requestTimeoutMillis = CHECK_TIMEOUT_MILLIS }
        }
        if (response.status == HttpStatusCode.NotFound) return null
        check(response.status.isSuccess()) { "Personal release check failed: ${response.status}" }
        val release = json.decodeFromString(Release.serializer(), response.bodyAsText())
        if (release.draft || release.prerelease) return null
        val version = release.tag.removePrefix("v")
        val latest = buildNumber(version) ?: return null
        val installed = buildNumber(currentVersion.removePrefix("v"))
        if (installed != null && (latest.first < installed.first ||
                    (latest.first == installed.first && latest.second <= installed.second))) return null

        val baseUrl = "$RELEASES_URL/download/${release.tag}/"
        // A release is usable only after the package and both verification sidecars have uploaded.
        val required = listOf(PACKAGE_NAME, "$PACKAGE_NAME.sha1", "$PACKAGE_NAME.manifest.json")
        if (required.any { name -> release.assets.none {
                it.name == name && it.state == "uploaded" && it.size > 0 && it.url == baseUrl + name
            } }) return null
        val publishedAt = release.publishedAt.substringBefore('T')
        return NewVersion(
            name = version,
            changelogs = listOf(Changelog(version, publishedAt, release.body.orEmpty())),
            downloadUrlAlternatives = listOf(baseUrl + PACKAGE_NAME),
            publishedAt = publishedAt,
        )
    }

    private fun buildNumber(version: String): Pair<Long, Long>? {
        val match = versionPattern.matchEntire(version) ?: return null
        val run = match.groupValues[1].toLongOrNull() ?: return null
        val attempt = match.groupValues[2].toLongOrNull() ?: return null
        return run to attempt
    }

    @Serializable
    private class Release(
        @SerialName("tag_name") val tag: String,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val body: String? = null,
        @SerialName("published_at") val publishedAt: String = "",
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private class Asset(
        val name: String,
        @SerialName("browser_download_url") val url: String,
        val state: String = "",
        val size: Long = 0,
    )
}
