#!/usr/bin/env python3
"""ADB UI smoke tests for the separate v0.4 simulator package only."""
import os,subprocess,time,re,json,xml.etree.ElementTree as E
from pathlib import Path
ADB=os.environ.get('DRIVE_ATLAS_ADB',str(Path.home()/'Developer/pleos-emulator/existing/android-sdk/platform-tools/adb'))
PKG='com.driveatlas.real.simulator'
OUT=Path(__file__).resolve().parents[1]/'evidence/real-v04'
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
 adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(.7)
def expect(text):
 for attempt in range(4):
  try: return node(text)
  except AssertionError:
   if attempt==3:raise
   time.sleep(1)
def no(text):assert text not in texts(), 'Unexpected: '+text
def back():adb('shell','input','keyevent','4');time.sleep(.7)
def snap(name):
 ui();time.sleep(.3)
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

