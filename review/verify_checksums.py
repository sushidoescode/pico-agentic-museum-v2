#!/usr/bin/env python3
"""Check that this repository's files match SHA256SUMS and MANIFEST.json.

Usage, from anywhere:  python3 review/verify_checksums.py
It only reads files. It never writes, downloads, builds or installs anything.
"""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# Files an operating system may add by itself; they are reported but never fail the check.
SYSTEM_FILES = {'.DS_Store', 'Thumbs.db', 'desktop.ini'}


def sha256(path):
    digest = hashlib.sha256()
    with open(path, 'rb') as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b''):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    problems = []
    sums = {}
    for line in (ROOT / 'SHA256SUMS').read_text(encoding='utf-8').splitlines():
        expected, name = line.split('  ', 1)
        sums[name] = expected
    actual = {}
    for name, expected in sorted(sums.items()):
        path = ROOT / name
        if not path.is_file():
            problems.append('missing: ' + name)
            continue
        actual[name] = sha256(path)
        if actual[name] != expected:
            problems.append('changed: ' + name)

    manifest = json.loads((ROOT / 'MANIFEST.json').read_text(encoding='utf-8'))
    listed = {row['path'] for row in manifest['files']}
    if listed | {'MANIFEST.json'} != set(sums):
        problems.append('MANIFEST.json and SHA256SUMS do not list the same files')
    for row in manifest['files']:
        path = ROOT / row['path']
        if path.is_file() and (path.stat().st_size != row['bytes'] or actual.get(row['path']) != row['sha256']):
            problems.append('does not match MANIFEST.json: ' + row['path'])

    known = set(sums) | {'SHA256SUMS'}
    extra = sorted(
        rel for rel in (p.relative_to(ROOT).as_posix() for p in ROOT.rglob('*') if p.is_file())
        if rel not in known and not rel.startswith('.git/') and rel.rsplit('/', 1)[-1] not in SYSTEM_FILES
    )

    for problem in problems:
        print('PROBLEM  ' + problem)
    for rel in extra:
        print('NOTE     not part of the package: ' + rel)
    held = len(manifest.get('heldItems', []))
    if problems:
        print('FAILED: {} problem(s) in {} listed files.'.format(len(problems), len(sums)))
        return 1
    print('OK: all {} listed files match SHA256SUMS and MANIFEST.json.'.format(len(sums)))
    print('{} held items are intentionally absent; see ASSET_SUPPLEMENT.md.'.format(held))
    return 0


if __name__ == '__main__':
    sys.exit(main())
