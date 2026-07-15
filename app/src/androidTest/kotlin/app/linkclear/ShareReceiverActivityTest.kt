package app.linkclear

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test

class ShareReceiverActivityTest {
    @Test fun launchesWithSharedText() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(ctx, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://a.com/p?utm_source=x")
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { scenario ->
            assertNotNull(scenario)
        }
    }

    @Test fun malformedSendIntentWithNoExtraDoesNotCrash() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(ctx, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                // Deliberately no EXTRA_TEXT.
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { scenario ->
            assertNotNull(scenario)
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test fun launchesWithSharedTextAndCharsetParameter() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(ctx, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain; charset=utf-8"
                putExtra(Intent.EXTRA_TEXT, "https://a.com/p?utm_source=x")
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { scenario ->
            assertNotNull(scenario)
        }
    }

    @Test fun launchesWithSendMultipleSharedTextAndCharsetParameter() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(ctx, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "text/plain; charset=utf-8"
                putCharSequenceArrayListExtra(
                    Intent.EXTRA_TEXT,
                    arrayListOf<CharSequence>("https://a.com/p?utm_source=x", "https://b.com/q?utm_source=y"),
                )
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { scenario ->
            assertNotNull(scenario)
        }
    }

    @Test fun launchesWithSendMultipleSharedText() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(ctx, ShareReceiverActivity::class.java).apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "text/plain"
                putCharSequenceArrayListExtra(
                    Intent.EXTRA_TEXT,
                    arrayListOf<CharSequence>("https://a.com/p?utm_source=x", "https://b.com/q?utm_source=y"),
                )
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { scenario ->
            assertNotNull(scenario)
        }
    }
}
