"""v0.6 card navigation and approved local GPS checks. Does not send GPS externally."""
import importlib.util,time,re,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q);q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/cards-gps-v06')
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.expect('어떤 길로 떠날까요?');q.expect('P · UX 해제 확인')
r=q.ui();titles=['추천길','계절 드라이브','테마 드라이브'];bounds=[]
for title in titles:
 n=q.node(title,r);bounds.append(list(map(int,re.findall(r'\d+',n.attrib['bounds']))))
assert bounds[0][0]<bounds[1][0]<bounds[2][0] and max(b[1] for b in bounds)-min(b[1] for b in bounds)<5,bounds
q.no('지역·조건');q.snap('01-three-card-home');q.passed('Three large horizontal category cards, not top tabs, render with verified P state')
q.tap('추천길');q.expect('3개의 길');q.snap('02-recommended-list');q.tap('강릉 헌화로');q.expect('지도 연결 옵션');q.snap('03-real-map');q.back();q.expect('3개의 길');q.back();q.expect('어떤 길로 떠날까요?');q.passed('Recommended card opens a list; list/detail/system Back returns correctly')
q.tap('계절 드라이브');q.expect('계절');q.tap('겨울');q.expect('1개의 길');q.snap('04-winter-list');q.tap('춘천 의암호 서쪽길');q.back();q.expect('1개의 길');q.back();q.expect('어떤 길로 떠날까요?');q.passed('Season card filters inside the list and preserves winter after detail Back')
q.tap('테마 드라이브');q.tap('초록 충전');q.expect('1개의 길');q.snap('05-theme-list');q.tap('담양 메타길 나들이');q.back();q.expect('1개의 길');q.back();q.expect('어떤 길로 떠날까요?');q.passed('Theme card opens its own list and preserves choice after detail Back')
q.tap('추천길');q.expect('3개의 길');q.back();q.passed('Category filters are independent; theme filter does not hide recommended courses')
q.tap('연결 상태 보기  +');time.sleep(2);visible=q.texts();assert any('에뮬레이터 위치 수신' in x for x in visible),visible;assert any('가장 가까운 코스 지점:' in x for x in visible),visible;assert any('방문은 미확인' in x for x in visible),visible;q.snap('06-approved-local-gps');q.tap('닫기');q.passed('Approved FusedLocation emits emulator data and local-only proximity, explicitly not a certified visit')
permission=q.adb('shell','dumpsys','package',q.PKG).decode();assert 'pleos.car.permission.FUSED_LOCATION: granted=true' in permission
q.tap('우표 도감');q.snap('07-atlas');q.tap('다녀온 길');q.snap('08-records');q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');q.passed('Existing atlas and records remain accessible')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3);q.expect('어떤 길로 떠날까요?');q.expect('P · UX 해제 확인');q.snap('09-final-home');q.passed('Cold restart restores safe three-card home; final app left open')
print('DONE',flush=True)
