package com.driveatlas

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject

/** Explicit requests only, one shared serial queue, no private coordinates cached or logged. */
internal object EtaService {
 private val worker=Executors.newSingleThreadExecutor()
 private var lastStart=0L
 suspend fun calculate(request:EtaRequest):EtaEstimate=suspendCancellableCoroutine{continuation->
  val connection=AtomicReference<HttpURLConnection?>()
  continuation.invokeOnCancellation{connection.get()?.disconnect()}
  worker.execute {
   if(!continuation.isActive)return@execute
   try {
    if(!BuildConfig.DEBUG)throw EtaUnavailable("ProductionProviderRequired")
    val wait=1100-(System.nanoTime()-lastStart)/1_000_000
    if(wait>0)Thread.sleep(wait)
    if(!continuation.isActive)return@execute
    val conn=URL(etaUrl(request)).openConnection() as HttpURLConnection;connection.set(conn)
    if(!continuation.isActive){conn.disconnect();return@execute}
    conn.instanceFollowRedirects=false;conn.connectTimeout=8000;conn.readTimeout=12000
    conn.setRequestProperty("User-Agent","DriveAtlasPrototype/0.9 (Android; private noncommercial test; https://github.com/Arrayst/drive-atlas-connected)")
    lastStart=System.nanoTime()
    val status=conn.responseCode
    if(status==429)throw EtaUnavailable("RateLimit")
    if(status!=200&&status!=400)throw EtaUnavailable("Network")
    val stream=if(status==200)conn.inputStream else conn.errorStream?:throw EtaUnavailable("InvalidResponse")
    val body=stream.use{input->val bytes=input.readBytesLimited(262144);bytes.toString(Charsets.UTF_8)}
    val result=parseEta(request,body,System.currentTimeMillis())
    if(continuation.isActive)continuation.resume(result)
   }catch(e:Exception){
    val error=when(e){is EtaUnavailable->e;is SocketTimeoutException->EtaUnavailable("Timeout");is IOException->EtaUnavailable("Network");else->EtaUnavailable("InvalidResponse")}
    if(continuation.isActive)continuation.resumeWithException(error)
   }finally{connection.get()?.disconnect()}
  }
 }
}
private fun java.io.InputStream.readBytesLimited(max:Int):ByteArray {
 val out=java.io.ByteArrayOutputStream();val buf=ByteArray(4096)
 while(true){val n=read(buf);if(n<0)break;if(out.size()+n>max)throw EtaUnavailable("InvalidResponse");out.write(buf,0,n)}
 return out.toByteArray()
}
internal class EtaCache(context:Context) {
 private val prefs=context.getSharedPreferences("atlas_public_eta_v1",Context.MODE_PRIVATE)
 fun save(request:EtaRequest,value:EtaEstimate){
  val key=publicEtaCacheKey(request)?:return
  if(!request.valid()||!value.valid()||value.routeId!=request.routeId)return
  val j=JSONObject().put("route",value.routeId).put("origin",value.originLabel).put("out",value.outboundSeconds).put("course",value.courseSeconds).put("back",value.returnSeconds).put("meters",value.meters).put("at",value.calculatedAt)
  check(prefs.edit().putString(key,j.toString()).commit())
 }
 fun load(publicId:String?,routeId:String,now:Long):EtaEstimate? {
  if(publicId==null||DepartureReferences.all.none{it.id==publicId}||Catalog.routes.none{it.id==routeId})return null
  return runCatching{val raw=prefs.getString(publicId+":"+routeId,null)?:return null;val j=JSONObject(raw)
   EtaEstimate(j.getString("route"),j.getString("origin"),j.getDouble("out"),j.getDouble("course"),j.getDouble("back"),j.getDouble("meters"),j.getLong("at"),true)
  }.getOrNull()?.takeIf{it.routeId==routeId&&freshCachedEta(it,now)}
 }
}
