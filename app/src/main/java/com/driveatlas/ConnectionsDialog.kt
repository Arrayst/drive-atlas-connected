package com.driveatlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable internal fun ConnectionsDialog(state: ConnectionState, onDismiss: () -> Unit, onRetry: () -> Unit, onLocationPermission: () -> Unit) {
 Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
  Surface(Modifier.widthIn(max = 900.dp).padding(24.dp), color = Paper, shape = Corner) {
   Column(Modifier.verticalScroll(rememberScrollState()).padding(32.dp)) {
    PageHeading("PLEOS CONNECTION / 06", "연결 상태", if (state.emulator) "에뮬레이터 서비스의 실제 응답 · 실차 주행/방문 증거 아님" else "차량 SDK 응답 · 위치만으로 방문을 인증하지 않음")
    NoteBlock("차량", state.vehicle)
    Spacer(Modifier.height(16.dp)); NoteBlock("기본 내비", state.navigation)
    Spacer(Modifier.height(16.dp)); NoteBlock("위치", state.location)
    if (state.safety == Safety.PARKED) state.coordinates?.let { Text(it, color = Ink, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp)) }
    if (state.safety == Safety.PARKED) state.visitCheck?.let { Text(it, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
    Text("차량 상태 미확인과 주행 제한을 구분합니다. P 기어와 플랫폼 UX 제한 해제를 모두 확인해야 탐색을 엽니다. 위치는 앱 안에서 코스 지점과만 비교하며 외부 지도에 보내지 않습니다. 정확도와 원본 위치 시각이 검증되지 않아 우표나 방문 인증으로 쓰지 않습니다.", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(vertical = 20.dp))
    if (state.safety == Safety.PARKED) {
     SecondaryButton("위치 사용 권한 요청", onLocationPermission, Modifier.fillMaxWidth())
     Spacer(Modifier.height(12.dp))
    }
    SecondaryButton("연결 다시 확인", onRetry, Modifier.fillMaxWidth())
    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("닫기", fontSize = 18.sp) }
   }
  }
 }
}
