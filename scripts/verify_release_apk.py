#!/usr/bin/env python3
"""Verify release artifact logging and manifest policy without publishing/installing it."""
import argparse
from pathlib import Path
import shutil
import re
import subprocess
import xml.etree.ElementTree as ET
from zipfile import ZipFile

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--mapping', type=Path, help='R8 mapping.txt; verify app classes were renamed')
parser.add_argument('--app-package', default='com.daro.agentapp')
parser.add_argument('--apkanalyzer', default=shutil.which('apkanalyzer'))
args = parser.parse_args()
if not args.apkanalyzer:
    parser.error('apkanalyzer is required for release manifest verification')
with ZipFile(args.apk) as archive:
    dex_files = [archive.read(name) for name in archive.namelist() if name.endswith('.dex')]
    assert dex_files, 'APK has no DEX files'
    for tag in (b'AgentNetwork', b'ResponseData==>'):
        assert not any(tag in dex for dex in dex_files), 'Debug response/network log tag is present'
manifest_text = subprocess.run([args.apkanalyzer, 'manifest', 'print', str(args.apk)],
                               check=True, capture_output=True, text=True).stdout
manifest = ET.fromstring(manifest_text)
android = '{http://schemas.android.com/apk/res/android}'
application = manifest.find('application')
assert application.get(android + 'debuggable', 'false') == 'false', 'Release is debuggable'
assert application.get(android + 'usesCleartextTraffic') == 'false', 'Release permits cleartext'
if args.mapping:
    mappings = re.findall(r'^([^ #].*?) -> ([^ ]+):$', args.mapping.read_text(), re.MULTILINE)
    renamed = [(old, new) for old, new in mappings
               if old.startswith(args.app_package + '.') and old != new]
    assert renamed, 'No renamed application classes found in R8 mapping'
    print(f'R8 mapping verified: {len(renamed)} application class entries renamed')
print('Release APK checks passed: non-debuggable, cleartext disabled, network/response log tags absent')
