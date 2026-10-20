#!/usr/bin/env bash
# Comprova limite do console, preservação dos bytes e propagação de falha sem executar modelos.
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fixture="$(mktemp -d)"
trap 'rm -rf "$fixture"' EXIT
mkdir -p "$fixture/bin"
cat > "$fixture/bin/mvn" <<'MAVEN'
#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
print('x' * 4800000)
PY
exit "${SYNTHETIC_MAVEN_STATUS:-0}"
MAVEN
chmod +x "$fixture/bin/mvn"
for expected in 0 17; do
  actual=0
  (cd "$fixture" && PATH="$fixture/bin:$PATH" SYNTHETIC_MAVEN_STATUS="$expected" bash "$repository_root/scripts/run-ai-worker-tests.sh") > "$fixture/console" 2>&1 || actual=$?
  [[ "$actual" == "$expected" ]]
  [[ "$(wc -c < "$fixture/target/test-evidence/maven-test.log")" -eq 4800001 ]]
  [[ "$(wc -c < "$fixture/console")" -lt 4096 ]]
done
printf 'Saída do Worker IA preservada; console limitado e exit codes 0/17 comprovados.\n'
