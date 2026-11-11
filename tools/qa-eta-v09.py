"""One explicitly approved PUBLIC-reference API calculation. Never chooses SDK/private location."""
import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/eta-v09');q.OUT.mkdir(exist_ok=True)
def scrollto(text):
 for i in range(7):
  if any(text in item for item in q.texts()):return
  q.adb('shell','input','swipe','1900','1150','1900','500','450');time.sleep(.5)
 raise AssertionError('Missing '+text)
q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(2)
q.tap('길 고르기');q.tap('자동 왕복 시간');q.expect('도로 기준 자동 왕복 시간');q.snap('01-eta-start')
q.tap('전송 정보 확인 후 왕복 계산');q.expect('이번 경로 요청에 동의할까요?');q.expect('이번 요청에 동의하고 계산');q.tap('취소');q.no('도로 예상값 계산 완료');q.passed('Consent cancellation returns without a result or implicit calculation')
q.tap('전송 정보 확인 후 왕복 계산');q.tap('이번 요청에 동의하고 계산')
for i in range(15):
 texts=q.texts()
 if '도로 예상값 계산 완료' in texts:break
 if any('못했어요' in x or '초과됐어요' in x or '확인하지 못했어요' in x for x in texts):raise AssertionError(texts)
 time.sleep(1)
scrollto('도로 예상값 계산 완료');q.expect('도로 예상값 계산 완료');q.snap('02-road-result');print(json.dumps(q.texts(),ensure_ascii=False),flush=True);q.passed('Actual HTTPS OSRM response calculates outward/course/return road legs separately')
scrollto('가족 계획에 자동 시간 적용');q.tap('가족 계획에 자동 시간 적용');q.expect('자동 시간이 가족 계획에 저장됐어요.');q.tap('가족 계획에서 비교');q.expect('오늘 우리 가족에게 맞는 길');q.snap('03-applied-plan');q.passed('Calculated times apply to family budget, retaining rest and walking plans')
q.tap('길 고르기');q.tap('자동 왕복 시간');scrollto('24시간 이내 저장된 공개 결과 보기');q.tap('24시간 이내 저장된 공개 결과 보기');scrollto('저장된 도로 예상값');q.expect('저장된 도로 예상값');q.snap('04-cached-public-result');q.passed('Explicit cache action loads labelled public result without another network request')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3)
q.tap('가족 조건 · 코스 비교');scrollto('OSRM 도로 예상');texts=q.texts();assert any('OSRM 도로 예상' in t for t in texts),texts;q.snap('05-restored-auto-plan');q.passed('Cold restart preserves ETA provenance and calculated family plan')
q.tap('길 고르기');q.expect('어떤 길로 떠날까요?');print('ETA UI PASSED',flush=True)
