package app.nexus.discovery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import app.nexus.domain.*

private val Red = Color(0xFFFF354A)
private val Background = Color(0xFF10070B)
private val Panel = Color(0xFF201017)
private val Muted = Color(0xFFCDB8BF)
private val Samples = listOf(
    MediaTitle("spirited", "Spirited Away", 2001, MediaType.ANIME, listOf("Sen to Chihiro")),
    MediaTitle("general", "The General", 1926, MediaType.MOVIE),
    MediaTitle("sherlock", "Sherlock Jr.", 1924, MediaType.MOVIE),
    MediaTitle("bunny", "Big Buck Bunny", 2008, MediaType.CARTOON))
private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val Destinations = listOf(
    Destination("home", "Home", Icons.Default.Home),
    Destination("search", "Search", Icons.Default.Search),
    Destination("watchlist", "Watchlist", Icons.Default.FavoriteBorder),
    Destination("downloads", "Downloads", Icons.Default.ArrowDropDown),
    Destination("browser", "Browser", Icons.Default.Menu))

@Composable
fun NexusApp() {
    var sourceUrl by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    var protoTab by rememberSaveable { mutableIntStateOf(1) }
    val prototype=route=="home" || route=="search" || route=="results"
    var reduceMotion by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var audio by rememberSaveable { mutableStateOf("Any") }
    var type by rememberSaveable { mutableStateOf("All") }
    val filters = SearchFilters(MediaType.entries.firstOrNull { it.name == type }, year.toIntOrNull(), audio.takeUnless { it == "Any" })
    val results = remember(query, year, audio, type) { Discovery.search(query.take(200), Samples, filters) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Background, surface = Panel,
        onPrimary = Color.White, onBackground = Color(0xFFF5F5F7), onSurface = Color(0xFFF5F5F7))) {
        Scaffold(containerColor = Background, bottomBar = {
            if(!prototype) NexusDock(route, reduceMotion) { destination ->
                navigation.navigate(destination) {
                    popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true
                }
            }
        }) { inset ->
            Column(Modifier.padding(inset).fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Background,Color(0xFF1C0A10),Color(0xFF320D16)))).padding(horizontal = if(prototype) 0.dp else 14.dp)) {
                if(!prototype) Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("N", color = Red, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("N E X U S", fontWeight = FontWeight.Bold, fontSize = 23.sp)
                        Text("Universal Media Discovery Browser", color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { navigation.navigate("settings") { launchSingleTop = true } }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
                NavHost(navigation, startDestination = "home", modifier = Modifier.weight(1f),
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None }) {
                    listOf("home", "search").forEach { screen ->
                        composable(screen) {
                            Prototype(protoTab,{protoTab=it},{ title ->
                                submitted = title
                                navigation.navigate("results")
                            },{sourceUrl=it;navigation.navigate("browser")},{navigation.navigate("settings")})
                        }
                    }
                    composable("results") { Prototype(1,{ protoTab=it;navigation.popBackStack() },{submitted=it},{sourceUrl=it;navigation.navigate("browser")},{navigation.navigate("settings")},submitted) }
                    composable("watchlist") { SavedSources { sourceUrl=it; navigation.navigate("browser") } }
                    composable("downloads") { EmptyPage("Downloads", "No authorized downloads yet. Download controls will appear only after permission and transport checks are implemented.", Icons.Default.ArrowDropDown) }
                    composable("browser") { if(sourceUrl.isEmpty()) Text("Open a source from search results.") else CatalogueBrowser(sourceUrl) { navigation.popBackStack() } }
                    composable("history") { EmptyPage("Search history", "This preview does not persist queries. Local history and deletion controls are planned for the data phase.", Icons.Default.Search) }
                    composable("source") { EmptyPage("Source details", "Sample metadata only. No source URL, viewing availability, audio language or security check is associated with this item.", Icons.Default.Info) }
                    composable("warning") { EmptyPage("Security warning", "Warning screen preview. No live destination has been assessed.", Icons.Default.Warning) }
                    composable("settings") {
                        Page {
                            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("Reduce motion"); Text("Limit dock movement", color = Muted) }
                                Switch(reduceMotion, { reduceMotion = it })
                            }
                            InfoPanel("On your device", "Title search runs on your phone. Opening a source connects to that website.")
                            Text("Designed for Android 10–16", color = Muted)
                            TextButton(onClick = { navigation.navigate("warning") }) { Text("Preview security warning") }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}
@Composable private fun Eyebrow(text: String) { Text(text, color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
@Composable private fun InfoPanel(title: String, body: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = Muted, lineHeight = 22.sp)
    }
}
@Composable private fun EmptyPage(title: String, description: String, icon: ImageVector) {
    Page { Spacer(Modifier.height(24.dp)); Icon(icon, null, tint = Red, modifier = Modifier.size(44.dp)); Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold); InfoPanel("Nothing here yet", description) }
}
@Composable private fun FilterRow(options: List<String>, selected: String, select: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(selected == option, { select(option) }, label = { Text(option.lowercase().replaceFirstChar(Char::titlecase)) }) }
    }
}
@Composable private fun MediaCard(item: MediaTitle, details: () -> Unit) {
    Surface(onClick = details, shape = RoundedCornerShape(20.dp), color = Panel) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp, 76.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF382027)), contentAlignment = Alignment.Center) {
                Text(item.title.take(1), color = Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${item.year} · ${item.type.name.lowercase()}", color = Muted)
                Text("Sample · Metadata only", color = Muted, fontSize = 12.sp)
            }
        }
    }
}
@Composable private fun NexusDock(route: String, reduceMotion: Boolean, navigate: (String) -> Unit) {
    val index = Destinations.indexOfFirst { it.route == route }.let { if (route == "results") 1 else it }
    val position by animateFloatAsState(index.coerceAtLeast(0).toFloat(), if (reduceMotion) snap() else spring(dampingRatio = 0.85f), label = "dock position")
    Box(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).clip(RoundedCornerShape(30.dp))
        .background(Panel).border(1.dp, Color(0xFF44202B), RoundedCornerShape(30.dp))) {
        if (index >= 0) Canvas(Modifier.matchParentSize()) {
            val slot = size.width / 5
            val center = slot * (position + 0.5f)
            val base = size.height - 3.dp.toPx()
            val path = Path().apply {
                moveTo(0f, base); lineTo(center - slot * 0.55f, base)
                cubicTo(center - slot * 0.3f, base, center - slot * 0.4f, 3.dp.toPx(), center, 3.dp.toPx())
                cubicTo(center + slot * 0.4f, 3.dp.toPx(), center + slot * 0.3f, base, center + slot * 0.55f, base)
                lineTo(size.width, base)
            }
            drawPath(path, Red.copy(alpha = 0.75f), style = Stroke(1.5.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Destinations.forEachIndexed { i, destination ->
                Column(Modifier.weight(1f).fillMaxHeight().selectable(index == i, role = Role.Tab, onClick = { navigate(destination.route) })
                    .padding(horizontal = 2.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(destination.icon, null, tint = if (index == i) Red else Muted)
                    Spacer(Modifier.height(6.dp))
                    Text(destination.label, fontSize = 10.sp, color = if (index == i) Color.White else Muted)
                }
            }
        }
    }
}








