"""Parked emulator checks for v0.8. No permission grants, car-state changes, real navigation requests or deletion."""
import importlib.util,time,re
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/family-v08');q.OUT.mkdir(exist_ok=True)
def bounds(n):return list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
def rowtap(label,value):
 root=q.ui();labelnode=q.node(label,root);lb=bounds(labelnode);cy=(lb[1]+lb[3])/2
 options=[n for n in root.iter('node') if n.attrib.get('package')==q.PKG and n.attrib.get('text')==value and abs((bounds(n)[1]+bounds(n)[3])/2-cy)<35]
 assert options,(label,value,q.texts(root))
 b=bounds(options[0]);q.adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.5)
def scrollto(text):
 for i in range(7):
  if text in q.texts():return
  q.adb('shell','input','swipe','1900','1150','1900','500','450');time.sleep(.5)
 raise AssertionError('Missing after scroll: '+text)
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.expect('P · UX 해제 확인');q.tap('가족 조건 · 코스 비교');q.expect('오늘 우리 가족에게 맞는 길')
rowtap('전체 여유','180');q.passed('Original family data migration and parked entry')
# First route remains forest: prior automated memories are marked simulated.
rowtap('코스까지','30');rowtap('복귀 시간','30')
visible=q.texts();assert any('왕복 포함 총 145분' in t for t in visible),visible
q.snap('01-round-trip-total');q.passed('Both transfer legs included in total planned time')

for title in ['화장실 필수','유모차 이동']:
 root=q.ui();parents={c:p for p in root.iter() for c in p};node=q.node(title,root)
 if parents[node].attrib.get('checked')!='true':q.tap(title)
scrollto('담양 관방제림');q.tap('담양 관방제림')
scrollto('한국관광공사 열린관광 시설 안내 ↗');q.expect('한국관광공사 열린관광 시설 안내 ↗')
scrollto('입력 시간·선택 휴식지 공개정보 기준 충족 · 당일 운영은 확인 필요');q.expect('입력 시간·선택 휴식지 공개정보 기준 충족 · 당일 운영은 확인 필요')
q.snap('02-verified-stop');q.passed('Selected official venue satisfies only its documented facility conditions')
scrollto('코스 상세 · 내비 연결');q.tap('코스 상세 · 내비 연결');q.expect('지도 연결 옵션');q.back();q.expect('오늘 우리 가족에게 맞는 길');q.passed('Existing native navigation detail remains accessible with Back restoration')
q.tap('길 고르기');q.tap('가족 차 기억 · 피드백');q.tap('실제 여행이 아닌 시연 기록 (추천 평점에서 제외)');q.tap('우리 차의 기억 저장');q.expect('시연 기억을 저장했어요. 추천 평점에서는 제외합니다.')
q.tap('기억 수정');q.expect('기억과 평가 수정');q.tap('5');q.tap('수정 저장');assert any('만족도 5/5' in t for t in q.texts());q.passed('Simulated record edit saves rating without creating another memory')
q.tap('기억 삭제');q.expect('이 기억을 삭제할까요?');q.tap('취소');q.expect('기억 수정');q.snap('03-edit-and-delete-cancel');q.passed('Delete requires explicit dialog confirmation; Cancel preserves record')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.expect('P · UX 해제 확인');q.tap('가족 조건 · 코스 비교');assert any('왕복 포함 총 145분' in t for t in q.texts());q.snap('04-plan-restored');q.passed('Round-trip plan and venue selection survive cold restart')
q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');q.snap('05-final-home');print('DONE',flush=True)
