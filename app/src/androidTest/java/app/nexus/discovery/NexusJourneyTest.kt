package app.nexus.discovery

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NexusJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun duneMovieSource() = checkSource("Movies","Dune: Part Two","www.warnerbros.com","Dune")
    @Test fun tomJerryCartoonSource() = checkSource("Cartoons","Tom and Jerry","www.warnerbros.com","Jerry")
    @Test fun inceptionMovieSource() = checkSource("Movies","Inception","www.warnerbros.com","Inception")
    @Test fun blueyCartoonSource() = checkSource("Cartoons","Bluey","www.bluey.tv","Bluey")
    @Test fun narutoSource() = checkSource("Anime","Naruto","myanimelist.net","Naruto")
    @Test fun overflowSource() = checkSource("Anime","Overflow","myanimelist.net","Overflow")
    @Test fun copySourceLinks() {
        compose.onNode(hasSetTextAction()).performTextInput("Inception")
        compose.onNodeWithText("Search titles").performClick()
        compose.waitUntil(30000){compose.onAllNodesWithText("Copy all source links",substring=true).fetchSemanticsNodes().isNotEmpty()}
        fun clipboard()=compose.runOnIdle {
            compose.activity.getSystemService(android.content.ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text.toString()
        }
        compose.onNodeWithText("Copy all source links",substring=true).performClick()
        val all=clipboard().lines()
        org.junit.Assert.assertTrue(all.isNotEmpty() && all.all(::validSource) && all==all.distinct())
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Copy link"))
        compose.onAllNodesWithText("Copy link").onFirst().performClick()
        org.junit.Assert.assertTrue(clipboard() in all)
    }
    @Test fun documentedDub() {
        compose.onNode(hasSetTextAction()).performTextInput("Solo Leveling Hindi dub")
        compose.onNodeWithText("Search titles").performClick()
        compose.waitUntil(20000){ compose.onAllNodesWithText("Hindi dub documented",substring=true).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun checkSource(category:String,query:String,host:String,expected:String) {
        compose.onNodeWithText(category).performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput(query)
        compose.onNodeWithText("Search titles").performClick()
        compose.waitUntil(20000){ compose.onAllNodesWithText("Open "+host).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Open "+host).onFirst().performScrollTo().performClick()
        fun find(view:android.view.View):android.webkit.WebView? {
            if(view is android.webkit.WebView) return view
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let{return it}
            return null
        }
        compose.waitUntil(45000) {
            compose.runOnUiThread { find(compose.activity.window.decorView)?.title?.contains(expected,ignoreCase=true)==true }
        }
    }
    @Test fun overflowSearchAndWatchlist() {
        compose.onNode(hasSetTextAction()).performTextInput("overflow")
        compose.onNodeWithText("Search titles").performClick()
        compose.waitUntil(20000) {
            compose.onAllNodesWithText("2020 · ONA · Metadata only").fetchSemanticsNodes().isNotEmpty()
        }
        val card = hasAnyChild(hasText("2020 · ONA · Metadata only"))
        val save = hasText("Save to Watchlist") and hasAnyAncestor(card)
        val alreadySaved = compose.onAllNodes(hasText("Remove from Watchlist") and hasAnyAncestor(card)).fetchSemanticsNodes().isNotEmpty()
        if (!alreadySaved) compose.onAllNodes(save).onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Watchlist").performClick()
        compose.onNodeWithText("Overflow · 2020").assertIsDisplayed()
        if (!alreadySaved) {
            val item = hasAnyAncestor(hasAnyChild(hasText("Overflow · 2020")))
            compose.onNode(hasText("Remove") and item).performClick()
            compose.onNodeWithText("Overflow · 2020").assertDoesNotExist()
        }
    }
}


