# Release validation

This checklist separates distribution APK checks, isolated release integration tests, and live-backend acceptance. Passing one does not imply the others passed.

## Production HTTPS

```bash
python3 scripts/check_production_https.py
```

The script reads the literal production URL from `app/build.gradle.kts`, verifies the certificate chain and host/IP identity, and sends an unauthenticated HEAD request without following redirects. An HTTP error status still proves TLS and HTTP connectivity, not endpoint correctness. It never uses a trust-all client or sends login credentials.

Current endpoint: `https://43.224.34.135:3003/`. Validation on 2026-10-29 failed with Python `SSL: WRONG_VERSION_NUMBER`; curl also failed during TLS negotiation with no HTTP response. Backend/infrastructure must expose a valid HTTPS listener with a certificate matching the IP address, or provide a hostname and matching certificate. Do not work around this by disabling TLS verification or allowing production HTTP.

## Isolated release session tests

With an API 23+ emulator connected:

```bash
python3 scripts/validate_release_sessions.py --device emulator-5554
```

The helper copies the current Git-visible project into a temporary directory, excluding local signing files. Only that copy is configured to run instrumentation against the release build, with Android's debug signing key and a separate `.releasevalidation` application ID. Your original Gradle/signing configuration and normal app data are unchanged. Reports remain in the printed temporary path. No production account is created or used.

The tests use the real `KeystoreSessionStore`, AES-GCM encryption, and session repository with synthetic credentials. They check:

- Session establishment and restoration through a new store/repository instance.
- Expired credentials cleared during restoration.
- Active timer expiry removing stored credentials.
- Logout clearing storage, invoking user-data cleanup, and preventing restoration.
- Ciphertext contains no raw tokens, repeated writes use different ciphertext, and tampering is rejected.

The test APK needs JUnit discovery metadata and a test-only rule for a compile-time Error Prone annotation dependency. These rules are in `src/androidTest/keepRules/test-rules.keep`; they are not app release keep rules. The isolated harness preserves Kotlin/coroutine runtime APIs and the session API boundaries used by the external test APK; session implementation methods remain eligible for optimization. Those harness-only keep rules are not copied back to the distribution configuration. These tests exercise release behavior with instrumentation boundaries, not the exact distribution DEX graph, so separately audit the distribution APK and smoke-test it. A new repository instance tests disk restoration, not a full OS process-death/navigation acceptance scenario.

## Distribution artifact

```bash
./gradlew :app:assembleProdRelease
python3 scripts/verify_release_apk.py \
  app/build/outputs/apk/prod/release/app-prod-release.apk \
  --mapping app/build/outputs/mapping/prodRelease/mapping.txt \
  --apkanalyzer "$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer"
```

Use the `-unsigned.apk` filename when local release signing is absent. Verify non-debuggable mode, disabled cleartext, absent debug log tags, and renamed application classes. Preserve each release mapping privately. Previous optimized APK smoke checks covered startup, login navigation, empty-login validation, and registration navigation.

## Recorded result — 2026-10-29

On the API 35 emulator, all six data-layer instrumentation tests executed and passed against the isolated minified production-release harness, with zero failures, errors, or skipped tests. This validates real Keystore encryption/tamper rejection and local session restoration, expiry, and logout behavior with synthetic data. Production HTTPS failed, so live login and user-visible process-restart/logout acceptance are not certified.

## Acceptance gaps that remain

The data repository implements session restoration, expiry, and logout. However, the current `AppNavHost` starts at Welcome, does not observe `SessionRepository.state`, and Home has no logout action. Therefore these user-visible flows are not yet complete:

1. Valid restored session routes to the signed-in destination after restarting the process.
2. Expiry redirects away from authenticated screens and clears their back stack.
3. A visible logout action clears credentials and returns to the signed-out flow.

Home is currently also reachable as a preview from Welcome; it must not be treated as proof of authentication or server authorization. Authenticated-request token handling and automatic refresh are not implemented.

After valid production TLS and session navigation are available, use a dedicated test account to verify a real login, force-stop/relaunch restoration, expiry handling, logout, and a second relaunch remaining signed out. Never put passwords/tokens in screenshots, logs, source files, or this report. Production authentication is blocked until the TLS endpoint works; the isolated tests cannot certify it.
