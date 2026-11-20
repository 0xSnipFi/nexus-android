package app.nexus.discovery
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
@Composable
internal fun SavedSources(open:(String)->Unit) {
    val context=LocalContext.current
    val library=remember { context.getSharedPreferences("watchlist",0) }
    var entries by remember { mutableStateOf(library.all.toMap()) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { Text("Your watchlist",style=MaterialTheme.typography.headlineSmall) }
        if(entries.isEmpty()) item { Text("Save a title from search results to see it here.") }
        items(entries.keys.sortedBy { entries[it].toString() },key={it}) { url ->
            Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(12.dp)) {
                Text(entries[url].toString())
                Row { TextButton({open(url)}) { Text("Open source") }
                    TextButton({library.edit().remove(url).apply();entries=library.all.toMap()}){Text("Remove")} }
            } }
        }
    }
}










