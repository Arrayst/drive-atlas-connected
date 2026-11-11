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
 suspend fun calculate(request:EtaRequest):EtaEstimate=suspendCancellableCoroutine{continuation->
  val connection=AtomicReference<HttpURLConnection?>()
  continuation.invokeOnCancellation{connection.get()?.disconnect()}
  worker.execute {
   if(!continuation.isActive)return@execute
   try {
    if(!BuildConfig.DEBUG)throw EtaUnavailable("ProductionProviderRequired")
    PublicRoutingThrottle.awaitTurn()
    if(!continuation.isActive)return@execute
    val conn=URL(etaUrl(request)).openConnection() as HttpURLConnection;connection.set(conn)
    if(!continuation.isActive){conn.disconnect();return@execute}
    conn.instanceFollowRedirects=false;conn.connectTimeout=8000;conn.readTimeout=12000
    conn.setRequestProperty("User-Agent","DriveAtlasPrototype/0.9 (Android; private noncommercial test; https://github.com/Arrayst/drive-atlas-connected)")
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
 // v1 had no itinerary binding. Do not migrate those results as current estimates.
 private val prefs=context.getSharedPreferences("atlas_public_eta_v2",Context.MODE_PRIVATE)
 fun save(request:EtaRequest,value:EtaEstimate){
  val key=publicEtaCacheKey(request)?:return
  if(!request.valid()||!value.valid()||value.routeId!=request.routeId||value.itineraryKey!=request.itineraryKey()||value.selectedRestIds!=request.selectedRestIds||value.originLabel!=request.originLabel)return
  val j=JSONObject().put("route",value.routeId).put("origin",value.originLabel).put("out",value.outboundSeconds).put("course",value.courseSeconds).put("back",value.returnSeconds).put("meters",value.meters).put("at",value.calculatedAt)
   .put("itinerary",value.itineraryKey).put("legs",org.json.JSONArray(value.legSeconds)).put("breaks",org.json.JSONArray(value.restBreakAfterLegs))
   .put("rests",org.json.JSONArray(value.selectedRestIds)).put("slots",JSONObject(value.restAfterStopIndex))
  check(prefs.edit().putString(key,j.toString()).commit())
 }
 fun load(publicId:String?,routeId:String,now:Long):EtaEstimate? {
  val ref=DepartureReferences.all.firstOrNull{it.id==publicId}?:return null
  return load(EtaRequest(routeId,ref.point,ref.label,ref.id),now)
 }
 fun load(request:EtaRequest,now:Long):EtaEstimate? {
  val key=publicEtaCacheKey(request)?:return null
  return runCatching{val raw=prefs.getString(key,null)?:return null;val j=JSONObject(raw)
   val legs=j.getJSONArray("legs");val breaks=j.getJSONArray("breaks");val rests=j.getJSONArray("rests");val slots=j.getJSONObject("slots")
   EtaEstimate(j.getString("route"),j.getString("origin"),j.getDouble("out"),j.getDouble("course"),j.getDouble("back"),j.getDouble("meters"),j.getLong("at"),true,
    j.getString("itinerary"),(0 until legs.length()).map{legs.getDouble(it)},(0 until breaks.length()).map{breaks.getInt(it)},
    (0 until rests.length()).map{rests.getString(it)},slots.keys().asSequence().associateWith{slots.getInt(it)})
  }.getOrNull()?.takeIf{it.routeId==request.routeId&&it.originLabel==request.originLabel&&it.itineraryKey==request.itineraryKey()&&it.selectedRestIds==request.selectedRestIds&&freshCachedEta(it,now)}
 }
}
