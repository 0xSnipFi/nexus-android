package app.nexus.domain

import org.junit.Assert.*
import org.junit.Test

class DiscoveryTest {
    private val titles = listOf(
        MediaTitle("1", "Spirited Away", 2001, MediaType.ANIME, listOf("Sen to Chihiro")),
        MediaTitle("2", "The General", 1926, MediaType.MOVIE, audio = setOf("English")))

    @Test fun titleOnlyAndAliasSearch() {
        assertEquals("1", Discovery.search("sen to chihiro", titles).single().id)
        assertEquals("1", Discovery.search("Spirited", titles).single().id)
    }
    @Test fun unicodeAndWhitespaceNormalization() {
        assertEquals("nexus বাংলা", Discovery.normalize(" ＮＥＸＵＳ  বাংলা! "))
    }
    @Test fun typoTolerance() { assertEquals("1", Discovery.search("Spirted Away", titles).single().id) }
    @Test fun unknownAudioDoesNotPassFilter() {
        assertTrue(Discovery.search("Spirited", titles, SearchFilters(audio = "English")).isEmpty())
    }
    @Test fun filtersAreIndependent() {
        assertEquals(1, Discovery.search("General", titles, SearchFilters(year = 1926)).size)
        assertTrue(Discovery.search("General", titles, SearchFilters(type = MediaType.ANIME)).isEmpty())
    }
    @Test fun emptyQueryIsNotTrending() { assertTrue(Discovery.search(" ", titles).isEmpty()) }
    @Test fun unsafeNavigationIsRejected() {
        listOf("javascript:alert(1)", "file:///secret", "content://data", "intent://x",
            "https://trusted.com@evil.com", "https:///missing", "https://example.com:99999",
            "https://example.com\\@evil.com").forEach { assertFalse(it, NavigationPolicy.isWebUrl(it)) }
        assertTrue(NavigationPolicy.isWebUrl("https://example.com/title?q=movie"))
    }
    @Test fun downloadRequiresAllConditions() {
        for (permission in listOf(false, true)) for (supported in listOf(false, true))
            for (restricted in listOf(false, true)) assertEquals(permission && supported && !restricted,
                DownloadPolicy.canDownload(permission, supported, restricted))
    }
}
