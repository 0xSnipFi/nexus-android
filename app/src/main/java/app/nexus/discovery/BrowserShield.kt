package app.nexus.discovery

import android.content.res.AssetManager

internal class BrowserShield(private val assets: AssetManager) {
    private val domains by lazy {
        val hosts = mutableSetOf("doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "googletagmanager.com", "adservice.google.com", "popads.net", "popcash.net", "adsterra.com",
            "propellerads.com", "exoclick.com", "adnxs.com", "criteo.com")
        assets.open("adaway-hosts.txt").bufferedReader().useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("127.0.0.1")) continue
                val host = line.substringAfter("127.0.0.1").substringBefore("#").trim()
                if (host.contains('.')) hosts.add(host.lowercase())
            }
        }
        hosts
    }

    fun blocks(host: String?): Boolean {
        var candidate = host?.lowercase()?.trimEnd('.') ?: return false
        while (candidate.contains('.')) {
            if (candidate in domains) return true
            candidate = candidate.substringAfter('.')
        }
        return false
    }
}
