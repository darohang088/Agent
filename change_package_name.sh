#!/usr/bin/env bash
# Rename this app's namespace/application ID without touching generated or local configuration.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v python3 >/dev/null 2>&1; then
    echo "Error: Python 3 is required." >&2
    exit 1
fi

python3 - "$SCRIPT_DIR" "$@" <<'PY'
import argparse
import os
from pathlib import Path
import re
import stat
import sys
import tempfile


class RenameError(Exception):
    pass


parser = argparse.ArgumentParser(
    prog="change_package_name.sh",
    description="Change the app namespace, application ID, imports, and package folders.",
    epilog="Examples: ./change_package_name.sh --dry-run com.example.newapp | ./change_package_name.sh com.example.newapp",
)
parser.add_argument("new_package", help="New Android package/application ID, e.g. com.example.newapp")
parser.add_argument("--dry-run", action="store_true", help="List proposed changes without changing files")
args = parser.parse_args(sys.argv[2:])
root = Path(sys.argv[1]).resolve()

KEYWORDS = set("""abstract as assert boolean break byte case catch char class const continue default
do double else enum extends false final finally float for fun goto if implements import in instanceof
int interface is long native new null object package private protected public return short static
strictfp super switch synchronized this throw throws transient true try typealias typeof val var void
volatile when while""".split())
TEXT_SUFFIXES = {".kt", ".kts", ".java", ".gradle", ".toml", ".xml", ".md", ".txt",
                 ".py", ".sh", ".yml", ".yaml", ".properties", ".json", ".pro", ".keep"}
GENERATED_DIRS = {".git", ".gradle", ".idea", ".kotlin", ".cxx", ".externalNativeBuild",
                  "build", "node_modules", "__pycache__", ".venv", "venv"}
EXTERNAL_CONFIGS = {"google-services.json", "agconnect-services.json"}
PROTECTED_TEXT = re.compile(
    r'''((?:https?|wss?)://[^\s"'<>]+|<string\b[^>]*\bname=["']app_name["'][^>]*>.*?</string>|^\s*rootProject\.name\s*=[^\r\n]*)''',
    re.MULTILINE | re.DOTALL,
)


def validate_package(value):
    parts = value.split(".")
    if len(parts) < 2 or any(not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", part) or part in KEYWORDS
                             for part in parts):
        raise RenameError("Use at least two dot-separated identifiers starting with letters; keywords are not allowed.")


def atomic_write(path, content, mode):
    descriptor, temporary = tempfile.mkstemp(prefix=".package-edit-", dir=path.parent)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.chmod(temporary, mode)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def project_files():
    for current, directories, names in os.walk(root, followlinks=False):
        parent = Path(current)
        relative = parent.relative_to(root)
        in_source = relative.parts[:2] == ("app", "src")
        directories[:] = sorted(name for name in directories
                                if not (parent / name).is_symlink()
                                and (in_source or name not in GENERATED_DIRS)
                                and not name.startswith(".package-rename-")
                                and (parent / name).relative_to(root).parts[:3]
                                != ("app", "document", "reference-imports"))
        for name in sorted(names):
            path = parent / name
            if path.is_symlink() or name == "local.properties" or name.startswith(".env"):
                continue
            if name == "change_package_name.sh" or name in EXTERNAL_CONFIGS:
                continue
            # Tooling fixtures describe unrelated sample packages, not this app's identity.
            if path.relative_to(root).parts[:2] == ("scripts", "tests"):
                continue
            if path.suffix in TEXT_SUFFIXES:
                yield path


def prune_empty_packages(moves):
    for source, _, language_root in moves:
        parent = source.parent
        while parent != language_root and language_root in parent.parents:
            try:
                parent.rmdir()
            except OSError:
                break
            parent = parent.parent


try:
    validate_package(args.new_package)
    gradle = root / "app/build.gradle.kts"
    if not gradle.is_file():
        raise RenameError("Run the script from an Android project containing app/build.gradle.kts.")
    configuration = gradle.read_text(encoding="utf-8")
    namespaces = re.findall(r'^\s*namespace\s*=\s*"([^"]+)"', configuration, re.MULTILINE)
    application_ids = re.findall(r'^\s*applicationId\s*=\s*"([^"]+)"', configuration, re.MULTILINE)
    if len(namespaces) != 1 or len(application_ids) != 1:
        raise RenameError("Expected one literal namespace and one literal default applicationId in app/build.gradle.kts.")
    namespace, application_id = namespaces[0], application_ids[0]
    validate_package(namespace)
    validate_package(application_id)
    target = args.new_package
    if namespace == application_id == target:
        print(f"Already configured as {target}; nothing to change.")
        sys.exit(0)

    old_path, new_path = namespace.replace(".", "/"), target.replace(".", "/")
    moves = []
    for source_set in sorted((root / "app/src").iterdir()):
        if not source_set.is_dir() or source_set.is_symlink():
            continue
        for language in ("java", "kotlin"):
            language_root = source_set / language
            old_root = language_root / old_path
            if not old_root.exists():
                continue
            if any(parent.is_symlink() for parent in (old_root, *old_root.parents) if parent != root):
                raise RenameError(f"Refusing a symlinked source package: {old_root.relative_to(root)}")
            for source in sorted(old_root.rglob("*")):
                if source.is_symlink():
                    raise RenameError(f"Refusing a symlink in source package: {source.relative_to(root)}")
                if not source.is_file():
                    continue
                destination = language_root / new_path / source.relative_to(old_root)
                if destination != source:
                    if destination.exists():
                        raise RenameError(f"Destination already exists; no files changed: {destination.relative_to(root)}")
                    moves.append((source, destination, language_root))

    replacements = {namespace: target, application_id: target}
    dotted = re.compile(r"(?<![A-Za-z0-9_$.])(?:" + "|".join(re.escape(item)
                         for item in sorted(replacements, key=len, reverse=True)) + r")(?![A-Za-z0-9_$])")
    package_path = re.compile(r"(?<![A-Za-z0-9_])" + re.escape(old_path) + r"(?![A-Za-z0-9_])")
    edits = []
    for path in project_files():
        original = path.read_bytes()
        try:
            text = original.decode("utf-8")
        except UnicodeDecodeError:
            continue
        pieces = PROTECTED_TEXT.split(text)
        for index in range(0, len(pieces), 2):
            pieces[index] = dotted.sub(lambda match: replacements[match.group()], pieces[index])
            if old_path != new_path:
                pieces[index] = package_path.sub(new_path, pieces[index])
        changed = "".join(pieces)
        if changed != text:
            edits.append((path, original, changed.encode("utf-8"), stat.S_IMODE(path.stat().st_mode)))

    print(f"Namespace:     {namespace} -> {target}")
    print(f"Application ID: {application_id} -> {target}")
    print(f"{'Dry run: would update' if args.dry_run else 'Updating'} {len(edits)} text files and move {len(moves)} source files.")
    if args.dry_run:
        for path, _, _, _ in edits:
            print(f"  update {path.relative_to(root)}")
        for source, destination, _ in moves:
            print(f"  move   {source.relative_to(root)} -> {destination.relative_to(root)}")
        print("Dry run complete; no files changed.")
        sys.exit(0)

    staging = Path(tempfile.mkdtemp(prefix=".package-rename-", dir=root))
    staged = []
    try:
        for path, _, changed, mode in edits:
            atomic_write(path, changed, mode)
        # Stage every source before placing destinations, supporting parent/child package changes.
        for index, (source, destination, _) in enumerate(moves):
            temporary = staging / str(index)
            os.replace(source, temporary)
            staged.append([source, temporary, destination, False])
        for item in staged:
            source, temporary, destination, _ = item
            destination.parent.mkdir(parents=True, exist_ok=True)
            os.replace(temporary, destination)
            item[3] = True
    except BaseException:
        rollback_failed = False
        # Bring placed files back to staging before restoring any potentially overlapping paths.
        for source, temporary, destination, placed in reversed(staged):
            try:
                if placed and destination.exists():
                    os.replace(destination, temporary)
            except OSError:
                rollback_failed = True
        for source, temporary, destination, placed in staged:
            try:
                if temporary.exists():
                    source.parent.mkdir(parents=True, exist_ok=True)
                    os.replace(temporary, source)
            except OSError:
                rollback_failed = True
        for path, original, _, mode in edits:
            try:
                atomic_write(path, original, mode)
            except OSError:
                rollback_failed = True
        if rollback_failed:
            print(f"Rollback needs manual recovery; preserved staging directory: {staging}", file=sys.stderr)
        else:
            staging.rmdir()
            print("Changes rolled back after an error.", file=sys.stderr)
        raise
    staging.rmdir()
    prune_empty_packages(moves)
    print("Package rename complete. Flavor applicationIdSuffix values and app branding are unchanged.")
    print("Sync Gradle in Android Studio, then run ./gradlew :app:assembleDebug to verify.")
    print("A changed application ID produces a separate installed app; existing app sessions do not transfer.")
    print("Register the new ID with external services and replace provider configuration files separately.")
    print("Generated builds/caches, local settings, and archived reference imports were left unchanged.")
except (RenameError, OSError) as error:
    print(f"Error: {error}", file=sys.stderr)
    sys.exit(1)
PY
