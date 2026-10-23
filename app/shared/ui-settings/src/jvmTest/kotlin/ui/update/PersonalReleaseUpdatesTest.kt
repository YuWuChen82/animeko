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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import me.him188.ani.utils.platform.Arch
import me.him188.ani.utils.platform.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PersonalReleaseUpdatesTest {
    private val windows = Platform.Windows(Arch.X86_64)
    private val version = "6.3.0-personal.42.1"
    private val baseUrl = "${PersonalReleaseUpdates.RELEASES_URL}/download/v$version/"

    private fun release(
        releaseVersion: String = version,
        missingSidecar: Boolean = false,
        foreignAsset: Boolean = false,
        prerelease: Boolean = false,
    ): String {
        val names = listOfNotNull(
            PersonalReleaseUpdates.PACKAGE_NAME,
            "${PersonalReleaseUpdates.PACKAGE_NAME}.sha1",
            "${PersonalReleaseUpdates.PACKAGE_NAME}.manifest.json".takeUnless { missingSidecar },
        )
        val root = if (foreignAsset) "https://github.com/open-ani/animeko/releases/download/v$releaseVersion/"
        else "${PersonalReleaseUpdates.RELEASES_URL}/download/v$releaseVersion/"
        val assets = names.joinToString(",") {
            """{"name":"$it","browser_download_url":"$root$it","size":100,"state":"uploaded"}"""
        }
        return """{"tag_name":"v$releaseVersion","draft":false,"prerelease":$prerelease,
            "published_at":"2026-10-23T12:00:00Z","body":"Personal features","assets":[$assets]}"""
    }

    private suspend fun check(
        current: String,
        body: String = release(),
        platform: Platform = windows,
    ): NewVersion? = HttpClient(MockEngine {
        assertEquals("https://api.github.com/repos/YuWuChen82/animeko/releases/latest", it.url.toString())
        assertEquals("application/vnd.github+json", it.headers[HttpHeaders.Accept])
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }).use { PersonalReleaseUpdates.check(it, current, platform) }

    @Test
    fun `a newer personal build is downloaded exclusively from the fork`() = runTest {
        val latest = check("6.3.0-personal.41.1")!!
        assertEquals(version, latest.name)
        assertEquals(listOf(baseUrl + PersonalReleaseUpdates.PACKAGE_NAME), latest.downloadUrlAlternatives)
        assertEquals("Personal features", latest.changelogs.single().changes)
    }

    @Test
    fun `the installed version and older releases do not trigger update loops`() = runTest {
        assertNull(check(version))
        assertNull(check("6.3.0-personal.43.1"))
        assertNull(check("6.3.0-personal.42.2"))
        assertEquals(version, check("6.3.0-personal.42.0")?.name)
    }

    @Test
    fun `official tags incomplete uploads and foreign download URLs are ignored`() = runTest {
        assertNull(check("6.3.0", release(releaseVersion = "6.4.0")))
        assertNull(check("6.3.0", release(missingSidecar = true)))
        assertNull(check("6.3.0", release(foreignAsset = true)))
        assertNull(check("6.3.0", release(prerelease = true)))
        assertEquals(version, check("6.3.0")?.name)
    }

    @Test
    fun `other platforms do not receive a Windows package`() = runTest {
        assertNull(check("6.3.0", platform = Platform.Windows(Arch.AARCH64)))
        assertNull(check("6.3.0", platform = Platform.MacOS(Arch.X86_64)))
    }

    @Test
    fun `a new fork without releases is up to date and API errors remain visible`() = runTest {
        var requests = 0
        HttpClient(MockEngine {
            requests++
            respond("{}", HttpStatusCode.NotFound)
        }) { expectSuccess = true }.use {
            assertNull(PersonalReleaseUpdates.check(it, "6.3.0", windows))
        }
        HttpClient(MockEngine {
            requests++
            respond("{}", HttpStatusCode.Forbidden)
        }).use {
            assertFailsWith<IllegalStateException> { PersonalReleaseUpdates.check(it, "6.3.0", windows) }
        }
        assertEquals(2, requests)
    }
}
