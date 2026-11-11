package com.driveatlas

internal data class NavigationPresentation(val title: String, val confirmed: Boolean)

// Only the connector's coordinate-verified result is a confirmed route.
internal fun navigationPresentation(status: String): NavigationPresentation = when {
    status.startsWith("내비 경로 완료 · 목적지 + 경유지 ") && status.endsWith("곳 좌표 대조 확인 (약 100m 이내)") ->
        NavigationPresentation("코스 좌표 확인 완료", true)
    status == "선택 구간 내비 확인 · 목적지 좌표 일치 · 전체 코스 연속 안내 아님" ->
        NavigationPresentation("선택 구간 좌표 확인 완료", true)
    listOf("실패", "오류", "거절", "불일치").any { it in status } ->
        NavigationPresentation("연결 결과를 확인해 주세요", false)
    else -> NavigationPresentation("경로 전달 확인 전", false)
}
