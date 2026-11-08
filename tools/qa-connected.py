"""Actual connected emulator QA. Never grants location permission or overrides vehicle state."""
import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/live-v05')
q.expect('P · UX 제한 해제 확인');q.passed('Actual P gear + platform UX 0 automatically opens home, without mock unlock')
q.tap('연결 상태 보기  +');q.expect('위치 권한 승인 전 · 읽지 않음');q.snap('02-connections');q.passed('GPS not read before permission; vehicle and native service diagnostics displayed')
q.tap('닫기');q.adb('shell','input','keyevent','3');time.sleep(1);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3);q.expect('P · UX 제한 해제 확인');q.snap('03-resume');q.passed('Home navigation and return retain verified parked state (multi-window may remain started)')
q.tap('추천길');q.tap('강릉 헌화로');q.expect('6.3 km · 주행 약 10분');q.tap('지도 연결 옵션  →');q.snap('04-native-options');q.tap('공개 좌표 코스 전달 · 실험');time.sleep(5);q.snap('05-native-request');q.passed('Explicit native route request submitted; result not assumed to be success')
print('AFTER_NATIVE_REQUEST',q.texts(),flush=True)
