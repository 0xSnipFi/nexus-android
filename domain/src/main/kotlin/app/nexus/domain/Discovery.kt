package app.nexus.domain

import java.net.URI
import java.text.Normalizer
import java.util.Locale

enum class MediaType { ANIME, MOVIE, SERIES, CARTOON }
enum class Availability { NOT_CHECKED, METADATA_ONLY, WATCH_LINK, AUTHORIZED_DOWNLOAD }
data class MediaTitle(val id: String, val title: String, val year: Int, val type: MediaType,
    val aliases: List<String> = emptyList(), val audio: Set<String> = emptySet(),
    val availability: Availability = Availability.METADATA_ONLY)
data class SearchFilters(val type: MediaType? = null, val year: Int? = null, val audio: String? = null)

object Discovery {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
        .trim().replace(Regex("\\s+"), " ")

    fun search(query: String, items: List<MediaTitle>, filters: SearchFilters = SearchFilters()): List<MediaTitle> {
        val needle = normalize(query)
        if (needle.isBlank()) return emptyList()
        return items.asSequence().filter {
            (filters.type == null || it.type == filters.type) &&
                (filters.year == null || it.year == filters.year) &&
                (filters.audio == null || it.audio.any { language -> normalize(language) == normalize(filters.audio) })
        }.map { item ->
            item to (listOf(item.title) + item.aliases).maxOf { title ->
                val candidate = normalize(title)
                when {
                    candidate == needle -> 100
                    candidate.startsWith(needle) -> 90
                    candidate.contains(needle) -> 80
                    needle.length >= 4 && distance(needle, candidate) <= 2 -> 60
                    else -> 0
                }
            }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.toList()
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1,
                previous[j] + if (a[i] == b[j]) 0 else 1)
            previous = current
        }
        return previous[b.length]
    }
}

object NavigationPolicy {
    fun isWebUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme?.lowercase(Locale.ROOT) in setOf("https", "http") &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port in 1..65535) && !value.contains('\\')
    }.getOrDefault(false)
}

object DownloadPolicy {
    fun canDownload(permissionEstablished: Boolean, technicallySupported: Boolean,
        accessRestricted: Boolean): Boolean = permissionEstablished && technicallySupported && !accessRestricted
}
