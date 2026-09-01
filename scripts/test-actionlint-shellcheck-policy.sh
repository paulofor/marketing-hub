#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPORARY_DIRECTORY="$(mktemp -d)"
trap 'rm -rf -- "${TEMPORARY_DIRECTORY}"' EXIT

CAPTURE_FILE="${TEMPORARY_DIRECTORY}/shellcheck-arguments"
FAKE_SHELLCHECK="${TEMPORARY_DIRECTORY}/shellcheck"
FIXTURE="${TEMPORARY_DIRECTORY}/workflow.yml"

cat >"${FAKE_SHELLCHECK}" <<'FAKE_SHELLCHECK_SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" >"${ACTIONLINT_SHELLCHECK_CAPTURE}"
cat >/dev/null
printf '%s\n' '[]'
FAKE_SHELLCHECK_SCRIPT
chmod 0755 "${FAKE_SHELLCHECK}"

cat >"${FIXTURE}" <<'WORKFLOW_FIXTURE'
name: Actionlint ShellCheck policy
on: push
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - run: echo "$GITHUB_SHA"
WORKFLOW_FIXTURE

ACTIONLINT_SHELLCHECK_EXECUTABLE="${FAKE_SHELLCHECK}" \
ACTIONLINT_SHELLCHECK_CAPTURE="${CAPTURE_FILE}" \
  bash "${SCRIPT_DIRECTORY}/run-actionlint.sh" "${FIXTURE}"

[[ -s "${CAPTURE_FILE}" ]]
[[ "$(head -n 1 "${CAPTURE_FILE}")" == "--severity=warning" ]]
grep -Fx -- "-f" "${CAPTURE_FILE}" >/dev/null
grep -Ex -- 'json1?' "${CAPTURE_FILE}" >/dev/null

echo "Política ShellCheck do Actionlint validada."
