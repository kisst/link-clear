package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CleaningEngineTest {
    // Minimal ruleset exercising rules, referralMarketing, rawRules, redirections, exceptions.
    private val ruleSet =
        RuleLoader.parse(
            """
            {"providers":{
              "Global":{
                "urlPattern":".*",
                "completeProvider":false,
                "rules":["utm_source","utm_campaign","utm_medium","fbclid","gclid","igshid"]
              },
              "Referral":{
                "urlPattern":"^https?://shop\\.com",
                "completeProvider":false,
                "referralMarketing":["ref"]
              },
              "Raw":{
                "urlPattern":"^https?://raw\\.com",
                "completeProvider":false,
                "rawRules":["/track/[^/?]*"]
              },
              "Redir":{
                "urlPattern":"^https?://out\\.com/go",
                "completeProvider":false,
                "redirections":["^https?://out\\.com/go\\?u=([^&]+)"]
              },
              "Except":{
                "urlPattern":"^https?://safe\\.com",
                "completeProvider":false,
                "rules":["keep"],
                "exceptions":["^https?://safe\\.com/admin"]
              },
              "Block":{
                "urlPattern":"^https?://tracker\\.com",
                "completeProvider":true,
                "exceptions":["^https?://tracker\\.com/ok"]
              }
            }}
            """.trimIndent(),
        )
    private val engine = CleaningEngine(ruleSet)

    private data class Case(val name: String, val dirty: String, val clean: String)

    private val cases =
        listOf(
            Case("utm stripped", "https://a.com/p?utm_source=nl&id=7", "https://a.com/p?id=7"),
            Case("fbclid stripped", "https://a.com/p?fbclid=abc", "https://a.com/p"),
            Case(
                "multiple trackers",
                "https://a.com/p?utm_source=x&utm_campaign=y&keep=1",
                "https://a.com/p?keep=1",
            ),
            Case("already clean unchanged", "https://a.com/p?id=7", "https://a.com/p?id=7"),
            Case(
                "referral marketing stripped",
                "https://shop.com/p?ref=xyz&id=1",
                "https://shop.com/p?id=1",
            ),
            Case("raw rule strips path", "https://raw.com/track/abc/item", "https://raw.com/item"),
            Case(
                "redirection followed then cleaned",
                "https://out.com/go?u=https%3A%2F%2Fb.com%2Fx%3Futm_source%3Dz",
                "https://b.com/x",
            ),
        )

    @Test fun `table of dirty to clean`() {
        for (c in cases) {
            val r = engine.cleanUrl(c.dirty)
            assertEquals(c.clean, r.cleaned, "case: ${c.name}")
        }
    }

    @Test fun `records removed params`() {
        val r = engine.cleanUrl("https://a.com/p?utm_source=nl&id=7")
        assertEquals(listOf(RemovedParam("utm_source", "nl")), r.removed)
        assertTrue(r.wasChanged)
    }

    @Test fun `completeProvider match flags blocked`() {
        val r = engine.cleanUrl("https://tracker.com/path?utm_source=x")
        assertTrue(r.blocked, "completeProvider host should be flagged blocked")
    }

    @Test fun `completeProvider exception is not blocked`() {
        val r = engine.cleanUrl("https://tracker.com/ok?utm_source=x")
        assertFalse(r.blocked, "exception path overrides completeProvider block")
    }

    @Test fun `non-matching url is not blocked`() {
        val r = engine.cleanUrl("https://a.com/p?utm_source=x")
        assertFalse(r.blocked)
    }

    @Test fun `exception url is skipped`() {
        val r = engine.cleanUrl("https://safe.com/admin?keep=1")
        assertEquals("https://safe.com/admin?keep=1", r.cleaned)
        assertFalse(r.wasChanged)
    }

    @Test fun `malformed url returned unchanged`() {
        val r = engine.cleanUrl("not a url ((")
        assertEquals("not a url ((", r.cleaned)
        assertFalse(r.wasChanged)
    }

    @Test fun `cleanText handles multiple urls in a message`() {
        val results =
            engine.cleanText(
                "a https://a.com/p?utm_source=x b https://a.com/q?fbclid=z",
            )
        assertEquals(2, results.size)
        assertEquals("https://a.com/p", results[0].cleaned)
        assertEquals("https://a.com/q", results[1].cleaned)
    }

    @Test fun `fragment with query punctuation preserved`() {
        val r = engine.cleanUrl("https://a.com/p?utm_source=nl#frag?weird&more")
        assertEquals("https://a.com/p#frag?weird&more", r.cleaned)
    }

    @Test fun `plain fragment preserved when query fully stripped`() {
        val r = engine.cleanUrl("https://a.com/p?fbclid=x#section")
        assertEquals("https://a.com/p#section", r.cleaned)
    }

    // A rule key must match only the exact query key, never a key of which it is
    // a prefix (or suffix). These lock the `=`-anchored boundary in the matcher.
    @Test fun `rule key does not strip a longer key it prefixes`() {
        val ruleSet =
            RuleLoader.parse(
                """
                {"providers":{"P":{"urlPattern":".*","rules":["id"]}}}
                """.trimIndent(),
            )
        val r = CleaningEngine(ruleSet).cleanUrl("https://a.com/p?id=1&identifier=2&userid=3")
        assertEquals("https://a.com/p?identifier=2&userid=3", r.cleaned)
    }

    @Test fun `rule key does not strip a key it is a suffix of`() {
        val ruleSet =
            RuleLoader.parse(
                """
                {"providers":{"P":{"urlPattern":".*","rules":["ref"]}}}
                """.trimIndent(),
            )
        val r = CleaningEngine(ruleSet).cleanUrl("https://a.com/p?ref=a&referrer=b&preref=c")
        assertEquals("https://a.com/p?referrer=b&preref=c", r.cleaned)
    }

    @Test fun `repeated param records each occurrence`() {
        val r = engine.cleanUrl("https://a.com/p?utm_source=a&utm_source=b&id=1")
        assertEquals("https://a.com/p?id=1", r.cleaned)
        assertEquals(
            listOf(RemovedParam("utm_source", "a"), RemovedParam("utm_source", "b")),
            r.removed,
        )
    }

    @Test fun `construction skips provider with invalid regex instead of throwing`() {
        val ruleSetWithBadProvider =
            RuleLoader.parse(
                """
                {"providers":{
                  "Good":{
                    "urlPattern":"^https?://good\\.com",
                    "completeProvider":false,
                    "rules":["utm_source"]
                  },
                  "Bad":{
                    "urlPattern":"[unclosed",
                    "completeProvider":false,
                    "rules":["utm_source"]
                  }
                }}
                """.trimIndent(),
            )

        val badEngine = assertDoesNotThrow<CleaningEngine> { CleaningEngine(ruleSetWithBadProvider) }

        val r = badEngine.cleanUrl("https://good.com/p?utm_source=nl&id=7")
        assertEquals("https://good.com/p?id=7", r.cleaned)
    }
}
