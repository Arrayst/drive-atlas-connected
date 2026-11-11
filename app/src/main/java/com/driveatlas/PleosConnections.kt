package com.driveatlas

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.*
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

internal data class ConnectionState(
    val safety: Safety = Safety.UNKNOWN,
    val vehicle: String = "차량 서비스 연결 대기",
    val navigation: String = "내비 서비스 연결 대기",
    val location: String = "위치 권한 승인 전 · 읽지 않음",
    val coordinates: String? = null,
    val visitCheck: String? = null,
    val emulator: Boolean = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"
)

/** SDK events are real IPC, but an emulator is not a real vehicle or visit certificate. */
internal class PleosConnections(private val context: Context, private val vehicleOnly: Boolean = false, private val changed: (ConnectionState) -> Unit) {
    companion object { const val LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION" }
    private val handler = Handler(Looper.getMainLooper())
    var state = ConnectionState(); private set
    private var active = false
    private var destroyed = false
    private var generation = 0
    private var car: Car? = null
    private var property: CarPropertyManager? = null
    private var ux: CarUxRestrictionsManager? = null
    private var gear: Int? = null
    private var requiresDo: Boolean? = null
    private var restrictions: Int? = null
    private val vehicleReader = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var refreshPending = false
    private var vehicleVersion = 0
    private var lastVehicleRead = 0L
    private var navi: NaviHelper? = null
    private var naviReady = false
    private var requestToken = 0
    private var pendingRoute: Route? = null
    private var routeConfirmed = false
    private var routeStarted = false
    private var routeVerification: NativeRouteVerification? = null
    private var fused: FusedLocation? = null
    private var locationReceivedAt = 0L
    private fun post(block: () -> Unit) { handler.post { if (!destroyed) block() } }
    private fun publish(next: ConnectionState) { if (next != state) { state = next; changed(next) } }
    private fun nav(message: String) { Log.i("AtlasConnection", "navigation: $message"); publish(state.copy(navigation = message)) }
    private val propertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>?) { post {
            if (active && value?.propertyId == VehiclePropertyIds.GEAR_SELECTION) {
                vehicleVersion++
                val previousGear = gear
                gear = if (value.status == CarPropertyValue.STATUS_AVAILABLE) value.value as? Int else null
                if (gear == 4 && previousGear != 4) requiresDo = null // Recheck platform UX before reopening on a P transition.
                updateSafety(); refreshVehicle()
            }
        } }
        override fun onErrorEvent(propertyId: Int, areaId: Int) { post { if (active && propertyId == VehiclePropertyIds.GEAR_SELECTION) { vehicleVersion++; gear = null; updateSafety("기어 신호 오류") } } }
    }
    private val poll = object : Runnable { override fun run() {
        if (!active) return
        if (lastVehicleRead > 0 && SystemClock.elapsedRealtime() - lastVehicleRead > 6000) {
            gear = null; requiresDo = null; restrictions = null; updateSafety("차량 신호 갱신 지연 · 안전 확인 대기")
        }
        refreshVehicle()
        if (fused != null && context.checkSelfPermission(LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            disconnectLocation()
            publish(state.copy(location = "위치 권한 없음 · 읽지 않음", coordinates = null, visitCheck = null))
        }
        if (locationReceivedAt > 0 && SystemClock.elapsedRealtime() - locationReceivedAt > 15_000) {
            publish(state.copy(location = "위치 콜백 15초 이상 없음 · 오래된 값 사용 안 함", coordinates = null, visitCheck = null))
        }
        handler.postDelayed(this, 2000)
    } }
    fun start() {
        if (active || destroyed) return
        active = true
        val currentGeneration = ++generation
        publish(state.copy(safety = Safety.UNKNOWN, vehicle = "차량 서비스 연결 중"))
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready -> post {
                if (!active || generation != currentGeneration) return@post
                if (!ready) { vehicleVersion++; lastVehicleRead = 0; property = null; ux = null; gear = null; requiresDo = null; restrictions = null; updateSafety("차량 서비스 연결 끊김"); return@post }
                try {
                    property = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                    ux = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                    ux?.registerListener { value -> post {
                        if (active && generation == currentGeneration) {
                            vehicleVersion++
                            requiresDo = value.isRequiresDistractionOptimization
                            restrictions = value.activeRestrictions
                            updateSafety() // Apply new restrictions before any vehicle IPC.
                            refreshVehicle()
                        }
                    } }
                    property?.registerCallback(propertyListener, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
                    refreshVehicle()
                } catch (error: Exception) { gear = null; requiresDo = null; restrictions = null; updateSafety("차량 신호 접근 실패: ${error.javaClass.simpleName}") }
            } }
        } catch (error: Exception) { updateSafety("차량 서비스 없음: ${error.javaClass.simpleName}") }
        handler.post(poll)
        if (!vehicleOnly) { connectNavigation(); connectLocationIfGranted() }
    }
    private fun refreshVehicle() {
        if (!active || refreshPending) return
        val manager = property ?: return
        val uxManager = ux ?: return
        val expectedGeneration = generation
        val expectedVersion = vehicleVersion
        refreshPending = true
        vehicleReader.execute {
            val result = runCatching {
                val gearValue = manager.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0)
                val value = uxManager.currentCarUxRestrictions
                Triple(if (gearValue.status == CarPropertyValue.STATUS_AVAILABLE) gearValue.value else null,
                    value?.isRequiresDistractionOptimization, value?.activeRestrictions)
            }
            post {
                refreshPending = false
                if (!active || generation != expectedGeneration || vehicleVersion != expectedVersion || property !== manager || ux !== uxManager) return@post
                result.onSuccess { (newGear, newRequiresDo, newRestrictions) ->
                    gear = newGear; requiresDo = newRequiresDo; restrictions = newRestrictions
                    lastVehicleRead = SystemClock.elapsedRealtime()
                    updateSafety()
                }.onFailure { error ->
                    gear = null; requiresDo = null; restrictions = null
                    updateSafety("차량 신호 갱신 실패: ${error.javaClass.simpleName}")
                }
            }
        }
    }
    private fun updateSafety(error: String? = null) {
        val safety = vehicleSafety(active && property != null && ux != null, gear, requiresDo, restrictions)
        val label = when (gear) { 4 -> "P"; 8 -> "D"; 2 -> "R"; 1 -> "N"; else -> gear?.toString() ?: "미확인" }
        val status = error ?: "기어 $label · 주의분산 제한 ${requiresDo ?: "미확인"} · UX ${restrictions ?: "미확인"}"
        if (state.safety != safety || state.vehicle != status) Log.i("AtlasConnection", "vehicle: $safety / $status")
        publish(state.copy(safety = safety, vehicle = status))
    }
    fun stop() {
        active = false; generation++; vehicleVersion++; lastVehicleRead = 0
        handler.removeCallbacks(poll)
        runCatching { property?.unregisterCallback(propertyListener) }
        runCatching { ux?.unregisterListener() }
        runCatching { car?.disconnect() }
        car = null; property = null; ux = null; gear = null; requiresDo = null; restrictions = null
        disconnectLocation()
        publish(state.copy(safety = Safety.UNKNOWN, vehicle = "화면 복귀 시 차량 신호 다시 확인", coordinates = null, visitCheck = null))
    }
    fun retry() {
        stop()
        requestToken++; pendingRoute = null; routeConfirmed = false
        runCatching { navi?.removeListener(naviListener) }; runCatching { navi?.release() }
        navi = null; naviReady = false
        publish(state.copy(navigation = "내비 서비스 재연결 중"))
        start()
    }
    fun destroy() { stop(); destroyed = true; vehicleReader.shutdownNow(); handler.removeCallbacksAndMessages(null); runCatching { navi?.removeListener(naviListener) }; runCatching { navi?.release() }; navi = null }

    private val naviListener = object : NaviHelperEventListener {
        override fun onNavigationStatus(status: NavigationStatus) { post { if (pendingRoute == null && !routeConfirmed) nav("내비 콜백 수신: ${status.name} · 차량 주차 판정과 별개") } }
        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) { post {
            if (!routeStateInfo.isRouting && pendingRoute == null) routeConfirmed = false
            if (pendingRoute == null && !routeConfirmed) nav("내비 경로 상태 콜백 수신 · 서비스 응답 확인")
        } }
        override fun onDestinationChanged() { post {
            routeVerification?.invalidateDestination()
            if (pendingRoute == null) { routeConfirmed = false; nav("내비 목적지 변경됨 · 코스 대조 다시 필요") }
        } }
        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) { post {
            routeVerification?.invalidateWaypoints()
            if (pendingRoute == null) { routeConfirmed = false; nav("내비 경유지 변경됨 · 코스 대조 다시 필요") }
        } }
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { post { if (pendingRoute != null) { routeStarted = true; nav("내비가 경로 탐색 시작을 알림 · 완료 대기") } } }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onRouteEnded() { onRoutingCompleted() } // Installed 3.0.4 provider emits the legacy completion event.
        override fun onRoutingCompleted() { post { if (pendingRoute != null) {
            routeVerification?.complete()
            nav("내비 경로 탐색 완료 콜백 · 목적지/경유지 대조 중")
            try {
                val helper = checkNotNull(navi)
                helper.getDestinationInfo(); helper.getWaypointInfo()
            } catch (error: Exception) {
                requestToken++; pendingRoute = null; routeConfirmed = false
                nav("내비 경로 정보 조회 실패: ${error.javaClass.simpleName} · 코스 확인 보류")
            }
        } } }
        override fun onDestinationInfo(destinationInfo: DestinationInfo) { post { pendingRoute?.let { route ->
            val point = Point(destinationInfo.latitude, destinationInfo.longitude)
            routeVerification?.destination(point)
            Log.i("AtlasConnection", "destination check: expectedLast=${routeVerification?.destinationMatches}, matchedCourseIndex=${route.stops.indexOfFirst { nativePointMatches(it.point, point) }}")
            checkRouteResult()
        } } }
        override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) { post { pendingRoute?.let { route ->
            routeVerification?.waypoints(waypointInfoList.map { Point(it.latitude, it.longitude) })
            Log.i("AtlasConnection", "waypoint check: count=${waypointInfoList.size}, orderedMatch=${routeVerification?.waypointsMatch}, courseIndexes=${waypointInfoList.map { waypoint -> route.stops.indexOfFirst { nativePointMatches(it.point, Point(waypoint.latitude, waypoint.longitude)) } }}")
            checkRouteResult()
        } } }
        override fun onError(errorCode: NaviErrorCode) { post { requestToken++; pendingRoute = null; routeConfirmed = false; nav("내비 오류: ${errorCode.name} · 안내 성공 아님") } }
        override fun onRouteCancelled() { post {
            if (pendingRoute != null && !routeStarted) {
                nav("기존 경로 취소 콜백 · 새 코스 요청 결과 대기")
            } else { requestToken++; pendingRoute = null; routeConfirmed = false; nav("내비 경로 취소 콜백 수신") }
        } }
    }
    private fun checkRouteResult() {
        if (routeVerification?.confirmed == true && pendingRoute != null) {
            val count = pendingRoute!!.stops.size - 1
            requestToken++; pendingRoute = null; routeConfirmed = true
            nav(if(count==0) "선택 구간 내비 확인 · 목적지 좌표 일치 · 전체 코스 연속 안내 아님" else "내비 경로 완료 · 목적지 + 경유지 ${count}곳 좌표 대조 확인 (약 100m 이내)")
        }
    }
    private fun connectNavigation() {
        if (navi != null) return
        try { navi = NaviHelper(context).also { helper -> helper.initialize(onConnected = { post {
            if (navi !== helper) return@post
            naviReady = true; helper.addListener(naviListener); nav("NaviHelper 초기 연결 확인 · 내비 응답 대기")
            helper.getRouteStateInfo()
        } }, onFailed = { error -> post { if (navi !== helper) return@post; naviReady = false; nav("내비 연결 실패: ${error.javaClass.simpleName}") } }) } }
        catch (error: Exception) { naviReady = false; nav("내비 SDK 초기화 실패: ${error.javaClass.simpleName}") }
    }
    fun openNativeSearch(route: Route) {
        if (state.safety != Safety.PARKED) return
        if (!naviReady) { nav("내비 연결되지 않음 · 재연결 후 시도하세요"); return }
        try { navi?.requestNavigationScreen(NavigationScreenRequest.SearchPlace(route.stops.last().name)); nav("내비에 목적지 검색 화면 요청 · 경로 시작/화면 표시 확인은 별도") }
        catch (error: Exception) { nav("내비 검색 요청 실패: ${error.javaClass.simpleName}") }
    }
    fun showNative() {
        if (state.safety != Safety.PARKED) return
        // The emulator provider ignores some screen requests. Opening its verified launcher
        // is a separate, explicit action and does not claim that a route was accepted.
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("ai.umos.maps.android.navigation.app")
            if (intent != null) {
                context.startActivity(intent)
                nav("설치된 기본 내비 앱 열기 · 경로 전달 성공과 별개")
            } else if (naviReady) {
                navi?.requestNavigationScreen(NavigationScreenRequest.Foreground)
                nav("내비 화면 요청 전송 · 화면 표시 확인은 별도")
            } else nav("열 수 있는 기본 내비 앱을 찾지 못함")
        } catch (error: Exception) { nav("내비 앱 열기 실패: ${error.javaClass.simpleName}") }
    }
    fun requestNativeLeg(route: Route, index: Int) {
        val stop=route.stops.getOrNull(index) ?: return
        requestNativeRoute(route.copy(id=route.id+"-leg-"+index,title=route.title+" · "+(index+1)+"구간",stops=listOf(stop)))
    }
    fun requestNativeRoute(route: Route) {
        if (pendingRoute != null) { nav("이전 코스 요청 응답 대기 중 · 중복 요청하지 않음"); return }
        if (state.safety != Safety.PARKED || !naviReady) { nav("주차 확인 및 내비 연결이 필요합니다"); return }
        if (route.stops.size !in 1..3 || route.stops.any { !validNativePoint(it.point) }) {
            nav("유효한 목적지와 최대 경유지 2곳이 필요합니다 · 요청하지 않음"); return
        }
        val token = ++requestToken
        pendingRoute = route; routeConfirmed = false; routeStarted = false
        routeVerification = NativeRouteVerification(route.stops.map { it.point })
        val destination = route.stops.last()
        // Public road coordinates only. Empty IDs are not fabricated POIs; support must be confirmed by callbacks.
        val request = RouteWithWaypointsRequest(RouteInfo(latitude = destination.point.lat, longitude = destination.point.lon,
            poiName = destination.name, poiId = "", address = "", poiSubId = ""),
            route.stops.dropLast(1).mapIndexed { i, stop -> RequestWaypointInfo(latitude = stop.point.lat, longitude = stop.point.lon,
                poiName = stop.name, waypointIndex = if (i == 0) WaypointIndex.FIRST else WaypointIndex.SECOND,
                poiId = "", address = "", poiSubId = "") })
        nav(if(route.stops.size==1) "선택 구간 목적지 요청 중 · 완료·좌표 확인 대기" else "공개 좌표 코스 요청 중 · POI ID 없는 실험 연동 · 성공 미확인")
        try { if(route.stops.size==1)navi?.requestRoute(request.destination) else navi?.requestRouteWithWaypoints(request) }
        catch (error: Exception) { pendingRoute = null; nav("내비 요청 실패: ${error.javaClass.simpleName}") }
        handler.postDelayed({ if (!destroyed && requestToken == token && pendingRoute != null) {
            pendingRoute = null
            fun result(value: Boolean?) = when (value) { true -> "일치"; false -> "대조 불일치"; null -> "응답 미확인" }
            nav(if (routeVerification?.completed == true) "내비 경로 생성 응답 수신 · 목적지 ${result(routeVerification?.destinationMatches)} / 경유지 ${result(routeVerification?.waypointsMatch)} · 전체 코스 확인 보류"
                else "30초 내 경로·목적지·경유지 확인 불가 · 안내 성공으로 처리하지 않음")
        } }, 30_000)
    }
    private var locationListener: FusedLocationListener? = null
    private fun newLocationListener(expectedGeneration: Int) = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) { post {
            if (!active || generation != expectedGeneration || context.checkSelfPermission(LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) return@post
            if (location == null || !location.latitude.isFinite() || !location.longitude.isFinite() || location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                publish(state.copy(location = "유효한 위치가 아직 없음", coordinates = null, visitCheck = null)); return@post
            }
            locationReceivedAt = SystemClock.elapsedRealtime()
            // Receipt freshness only; the SDK timestamp clock/accuracy are not documented.
            publish(state.copy(location = "${if(state.emulator) "에뮬레이터" else "SDK"} 위치 수신 · 정확도(m) 미제공 · 실제 방문 인증 아님",
                coordinates = String.format(java.util.Locale.US, "SDK 좌표 %.5f, %.5f", location.latitude, location.longitude),
                visitCheck = localCourseSummary(Point(location.latitude, location.longitude))))
        } }
        override fun onError(exception: Exception) { post { if (active && generation == expectedGeneration) publish(state.copy(location = "위치 SDK 오류: ${exception.javaClass.simpleName}", coordinates = null, visitCheck = null)) } }
    }
    fun connectLocationIfGranted() {
        if (!active || fused != null) return
        if (context.checkSelfPermission(LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            publish(state.copy(location = "위치 권한 승인 전 · 읽지 않음", coordinates = null, visitCheck = null)); return
        }
        try {
            val service = FusedLocation(context)
            if (!service.isSupported()) { publish(state.copy(location = "FusedLocation 서비스가 설치되지 않음")); return }
            fused = service
            val listener = newLocationListener(generation)
            locationListener = listener
            service.initialize(); service.registerFusedLocationCallback(listener); service.getLastKnownLocation(listener)
            publish(state.copy(location = "위치 권한 허용 · SDK 콜백 대기"))
        } catch (error: Exception) { disconnectLocation(); publish(state.copy(location = "위치 연결 실패: ${error.javaClass.simpleName}", coordinates = null, visitCheck = null)) }
    }
    private fun disconnectLocation() {
        runCatching { if (context.checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) locationListener?.let { fused?.unregisterFusedLocationCallback(it) } }
        runCatching { fused?.release() }; fused = null; locationListener = null; locationReceivedAt = 0
    }
}
