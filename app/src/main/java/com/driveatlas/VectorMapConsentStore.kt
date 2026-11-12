package com.driveatlas

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Both screens show public reference regions. This scope never includes GPS or private places. */
internal data class VectorMapConsentScope(val provider: String, val purpose: String, val disclosureVersion: Int) {
 val token: String get() = "$provider\n$purpose\n$disclosureVersion"
 companion object {
  val Current = VectorMapConsentScope(
   listOf("OpenFreeMap + Cloudflare", NavigationVectorStyle.tileJson, NavigationVectorStyle.glyphs, NavigationVectorStyle.terms, NavigationVectorStyle.privacy).joinToString("|"),
   "public-course-and-reference-map-including-sim-replay", 2,
  )
 }
}
internal interface VectorConsentStorage {
 fun read(): String?
 fun write(token: String): Boolean
 fun clear(): Boolean
}
/** Separate private file: no family records, route coordinates or location permissions. */
private class LocalVectorConsentStorage(context: Context) : VectorConsentStorage {
 private val file = AtomicFile(File(context.filesDir, "vector_map_consent_v1"))
 override fun read(): String? = runCatching { file.openRead().use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
 override fun write(token: String): Boolean {
  var stream: java.io.FileOutputStream? = null
  return try { stream = file.startWrite(); stream.write(token.toByteArray(Charsets.UTF_8)); file.finishWrite(stream); read() == token }
  catch (_: Exception) { stream?.let { runCatching { file.failWrite(it) } }; false }
 }
 override fun clear(): Boolean = runCatching { file.delete(); !file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists() && !File(file.baseFile.path + ".new").exists() }.getOrDefault(false)
}
internal class VectorMapConsentState(private val storage: VectorConsentStorage, private val scope: VectorMapConsentScope = VectorMapConsentScope.Current) {
 var granted by mutableStateOf(runCatching { storage.read() == scope.token }.getOrDefault(false))
  private set
 var error by mutableStateOf("")
  private set
 var withdrawalPending by mutableStateOf(false)
  private set
 fun accept(): Boolean {
  if (withdrawalPending) return false
  val saved = runCatching { storage.write(scope.token) }.getOrDefault(false)
  granted = saved
  error = if (saved) "" else "지도 동의를 저장하지 못해 연결하지 않았어요. 다시 시도해 주세요."
  return saved
 }
 fun withdraw(): Boolean {
  // Remove the connected composition immediately, even if durable deletion fails.
  granted = false
  val saved = runCatching { storage.clear() }.getOrDefault(false)
  withdrawalPending = !saved
  error = if (saved) "" else "이 화면의 연결은 껐지만 저장된 동의를 삭제하지 못했어요. 앱을 다시 열기 전에 철회를 다시 시도해 주세요."
  return saved
 }
}
private object VectorMapConsentOwner {
 private var state: VectorMapConsentState? = null
 fun get(context: Context): VectorMapConsentState = state ?: VectorMapConsentState(LocalVectorConsentStorage(context.applicationContext)).also { state = it }
}
@Composable internal fun rememberVectorMapConsentState(): VectorMapConsentState {
 val context = LocalContext.current
 return remember(context.applicationContext) { VectorMapConsentOwner.get(context) }
}
