#!/usr/bin/env python3
"""Repository policy checks; not a replacement for a security audit."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
failures = []


def require(condition, message):
    if not condition:
        failures.append(message)


manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
application = manifest.find('application')
require(application.get(ANDROID + 'usesCleartextTraffic') == 'false', 'Main manifest must forbid cleartext')
network = ET.parse(ROOT / 'app/src/main/res/xml/network_security_config.xml').getroot()
require(all(item.get('cleartextTrafficPermitted') != 'true' for item in network.iter()),
        'Release network configuration must not permit cleartext')
prod = ET.parse(ROOT / 'app/src/prod/res/values/config.xml').getroot()
url = next((item.text or '' for item in prod.findall('string') if item.get('name') == 'api_base_url'), '')
require(url.startswith('https://') and url.endswith('/'), 'Production must explicitly configure an HTTPS base URL')
for filename, sections in [('backup_rules.xml', ['.']),
                           ('data_extraction_rules.xml', ['cloud-backup', 'device-transfer'])]:
    root = ET.parse(ROOT / 'app/src/main/res/xml' / filename).getroot()
    for section in sections:
        exclusions = root.findall(f'{section}/exclude') if section != '.' else root.findall('exclude')
        require(any(item.get('domain') == 'sharedpref' and item.get('path') == 'biz_shared_prefs.xml'
                    for item in exclusions), f'{filename}:{section} must exclude encrypted preferences')
release_logging = (ROOT / 'app/src/release/java/com/daro/agentapp/data/remote/NetworkLogging.kt').read_text()
require('emptyList()' in release_logging, 'Release logging factory must return no interceptors')
for source in ('main', 'release', 'prod'):
    for path in (ROOT / f'app/src/{source}').rglob('*.kt'):
        require('PrettyJsonLoggingInterceptor' not in path.read_text(), f'Debug logger leaked into {path.relative_to(ROOT)}')
for source in ('main', 'release'):
    for path in (ROOT / f'app/src/{source}').rglob('*.kt'):
        require(not path.name.startswith('Fake'), f'Runtime fake in {path.relative_to(ROOT)}')

if failures:
    for failure in failures:
        print('FAIL:', failure)
    sys.exit(1)
print('Foundation configuration checks passed')
