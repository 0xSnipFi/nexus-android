package app.nexus.discovery

import android.text.Html
import app.nexus.domain.FederatedSearch
import app.nexus.domain.TitleMatcher
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/** The submitted .gp site moved to .vet. Pin that migration, never follow unrelated redirects. */
internal class Movies4uSearch(private val category:String):FederatedSearch.Adapter<List<String>> {
    override fun name()="Movies4U"
    override fun enabled()=true
    override fun search(query:String):List<List<String>>{
        if(query.isBlank() || query.length>200)return emptyList()
        val conn=URL("https://movies4u.vet/?s="+URLEncoder.encode(query,"UTF-8")).openConnection() as HttpURLConnection
        try{
            conn.instanceFollowRedirects=false
            conn.connectTimeout=5000;conn.readTimeout=6500
            if(conn.responseCode!=200)throw IOException("Source unavailable")
            val body=ByteArrayOutputStream()
            conn.inputStream.use { stream ->
                val buf=ByteArray(8192)
                while(true){
                    val n=stream.read(buf);if(n<0)break
                    if(body.size()+n>2000000)throw IOException("Page too large")
                    body.write(buf,0,n)
                }
            }
            val html=body.toString("UTF-8")
            if(!html.contains("wp-block-search") && !html.contains("entry-card"))throw IOException("Unexpected search page")
            val matcher=TitleMatcher(query)
            return article.findAll(html).mapNotNull { m->
                val card=m.value
                val heading=entry.find(card)?:return@mapNotNull null
                val page=heading.groupValues[1]
                if(!sameHost(page))return@mapNotNull null
                val label=Html.fromHtml(heading.groupValues[2],Html.FROM_HTML_MODE_LEGACY).toString().trim()
                val date=year.find(label)
                val title=(date?.let{label.substring(0,it.range.first)}?:label).trim().ifBlank{label}
                val type=if(Regex("""(?i)season|web.series|tv.show|episode""").containsMatchIn(label+" "+m.groupValues[1])) "TV" else "MOVIE"
                if(!matcher.matches(title) || category=="Movies" && type!="MOVIE" || category=="Series" && type!="TV")return@mapNotNull null
                val pic=image.find(card)?.groupValues?.get(1)?.takeIf(::sameHost).orEmpty()
                listOf(title,date?.groupValues?.get(1).orEmpty(),type,page.replaceFirst("https://movies4u.vet/","https://movies4u.gp/"),"","",pic,label)
            }.distinctBy{it[3]}.take(50).toList()
        }finally{conn.disconnect()}
    }
    companion object{
        private fun sameHost(url:String)=validSource(url) && URI(url).host=="movies4u.vet"
        private val article=Regex("""<article\b([^>]*)>.*?</article>""",RegexOption.DOT_MATCHES_ALL)
        private val entry=Regex("""<h2\b[^>]*class="[^"]*entry-title[^"]*"[^>]*>\s*<a\b[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",RegexOption.DOT_MATCHES_ALL)
        private val year=Regex("""\(((?:19|20)\d{2})\)""")
        private val image=Regex("""<img\b[^>]*?\ssrc="([^"]+)"""")
    }
}

