package app.nexus.discovery

import android.content.ClipboardManager
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/** Exercises the shipped WebView controls and their Android handlers on a phone. */
class NexusJourneyTest {
    private val ctx=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario:ActivityScenario<MainActivity>
    private val history=ctx.getSharedPreferences("search-history",0)
    private val saved=ctx.getSharedPreferences("watchlist",0)
    private var originalHistory:String?=null
    private var originalSaved:String?=null
    private var url=""

    @Before fun launch(){
        originalHistory=history.getString("titles",null)
        originalSaved=null
        history.edit().putString("titles","[]").commit()

        scenario=ActivityScenario.launch(MainActivity::class.java)
        waitFor("!!document.querySelector('#run')")
    }
    @After fun restore(){
        scenario.close()
        history.edit().putString("titles",originalHistory).commit()
        if(url.isNotEmpty()) saved.edit().putString(url,originalSaved).commit()
    }
    private fun find(v:View):WebView? {
        if(v is WebView)return v
        if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it}
        return null
    }
    private fun js(script:String):String{
        val q=ArrayBlockingQueue<String>(1)
        scenario.onActivity { a->
            val w=find(a.window.decorView)
            if(w==null)q.offer("null") else w.evaluateJavascript(script){q.offer(it?:"null")}
        }
        return q.poll(3,TimeUnit.SECONDS)?:"null"
    }
    private fun waitFor(condition:String){
        val end=android.os.SystemClock.elapsedRealtime()+35000
        do{if(js(condition)=="true")return;Thread.sleep(200)}while(android.os.SystemClock.elapsedRealtime()<end)
        fail("Screen condition failed: "+condition+"; "+js("document.body.innerText.slice(0,450)"))
    }
    private fun click(selector:String){waitFor("!!document.querySelector("+org.json.JSONObject.quote(selector)+")")
        js("document.querySelector("+org.json.JSONObject.quote(selector)+").click()")}
    private fun submit(query:String){waitFor("!!document.querySelector('#run')");js("document.querySelector('#q').value="+org.json.JSONObject.quote(query));click("#run")}
    private fun tab(i:Int){click("[data-i='$i']")}
    @Test fun movies4uCharlie(){
        click("[data-cat=movie]");submit("777 Charlie")
        waitFor("document.querySelector('.hit h3')?.textContent==='777 Charlie'")
        waitFor("document.querySelector('.hit small')?.innerText.includes('2022')===true")
        val address=org.json.JSONTokener(js("document.querySelector('.hit [data-url]').dataset.url")).nextValue().toString()
        assertEquals("movies4u.gp",java.net.URI(address).host)
        click(".hit [data-copy]");waitForClipboard(address)
        waitFor("document.querySelector('.hit .cover img')?.naturalWidth>0")
        click(".hit [data-url]")
        waitFor("location.hostname==='movies4u.vet' && document.title.toLowerCase().includes('777 charlie')")
    }
    @Test fun sourceOnlySearch(){
        click("[data-cat=anime]");submit("Naruto")
        waitFor("!!document.querySelector('.hit [data-copy]')")
        val urls=org.json.JSONArray(js("Array.from(new Set([...document.querySelectorAll('.hit [data-url]')].map(b=>b.dataset.url)))"))
        val hosts=ctx.assets.open("submitted-sources.tsv").bufferedReader().useLines { lines -> lines.map { java.net.URI(it.substringBefore('\t')).host.removePrefix("www.") }.toSet() }
        val links=(0 until urls.length()).map(urls::getString)
        assertTrue(links.isNotEmpty() && links.all { java.net.URI(it).host.removePrefix("www.") in hosts })
        click("[data-copy=all]");waitForClipboard(links.joinToString("\n"))
        click(".hit [data-copy]");waitForClipboard(links.first())
        tab(1);submit("777 Charlie")
        waitFor("!!document.querySelector('details')")
        val movieLinks=org.json.JSONArray(js("Array.from(document.querySelectorAll('.hit [data-url]'),b=>b.dataset.url)"))
        assertTrue(movieLinks.length()>0 && (0 until movieLinks.length()).all { java.net.URI(movieLinks.getString(it)).host.removePrefix("www.") in hosts })
        tab(1);submit("zznexusnomatch7343")
        waitFor("document.body.innerText.includes('No verified match')")
        assertEquals("0",js("document.querySelectorAll('[data-url],[data-copy]').length"))
    }
    @Test fun watchlistClipboardBrowserAndTabs(){
        click("[data-cat=cartoon]");submit("Pokémon");waitFor("!!document.querySelector('.hit [data-url]')")
        url=org.json.JSONTokener(js("document.querySelector('.hit [data-url]').dataset.url")).nextValue().toString()
        originalSaved=saved.getString(url,null)
        click("summary");waitFor("document.querySelector('details')?.open===true")
        click(".hit [data-copy]")
        waitForClipboard(url)
        click("[data-save]");tab(2)
        click("[data-url='$url']")
        waitFor("location.protocol==='https:' && location.hostname.length>0")
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        click("[data-remove='$url']");waitFor("!Array.from(document.querySelectorAll('[data-remove]')).some(b=>b.dataset.remove==='$url')")
        assertFalse(saved.contains(url))
        tab(3);waitFor("!!document.querySelector('#fileLink')")
        tab(4);waitFor("!!document.querySelector('.tile')")
        tab(0);waitFor("!!document.querySelector('.big')")
        tab(1);click("[data-x]");waitFor("!document.querySelector('[data-x]')")
    }
    private fun waitForClipboard(expected:String){
        for(i in 0..30){
            var match=false
            scenario.onActivity{a->match=a.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()==expected}
            if(match)return
            Thread.sleep(100)
        };fail("Clipboard did not contain expected source")
    }
    @Test fun movieFiltersHistoryAndNoMatch(){
        click("[data-cat=movie]");click("[data-f=Year]");click("[data-so=Newest]")
        submit("Bohurupi");waitFor("document.querySelector('.hit h3')?.textContent==='Bohurupi'")
        waitFor("document.querySelector('.hit small')?.textContent.includes('2024')===true")
        click("[data-copy]:not([data-copy=all])")
        waitForClipboard(org.json.JSONTokener(js("document.querySelector('.hit [data-copy]').dataset.copy")).nextValue().toString())
        val urls=org.json.JSONArray(js("JSON.stringify([...new Set([...document.querySelectorAll('.hit [data-url]')].map(b=>b.dataset.url))])").let { org.json.JSONTokener(it).nextValue().toString() })
        assertTrue(urls.length()>1)
        click("[data-copy=all]");waitForClipboard((0 until urls.length()).joinToString("\n"){urls.getString(it)})
        tab(1);click("[data-s=Bohurupi]");waitFor("!!document.querySelector('.hit')")
        tab(1);submit("zznexusnomatch7343");waitFor("document.body.innerText.includes('No verified match')")
        tab(1);click("#clr");waitFor("!document.querySelector('[data-x]')")
        assertEquals("[]",history.getString("titles",null))
    }
    @Test fun yearExclusionAndEditedResultQuery(){
        click("[data-cat=movie]");click("[data-f=Year]");submit("777 Charlie")
        waitFor("document.body.innerText.includes('No verified match')")
        js("var f=document.querySelector('#q'); f.value='Bohurupi'; f.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("document.querySelector('.hit h3')?.textContent==='Bohurupi'")
        js("var f=document.querySelector('#q'); f.value='777 Charlie'; f.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("document.body.innerText.includes('No verified match')")
    }
    @Test fun languageTypeAndAlphabeticalSort(){
        click("[data-cat=anime]")
        click("[data-f=Language]");click("[data-f=Language]")
        click("[data-f=Type]");click("[data-so='A – Z']")
        submit("Solo Leveling")
        waitFor("document.querySelectorAll('.hit').length>=2")
        waitFor("document.querySelector('#q').value.toLowerCase().includes('hindi dub')")
        waitFor("Array.from(document.querySelectorAll('.hit')).every(e=>e.querySelector('small').innerText.includes('TV'))")
        waitFor("Array.from(document.querySelectorAll('.hit')).every(e=>/Source lists Hindi dub|Requested track unverified/i.test(e.innerText))")
        val titles=js("Array.from(document.querySelectorAll('.hit h3')).map(e=>e.textContent.toLowerCase())")
        val arr=org.json.JSONArray(titles)
        val actual=(0 until arr.length()).map(arr::getString)
        assertEquals(actual.sorted(),actual)
    }
    private fun confirmDialog(){
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val info=automation.serviceInfo
        info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo=info
        repeat(40){
            val node=automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId("android:id/button1")?.firstOrNull()
            if(node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)==true)return
            Thread.sleep(150)
        }
        fail("Download confirmation did not appear")
    }
    @Test fun directDownloadOnPhone()=downloadOnPhone(false)
    @Test fun shortMovieDownloadOnPhone()=downloadOnPhone(true)
    private fun downloadOnPhone(movie:Boolean){
        tab(3)
        waitFor("!!document.querySelector('#fileLink')")
        val source=if(movie)"https://www.w3schools.com/html/mov_bbb.mp4" else "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
        js("document.querySelector('#fileLink').value="+org.json.JSONObject.quote(source))
        val prefs=ctx.getSharedPreferences("source-downloads",0)
        val before=prefs.all.keys
        click("#getFile")
        confirmDialog()
        val end=android.os.SystemClock.elapsedRealtime()+6000
        while((prefs.all.keys-before).isEmpty() && android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(100)
        val id=(prefs.all.keys-before).single().toLong()
        waitFor("downloadItems.some(f=>f.id=="+id+" && f.state==='Completed')")
        val dm=ctx.getSystemService(android.app.DownloadManager::class.java)
        val file=dm.getUriForDownloadedFile(id)
        val header=ByteArray(5)
        ctx.contentResolver.openInputStream(file!!)!!.use { java.io.DataInputStream(it).readFully(header) }
        if(movie){
            val player=android.media.MediaPlayer()
            try{
                player.setDataSource(ctx,file)
                player.prepare()
                assertTrue(player.duration>1000 && player.videoWidth>0)
                player.setVolume(0f,0f)
                player.start()
                val playbackDeadline=android.os.SystemClock.elapsedRealtime()+8000
                while(player.currentPosition<=0 && android.os.SystemClock.elapsedRealtime()<playbackDeadline)Thread.sleep(100)
                assertTrue(player.currentPosition>0)
                android.util.Log.i("NexusMovieTest","Downloaded MP4: "+player.videoWidth+"x"+player.videoHeight+", duration="+player.duration+"ms, played="+player.currentPosition+"ms")
            }finally{player.release()}
        }else assertEquals("%PDF-",String(header))
        js("document.querySelectorAll('#transfers article')[downloadItems.findIndex(f=>f.id=="+id+")].querySelector('[data-file=shareFile]').click()")
        val inst=InstrumentationRegistry.getInstrumentation()
        val deadline=android.os.SystemClock.elapsedRealtime()+20000
        var chooser=false
        while(android.os.SystemClock.elapsedRealtime()<deadline && !chooser){
            val root=inst.uiAutomation.rootInActiveWindow
            chooser=root?.packageName=="com.android.intentresolver" ||
                root?.findAccessibilityNodeInfosByText("Share file")?.isNotEmpty()==true
            if(!chooser && root?.packageName=="android"){
                val dump=inst.uiAutomation.executeShellCommand("dumpsys activity activities")
                chooser=android.os.ParcelFileDescriptor.AutoCloseInputStream(dump).bufferedReader().useLines { lines ->
                    lines.any { it.contains("ResumedActivity") && it.contains("android/com.android.internal.app.ChooserActivity") }
                }
            }
            if(!chooser)Thread.sleep(200)
        }
        assertTrue("Android share chooser was not visible",chooser)
        assertTrue(inst.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
        waitFor("!!document.querySelector('#transfers article')")
        js("document.querySelectorAll('#transfers article')[downloadItems.findIndex(f=>f.id=="+id+")].querySelector('[data-file=removeFile]').click()")
        confirmDialog()
        waitFor("downloadItems.every(f=>f.id!="+id+")")
        assertFalse(prefs.contains(id.toString()))
    }
    @Test fun onlineMoviePoster(){
        click("[data-cat=movie]");submit("777 Charlie")
        waitFor("document.querySelector('.hit h3')?.textContent==='777 Charlie'")
        waitFor("document.querySelector('.hit .cover img')?.naturalWidth>0")
    }
    @Test fun browserShieldInterceptsAdResources(){
        tab(4)
        js("var f=document.querySelector('.sb input');f.value='https://movies4u.gp/';f.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("location.hostname==='movies4u.vet'")
        scenario.onActivity { activity ->
            val web=find(activity.window.decorView)!!
            fun inspect(address:String):android.webkit.WebResourceResponse? {
                val request=object:android.webkit.WebResourceRequest {
                    override fun getUrl()=android.net.Uri.parse(address)
                    override fun isForMainFrame()=false
                    override fun isRedirect()=false
                    override fun hasGesture()=false
                    override fun getMethod()="GET"
                    override fun getRequestHeaders()=emptyMap<String,String>()
                }
                return web.webViewClient.shouldInterceptRequest(web,request)
            }
            assertEquals(204,inspect("https://ads.doubleclick.net/banner")?.statusCode)
            assertNull(inspect("https://movies4u.vet/video.mp4"))
            assertNull(inspect("https://doubleclick.net.example.org/media"))
            assertFalse(web.settings.javaScriptCanOpenWindowsAutomatically)
            web.loadDataWithBaseURL("https://analytics.163.com/","<script>fetch('/nexus-shield-test').then(r=>document.title=String(r.status))</script>","text/html","UTF-8",null)
        }
        waitFor("document.title==='204'")
    }
    @Test fun originalVideoPlayback(){
        tab(4)
        waitFor("!!document.querySelector('.sb input:not(#q)')")
        js("var f=document.querySelector('.sb input');f.value='https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4';f.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("!!document.querySelector('video') && document.querySelector('video').readyState>=1")
        val inst=InstrumentationRegistry.getInstrumentation()
        val coords=IntArray(2)
        scenario.onActivity{a->val w=find(a.window.decorView)!!; w.getLocationOnScreen(coords);coords[0]+=w.width/2;coords[1]+=w.height/2}
        val now=android.os.SystemClock.uptimeMillis()
        for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)){
            val e=android.view.MotionEvent.obtain(now,android.os.SystemClock.uptimeMillis(),action,coords[0].toFloat(),coords[1].toFloat(),0)
            inst.sendPointerSync(e);e.recycle()
        }
        js("document.querySelector('video').play().catch(()=>{});true")
        waitFor("document.querySelector('video').currentTime>0.25")
    }
    private fun tapNativeText(label:String, exact:Boolean=false){
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val end=android.os.SystemClock.elapsedRealtime()+12000
        while(android.os.SystemClock.elapsedRealtime()<end){
            val nodes=java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
            automation.rootInActiveWindow?.let(nodes::add)
            while(nodes.isNotEmpty()){
                val node=nodes.removeFirst()
                for(i in 0 until node.childCount)node.getChild(i)?.let(nodes::add)
                val texts=listOf(node.text?.toString().orEmpty(),node.contentDescription?.toString().orEmpty())
                if(texts.none { if(exact) it==label else it.contains(label,true) }) continue
                var target:android.view.accessibility.AccessibilityNodeInfo?=node
                while(target!=null){
                    if(target.isClickable && target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return
                    target=target.parent
                }
            }
            Thread.sleep(150)
        }
        fail("Missing native control: "+label)
    }
    @Test fun shortFilmSourceDownloadHandoff(){
        click("[data-cat=movie]")
        submit("The Elephant Whisperers")
        click(".hit [data-url^='https://movies4u.gp/']")
        waitFor("Array.from(document.links).some(a=>a.hostname==='mdrive.sbs')")
        tapNativeText("Download links",exact=true)
        tapNativeText("mdrive.sbs")
        tapNativeText("Open",exact=true)
        waitFor("location.hostname==='mdrive.sbs' && document.title.toLowerCase().includes('elephant whisperers')")
    }
    @Test fun webSeriesFromSearchScreen(){
        click("[data-cat=series]");submit("Panchayat")
        waitFor("Array.from(document.querySelectorAll('.hit')).some(e=>e.querySelector('h3').textContent.startsWith('Panchayat') && e.innerText.includes('TV') && e.querySelector('[data-url]'))")
    }
}














