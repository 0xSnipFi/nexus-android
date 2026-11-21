package app.nexus.discovery

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal object Transfers {
    private val formats=mapOf("mp4" to "video/mp4","webm" to "video/webm","mkv" to "video/x-matroska","m4v" to "video/mp4","mp3" to "audio/mpeg","pdf" to "application/pdf","ogg" to "audio/ogg","m4a" to "audio/mp4")
    fun isMediaLink(url:String)=validSource(url) && Uri.parse(url).lastPathSegment.orEmpty().substringAfterLast('.',"").lowercase() in formats
    private fun manager(c:Context)=c.getSystemService(DownloadManager::class.java)
    private fun prefs(c:Context)=c.getSharedPreferences("source-downloads",0)
    private fun message(c:Context,s:String)=Toast.makeText(c,s,Toast.LENGTH_SHORT).show()
    fun confirm(c:Context,url:String,mime:String?=null, disposition:String?=null){
        val uri=Uri.parse(url)
        val filename=if(disposition.isNullOrBlank()) uri.lastPathSegment.orEmpty()
            else android.webkit.URLUtil.guessFileName(url,disposition,mime)
        val ext=filename.substringAfterLast('.',"").lowercase()
        val kind=formats[ext]?:formats.values.firstOrNull { it==mime }
        if(!validSource(url)||kind==null){message(c,"Use a direct HTTPS video, audio or PDF file link.");return}
        val suffix=formats.entries.first { it.value==kind }.key
        val name=filename.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N} _-]"),"_").take(72).ifBlank { "Download" }+"."+suffix
        AlertDialog.Builder(c).setTitle("Download file?").setMessage(name+"\nFrom "+uri.host)
            .setNegativeButton("Cancel",null).setPositiveButton("Download"){_,_->
                try {
                    val req=DownloadManager.Request(uri).setTitle(name).setMimeType(kind)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalFilesDir(c,Environment.DIRECTORY_DOWNLOADS,UUID.randomUUID().toString()+"-"+name)
                    val id=manager(c).enqueue(req)
                    prefs(c).edit().putString(id.toString(),name).apply()
                    message(c,"Added to Downloads")
                }catch(_:RuntimeException){message(c,"Could not start download")}
            }.show()
    }
    fun rows(c:Context):JSONArray {
        val result=JSONArray()
        val ids=prefs(c).all.keys.mapNotNull { it.toLongOrNull() }.toLongArray()
        if(ids.isEmpty())return result
        manager(c).query(DownloadManager.Query().setFilterById(*ids))?.use { cursor ->
            fun number(col:String)=cursor.getLong(cursor.getColumnIndexOrThrow(col))
            while(cursor.moveToNext()){
                val id=number(DownloadManager.COLUMN_ID)
                val state=number(DownloadManager.COLUMN_STATUS).toInt()
                val done=number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val total=number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val label=when(state){
                    DownloadManager.STATUS_SUCCESSFUL->"Completed"
                    DownloadManager.STATUS_FAILED->"Failed"
                    DownloadManager.STATUS_RUNNING->if(total>0)"Downloading "+(100*done/total)+"%" else "Downloading"
                    DownloadManager.STATUS_PAUSED->"Waiting for network or retry"
                    else->"Queued"
                }
                result.put(JSONObject().put("id",id).put("title",prefs(c).getString(id.toString(),"File")).put("state",label).put("bytes",done))
            }
        };return result
    }
    fun action(c:Context,id:Long,action:String){
        if(!prefs(c).contains(id.toString()))return
        val dm=manager(c)
        if(action=="removeFile"){
            AlertDialog.Builder(c).setTitle("Remove download?").setMessage("This cancels the transfer or deletes the downloaded file.").setNegativeButton("Keep",null)
                .setPositiveButton("Remove"){_,_->dm.remove(id);prefs(c).edit().remove(id.toString()).apply()}.show()
        }else{
            val uri=dm.getUriForDownloadedFile(id)?:return
            val mime=dm.getMimeTypeForDownloadedFile(id)
            val intent=if(action=="shareFile") Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri) else Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try{c.startActivity(if(action=="shareFile")Intent.createChooser(intent,"Share file") else intent)}catch(_:android.content.ActivityNotFoundException){message(c,"No app available to open this file")}
        }
    }
}
