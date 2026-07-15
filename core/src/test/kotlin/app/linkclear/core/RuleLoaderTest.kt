package app.linkclear.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleLoaderTest {
    private val sample =
        """
        {"providers":{
          "Example":{
            "urlPattern":"^https?://(?:[a-z0-9-]+\\.)*?example\\.com",
            "completeProvider":false,
            "rules":["utm_source","utm_campaign"],
            "rawRules":["/tracking/[^/]*"],
            "referralMarketing":["ref"],
            "exceptions":["^https?://example\\.com/admin/"],
            "redirections":["^https?://example\\.com/goto\\?url=([^&]+)"]
          },
          "Empty":{"urlPattern":".*","completeProvider":false}
        }}
        """.trimIndent()

    @Test fun `parses providers with all fields`() {
        val rs = RuleLoader.parse(sample)
        val p = rs.providers.first { it.name == "Example" }
        assertEquals("^https?://(?:[a-z0-9-]+\\.)*?example\\.com", p.urlPattern)
        assertEquals(listOf("utm_source", "utm_campaign"), p.rules)
        assertEquals(listOf("/tracking/[^/]*"), p.rawRules)
        assertEquals(listOf("ref"), p.referralMarketing)
        assertEquals(1, p.exceptions.size)
        assertEquals(1, p.redirections.size)
    }

    @Test fun `absent optional arrays default to empty`() {
        val rs = RuleLoader.parse(sample)
        val p = rs.providers.first { it.name == "Empty" }
        assertTrue(p.rules.isEmpty())
        assertTrue(p.rawRules.isEmpty())
        assertTrue(p.redirections.isEmpty())
    }
}
