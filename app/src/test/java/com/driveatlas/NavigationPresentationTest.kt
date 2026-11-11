package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class NavigationPresentationTest {
    @Test fun serviceAndRequestAreNotSuccess() {
        listOf("내비 경로 상태 콜백 수신 · 서비스 응답 확인", "공개 좌표 코스 요청 중 · POI ID 없는 실험 연동 · 성공 미확인",
            "내비 경로 완료 콜백 수신 · 좌표 대조 대기", "내비 오류: UNKNOWN · 안내 성공 아님", "", "성공").forEach {
            assertFalse(it, navigationPresentation(it).confirmed)
        }
    }
    @Test fun onlyCoordinateVerifiedResultsAreConfirmed() {
        assertTrue(navigationPresentation("내비 경로 완료 · 목적지 + 경유지 2곳 좌표 대조 확인 (약 100m 이내)").confirmed)
        val leg = navigationPresentation("선택 구간 내비 확인 · 목적지 좌표 일치 · 전체 코스 연속 안내 아님")
        assertTrue(leg.confirmed)
        assertEquals("선택 구간 좌표 확인 완료", leg.title)
    }
}
