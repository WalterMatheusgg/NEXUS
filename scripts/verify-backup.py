#!/usr/bin/env python3
"""Validate a trusted backup bundle and optionally extract attachments safely."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import tarfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('bundle', type=Path)
parser.add_argument('--extract-attachments', type=Path)
args = parser.parse_args()
bundle = args.bundle.resolve(strict=True)
expected_files = {'oracle.dmp', 'attachments.tar.gz', 'manifest.json'}
entries = {}
for line in (bundle / 'SHA256SUMS').read_text().splitlines():
    digest, name = line.split('  ', 1)
    if name not in expected_files or name in entries or not re.fullmatch(r'[0-9a-f]{64}', digest):
        raise SystemExit('Manifesto de checksums inválido.')
    path = bundle / name
    if path.is_symlink() or not path.is_file():
        raise SystemExit('Arquivo de backup ausente ou link recusado.')
    h = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            h.update(chunk)
    if h.hexdigest() != digest:
        raise SystemExit(f'Checksum divergente: {name}')
    entries[name] = digest
if set(entries) != expected_files:
    raise SystemExit('Backup incompleto.')
manifest = json.loads((bundle / 'manifest.json').read_text())
if manifest.get('formatVersion') != 1 or not re.fullmatch(r'nexus-\d{8}T\d{6}Z-\d+', manifest.get('id', '')):
    raise SystemExit('Formato ou identificador de backup inválido.')
if not re.fullmatch(r'[A-Z][A-Z0-9_]{0,29}', manifest.get('schema', '')):
    raise SystemExit('Schema no manifesto inválido.')
max_bytes = int(os.environ.get('NEXUS_RESTORE_MAX_BYTES', str(20 * 1024 ** 3)))
with tarfile.open(bundle / 'attachments.tar.gz', 'r:gz') as archive:
    members = archive.getmembers()
    if len(members) > 100000 or sum(m.size for m in members) > max_bytes:
        raise SystemExit('Arquivo de anexos excede os limites de restauração.')
    names = set()
    for member in members:
        path = PurePosixPath(member.name)
        if path.is_absolute() or '..' in path.parts or not (member.isfile() or member.isdir()):
            raise SystemExit('Caminho ou tipo de arquivo inseguro no backup de anexos.')
        if str(path) in names:
            raise SystemExit('Caminho duplicado no backup de anexos.')
        names.add(str(path))
    if args.extract_attachments:
        target = args.extract_attachments
        if target.is_symlink() or (target.exists() and (not target.is_dir() or any(target.iterdir()))):
            raise SystemExit('Destino de anexos deve ser um diretório vazio, sem links.')
        target.mkdir(parents=True, exist_ok=True, mode=0o700)
        target = target.resolve()
        for member in members:
            path = target.joinpath(*PurePosixPath(member.name).parts)
            if member.isdir():
                path.mkdir(parents=True, exist_ok=True, mode=0o700)
            else:
                path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
                with archive.extractfile(member) as source, path.open('xb') as output:
                    shutil.copyfileobj(source, output)
                path.chmod(0o600)
print('Checksums e caminhos dos anexos válidos. Isso não substitui a restauração Oracle.')
