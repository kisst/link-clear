package app.linkclear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.linkclear.core.CleanResult
import app.linkclear.core.RemovedParam
import app.linkclear.ui.DiffView
import org.junit.Rule
import org.junit.Test

class DiffViewTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsCleanedUrlAndCount() {
        val result =
            CleanResult(
                original = "https://a.com/p?utm_source=x&id=1",
                cleaned = "https://a.com/p?id=1",
                removed = listOf(RemovedParam("utm_source", "x")),
                wasChanged = true,
            )
        rule.setContent { DiffView(result) }
        rule.onNodeWithText("https://a.com/p?id=1").assertIsDisplayed()
        rule.onNodeWithText("1 tracker removed").assertIsDisplayed()
    }
}
