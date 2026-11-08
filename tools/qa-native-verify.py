import importlib.util,time
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q);q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/live-v05')
q.expect('P · UX 해제 확인');q.tap('코스 살펴보기');q.tap('지도 연결 옵션');q.tap('공개 좌표 코스 전달 · 실험');time.sleep(18)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3);q.expect('P · UX 해제 확인');q.tap('연결 상태 보기  +');q.snap('21-native-verified');print('RESULT',q.texts(),flush=True)
