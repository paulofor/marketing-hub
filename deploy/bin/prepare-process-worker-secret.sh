#!/usr/bin/env bash
set -euo pipefail
secret_directory="${1:?Informe o diretório protegido da credencial do conciliador}"
python3 - "$secret_directory" <<'PY'
import os
import secrets
import sys
from pathlib import Path

directory = Path(sys.argv[1])
directory.mkdir(parents=True, exist_ok=True, mode=0o700)
os.chmod(directory, 0o700)
token_file = directory / 'token'
try:
    descriptor = os.open(token_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o400)
except FileExistsError:
    if token_file.is_symlink() or len(token_file.read_bytes().strip()) < 32:
        raise SystemExit('Credencial existente do conciliador inválida; publicação interrompida.')
else:
    with os.fdopen(descriptor, 'w') as output:
        output.write(secrets.token_urlsafe(48))
# Diretório restrito no host; arquivo somente leitura para usuários dos containers.
os.chmod(token_file, 0o444)
PY
