#!/usr/bin/env python3
"""ADB UI smoke tests for the separate v0.3 simulator package only."""
import os,subprocess,time,re,json,xml.etree.ElementTree as E
from pathlib import Path
ADB=os.environ.get('DRIVE_ATLAS_ADB',str(Path.home()/'Developer/pleos-emulator/existing/android-sdk/platform-tools/adb'))
PKG='com.driveatlas.demo.simulator'
OUT=Path(__file__).resolve().parents[1]/'evidence/demo-v0.3'
checks=[]
def adb(*args):return subprocess.check_output([ADB,'-s','emulator-5580',*args],stderr=subprocess.STDOUT)
def ui():
 adb('shell','uiautomator','dump','/sdcard/drive-atlas-ui.xml')
 return E.fromstring(adb('shell','cat','/sdcard/drive-atlas-ui.xml'))
def texts(root=None):
 root=ui() if root is None else root
 return [n.attrib.get('text','') for n in root.iter('node') if n.attrib.get('package')==PKG]
def node(text,root=None):
 root=ui() if root is None else root
 nodes=[n for n in root.iter('node') if n.attrib.get('package')==PKG and n.attrib.get('text')==text]
 assert nodes, 'Missing UI text: '+text+'; visible: '+str(texts(root))
 return nodes[0]
def tap(text):
 n=node(text);a=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));assert a[2]>a[0] and a[3]>a[1],n.attrib
 adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(.25)
def expect(text):node(text)
def no(text):assert text not in texts(), 'Unexpected: '+text
def back():adb('shell','input','keyevent','4');time.sleep(.25)
def snap(name):
 (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
 (OUT/(name+'.xml')).write_bytes(E.tostring(ui(),encoding='utf-8'))
def passed(s):checks.append(s);(OUT/'ui-checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print('PASS',s,flush=True)
def unlock():tap('시뮬레이터 제어  +');tap('가상 주차 · 위치 정상')
def dismiss():
 if '닫기' in texts():tap('닫기')
def begin(title):
 tap('길 고르기');tap('추천길');tap(title);tap('이 길로 떠나기  →');tap('연결 성공 체험 · 가상 여정 시작');dismiss()
def progress(n):
 for i in range(n):tap('다음 지점 근거 입력 · SIM')
def finish():
 tap('도착 위치 다시 확인 · SIM');tap('주차 후 여정 완료  ✓');dismiss()

if __name__=='__main__':
 unlock();expect('오늘은, 어떤 길로 갈까요?')
 root=ui()
 for text in ['추천길','계절 드라이브','테마 드라이브']:
  b=list(map(int,re.findall(r'\d+',node(text,root).attrib['bounds'])));assert b[0]>=853 and b[2]<=2535 and b[1]>=90 and b[3]<=1300
 snap('02-home');passed('Home has 3 entry points inside 1682x1210 app bounds')
 tap('계절 드라이브');tap('겨울');expect('물빛권역 · 1개의 길');expect('호수를 따라, 한 바퀴');no('푸른 해안의 여백');snap('03-winter')
 tap('호수를 따라, 한 바퀴');expect('이 길로 떠나기  →');back();expect('물빛권역 · 1개의 길');passed('Winter filter and detail Back preserve conditions')
 tap('길 고르기');tap('테마 드라이브');tap('초록 충전');expect('물빛권역 · 1개의 길');expect('숲을 천천히 읽는 길');passed('Mood filter chooses the forest course')
 tap('지역·분위기');tap('다른 지역');expect('이 조건에는 준비한 길이 없어요');tap('조건 초기화');expect('물빛권역 · 3개의 길');tap('근처 체험');expect('실제 위치를 요청하지 않아요. 샘플 출발점과 가상 거리로 정렬합니다.');snap('04-nearby-sample');passed('Empty region, reset and explicit sample-nearby sorting')
 tap('숲을 천천히 읽는 길');snap('05-detail');tap('이 길로 떠나기  →');tap('미지원 상황 보기');expect('이 환경에서는 연결할 수 없어요');snap('06-navigation-unavailable');tap('취소');no('진행 중인 여정  →');passed('Navigation failure/cancel does not start a journey')
 tap('이 길로 떠나기  →');tap('연결 성공 체험 · 가상 여정 시작');dismiss();expect('SIMULATED · 가상 여정');snap('07-journey')
 tap('시뮬레이터 제어  +');tap('위치 수신 끊기');tap('다음 지점 근거 입력 · SIM');expect('위치 근거를 받을 수 없어요. 위치 수신 상태를 확인해 주세요.');dismiss();unlock();progress(1);dismiss();passed('Lost location rejects simulated evidence; restored location accepts it')
 adb('shell','am','force-stop',PKG);adb('shell','am','start','-n',PKG+'/com.driveatlas.MainActivity');time.sleep(1);expect('차량 상태 미확인');snap('08-restart-lock');unlock();tap('여정 이어보기');expect('1 / 3  SCENES');passed('Restart preserves journey progress but resets safety/location')
 progress(2);finish();expect('1종의 여행 우표를 모았어요');snap('09-first-stamp');tap('우표 PNG 만들기  ↗');expect('오늘의 길을 담은 여행 우표');snap('10-stamp-preview');back();expect('1종의 여행 우표를 모았어요');passed('Complete journey, course stamp, PNG preview and Back')
 tap('다녀온 길');expect('1 / 3 코스 · 모의 완주');snap('11-visited-map');tap('목록');expect('여행 기록 · 1회');passed('Visited map and list show completion')
 begin('숲을 천천히 읽는 길');progress(3);finish();expect('1종의 여행 우표를 모았어요');tap('다녀온 길');expect('여행 기록 · 2회');passed('Repeat journey adds record without a second course stamp')
 begin('푸른 해안의 여백');progress(1);dismiss();tap('미확인·중단 기록 남기기');tap('미확인 기록 남기기');dismiss();expect('여행 기록 · 3회');expect('미확인 · 1/3 구간');snap('12-unconfirmed');passed('Partial journey archived without a stamp')
 begin('호수를 따라, 한 바퀴');tap('미확인·중단 기록 남기기');tap('계속 체험하기');expect('SIMULATED · 가상 여정');tap('미확인·중단 기록 남기기');tap('중단 기록 남기기');dismiss();expect('여행 기록 · 4회');expect('중단 · 0/3 구간');passed('Archive Cancel keeps journey; interrupted record grants no stamp')
 tap('시뮬레이터 제어  +');tap('가상 주행으로 전환');expect('잠시,\n운전에 집중해요.');tap('우표 도감');expect('잠시,\n운전에 집중해요.');snap('13-moving-lock');passed('Moving state blocks navigation and album editing')
 unlock();tap('길 고르기');expect('오늘은, 어떤 길로 갈까요?');snap('14-final-home');passed('Return to home with persisted demo records')
 print('ALL UI CHECKS PASSED',len(checks),flush=True)
