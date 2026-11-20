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
    val host=Uri.parse(url).host ?: return
    BackHandler(back){web?.goBack()}
    Column {
        Row { Text(host,Modifier.weight(1f).padding(10.dp)); TextButton({failure=null;web?.loadUrl(url)}){Text("Reload")} }
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
                override fun shouldOverrideUrlLoading(view:WebView,r:WebResourceRequest):Boolean =
                    !validSource(r.url.toString()) || r.url.host?.removePrefix("www.")!=host.removePrefix("www.")
                override fun onReceivedHttpError(view:WebView,r:WebResourceRequest,response:WebResourceResponse){ if(r.isForMainFrame && response.statusCode>=400) failure="Source returned HTTP "+response.statusCode+". Return to results to choose another source." }
                override fun onPageStarted(view:WebView,url:String,icon:Bitmap?){failure=null}
                override fun onPageFinished(view:WebView,url:String){back=view.canGoBack()}
                override fun onReceivedError(view:WebView,r:WebResourceRequest,e:WebResourceError){if(r.isForMainFrame)failure=if(e.errorCode==ERROR_HOST_LOOKUP) "Your network could not resolve $host. Retry or choose another source." else e.description.toString()}
            }
            web=this
            loadUrl(url)
        } },onRelease={it.stopLoading();it.destroy()})
    }
}





