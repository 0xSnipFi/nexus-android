package app.nexus.discovery

import app.nexus.domain.FederatedSearch
import app.nexus.domain.TitleMatcher
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Reads public search forms and result cards. Each connector is confined to its submitted host. */
internal class WebsiteSearch(private val home: String, private val category: String) : FederatedSearch.Adapter<List<String>> {
    val host = URI(home).host.lowercase()
    @Volatile var searchPage = home
        private set

    override fun name() = host
    override fun enabled() = true

    private fun local(url: String) = validSource(url) &&
        URI(url).host.removePrefix("www.").equals(host.removePrefix("www."), true)

    private fun read(address: String): Document {
        var current = address
        repeat(4) {
            val response = Jsoup.connect(current).followRedirects(false).ignoreHttpErrors(true)
                .timeout(4500).maxBodySize(1_500_000).execute()
            if (response.statusCode() in 300..399) {
                val next = URI(current).resolve(response.header("Location") ?: "").toString()
                if (!local(next) || next == current) throw FederatedSearch.SourceException("Site redirected to another host")
                current = next
            } else {
                if (response.statusCode() !in 200..299) throw FederatedSearch.SourceException("Site returned HTTP " + response.statusCode())
                val doc = response.parse()
                if (doc.title().contains("Just a moment", true)) throw FederatedSearch.SourceException("Manual verification required")
                return doc
            }
        }
        throw FederatedSearch.SourceException("Too many redirects")
    }

    private fun discover(): Pair<String, String> {
        val page = read(home)
        for (form in page.select("form")) {
            if (form.attr("method").equals("post", true)) continue
            val field = form.select("input[name]").firstOrNull {
                it.attr("name") in setOf("s", "q", "keyword", "query", "search", "story") &&
                    it.attr("type").lowercase() in setOf("", "text", "search")
            } ?: continue
            val action = form.absUrl("action").ifEmpty { page.location() }
            if (local(action)) return action to field.attr("name")
        }
        throw FederatedSearch.SourceException("Search in the original site")
    }

    override fun search(query: String): List<List<String>> {
        if (query.isBlank() || query.length > 200) return emptyList()
        val route = when (host) {
            "new15.hdhub4u.cl" -> "https://$host/search.html" to "q"
            "hdboss.in", "hdhub4you.xyz", "animejoker.com", "watchanimeworld.one", "hindianimeszone.com",
            "anime4u.site", "animedekho.lat", "moviesmod.fi", "hindianime.org" -> "https://$host/" to "s"
            else -> routes[home] ?: discover().also { routes[home] = it }
        }
        searchPage = route.first + (if ("?" in route.first) "&" else "?") + route.second + "=" + URLEncoder.encode(query, "UTF-8")
        val doc = read(searchPage)
        doc.select("aside, header nav, footer, script, nav, .sidebar").remove()
        val matcher = TitleMatcher(query)
        val results = linkedMapOf<String, List<String>>()
        val scope = doc.selectFirst("main, #content, .main-content") ?: doc.body()
        val cards = scope.select("article, .hd-card-item, .film-poster, .flw-item, .entry-title, .post-title, .poster, .result-item, .movie-card, .bsx, .item .name")
        for (card in cards) {
            val anchor = if (card.tagName() == "a") card else card.selectFirst("h2 a[href], h3 a[href], a.lnk-blk[href], a[href]") ?: continue
            val url = anchor.absUrl("href")
            if (!local(url) || URI(url).path.isNullOrBlank() || URI(url).path == "/" || url == searchPage) continue
            if (url in results) continue
            val img = card.selectFirst("img")
            val label = listOf(card.selectFirst("h2,h3,.entry-title,.film-name")?.text(), img?.attr("alt"), anchor.attr("title"), anchor.text())
                .filterNotNull().firstOrNull { matcher.matches(it) }?.take(350) ?: continue
            val year = yearPattern.find(label)
            val title = label.substring(0, year?.range?.first ?: label.length).trim(' ', '-', '(', '[').removePrefix("Download ").trim().ifEmpty { label }
            val type = when {
                tv.containsMatchIn("$url $label") -> "TV"
                movie.containsMatchIn("$url $label") || year != null -> "MOVIE"
                else -> "Title"
            }
            if (category == "Movies" && type == "TV" || category == "Series" && type == "MOVIE") continue
            val poster = img?.let { it.absUrl("data-src").ifEmpty { it.absUrl("src") } }.orEmpty()
            results[url] = listOf(title, year?.groupValues?.get(1).orEmpty(), type, url, "", "", poster, label)
            if (results.size == 40) break
        }
        if (cards.isEmpty() && !emptyPage.containsMatchIn(scope.text())) throw FederatedSearch.SourceException("Open site for interactive results")
        return results.values.toList()
    }

    companion object {
        private val routes = ConcurrentHashMap<String, Pair<String, String>>()
        private val yearPattern = Regex("""[\[(]((?:19|20)\d{2})[)\]]""")
        private val tv = Regex("""(?i)\b(season|series|episodes?|TV)\b""")
        private val movie = Regex("""(?i)\b(movies?|films?)\b""")
        private val emptyPage = Regex("""(?i)no (?:results|matches|posts)|nothing found|couldn.t find""")
    }
}
