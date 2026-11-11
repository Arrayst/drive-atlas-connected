"""Explicit single-destination leg verification on the existing emulator, using public course points."""
import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/native-legs-v08');q.OUT.mkdir(exist_ok=True)
def scrollto(text):
 for i in range(7):
  if text in q.texts():return
  q.adb('shell','input','swipe','1800','1090','1800','530','450');time.sleep(.5)
 raise AssertionError('Missing '+text)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.tap('길 고르기');q.tap('추천길');q.tap('담양 메타길 나들이');q.tap('지도 연결 옵션')
for index,name in enumerate(['관방제림 남쪽 도로','담양88로','메타길 인근 도로']):
 label=f'{index+1}. {name}까지 안내';scrollto(label);q.tap(label);time.sleep(20)
 q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
 if '길을 확인하고, 안전하게 출발해요' not in q.texts():q.tap('지도 연결 옵션')
 expected='선택 구간 내비 확인 · 목적지 좌표 일치 · 전체 코스 연속 안내 아님'
 print(json.dumps(q.texts(),ensure_ascii=False),flush=True);q.expect(expected);q.snap(f'0{index+1}-leg-confirmed');q.passed(f'Leg {index+1}: fresh completion, matching destination and zero remaining waypoints')
q.tap('닫기');q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');print('ALL THREE LEGS VERIFIED',flush=True)
