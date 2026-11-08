import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q)
q.checks=json.loads((q.OUT/'ui-checks.json').read_text())
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(4);q.expect('차량 상태 미확인');q.unlock();q.tap('다녀온 길');q.expect('1 / 3 코스 · 모의 완주');q.tap('목록');q.expect('여행 기록 · 1회');q.passed('Record persists after process restart; safety does not persist unlocked')
q.tap('길 고르기');q.tap('추천길');q.tap('강릉 헌화로');q.tap('지도 연결 옵션  →');q.tap('우표 수집 흐름 체험 · SIM');q.dismiss();q.progress(1);q.dismiss();q.tap('미확인·중단 기록 남기기');q.tap('계속 체험하기');q.expect('SIMULATED · 가상 여정');q.tap('미확인·중단 기록 남기기');q.tap('미확인 기록 남기기');q.dismiss();q.expect('여행 기록 · 2회');q.expect('미확인 · 1/3 구간');q.passed('Archive cancel and incomplete visit preserve correct state without extra stamp')
q.tap('시뮬레이터 제어  +');q.tap('가상 주행으로 전환');q.expect('잠시,\n운전에 집중해요.');q.tap('길 고르기');q.expect('잠시,\n운전에 집중해요.');q.passed('Moving simulation blocks editing')
q.unlock();q.tap('길 고르기');q.expect('오늘은, 어떤 길로 갈까요?');q.snap('10-final-home');q.tap('추천길');q.tap('강릉 헌화로');time.sleep(2);q.snap('11-final-coast');q.passed('Final app left on real coast map; all screenshots use the actual PLEOS window')
print('FINISHED',len(q.checks),flush=True)
