"""Non-destructive UI regression for the parked PLEOS test emulator.
Does not save memories/plans, send navigation requests, grant consent or alter vehicle state.
Uses the repository's existing emulator-5580 UI test adapter.
"""
import importlib.util, time, re, subprocess
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/design-refinement-20261113');q.OUT.mkdir(parents=True,exist_ok=True)
# PLEOS has two resumed windows. Read all windows instead of whichever one
# happens to hold accessibility focus; no taps or system setting changes needed.
def app_ui():
 q.adb('shell','uiautomator','dump','--windows','/sdcard/drive-atlas-ui.xml')
 return q.E.fromstring(q.adb('shell','cat','/sdcard/drive-atlas-ui.xml'))
q.ui=app_ui
def settled_tap(text):
 n=q.node(text); bounds=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
 assert bounds[2]>bounds[0] and bounds[3]>bounds[1],n.attrib
 # Let UiAutomation detach before injecting the next event into the car window.
 time.sleep(1)
 q.adb('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));time.sleep(1)
q.tap=settled_tap
# Run after the emulator and app are fully resumed and the real P/UX check succeeds.
# Cold-start timing and vehicle state are deliberately not changed by this script.
q.adb('shell','input','tap','1250','140');time.sleep(.5);q.tap('길 고르기');q.expect('어디로 떠나볼까요?');q.snap('after-home')
for route in ['담양 메타길 나들이','춘천 의암호 서쪽길','강릉 헌화로']:
 q.tap(route);q.adb('shell','input','tap','2190','240');q.tap('이 코스 살펴보기');q.expect('다른 지도·내비 연결 옵션');q.back();q.adb('shell','input','tap','2190','240');q.expect('어디로 떠나볼까요?')
q.passed('All three course selection, detail and Back flows retained')
q.tap('오프라인 체험');q.expect('오프라인 상황 체험');q.snap('after-offline');q.tap('온라인으로');time.sleep(2)
q.passed('Offline map state and return remain reachable without granting new consent')
q.tap('가족 비교');q.expect('가족 여행 계획');q.snap('after-family')
for label in ['시간·휴식 조정','코스 상세 · 내비 연결']:
 n=q.node(label); bounds=list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
 assert bounds[3]<=1300 and bounds[3]>bounds[1],(label,bounds)
q.passed('Family next actions visible in the actual split PLEOS viewport without scrolling')
q.tap('가족 조건 변경');q.expect('변경한 조건은 저장한 뒤 비교에 반영됩니다.');q.tap('닫기');q.tap('가족 조건 변경');q.back();q.expect('가족 여행 계획')
q.tap('시간·휴식 조정');q.expect('계획 저장');q.tap('닫기');q.expect('가족 여행 계획')
q.passed('Repeated family and plan editors dismiss without saving')
q.tap('춘천 의암호 서쪽길');assert any('전체 시간은 아직 미정' in t for t in q.texts());q.snap('after-family-unknown')
q.tap('강릉 헌화로');q.tap('코스 상세 · 내비 연결');q.tap('다른 지도·내비 연결 옵션');q.expect('기본 내비에서 도착지 검색');q.tap('닫기 ×');q.back()
q.passed('Unknown time remains explicit; native-navigation dialog dismissed without route request')
q.tap('우리 차 기억');q.expect('우리 차의 여행 기억');q.snap('after-memories');q.tap('새로운 기억 남기기');q.expect('이번 여행은 어땠나요?');q.tap('4점');q.snap('after-rating');q.tap('취소');q.expect('우리 차의 여행 기억')
q.passed('Five satisfaction choices render; selecting an unsaved rating then cancelling returns to journal')
q.tap('왕복 시간');q.snap('after-eta');q.tap('다녀온 길');q.tap('우표 도감');q.tap('길 고르기');q.expect('어디로 떠나볼까요?')
q.passed('ETA, visit archive and stamp atlas remain reachable')
for _ in range(4):q.adb('shell','input','keyevent','61');time.sleep(.2)
assert any(n.attrib.get('focused')=='true' and n.attrib.get('package')==q.PKG for n in q.ui().iter('node'))
q.snap('after-focus');q.passed('Keyboard traversal reaches a native focusable control')
print('DONE',flush=True)
