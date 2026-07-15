package app.linkclear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.linkclear.core.BundledRules
import app.linkclear.core.CleaningEngine
import app.linkclear.ui.EditorScreen
import org.junit.Rule
import org.junit.Test

/**
 * The editor pre-fills from [EditorScreen]'s initialText (which MainActivity feeds
 * from a URL found on the clipboard at launch) and shows the cleaned diff with no
 * user interaction. Empty initialText leaves the field blank.
 */
class EditorAutoFillTest {
    @get:Rule val rule = createComposeRule()

    private val engine = CleaningEngine(BundledRules.load())

    @Test fun initialTextPrefillsFieldAndShowsCleanedDiff() {
        rule.setContent {
            EditorScreen(
                engine = engine,
                clipboardText = { null },
                onCopy = {},
                resolveClean = { emptyList() },
                initialText = "https://ex.com/p?utm_source=nl",
            )
        }
        // Field shows the pre-filled link, and the live diff renders its cleaned form.
        rule.onNodeWithText("https://ex.com/p?utm_source=nl").assertIsDisplayed()
        rule.onNodeWithText("https://ex.com/p").assertIsDisplayed()
        rule.onNodeWithText("1 tracker removed").assertIsDisplayed()
    }

    @Test fun emptyInitialTextLeavesFieldBlank() {
        rule.setContent {
            EditorScreen(
                engine = engine,
                clipboardText = { null },
                onCopy = {},
                resolveClean = { emptyList() },
                initialText = "",
            )
        }
        // No result block: the "Copy Clean" action only appears once a link is present.
        rule.onNodeWithText("Copy Clean").assertDoesNotExist()
        // The placeholder label is shown (field empty).
        rule.onNodeWithText("Paste a link").assertIsDisplayed()
    }
}
