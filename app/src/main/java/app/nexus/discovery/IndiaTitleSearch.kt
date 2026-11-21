package app.nexus.discovery

import android.text.Html
import app.nexus.domain.FederatedSearch
import app.nexus.domain.TitleMatcher
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Reads title/year cards, never offer redirects, from the India discovery page. */
internal class IndiaTitleSearch(private val category:String): FederatedSearch.Adapter<List<String>> {
    override fun name()="JustWatch India · online catalogue"
    override fun enabled()=true
    override fun search(query:String): List<List<String>> {
        if(query.isBlank() || query.length>200)return emptyList()
        val url=URL("https://www.justwatch.com/in/search?q="+URLEncoder.encode(query,"UTF-8"))
        val conn=url.openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects=false
            conn.connectTimeout=3500
            conn.readTimeout=5000
            if(conn.responseCode!=200)throw IOException("Catalogue unavailable")
            val out=ByteArrayOutputStream()
            conn.inputStream.use { stream ->
                val buf=ByteArray(8192)
                while(true){
                    val n=stream.read(buf)
                    if(n<0)break
                    if(out.size()+n>3000000)throw IOException("Catalogue response too large")
                    out.write(buf,0,n)
                }
            }
            val html=out.toString("UTF-8")
            val matcher=TitleMatcher(query)
            return card.findAll(html).mapNotNull { m ->
                val type=if(m.groupValues[2]=="movie") "MOVIE" else "TV"
                val title=Html.fromHtml(m.groupValues[3],Html.FROM_HTML_MODE_LEGACY).toString().trim()
                if((category=="Movies" && type!="MOVIE") || (category=="Series" && type!="TV") || !matcher.matches(title)) null
                else {
                    val href=m.groupValues[1]
                    val previous=html.lastIndexOf("href=\""+href+"\"",m.range.first-1)
                    val img=if(previous>=0 && m.range.first-previous<8192)
                        poster.find(html.substring(previous,m.range.first).substringBefore("</a>"))?.groupValues?.get(1) else null
                    listOf(title,m.groupValues[4],type,href,"","",img.orEmpty())
                }
            }.distinctBy { it[3] }.take(40).toList()
        } finally { conn.disconnect() }
    }
    companion object {
        private val poster=Regex("""<img\b[^>]*\bsrc="(https://images\.justwatch\.com/poster/[a-zA-Z0-9_/%.-]+\.(?:jpg|webp))"""")
        private val card=Regex("""<a\s+href="(https://www\.justwatch\.com/in/(movie|tv-show)/[a-zA-Z0-9%-]+)"[^>]*>\s*(?:<!--[^>]*-->\s*)*<span class="header-title"[^>]*>([^<]+)</span>\s*(?:<span class="header-year"[^>]*>\((\d{4})\)</span>)?""")
    }
}

