#!/usr/bin/env python3
"""Run real-storage session tests in an isolated, minified release app on a selected device."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--device', required=True, help='ADB serial, for example emulator-5554')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
workspace = Path(tempfile.mkdtemp(prefix='agent-release-validation-'))
files = subprocess.check_output(
    ['git', 'ls-files', '-co', '--exclude-standard', '-z'], cwd=root,
).decode().split('\0')
for name in set(filter(None, files)):
    source = root / name
    relative = Path(name)
    if (not source.is_file() or source.is_symlink() or name.startswith('app/keys/')
            or relative.name.startswith('.env') or relative.suffix in {'.jks', '.keystore'}):
        continue
    target = workspace / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
# Only copy the SDK setting; signing files and other local properties stay behind.
local = root / 'local.properties'
if local.exists():
    sdk_lines = [line for line in local.read_text().splitlines() if line.startswith('sdk.dir=')]
    (workspace / 'local.properties').write_text('\n'.join(sdk_lines) + '\n')
gradle_file = workspace / 'app/build.gradle.kts'
gradle = gradle_file.read_text()
marker = '    buildFeatures {'
if gradle.count(marker) != 1:
    raise SystemExit(f'Unsupported Gradle layout; original project unchanged. Copy: {workspace}')
gradle = gradle.replace(marker, '''    // Isolated validation harness, never applied to the original project.
    testBuildType = "release"
    buildTypes.getByName("release").applicationIdSuffix = ".releasevalidation"
    buildTypes.getByName("release").signingConfig = signingConfigs.getByName("debug")
''' + marker)
gradle_file.write_text(gradle)
# The test runner calls this dependency from outside the app's optimized graph.
# Keep it in the isolated harness only, never in the distribution configuration.
harness_rules = workspace / 'app/src/release/keepRules/validation-entrypoints.keep'
harness_rules.parent.mkdir(parents=True, exist_ok=True)
harness_rules.write_text("""# Isolated harness only: preserve APIs used from the external test APK.
-keep class androidx.tracing.Trace { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep,allowoptimization class com.daro.agentapp.data.repository.DefaultSessionRepository { *; }
-keep,allowoptimization class com.daro.agentapp.data.local.session.** { *; }
-keep,allowoptimization class com.daro.agentapp.domain.session.** { *; }
-keep,allowoptimization class com.daro.agentapp.domain.model.AuthSession { *; }
-keep,allowoptimization class com.daro.agentapp.domain.model.AuthUser { *; }
-keep,allowoptimization class com.daro.agentapp.core.common.** { *; }
""")
env = os.environ.copy()
env['ANDROID_SERIAL'] = args.device
print(f'Isolated release test copy: {workspace}', flush=True)
print('Separate .releasevalidation app; synthetic credentials; no production login requests.', flush=True)
result = subprocess.run([
    './gradlew', ':app:connectedProdReleaseAndroidTest',
    '-Pandroid.testInstrumentationRunnerArguments.package=com.daro.agentapp.data',
], cwd=workspace, env=env)
print(f'Reports retained at: {workspace / "app/build/reports/androidTests"}', flush=True)
print('The test APK has instrumentation-driven keep rules; audit the separately built distribution APK too.', flush=True)
if result.returncode:
    raise SystemExit(result.returncode)
reports = list((workspace / 'app/build/outputs/androidTest-results').rglob('TEST-*.xml'))
suites = [ET.parse(path).getroot() for path in reports]
executed = sum(int(suite.get('tests', '0')) for suite in suites)
failed = sum(int(suite.get('failures', '0')) + int(suite.get('errors', '0'))
             + int(suite.get('skipped', '0')) for suite in suites)
if executed < 6 or failed:
    raise SystemExit(f'Validation incomplete: {executed} tests, {failed} failed/errors/skipped')
print(f'PASS: {executed} release storage/session tests executed.', flush=True)
