package app.nexus.discovery

import android.net.Uri
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun Prototype(tab:Int, select:(Int)->Unit, submit:(String)->Unit, open:(String)->Unit, onSettings:()->Unit,input:String="",reduceMotion:Boolean=false){
    var browser by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var rows by remember(input){mutableStateOf<List<List<String>>>(emptyList())}
    val liveRows by rememberUpdatedState(rows)
    LaunchedEffect(ready,input){
        val web=browser ?: return@LaunchedEffect
        if(!ready || input.isEmpty())return@LaunchedEffect
        val title=JSONObject.quote(input.substringBefore('\n'))
        web.evaluateJavascript("showHits("+title+",null,[])",null)
        val page=catalogue(web.context,input)
        rows=page.rows
        val data=JSONArray()
        rows.forEach { row->data.put(JSONObject().put("title",row[0]).put("year",row[1]).put("type",row[2]).put("track",row.getOrNull(7).orEmpty()).put("aliases",row.getOrNull(4).orEmpty()).put("links",JSONArray(resultLinks(row)))) }
        web.evaluateJavascript("showHits("+title+","+data+","+JSONArray(page.status)+","+JSONObject(page.sourcePages)+")",null)
        // Load covers near the viewport. A broad search can return hundreds of titles.
        while(true){
            val indices=kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                web.evaluateJavascript("visiblePosterIndices()") { result ->
                    if(continuation.isActive) continuation.resumeWith(Result.success(result ?: "" ))
                }
            }
            val visible=runCatching { JSONArray(indices) }.getOrNull()
            if(visible!=null) for(i in 0 until visible.length()){
                val index=visible.optInt(i,-1)
                val row=page.rows.getOrNull(index) ?: continue
                val address=row.getOrNull(6)?.takeIf { it.isNotBlank() } ?: continue
                val image=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){posterData(address,row[3])}
                if(image!=null)web.evaluateJavascript("setPoster("+index+","+JSONObject.quote(image)+")",null)
            }
            kotlinx.coroutines.delay(750)
        }
    }
    LaunchedEffect(tab,ready){
        val web=browser ?: return@LaunchedEffect
        if(tab==3 && ready)while(true){
            val files=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){Transfers.rows(web.context)}
            web.evaluateJavascript("updateDownloads("+files+")",null)
            kotlinx.coroutines.delay(1500)
        }
    }
    val activeInput by rememberUpdatedState(input)
    LaunchedEffect(ready,reduceMotion){
        if(ready) browser?.evaluateJavascript("document.documentElement.classList.toggle('reduce-motion', "+reduceMotion+")",null)
    }
    val selected by rememberUpdatedState(select)
    val search by rememberUpdatedState(submit)
    AndroidView(modifier=Modifier.fillMaxSize(),factory={ ctx ->
        val history=ctx.getSharedPreferences("search-history",0)
        val saved=ctx.getSharedPreferences("watchlist",0)
        val template=ctx.assets.open("prototype.html").bufferedReader().use { it.readText() }
        fun safe(json:String)=json.replace("<","\\u003c").replace(">","\\u003e")
        WebView(ctx).apply {
            browser=this
            settings.javaScriptEnabled=true
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.blockNetworkLoads=true
            webChromeClient=object:WebChromeClient(){
                override fun onConsoleMessage(msg:ConsoleMessage):Boolean {
                    android.util.Log.d("NexusUI",msg.message()+" line "+msg.lineNumber())
                    return true
                }
            }
            webViewClient=object:WebViewClient(){
                override fun onPageFinished(view:WebView,url:String){
                    ready=true
                    view.evaluateJavascript("[typeof go,innerWidth,innerHeight,document.body.childElementCount,document.getElementById('app')?.offsetHeight,document.getElementById('view')?.childElementCount].join(',')"){ android.util.Log.d("NexusUI",it) }
                }
                override fun shouldOverrideUrlLoading(view:WebView,r:WebResourceRequest):Boolean {
                    val u=r.url
                    if(!r.isForMainFrame || u.scheme!="nexus") return true
                    val v=u.getQueryParameter("value").orEmpty()
                    when(u.host){
                        "search" -> if(v.length<=280 && v.substringBefore('\n').isNotBlank()){
                            val a=JSONArray(history.getString("titles","[]"))
                            val titles=(listOf(v.substringBefore('\n'))+(0 until a.length()).map { a.optString(it) }).distinct().take(12)
                            history.edit().putString("titles",JSONArray(titles).toString()).apply()
                            search(if(activeInput.isEmpty())v else v.substringBefore('\n')+"\n"+activeInput.substringAfter('\n'))
                        }
                        "history" -> if(v.length<4000)runCatching { history.edit().putString("titles",JSONArray(v).toString()).apply() }
                        "copy" -> {
                            val all=liveRows.flatMap(::resultLinks).distinct()
                            val links=if(v=="all")all else listOf(v).filter { it in all }
                            if(links.isNotEmpty())copyLinks(ctx,links)
                        }
                        "save" -> liveRows.getOrNull(v.toIntOrNull() ?: -1)?.let { row->
                            val url=resultLinks(row).firstOrNull() ?: return@let
                            saved.edit().putString(url,row[0]+" · "+row[1]).apply()
                            android.widget.Toast.makeText(ctx,"Saved to Watchlist",android.widget.Toast.LENGTH_SHORT).show()
                        }
                        "download" -> Transfers.confirm(ctx,v)
                        "openFile","shareFile","removeFile" -> v.toLongOrNull()?.let { Transfers.action(ctx,it,u.host.orEmpty()) }
                        "open" -> if(validSource(v))open(v)
                        "remove" -> { saved.edit().remove(v).apply();view.evaluateJavascript("delete saved["+safe(JSONObject.quote(v))+"];render()",null) }
                        "settings" -> onSettings()
                        "tab" -> v.toIntOrNull()?.takeIf { it in 0..4 }?.let(selected)
                    }
                    return true
                }
            }
            val placeholders=Regex("INITIAL_(TAB|HISTORY|SAVED|ART)")
            val page=placeholders.replace(template){when(it.value){
                "INITIAL_ART"->ctx.assets.open("artwork.json").bufferedReader().use { it.readText() }
                "INITIAL_TAB"->tab.toString()
                "INITIAL_HISTORY"->safe(history.getString("titles","[]")?:"[]")
                else->safe(JSONObject(saved.all).toString())
            }}
            loadData(android.util.Base64.encodeToString(page.toByteArray(), android.util.Base64.NO_WRAP),"text/html; charset=utf-8","base64")
        }
    },onRelease={it.destroy()})
}







