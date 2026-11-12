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
internal fun vectorMapConnectionAllowed(purpose: VectorMapPurpose, debug: Boolean, simulator: Boolean, consent: Boolean) =
 consent && (purpose == VectorMapPurpose.PUBLIC_COURSE || debug && simulator)

/** The native view exists only inside the accepted branch, and is disposed on withdrawal. */
@Composable internal fun VectorMapConnectionGate(
 purpose: VectorMapPurpose, consent: VectorMapConsentState, modifier: Modifier = Modifier,
 controlsEnabled: Boolean = true, onRequestConsent: (() -> Unit)? = null, content: @Composable () -> Unit,
) {
 var showConsent by remember { mutableStateOf(false) }
 val permitted = vectorMapConnectionAllowed(purpose, BuildConfig.DEBUG, BuildConfig.SIMULATOR, consent.granted)
 LaunchedEffect(controlsEnabled) { if (!controlsEnabled) showConsent = false }
 Box(modifier.clipToBounds().background(Color(0xFFEEF0EF))) {
  if (permitted) content()
  else Surface(Modifier.align(Alignment.Center).padding(16.dp).widthIn(max = 560.dp), color = White, shape = Corner) {
   Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("실제 도로와 건물, 벡터 지도", color = Ink, fontSize = 21.sp)
    Text("공개 코스·기준점 지도용 연결을 한 번 동의하면 이 기기에 기억합니다. 실제 GPS는 사용하지 않습니다.", color = Muted, fontSize = 16.sp)
    Button(onClick = { if (controlsEnabled) { if (onRequestConsent != null) onRequestConsent() else showConsent = true } }, enabled = controlsEnabled && (purpose == VectorMapPurpose.PUBLIC_COURSE || BuildConfig.DEBUG && BuildConfig.SIMULATOR), modifier = Modifier.heightIn(min = 64.dp)) { Text("벡터 지도 연결", fontSize = 19.sp) }
    if (consent.error.isNotEmpty()) Text(consent.error, color = Ink, fontSize = 16.sp)
   }
  }
 }
 if (showConsent) VectorMapConsentDialog(consent, controlsEnabled, onDismiss = { showConsent = false }, onAccepted = { showConsent = false })
}

@Composable internal fun VectorMapConsentDialog(
 consent: VectorMapConsentState, controlsEnabled: Boolean,
 onDismiss: () -> Unit, onAccepted: () -> Unit = onDismiss, onWithdrawalResult: (Boolean) -> Unit = {},
) {
 val context = LocalContext.current
 AlertDialog(
  onDismissRequest = onDismiss,
  title = { Text(vectorMapConsentDialogTitle(consent.granted, consent.withdrawalPending)) },
  text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("범위: 공개 코스·기준점 지도 표시. 코스 탐색과 에뮬레이터 SIM 경로 화면에 함께 적용됩니다. SIM 재생은 개발용 에뮬레이터에서만 가능합니다.")
   Text("표시하는 공개 지역과 IP 주소가 OpenFreeMap 및 Cloudflare에 전달됩니다. 실제 위치·집 주소·가족 정보는 보내지 않습니다.")
   Text("인터넷 연결이 필요하며 서비스 가동·최신성은 보장되지 않습니다. 실시간 교통·오프라인 지도·실제 운전 안내가 아닙니다. OSRM 경로 계산 동의와는 별개입니다.")
   Text("동의는 이 기기에 저장되어 같은 지도에 다시 묻지 않습니다. 제공자·목적·고지 버전이 바뀌면 다시 확인합니다. 지도 이용 안내 또는 SIM 메뉴에서 언제든 연결 동의를 철회할 수 있습니다.")
   Text("연결하면 아래 이용약관이 적용됩니다.")
   Row {
    TextButton(onClick = { if (controlsEnabled) openWeb(context, NavigationVectorStyle.terms) }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("이용약관") }
    TextButton(onClick = { if (controlsEnabled) openWeb(context, NavigationVectorStyle.privacy) }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("개인정보 안내") }
   }
   if (consent.error.isNotEmpty()) Text(consent.error, color = Ink)
  } },
  confirmButton = {
   if (consent.granted || consent.withdrawalPending) TextButton(onClick = { if (controlsEnabled) { val saved = consent.withdraw(); onWithdrawalResult(saved); if (saved) onDismiss() } }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text(if (consent.withdrawalPending) "철회 다시 시도" else "지도 연결 동의 철회") }
   else TextButton(onClick = { if (controlsEnabled && consent.accept()) onAccepted() }, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 64.dp)) { Text("동의하고 연결 기억") }
  },
  dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 64.dp)) { Text(if (consent.granted || consent.withdrawalPending) "닫기" else "취소") } },
 )
}
