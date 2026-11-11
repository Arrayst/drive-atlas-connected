"""Explicit emulator route request for existing public course coordinates. No private GPS transmission or permission changes."""
import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/family-v08');q.OUT.mkdir(exist_ok=True)
q.tap('길 고르기');q.tap('추천길');q.tap('담양 메타길 나들이');q.tap('지도 연결 옵션')
q.tap('기본 내비 화면 보기');time.sleep(8)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
# Resume safely closes the old dialog; restore it explicitly.
if '공개 좌표 코스 전달 · 실험' not in q.texts():q.tap('지도 연결 옵션')
q.tap('공개 좌표 코스 전달 · 실험');time.sleep(35)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.tap('연결 상태 보기  +')
texts=q.texts();print(json.dumps(texts,ensure_ascii=False),flush=True);q.snap('06-native-result')
(q.OUT/'native-status.txt').write_text('\n'.join(texts))
(q.OUT/'native-callbacks.log').write_bytes(q.adb('logcat','-d','-s','AtlasConnection:I','*:S'))
if '닫기' in texts:q.tap('닫기')
q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');print('NATIVE CHECK RECORDED; inspect status, do not assume success',flush=True)
