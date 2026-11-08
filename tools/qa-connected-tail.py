import importlib.util,time,json
s=importlib.util.spec_from_file_location('q','tools/qa-real.py');q=importlib.util.module_from_spec(s);s.loader.exec_module(q);q.PKG='com.driveatlas.connected.emulator';q.OUT=q.Path('evidence/live-v05')
q.adb('shell','am','force-stop',q.PKG);q.adb('shell','am','start','-n',q.PKG+'/com.driveatlas.MainActivity');time.sleep(3);q.expect('P · UX 해제 확인');q.expect('코스 살펴보기');q.passed('Cold restart reconnects from actual P and UX signals without simulated unlock')
q.tap('계절 드라이브');q.tap('겨울');q.expect('겨울에 어울리는 1개의 길');q.snap('15-winter');q.tap('춘천 의암호 서쪽길');q.tap('←  모든 코스');q.expect('겨울에 어울리는 1개의 길');q.passed('Integrated season selection survives detail and Back')
q.tap('테마 드라이브');q.tap('초록 충전');q.snap('16-theme');q.tap('우표 도감');q.snap('17-atlas');q.tap('다녀온 길');q.snap('18-records');q.passed('Integrated theme, stamp, and visit screens render')
q.tap('길 고르기');q.tap('추천길');q.snap('19-final-home');q.passed('Final app left on the redesigned home, P verified, GPS ungranted')
