/*
 * Copyright (C) 2026 OpenAni and contributors.
 * Use of this source code is governed by the GNU AGPLv3 license.
 */
package me.him188.ani.app.ui.update

import me.him188.ani.app.ui.settings.tabs.AniHelperDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseNotesUrlTest {
    @Test
    fun `personal releases open the matching fork tag`() {
        assertEquals(
            "https://github.com/YuWuChen82/animeko/releases/tag/v6.3.0-personal.30.1",
            AniHelperDestination.releaseNotesUrl("6.3.0-personal.30.1"),
        )
    }

    @Test
    fun `official releases retain their upstream destination`() {
        for (version in listOf("6.3.0", "6.4.0-beta02")) {
            assertEquals(
                "https://github.com/open-ani/animeko/releases/tag/v$version",
                AniHelperDestination.releaseNotesUrl(version),
            )
        }
    }

    @Test
    fun `a tag prefix is included only once`() {
        assertEquals(
            AniHelperDestination.releaseNotesUrl("6.3.0-personal.30.1"),
            AniHelperDestination.releaseNotesUrl("v6.3.0-personal.30.1"),
        )
    }
}
