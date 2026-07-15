package app.linkclear

import app.linkclear.settings.ResolverMode
import app.linkclear.unshorten.DirectHeadResolver
import app.linkclear.unshorten.NoopResolver
import app.linkclear.unshorten.RemoteResolver
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnshortenGateTest {
    @Test fun `OFF maps to NoopResolver`() {
        assertSame(NoopResolver, UnshortenGate.resolverFor(ResolverMode.OFF, ""))
    }

    @Test fun `DIRECT maps to DirectHeadResolver`() {
        assertTrue(UnshortenGate.resolverFor(ResolverMode.DIRECT, "") is DirectHeadResolver)
    }

    @Test fun `CUSTOM with https maps to RemoteResolver`() {
        val r = UnshortenGate.resolverFor(ResolverMode.CUSTOM, "https://resolver.example/r?url=")
        assertTrue(r is RemoteResolver)
    }

    @Test fun `CUSTOM with non-https falls back to NoopResolver`() {
        // Security-relevant: a plaintext custom URL must never be used.
        assertSame(NoopResolver, UnshortenGate.resolverFor(ResolverMode.CUSTOM, "http://insecure.example/r?url="))
    }

    @Test fun `CUSTOM with blank url falls back to NoopResolver`() {
        assertSame(NoopResolver, UnshortenGate.resolverFor(ResolverMode.CUSTOM, ""))
    }
}
