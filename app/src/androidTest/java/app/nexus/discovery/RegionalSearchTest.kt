package app.nexus.discovery

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalSearchTest {
    private lateinit var foreground:androidx.test.core.app.ActivityScenario<MainActivity>
    @org.junit.Before fun keepAppVisible(){foreground=androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)}
    @org.junit.After fun closeApp(){foreground.close()}
    @Test fun submittedMovieSites() = runBlocking {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val page=catalogue(ctx,"Street Fighter\nMovies")
        for(host in listOf("hdhub4you.xyz","hdboss.in","new15.hdhub4u.cl")) {
            val status=page.status.single { it.startsWith(host+" · ") }
            val url=page.sourcePages.getValue(host)
            assertTrue(java.net.URI(url).host==host && java.net.URI(url).query?.contains("Street+Fighter")==true)
            android.util.Log.i("NexusSiteTest",status)
        }
        assertTrue("No matching HDHub4you title card",page.rows.any { row ->
            java.net.URI(row[3]).host=="hdhub4you.xyz" && row[0].contains("Street Fighter",true)
        })
    }
    @Test fun searchOnDevice() = runBlocking {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val cases=listOf("Big Buck Bunny" to "Movies","777 Charlie" to "Movies","Bohurupi" to "Movies","Pokémon" to "Cartoons","Pokemon" to "Cartoons","Stree 2" to "Movies","Kantara" to "Movies","Belashuru" to "Movies","Bluey" to "Cartoons","Panchayat" to "Series","Mirzapur" to "Series","Breaking Bad" to "Series","Naruto" to "Anime")
        val hosts=ctx.assets.open("submitted-sources.tsv").bufferedReader().useLines { lines -> lines.map { java.net.URI(it.substringBefore('\t')).host.removePrefix("www.") }.toSet() }
        for ((query,category) in cases){
            val page=catalogue(ctx,query+"\n"+category)
            val links=page.rows.flatMap(::resultLinks)
            if(query in setOf("777 Charlie","Bohurupi","Pokémon","Pokemon","Stree 2","Naruto","Panchayat")) assertTrue("No source returned "+query,links.isNotEmpty())
            assertTrue(page.rows.all { resultLinks(it).isNotEmpty() })
            assertTrue(links.all { java.net.URI(it).host.removePrefix("www.") in hosts })
            if(query=="Big Buck Bunny") android.util.Log.i("NexusSourceCheck","Short film pages: "+links.joinToString())
            android.util.Log.i("NexusSourceCheck","$query: "+links.distinct().size+" links")
        }
        val row=listOf("mixed","","MOVIE","https://primevideo.com/test","","https://animelok.cc.example.com/test|https://animelok.cc/anime/20")
        assertTrue(resultLinks(restrictRow(row,hosts)!!)==listOf("https://animelok.cc/anime/20"))
    }
}


