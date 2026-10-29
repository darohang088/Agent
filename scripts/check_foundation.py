#!/usr/bin/env python3
"""Repository policy checks; not a replacement for a security audit."""
from pathlib import Path
import sys
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
failures = []


def require(condition, message):
    if not condition:
        failures.append(message)


manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
application = manifest.find('application')
require(application.get(ANDROID + 'usesCleartextTraffic') == '${allowCleartextTraffic}',
        'Main manifest must use the build-controlled cleartext placeholder')
network = ET.parse(ROOT / 'app/src/main/res/xml/network_security_config.xml').getroot()
require(all(item.get('cleartextTrafficPermitted') != 'true' for item in network.iter()),
        'Release network configuration must not permit cleartext')
gradle = (ROOT / 'app/build.gradle.kts').read_text()
require('manifestPlaceholders["allowCleartextTraffic"] = "false"' in gradle,
        'Default manifest cleartext value must be false')
release_block = re.search(r'release\s*\{(.*?)optimization', gradle, re.S)
require(release_block is not None and 'allowCleartextTraffic' not in release_block.group(1),
        'Release must retain the default cleartext restriction')
require('packageScope' not in gradle, 'Release R8 must cover the whole application')
require('enable = true' in gradle, 'Release R8 must be enabled')
prod = re.search(r'create\("prod"\)\s*\{([^}]+)\}', gradle)
require(prod is not None and 'buildConfigField("String", "API_BASE_URL", "\\"https://' in prod.group(1),
        'Production must explicitly configure an HTTPS BuildConfig API URL')
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
