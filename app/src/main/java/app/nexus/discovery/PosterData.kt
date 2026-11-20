package app.nexus.discovery

import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

internal fun posterData(address:String):String? {
    if(!validSource(address) || java.net.URI(address).host!="s4.anilist.co") return null
    val http=URL(address).openConnection() as HttpURLConnection
    return try {
        http.instanceFollowRedirects=false
        http.connectTimeout=2500
        http.readTimeout=3000
        if(http.responseCode!=200) return null
        val body=ByteArrayOutputStream()
        http.inputStream.use { input ->
            val buf=ByteArray(8192)
            while(true){
                val n=input.read(buf)
                if(n<0) break
                if(body.size()+n>512000) return null
                body.write(buf,0,n)
            }
        }
        val bytes=body.toByteArray()
        val opts=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,opts)
        if(opts.outWidth !in 1..2048 || opts.outHeight !in 1..2048 ||
            opts.outMimeType !in listOf("image/jpeg","image/png","image/webp")) return null
        "data:"+opts.outMimeType+";base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP)
    } catch(_:java.io.IOException){null} finally {http.disconnect()}
}
