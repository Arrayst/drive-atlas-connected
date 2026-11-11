package com.driveatlas

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-demand public-reference prototype only. No location acquisition, cache or automatic retries. */
internal object NavPrototypeService {
 private val worker=Executors.newSingleThreadExecutor()
 suspend fun calculate(request:NavPrototypeRequest):NavPrototypeRoute=suspendCancellableCoroutine { continuation ->
  val connection=AtomicReference<HttpURLConnection?>()
  continuation.invokeOnCancellation{connection.get()?.disconnect()}
  worker.execute {
   if(!continuation.isActive)return@execute
   try {
    if(!navPrototypeAllowed(BuildConfig.DEBUG,BuildConfig.SIMULATOR))throw EtaUnavailable("ProductionProviderRequired")
    if(!request.valid())throw EtaUnavailable("InvalidInput")
    PublicRoutingThrottle.awaitTurn()
    if(!continuation.isActive)return@execute
    val conn=URL(navPrototypeUrl(request)).openConnection() as HttpURLConnection;connection.set(conn)
    if(!continuation.isActive){conn.disconnect();return@execute}
    conn.instanceFollowRedirects=false;conn.connectTimeout=8000;conn.readTimeout=20000
    conn.setRequestProperty("User-Agent","DriveAtlasPrototype/0.10 (Android; noncommercial public-reference replay)")
    val code=conn.responseCode
    if(code==429)throw EtaUnavailable("RateLimit")
    if(code!=200&&code!=400)throw EtaUnavailable("Network")
    val stream=if(code==200)conn.inputStream else conn.errorStream?:throw EtaUnavailable("InvalidResponse")
    val body=stream.use { input ->
     val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
     while(true){if(!continuation.isActive)throw IOException("Cancelled");val n=input.read(buffer);if(n<0)break;if(out.size()+n>8*1024*1024)throw EtaUnavailable("InvalidResponse");out.write(buffer,0,n)}
     out.toString("UTF-8")
    }
    val value=parseNavPrototype(request,body)
    if(continuation.isActive)continuation.resume(value)
   }catch(e:Exception){val error=when(e){is EtaUnavailable->e;is SocketTimeoutException->EtaUnavailable("Timeout");is IOException->EtaUnavailable("Network");else->EtaUnavailable("InvalidResponse")};if(continuation.isActive)continuation.resumeWithException(error)}
   finally{connection.get()?.disconnect()}
  }
 }
}

/** Shared by all public OSRM clients; screen changes cannot bypass the one-second request budget. */
internal object PublicRoutingThrottle {
 private var lastStart=0L
 @Synchronized fun awaitTurn() {
  val wait=1100-(System.nanoTime()-lastStart)/1_000_000
  if(wait>0)Thread.sleep(wait)
  lastStart=System.nanoTime()
 }
}
