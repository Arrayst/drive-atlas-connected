"""Public/synthetic screenshots from an isolated package, with no live location or online consent."""
import importlib.util,time,re,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.designreview';q.OUT=q.Path('evidence/design-refinement-20261113/gemini-safe')
def ui():
 q.adb('shell','uiautomator','dump','--windows','/sdcard/drive-review-ui.xml')
 return q.E.fromstring(q.adb('shell','cat','/sdcard/drive-review-ui.xml'))
q.ui=ui
def tap(text):
 n=q.node(text);a=list(map(int,re.findall(r'\d+',n.get('bounds'))));assert a[2]>a[0] and a[3]>a[1],n.attrib
 time.sleep(.6);q.adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(.7)
q.tap=tap
screens=json.loads((q.OUT/'screens.json').read_text()) if (q.OUT/'screens.json').exists() else []
def snap(name,note):
 root=q.ui();time.sleep(.6)
 (q.OUT/(name+'.png')).write_bytes(q.adb('exec-out','screencap','-p'))
 (q.OUT/(name+'.xml')).write_bytes(q.E.tostring(root,encoding='utf-8'))
 screens.append({'file':name+'.png','description':note,'data':'public course catalog + synthetic QA fixture only'})
 (q.OUT/'screens.json').write_text(json.dumps(screens,ensure_ascii=False,indent=2));print('CAPTURE',name,flush=True)
q.expect('어디로 떠나볼까요?');snap('01-home','Home, public catalog, map consent not granted')
q.tap('오프라인 체험');q.expect('오프라인 상황 체험');snap('02-offline','Offline state with visible recovery and direct place list actions')
q.tap('장소 목록 보기');snap('03-place-list','Offline public places list');q.back();q.tap('온라인으로')
q.tap('코스');snap('04-map-filter','Course filter selected; no network consent');q.tap('전체')
q.tap('이 코스 살펴보기');q.expect('앱 경로 미리보기 · SIM');snap('05-course-detail','Course detail, static references, SIM label')
q.tap('다른 지도·내비 연결 옵션');q.expect('기본 내비에서 도착지 검색');snap('06-navigation-options','Navigation options only; no request sent');q.tap('닫기 ×')
q.tap('앱 경로 미리보기 · SIM');snap('07-sim-preview','SIM preview setup only; no route request or voice playback')
q.tap('경로 설정');snap('08-sim-settings','Public reference route settings');q.back();q.back()
q.tap('길 고르기');q.tap('추천길');snap('09-curation','Recommendations and filters, public course data');q.tap('강릉');snap('10-curation-filter','Region filter, one public course');q.tap('길 고르기')
q.tap('가족 비교');q.expect('가족 여행 계획');snap('11-family','New isolated app default family conditions, no private preferences')
q.tap('가족 조건 변경');snap('12-family-settings','New isolated app default family editor');q.tap('닫기')
q.tap('시간·휴식 조정');snap('13-family-plan','Default plan editor, no saved changes');q.tap('닫기')
q.tap('춘천 의암호 서쪽길');snap('14-family-unknown','Missing transfer times remain explicit')
q.tap('왕복 시간');snap('15-roundtrip','Round trip calculator idle, no request sent')
q.tap('우리 차 기억');q.expect('우리 차의 여행 기억');snap('16-memories','Empty new-app memory state')
q.tap('새로운 기억 남기기');q.expect('이번 여행은 어땠나요?');snap('17-rating-empty','Required course, kind, rating not yet selected; save disabled')
q.tap('강릉 헌화로');q.tap('SIM · 시연');q.tap('4점');snap('18-rating-selected','Unsaved SIM rating; no actual memory assertion')
q.tap('날짜·메모·느낌 추가 (선택)');snap('19-rating-details','Optional blank date, feelings and note; no keyboard input');q.tap('취소')
q.tap('다녀온 길');snap('20-records','New isolated app records: empty or explicit synthetic fixture')
if '지도' in q.texts():
 q.tap('지도');snap('21-records-map','Reference-course map mode, not GPS history')
q.tap('우표 도감');snap('22-stamp-atlas','New isolated app stamp atlas: empty or synthetic SIM, never actual certification')
q.tap('연결 상태 보기');snap('23-connections','System connection states only, no permission requested');q.tap('닫기')
q.tap('길 고르기');q.expect('어디로 떠나볼까요?')
print('DONE',flush=True)
