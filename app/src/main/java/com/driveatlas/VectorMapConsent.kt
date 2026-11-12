package com.driveatlas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class VectorMapPurpose { PUBLIC_COURSE, SIM_REPLAY }
internal class VectorMapConsentState { var granted by mutableStateOf(false) }
internal fun vectorMapConnectionAllowed(purpose: VectorMapPurpose, debug: Boolean, simulator: Boolean, consent: Boolean) =
 consent && (purpose == VectorMapPurpose.PUBLIC_COURSE || debug && simulator)

/** Shared session-only provider gate. The native view exists only inside the accepted branch. */
@Composable internal fun VectorMapConnectionGate(
 purpose: VectorMapPurpose, consent: VectorMapConsentState, modifier: Modifier = Modifier,
 controlsEnabled: Boolean = true, content: @Composable () -> Unit,
) {
 val context = LocalContext.current
 var showConsent by remember { mutableStateOf(false) }
 val permitted = vectorMapConnectionAllowed(purpose, BuildConfig.DEBUG, BuildConfig.SIMULATOR, consent.granted)
 Box(modifier.clipToBounds().background(Color(0xFFEEF0EF))) {
  if (permitted) content()
  else Surface(Modifier.align(Alignment.Center).padding(16.dp).widthIn(max = 560.dp), color = White, shape = Corner) {
   Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("실제 도로와 건물, 벡터 지도", color = Ink, fontSize = 21.sp)
    Text("OpenFreeMap으로 공개 코스를 표시합니다. 실제 GPS는 사용하지 않습니다.", color = Muted, fontSize = 16.sp)
    Button(onClick = { if (controlsEnabled) showConsent = true }, enabled = controlsEnabled && (purpose == VectorMapPurpose.PUBLIC_COURSE || BuildConfig.DEBUG && BuildConfig.SIMULATOR), modifier = Modifier.heightIn(min = 64.dp)) { Text("벡터 지도 연결", fontSize = 19.sp) }
   }
  }
 }
 if (showConsent) AlertDialog(
  onDismissRequest = { showConsent = false },
  title = { Text("OpenFreeMap 지도 연결") },
  text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("표시하는 공개 코스 지역과 IP 주소가 OpenFreeMap 및 Cloudflare에 전달됩니다. 실제 위치·집 주소·가족 정보는 보내지 않습니다.")
   Text("인터넷 연결이 필요하며 서비스 가동·최신성은 보장되지 않습니다. 실시간 교통·오프라인 지도·실제 운전 안내가 아닙니다. OSRM 경로 계산 동의와는 별개입니다.")
   Text("연결하면 아래 이용약관이 적용됩니다. 동의는 이 지도 화면을 이용하는 동안에만 유지됩니다.")
   Row {
    TextButton(onClick = { if (controlsEnabled) openWeb(context, NavigationVectorStyle.terms) }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("이용약관") }
    TextButton(onClick = { if (controlsEnabled) openWeb(context, NavigationVectorStyle.privacy) }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("개인정보 안내") }
   }
  } },
  confirmButton = { TextButton(onClick = { if (controlsEnabled) { consent.granted = true; showConsent = false } }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("동의하고 지도 연결") } },
  dismissButton = { TextButton(onClick = { showConsent = false }, modifier = Modifier.heightIn(min = 64.dp)) { Text("취소") } },
 )
}
