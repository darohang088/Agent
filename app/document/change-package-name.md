# Change the package name

Run the executable script from the project root. It requires Bash and Python 3.

Preview the changes first:

```bash
./change_package_name.sh --dry-run com.example.newapp
```

Apply the rename:

```bash
./change_package_name.sh com.example.newapp
```

The script detects the current namespace and application ID from `app/build.gradle.kts`; you do not need to edit an old-package constant in the script. The new value becomes both the namespace and base application ID.

It updates package declarations, imports, fully qualified references, XML references, package paths in project scripts/documentation, and Java/Kotlin package directories across every existing `app/src` source set. The staging/UAT/pre-production suffixes stay unchanged.

API URLs, app display names, root project branding, generated builds/caches, local settings, provider configuration files, and archived reference imports are preserved. Images are not modified. Firebase and other external services must be registered for the new ID separately, and their configuration files must be downloaded again as needed.

The script rejects invalid identifiers and destination file collisions before editing. Files are staged during relocation, and ordinary application errors or interrupts trigger rollback. If rollback itself fails, the script preserves its staging directory and reports its location for recovery. Commit your current work before running a real rename so recovery from a machine failure is also straightforward.

After a rename, sync Gradle in Android Studio and verify:

```bash
python3 scripts/check_foundation.py
./gradlew :app:assembleDebug :app:testStagingDebugUnitTest
```

A changed application ID produces a separate installed app; existing app data and sessions do not automatically transfer. The script does not commit, push, publish, or modify external services. It targets this project's single app module and literal Kotlin Gradle DSL configuration; future additional modules, native/JNI symbols, or generated provider registrations require their own review.

Script regression tests:

```bash
python3 -m unittest discover -s scripts/tests -v
```
