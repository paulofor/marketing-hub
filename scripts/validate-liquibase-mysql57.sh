#!/usr/bin/env bash
set -euo pipefail

CHANGELOG_ROOT=${CHANGELOG_ROOT:-backend/ads-service/src/main/resources/db/changelog}
MASTER_CHANGELOG=${MASTER_CHANGELOG:-${CHANGELOG_ROOT}/db.changelog-master.yaml}
LIQUIBASE_VALIDATE_SCOPE=${LIQUIBASE_VALIDATE_SCOPE:-changed}

log() {
  printf '[%s] [validate-liquibase-mysql57] %s\n' "$(date -Is)" "$*"
}

fail() {
  printf '[%s] [validate-liquibase-mysql57] ERRO: %s\n' "$(date -Is)" "$*" >&2
  exit 1
}

[[ -f "${MASTER_CHANGELOG}" ]] || fail "changelog mestre não encontrado: ${MASTER_CHANGELOG}"

log "Validando includes relativos no changelog mestre"
python3 - "${MASTER_CHANGELOG}" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
lines = path.read_text(encoding="utf-8").splitlines()
errors = []

for index, line in enumerate(lines):
    match = re.match(r"^(\s*)-\s+include:\s*$", line)
    if not match:
        continue

    indent = len(match.group(1))
    block = []
    for block_line in lines[index + 1:]:
        if not block_line.strip():
            block.append(block_line)
            continue
        current_indent = len(block_line) - len(block_line.lstrip(" "))
        if current_indent <= indent:
            break
        block.append(block_line)

    block_text = "\n".join(block)
    file_match = re.search(r"^\s+file:\s*['\"]?([^'\"\s]+)", block_text, re.MULTILINE)
    if not file_match:
        continue
    file_value = file_match.group(1)
    if file_value.startswith("changesets/") and not re.search(
        r"^\s+relativeToChangelogFile:\s*true\s*$", block_text, re.MULTILINE
    ):
        errors.append(f"{path}:{index + 1}: include relativo sem relativeToChangelogFile: true ({file_value})")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
PY

log "Validando dependências do ledger financeiro do Estúdio"
python3 - "${MASTER_CHANGELOG}" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
required_order = [
    "changesets/2026-08-05-studio-financial-ledger-v1.yaml",
    "changesets/2026-08-06-studio-unassigned-cost-ledger.yaml",
    "changesets/2026-08-06-studio-media-complete-ledger.yaml",
]

positions = []
for changeset in required_order:
    count = text.count(f"file: {changeset}")
    if count != 1:
        print(
            f"{path}: esperado exatamente um include de {changeset}, encontrado(s): {count}",
            file=sys.stderr,
        )
        sys.exit(1)
    positions.append(text.index(f"file: {changeset}"))

if positions != sorted(positions):
    print(
        f"{path}: ordem inválida do ledger do Estúdio; crie a tabela, permita plano nulo e só então execute o backfill completo",
        file=sys.stderr,
    )
    sys.exit(1)
PY

log "Validando padrões temporais e erro MySQL 5.7 1093 (escopo=${LIQUIBASE_VALIDATE_SCOPE})"
python3 - "${CHANGELOG_ROOT}" "${LIQUIBASE_VALIDATE_SCOPE}" <<'PY'
import re
import subprocess
import sys
from pathlib import Path

root = Path(sys.argv[1])
scope = sys.argv[2]
allowed_suffixes = {".yaml", ".yml", ".sql", ".xml"}

if scope == "all":
    files = [
        path
        for path in root.rglob("*")
        if path.is_file() and path.suffix.lower() in allowed_suffixes
    ]
elif scope == "changed":
    candidates = set()
    commands = [
        ["git", "diff", "--name-only", "HEAD", "--", str(root)],
        ["git", "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD", "--", str(root)],
    ]
    for command in commands:
        result = subprocess.run(command, check=False, text=True, capture_output=True)
        if result.returncode == 0:
            candidates.update(line.strip() for line in result.stdout.splitlines() if line.strip())
    files = [
        Path(candidate)
        for candidate in sorted(candidates)
        if Path(candidate).is_file() and Path(candidate).suffix.lower() in allowed_suffixes
    ]
else:
    print(f"Escopo inválido: {scope}. Use changed ou all.", file=sys.stderr)
    sys.exit(1)

if not files:
    print("Nenhum changelog alterado encontrado para validação estática temporal/1093.")
    sys.exit(0)

timestamp_without_default = re.compile(
    r"\b(timestamp)\b(?:(?!\bdefault\b|[\n;]).)*\bnot\s+null\b|"
    r"\bnot\s+null\b(?:(?!\bdefault\b|[\n;]).)*\btimestamp\b",
    re.IGNORECASE,
)
target_statement = re.compile(r"\b(update|delete\s+from)\s+`?([a-zA-Z0-9_]+)`?", re.IGNORECASE)

errors = []

for path in files:
    text = path.read_text(encoding="utf-8", errors="replace")

    for line_no, line in enumerate(text.splitlines(), start=1):
        if timestamp_without_default.search(line):
            errors.append(
                f"{path}:{line_no}: TIMESTAMP NOT NULL sem DEFAULT explícito na mesma declaração"
            )

    for statement in re.split(r";\s*(?:\n|$)", text):
        target = target_statement.search(statement)
        if not target:
            continue
        table = target.group(2)
        subquery_same_table = re.search(
            rf"\(\s*select\b(?:(?!\)).)*\bfrom\s+`?{re.escape(table)}`?\b",
            statement,
            re.IGNORECASE | re.DOTALL,
        )
        if subquery_same_table:
            line_no = text[: text.find(statement)].count("\n") + 1
            errors.append(
                f"{path}:{line_no}: possível erro MySQL 5.7 1093: {target.group(1).upper()} em {table} lendo a mesma tabela em subconsulta"
            )

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
PY

log "Validações estáticas Liquibase/MySQL 5.7 concluídas"
