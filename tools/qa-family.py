"""Family UI smoke test; creates one explicitly simulated local memory, no grants or vehicle-state changes."""
import importlib.util,time
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/family-v07');q.OUT.mkdir(exist_ok=True)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(2)
q.tap('가족 조건 · 코스 비교');q.expect('오늘 우리 가족에게 맞는 길');q.expect('1. 담양 메타길 나들이');q.passed('Family comparison opens from actual parked PLEOS state')
q.tap('화장실 필수');q.expect('확인된 조건을 모두 만족하는 코스가 없어요. 부족한 정보와 시간 차이를 확인해 주세요.');q.expect('화장실 정보 확인 필요');q.snap('03-facility-unknown');q.passed('Unknown facility never passes a required condition')
q.tap('화장실 필수');q.tap('코스 상세 · 내비 연결');q.expect('지도 연결 옵션');q.snap('04-existing-route-detail');q.back();q.expect('오늘 우리 가족에게 맞는 길');q.passed('Family result opens existing real course detail; Back returns to comparison')
q.tap('가족 차 기억 · 피드백');q.expect('함께한 시간을 다음 길로');q.tap('실제 여행이 아닌 시연 기록 (추천 평점에서 제외)');q.tap('우리 차의 기억 저장');q.expect('기억과 평가가 저장됐어요. 같은 기록은 중복 저장하지 않습니다.');q.no('우리 차의 기억 저장');q.expect('시연 기록 · 추천 평점 제외');q.snap('05-memory-saved');q.passed('Simulated memory persists and duplicate save is not available')
q.tap('평가를 반영한 다음 길 보기');q.expect('1. 담양 메타길 나들이');q.passed('Simulation feedback does not boost recommendation ranking')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.expect('P · UX 해제 확인');q.tap('가족 차 기억 · 피드백');q.expect('시연 기록 · 추천 평점 제외');q.snap('06-memory-restart');q.passed('Cold restart preserves family memory and rechecks authoritative parked state')
q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');q.snap('07-final-home');q.passed('Returns to original three-card home')
print('DONE',flush=True)
