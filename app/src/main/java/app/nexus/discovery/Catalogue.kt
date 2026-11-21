package app.nexus.discovery
import android.content.Context
import app.nexus.domain.FederatedSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
internal data class CataloguePage(val rows:List<List<String>>, val status:List<String>, val sourcePages:Map<String,String>)
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
    val sources=context.assets.open("submitted-sources.tsv").bufferedReader().useLines { lines ->
        lines.map { it.substringBefore('\t') }.distinctBy { java.net.URI(it).host }.toList()
    }
    val hosts=sources.map { java.net.URI(it).host.removePrefix("www.") }.toSet()
    val adapters=if("animelok.cc" !in hosts) emptyList() else when(category){
        "Movies"->listOf(AnimeLokListing.MOVIES)
        "Series"->listOf(AnimeLokListing.SERIES)
        else->AnimeLokListing.values().toList()
    }
    val movies4u=if("movies4u.gp" in hosts && category in setOf("All","Movies","Series"))listOf(Movies4uSearch(category)) else emptyList()
    val otherSites=sources.filterNot { java.net.URI(it).host in setOf("animelok.cc","movies4u.gp") }
        .map { WebsiteSearch(it,category) }.sortedBy { if(it.host in setOf("hdhub4you.xyz","hdboss.in","new15.hdhub4u.cl","animejoker.com","watchanimeworld.one")) 0 else 1 }
    val outcomes=withContext(Dispatchers.IO){ FederatedSearch.search(title,adapters+movies4u+otherSites,20000) }
    val states=outcomes.map { result ->
        val count=result.hits.count(::accepted)
        result.source+" · "+result.status+" · "+count
    }
    val requested=language.find(raw)
    val aliases=when(requested?.groupValues?.get(1)?.lowercase()){
        "hin","hindi","হিন্দি","हिंदी" -> "Hindi|Hin|हिंदी|হিন্দি"
        "eng","english" -> "English|Eng"
        else -> "Bengali|Bangla|বাংলা"
    }
    val track=if(requested?.groupValues?.get(2)?.startsWith("sub",true)==true) "sub(?:titles?)?" else "(?:dub(?:bed)?|audio)"
    val evidencePattern=Regex("(?i)(?<![\\p{L}\\p{N}])(?:$aliases)\\s+"+track+"\\b")
    val matches=outcomes.flatMap { it.hits }.filter(::accepted).mapNotNull { restrictRow(it,hosts) }.distinctBy { it[3] }
        .map { row ->
            val documented=evidencePattern.containsMatchIn(row.getOrNull(7).orEmpty())
            val label=when {
                requested==null -> ""
                documented -> "Source lists "+requested.value
                else -> "Requested track unverified."
            }
            row.take(7)+List((7-row.size).coerceAtLeast(0)){""}+label
        }
    val ordered=when(options.getOrNull(2)){
        "Newest" -> matches.sortedByDescending { it[1].toIntOrNull() ?: 0 }
        "A – Z" -> matches.sortedBy { it[0].lowercase() }
        else -> matches.sortedByDescending { it[0].equals(title,true) }
    }
    val ranked=if(requested==null || options.getOrNull(2) in setOf("Newest","A – Z")) ordered else ordered.sortedByDescending { it.getOrNull(7)?.startsWith("Source lists")==true }
    return CataloguePage(ranked,states,otherSites.associate { it.host to it.searchPage })
}

internal fun restrictRow(row:List<String>,hosts:Set<String>):List<String>? {
    if(row.size<4)return null
    val allowed=resultLinks(row).filter { java.net.URI(it).host.lowercase().removePrefix("www.") in hosts }
    if(allowed.isEmpty())return null
    return row.toMutableList().apply {
        while(size<6)add("")
        this[3]=allowed.first()
        this[5]=allowed.drop(1).joinToString("|")
    }
}
internal fun validSource(link:String):Boolean = runCatching {
    val uri=java.net.URI(link)
    uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.rawUserInfo==null && uri.port in listOf(-1,443)
}.getOrDefault(false)

internal fun resultLinks(row:List<String>):List<String> = buildList {
    add(row[3])
    addAll(row.getOrNull(5).orEmpty().split('|'))
}.filter(::validSource).distinct()

internal fun copyLinks(context:android.content.Context, links:List<String>){
    context.getSystemService(android.content.ClipboardManager::class.java)
        .setPrimaryClip(android.content.ClipData.newPlainText("Nexus source links",links.joinToString("\n")))
    if(android.os.Build.VERSION.SDK_INT<33) android.widget.Toast.makeText(context,"Copied "+links.size+" source link(s)",android.widget.Toast.LENGTH_SHORT).show()
}









