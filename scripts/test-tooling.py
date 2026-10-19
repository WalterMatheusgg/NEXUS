#!/usr/bin/env python3
"""Offline checks for backup extraction boundaries and non-overwriting secret setup."""
import hashlib
import io
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent


class ToolingTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='nexus-tooling-')
        self.directory = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def bundle(self, member='safe.txt', symlink=False):
        directory = self.directory / 'bundle'
        directory.mkdir()
        (directory / 'oracle.dmp').write_bytes(b'validator fixture; not an Oracle dump')
        (directory / 'manifest.json').write_text(json.dumps({
            'formatVersion': 1, 'id': 'nexus-20261016T000000Z-1', 'schema': 'NEXUS_APP'
        }))
        with tarfile.open(directory / 'attachments.tar.gz', 'w:gz') as archive:
            entry = tarfile.TarInfo(member)
            if symlink:
                entry.type = tarfile.SYMTYPE
                entry.linkname = '/etc/passwd'
                archive.addfile(entry)
            else:
                data = b'attachment fixture'
                entry.size = len(data)
                archive.addfile(entry, io.BytesIO(data))
        files = ('oracle.dmp', 'attachments.tar.gz', 'manifest.json')
        (directory / 'SHA256SUMS').write_text(''.join(
            f'{hashlib.sha256((directory / name).read_bytes()).hexdigest()}  {name}\n'
            for name in files
        ))
        return directory

    def validate(self, bundle, target=None):
        args = [sys.executable, str(ROOT / 'scripts/verify-backup.py'), str(bundle)]
        if target:
            args += ['--extract-attachments', str(target)]
        return subprocess.run(args, capture_output=True, text=True)

    def test_valid_bundle_extracts_private_file(self):
        target = self.directory / 'out'
        result = self.validate(self.bundle(), target)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((target / 'safe.txt').read_bytes(), b'attachment fixture')
        self.assertEqual((target / 'safe.txt').stat().st_mode & 0o777, 0o600)

    def test_parent_path_is_rejected_before_extraction(self):
        target = self.directory / 'out'
        result = self.validate(self.bundle('../escaped.txt'), target)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.directory / 'escaped.txt').exists())
        self.assertFalse(target.exists())

    def test_absolute_path_is_rejected(self):
        result = self.validate(self.bundle('/absolute.txt'))
        self.assertNotEqual(result.returncode, 0)

    def test_symlink_in_archive_is_rejected(self):
        result = self.validate(self.bundle('link', symlink=True))
        self.assertNotEqual(result.returncode, 0)

    def test_corruption_is_rejected(self):
        directory = self.bundle()
        (directory / 'oracle.dmp').write_bytes(b'corrupt fixture')
        self.assertNotEqual(self.validate(directory).returncode, 0)

    def test_nonempty_destination_is_preserved(self):
        target = self.directory / 'out'
        target.mkdir()
        (target / 'keep.txt').write_text('preserve')
        self.assertNotEqual(self.validate(self.bundle(), target).returncode, 0)
        self.assertEqual((target / 'keep.txt').read_text(), 'preserve')

    def test_init_creates_private_secrets_and_refuses_overwrite(self):
        scripts = self.directory / 'scripts'
        scripts.mkdir()
        for name in ('common.sh', 'init-env.sh'):
            shutil.copyfile(ROOT / 'scripts' / name, scripts / name)
        shutil.copyfile(ROOT / '.env.example', self.directory / '.env.example')
        command = ['bash', str(scripts / 'init-env.sh')]
        first = subprocess.run(command, capture_output=True, text=True)
        self.assertEqual(first.returncode, 0, first.stderr)
        env = self.directory / '.env'
        before = env.read_bytes()
        self.assertEqual(env.stat().st_mode & 0o777, 0o600)
        values = dict(line.split('=', 1) for line in before.decode().splitlines() if '=' in line and not line.startswith('#'))
        secrets = [values[key] for key in ('ORACLE_SYS_PASSWORD', 'ORACLE_APP_PASSWORD', 'NEXUS_SETUP_TOKEN')]
        self.assertEqual(len(set(secrets)), 3)
        self.assertTrue(all(len(value) == 64 for value in secrets))
        self.assertTrue(all(value not in first.stdout + first.stderr for value in secrets))
        second = subprocess.run(command, capture_output=True, text=True)
        self.assertNotEqual(second.returncode, 0)
        self.assertEqual(env.read_bytes(), before)


if __name__ == '__main__':
    unittest.main(verbosity=2)
