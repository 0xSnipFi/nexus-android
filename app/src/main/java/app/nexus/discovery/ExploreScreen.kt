package app.nexus.discovery

import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray

private val accent = Color(0xFFF02D4F)
@Composable
internal fun ExploreScreen(query: String, edit: (String)->Unit, search: (String)->Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search-history", 0) }
    var recent by remember { mutableStateOf(prefs.getString("titles","[]").let { JSONArray(it) }.let { a -> List(a.length()){ a.getString(it) } }) }
    fun save(values: List<String>) { recent=values; prefs.edit().putString("titles",JSONArray(values).toString()).apply() }
    var category by rememberSaveable { mutableStateOf("All") }
    fun submit(title:String) {
        if(title.isBlank()) return
        save((listOf(title.trim()) + recent).distinct().take(12))
        search(title.trim() + "\n" + category)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(query, { edit(it.take(200)) }, singleLine=true, placeholder={Text("Search movies, anime, cartoons…")}, shape=RoundedCornerShape(32.dp),
            modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=accent),
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={ submit(query) }))
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF320D16),Color(0xFF160A0D),Color(0xFF230D14))),RoundedCornerShape(20.dp))
            .border(1.dp,Color(0xFFA34C5A),RoundedCornerShape(20.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("All","Anime","Movies","Series","Cartoons").forEachIndexed { i, label ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(label==category) Color(0xFFAA1429) else Color(0xFF28181D)).clickable { category=label }.padding(vertical=10.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
                        Text(listOf("▦","✦","▶","▣","☆")[i],fontSize=19.sp)
                        Text(label,fontSize=11.sp)
                    }
                }
            }
            Text("Search includes AnimeLok public listings. Title matching happens on your phone.",fontSize=12.sp)
            Button({submit(query)},enabled=query.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Search titles")}
            HorizontalDivider()
            Row { Text("Recent searches",Modifier.weight(1f),fontSize=18.sp); if(recent.isNotEmpty()) TextButton({save(emptyList())}){Text("Clear all")} }
            if(recent.isEmpty()) Text("Your searches will appear here.")
            recent.take(4).forEach { title -> Row { TextButton({submit(title)},Modifier.weight(1f)){Text(title)}; TextButton({save(recent-title)}){Text("Remove")} } }
            HorizontalDivider()
            Text("Popular searches",fontSize=18.sp)
            listOf(listOf("One Piece","Blue Lock"),listOf("Frieren","Naruto")).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach { AssistChip({submit(it)},{Text(it)}) }} }
        }
        Text("Explore titles",fontSize=22.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("Solo Leveling" to R.drawable.solo,"Demon Slayer" to R.drawable.demon,"Jujutsu Kaisen" to R.drawable.jujutsu,"Frieren" to R.drawable.frieren).forEach { (title,poster) ->
                Column(Modifier.width(102.dp).clickable { submit(title) }) {
                    Image(androidx.compose.ui.res.painterResource(poster),null,Modifier.fillMaxWidth().height(138.dp).clip(RoundedCornerShape(12.dp)),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
                    Text(title,modifier=Modifier.padding(vertical=5.dp))
                }        }
        }
    }
}

@Composable
internal fun CatalogueBrowser(url:String, returnToResults:()->Unit) {
    var web by remember { mutableStateOf<WebView?>(null) }
    var back by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    if(!validSource(url)){ Text("Invalid source address"); return }
    val target=url.replaceFirst(Regex("^https://movies4u\\.gp/"),"https://movies4u.vet/")
    val host=Uri.parse(target).host ?: return
    val assets=LocalContext.current.assets
    val rules=remember(assets){ BrowserShield(assets) }
    val prefs=LocalContext.current.getSharedPreferences("site-shield",0)
    val shield=remember(host){ java.util.concurrent.atomic.AtomicBoolean(prefs.getBoolean(host,true)) }
    var shieldOn by remember(host){mutableStateOf(shield.get())}
    var currentHost by remember(host){mutableStateOf(host)}
    var pending by remember { mutableStateOf<String?>(null) }
    pending?.let { address ->
        AlertDialog(onDismissRequest={pending=null},title={Text("Open linked site?")},text={Text(Uri.parse(address).host.orEmpty())},
            confirmButton={TextButton({pending=null;currentHost=Uri.parse(address).host.orEmpty();web?.loadUrl(address)}){Text("Open")}},
            dismissButton={TextButton({pending=null}){Text("Cancel")}})
    }

    var loadedUrl by remember { mutableStateOf("") }
    val directMedia=loadedUrl.takeIf { Transfers.isMediaLink(it) }
    BackHandler(back){web?.goBack()}
    Column {
        Row { Text(currentHost,Modifier.weight(1f).padding(10.dp)); TextButton({shieldOn=!shieldOn;shield.set(shieldOn);prefs.edit().putBoolean(currentHost,shieldOn).apply();web?.reload()}){Text(if(shieldOn)"Shield on" else "Shield off")}
            TextButton({failure=null;web?.reload()}){Text("Reload")} }
        TextButton({
            val view=web ?: return@TextButton
            val page=view.url
            view.evaluateJavascript(downloadAnchors){ result->
                if(view.url==page && view.isAttachedToWindow){
                    val links=readDownloadLinks(result,rules)
                    if(links.isEmpty()) android.widget.Toast.makeText(view.context,"No download links found on this page.",android.widget.Toast.LENGTH_SHORT).show()
                    else {
                        val labels=links.map { (label,address)->label+" · "+Uri.parse(address).host }.toTypedArray()
                        android.app.AlertDialog.Builder(view.context).setTitle("Download links from this page")
                            .setItems(labels){_,index->
                                val address=links[index].second
                                if(Transfers.isMediaLink(address)) Transfers.confirm(view.context,address) else pending=address
                            }.setNegativeButton("Cancel",null).show()
                    }
                }
            }
        }){Text("Download links")}
        if(directMedia!=null) TextButton({web?.context?.let { Transfers.confirm(it,directMedia) }}){Text("Download media file")}
        if(failure!=null) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
            Text("Source unavailable",style=MaterialTheme.typography.titleLarge)
            Text(failure.orEmpty())
            TextButton(returnToResults){Text("Back to results")}
        } }
        AndroidView(modifier=if(failure==null) Modifier.fillMaxSize() else Modifier.size(0.dp),factory={ context -> WebView(context).apply {
            settings.domStorageEnabled=true
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.javaScriptEnabled=true; settings.setSupportMultipleWindows(true); settings.javaScriptCanOpenWindowsAutomatically=false; settings.safeBrowsingEnabled=true
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient=object:WebViewClient(){
                override fun shouldInterceptRequest(view:WebView,r:WebResourceRequest):WebResourceResponse? {
                    if(shield.get() && !r.isForMainFrame && rules.blocks(r.url.host)) {
                        return WebResourceResponse("text/plain","UTF-8",204,"Blocked",emptyMap(),java.io.ByteArrayInputStream(byteArrayOf()))
                    }
                    return null
                }
                override fun shouldOverrideUrlLoading(view:WebView,r:WebResourceRequest):Boolean {
                    val address=r.url.toString()
                    if(!validSource(address) || shield.get() && rules.blocks(r.url.host)) return true
                    if(!r.isForMainFrame) return false
                    if(r.url.host?.removePrefix("www.")==Uri.parse(view.url ?: target).host?.removePrefix("www.")) return false
                    if(r.isForMainFrame && r.hasGesture()) pending=address
                    return true
                }
                override fun onReceivedHttpError(view:WebView,r:WebResourceRequest,response:WebResourceResponse){ if(r.isForMainFrame && response.statusCode>=400) failure="Source returned HTTP "+response.statusCode+". Return to results to choose another source." }
                override fun onPageStarted(view:WebView,url:String,icon:Bitmap?){failure=null;loadedUrl="";currentHost=Uri.parse(url).host ?: host;shieldOn=prefs.getBoolean(currentHost,true);shield.set(shieldOn)}
                override fun onPageFinished(view:WebView,url:String){
                    back=view.canGoBack();loadedUrl=url

                }
                override fun onReceivedError(view:WebView,r:WebResourceRequest,e:WebResourceError){if(r.isForMainFrame)failure=if(e.errorCode==ERROR_HOST_LOOKUP) "Your network could not resolve $host. Retry or choose another source." else e.description.toString()}
            }
            webChromeClient=object:WebChromeClient(){
                override fun onCreateWindow(view:WebView,isDialog:Boolean,isUserGesture:Boolean,resultMsg:android.os.Message):Boolean {
                    if(isUserGesture){
                        val link=view.hitTestResult.extra
                        if(link!=null && validSource(link) && !(shield.get() && rules.blocks(Uri.parse(link).host))) pending=link
                    }
                    return false
                }
            }
            setOnLongClickListener {
                val link=hitTestResult.extra
                if(link==null || !validSource(link) || shield.get() && rules.blocks(Uri.parse(link).host)) false
                else {
                    val options=if(Transfers.isMediaLink(link)) arrayOf("Open link","Download file") else arrayOf("Open link")
                    android.app.AlertDialog.Builder(context).setTitle(Uri.parse(link).host)
                        .setItems(options){_,i-> if(i==1) Transfers.confirm(context,link) else pending=link }
                        .setNegativeButton("Cancel",null).show()
                    true
                }
            }
            setDownloadListener { link,_,disposition,mime,_-> Transfers.confirm(context,link,mime,disposition) }
            web=this
            loadUrl(target)
        } },onRelease={it.stopLoading();it.destroy()})
    }
}

private const val downloadAnchors="""(()=>{
 const links=[];
 for(const a of document.querySelectorAll("main a[href], article a[href], body a[href]")){
  if(a.closest("header,nav,aside,footer"))continue;
  const label=(a.textContent||a.title||"").trim().slice(0,100);
  if(!a.hasAttribute("download") && !/\bdownload\b/i.test(label))continue;
  if(/\b(app|apk|telegram)\b/i.test(label) || !a.href.startsWith("https://"))continue;
  links.push([label||"Download",a.href]); if(links.length===40)break;
 }
 return links;
})()"""

private fun readDownloadLinks(result:String?,rules:BrowserShield):List<Pair<String,String>> {
    val rows=runCatching { JSONArray(result) }.getOrNull() ?: return emptyList()
    return (0 until rows.length()).mapNotNull { index->
        val item=rows.optJSONArray(index) ?: return@mapNotNull null
        val address=item.optString(1)
        if(address.length>4096 || !validSource(address) || rules.blocks(Uri.parse(address).host)) null
        else item.optString(0) to address
    }.distinctBy { it.second }
}
