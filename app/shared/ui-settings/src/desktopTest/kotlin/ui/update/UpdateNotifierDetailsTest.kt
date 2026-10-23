/*
 * Copyright (C) 2026 OpenAni and contributors.
 * Use of this source code is governed by the GNU AGPLv3 license.
 */
package me.him188.ani.app.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.framework.runAniComposeUiTest
import me.him188.ani.app.ui.lang.Lang
import me.him188.ani.app.ui.lang.settings_update_popup_see_details
import me.him188.ani.utils.platform.annotations.TestOnly
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(TestOnly::class)
class UpdateNotifierDetailsTest {
    private fun assertDetailsDestination(name: String, expected: String) = runAniComposeUiTest {
        val opened = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) { opened.add(uri) }
        }
        val version = NewVersion(name, emptyList(), emptyList(), "2026-10-24")
        lateinit var detailsText: String
        setContent {
            ProvideCompositionLocalsForPreview {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    detailsText = stringResource(Lang.settings_update_popup_see_details)
                    Box(Modifier.fillMaxSize()) {
                        UpdateNotifier(
                            presentation = AppUpdatePresentation.Placeholder.copy(
                                newVersion = version,
                                state = AppUpdateState.HasUpdate(version),
                                isPlaceholder = false,
                            ),
                            onStartUpdateClick = {},
                            onInstallClick = {},
                            onCancelClick = {},
                            onRetryClick = {},
                            layoutKind = UpdateNotifierLayoutKind.POPUP,
                        )
                    }
                }
            }
        }
        onNodeWithText(detailsText).performClick()
        runOnIdle { assertEquals(listOf(expected), opened) }
    }

    @Test
    fun `personal update popup opens the matching fork release`() = assertDetailsDestination(
        "6.3.0-personal.30.1",
        "https://github.com/YuWuChen82/animeko/releases/tag/v6.3.0-personal.30.1",
    )

    @Test
    fun `official update popup opens the matching upstream release`() = assertDetailsDestination(
        "6.4.0-beta02",
        "https://github.com/open-ani/animeko/releases/tag/v6.4.0-beta02",
    )
}
