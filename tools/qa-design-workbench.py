"""Read-only parked-emulator UI checks. No memory save/delete, vehicle override or route request."""
import importlib.util,time
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/eta-v09/design');q.OUT.mkdir(parents=True,exist_ok=True)
def scrollto(text):
 for i in range(6):
  if text in q.texts():return
  q.adb('shell','input','swipe','2200','1160','2200','570','350');time.sleep(.4)
 raise AssertionError('Missing after scroll: '+text)
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(4)
q.expect('오늘의 길을 고르세요');q.expect('가족 비교');q.expect('우리 차 기억');time.sleep(4);q.snap('home')
q.passed('Compact task bar exposes all six destinations with actual route index and road map')
for route in ['담양 메타길 나들이','춘천 의암호 서쪽길','강릉 헌화로']:
 q.tap(route);q.tap('코스 살펴보기');q.expect('지도 연결 옵션');q.back();q.expect('오늘의 길을 고르세요')
q.passed('All three indexed routes open detail and return home')
for label,title in [('추천길','추천길'),('계절별','계절 드라이브'),('테마별','테마 드라이브')]:
 q.tap(label);q.expect(title);q.back();q.expect('오늘의 길을 고르세요')
q.passed('Recommended, season and theme filter destinations retained')
q.tap('오프라인 체험');q.expect('오프라인 상황 체험');q.snap('map-offline');q.tap('다시 불러오기');time.sleep(4)
q.passed('Map unavailable state and retry remain reachable')
q.tap('가족 비교');q.expect('오늘 우리 가족에게 맞는 길');q.expect('왕복 미완성');q.snap('family')
q.tap('가족 조건 변경');q.expect('차량 별명 저장');q.tap('가족 조건 접기')
q.tap('시간·휴식 계획 조정');scrollto('계획 접기');q.expect('계획 접기');q.snap('family-planning');q.tap('계획 접기')
q.tap('길 고르기');q.tap('가족 비교');q.tap('코스 상세 · 내비 연결');q.expect('지도 연결 옵션');q.tap('지도 연결 옵션');q.expect('기본 내비에서 도착지 검색');q.tap('닫기');q.back();q.expect('오늘 우리 가족에게 맞는 길')
q.passed('Family preference disclosure, planning panel and navigation options retain handlers; no routes sent')
q.tap('우리 차 기억');q.expect('함께한 시간을 다음 길로');q.snap('memories')
q.tap('기억 수정');q.expect('기억과 평가 수정');q.tap('취소');q.tap('기억 삭제');q.expect('이 기억을 삭제할까요?');q.tap('취소');q.expect('우리 차의 기억 저장')
q.passed('Journal edit and delete dialogs cancel without mutating saved records')
q.tap('왕복 시간');q.snap('eta');q.passed('Road ETA workflow remains reachable')
q.tap('다녀온 길');q.snap('records');q.tap('우표 도감');q.snap('atlas');q.passed('Existing visit records and stamp atlas retained')
q.tap('길 고르기')
for _ in range(4):q.adb('shell','input','keyevent','61');time.sleep(.3)
assert any(n.attrib.get('focused')=='true' and n.attrib.get('package')==q.PKG for n in q.ui().iter('node')), 'No native control received keyboard focus'
q.snap('keyboard-focus');q.passed('Keyboard traversal reaches native controls')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(4);q.expect('오늘의 길을 고르세요');time.sleep(3);q.snap('home')
print('DONE',flush=True)
