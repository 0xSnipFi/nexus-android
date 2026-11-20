package app.nexus.discovery
import android.content.Context
import app.nexus.domain.FederatedSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
internal data class CataloguePage(val rows:List<List<String>>, val status:List<String>)
internal suspend fun catalogue(context:Context,input:String):CataloguePage {
    val raw=input.substringBefore('\n')
    val parts=input.lines()
    val category=parts.getOrElse(1){"All"}
    val options=parts.getOrNull(2).orEmpty().split('|')
    fun accepted(row:List<String>):Boolean {
        val type=options.getOrNull(0)
        val year=options.getOrNull(1)
        return (type.isNullOrEmpty() || type=="Type" || row[2].endsWith(type.uppercase())) &&
            (year==null || year=="Year" || (year=="Older" && (row[1].toIntOrNull() ?: 9999)<2023) || row[1]==year)
    }
    val language=Regex("""(?i)(hindi|hin|english|eng|bengali|bangla|বাংলা|হিন্দি|हिंदी)\s+(dub(?:bed)?|audio|sub(?:titles?)?)\b""")
    val title=language.replace(raw,"").trim()
    val matcher=app.nexus.domain.TitleMatcher(title)




        val outcomes=withContext(Dispatchers.IO) {
            val files=when(category) {
                "Cartoons" -> listOf("films.tsv")
                "Anime","Series" -> listOf("catalog.tsv")
                else -> listOf("films.tsv","catalog.tsv")
            }
            val adapters=files.map { file -> object: FederatedSearch.Adapter<List<String>> {
                override fun name()=if(file=="films.tsv") "Movie catalogue" else "Anime catalogue"
                override fun enabled()=true
                override fun search(query:String):List<List<String>> = context.assets.open(file).bufferedReader().useLines { lines ->
                lines.map { it.split('\t') }.filter { row ->
                    row.size >= 4 && accepted(row) && title.isNotBlank() && (category!="Movies" || row[2].endsWith("MOVIE")) &&
                    (category!="Series" || row[2]=="TV") && (category!="Cartoons" || row[2] in setOf("CARTOON","ANIMATED MOVIE")) &&
                    (matcher.matches(row[0]) || row.getOrNull(4).orEmpty().split('|').any(matcher::matches))
                }.sortedByDescending { it[0].equals(title,true) || it.getOrNull(4).orEmpty().split('|').any { alias -> alias.equals(title,true) } }.take(80).toList()
            } } }
            val remote=when(category) {
                "Cartoons" -> emptyList()
                "Movies" -> listOf(AnimeLokListing.MOVIES)
                "Series" -> listOf(AnimeLokListing.SERIES)
                else -> AnimeLokListing.values().toList()
            }
            FederatedSearch.search(title,adapters+remote,15000)
        }
        val sourceStates=outcomes.map { (if(it.source=="MOVIES" || it.source=="SERIES") "AnimeLok "+it.source.lowercase() else it.source)+" · "+it.status+" · "+it.hits.size }
        val matches=outcomes.sortedByDescending { it.source=="MOVIES" || it.source=="SERIES" }.flatMap { it.hits }.sortedByDescending { row -> row[0].equals(title,true) || row.getOrNull(4).orEmpty().split('|').any { it.equals(title,true) } }.take(80)
    val filtered=matches.filter(::accepted)
    val ordered=when(options.getOrNull(2)){
        "Newest" -> filtered.sortedByDescending { it[1].toIntOrNull() ?: 0 }
        "A – Z" -> filtered.sortedBy { it[0].lowercase() }
        else -> filtered
    }
    return CataloguePage(ordered,sourceStates)
}
private fun isSolo(row:List<String>) = row[0]=="Ore dake Level Up na Ken" && row[1]=="2024"

internal fun validSource(link:String):Boolean = runCatching {
    val uri=java.net.URI(link)
    uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.rawUserInfo==null && uri.port in listOf(-1,443)
}.getOrDefault(false)

internal fun resultLinks(row:List<String>):List<String> = buildList {
    add(row[3])
    addAll(row.getOrNull(5).orEmpty().split('|'))
    if(isSolo(row)){
        add("https://www.crunchyroll.com/series/GDKHZEJ0K/solo-leveling")
        add("https://www.crunchyroll.com/news/announcements/2023/12/13/solo-leveling-anime-hindi-dub-cast-ali-fazal")
    }
}.filter(::validSource).distinct()

internal fun copyLinks(context:android.content.Context, links:List<String>){
    context.getSystemService(android.content.ClipboardManager::class.java)
        .setPrimaryClip(android.content.ClipData.newPlainText("Nexus source links",links.joinToString("\n")))
    if(android.os.Build.VERSION.SDK_INT<33) android.widget.Toast.makeText(context,"Copied "+links.size+" source link(s)",android.widget.Toast.LENGTH_SHORT).show()
}


