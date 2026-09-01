#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
scan_script="$test_root/scripts/ssh-keyscan-with-retry.sh"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT HUP INT TERM

mock_scan="$test_tmp_dir/mock-ssh-keyscan"
mock_count="$test_tmp_dir/count"
known_hosts="$test_tmp_dir/known_hosts"

cat >"$mock_scan" <<'MOCK_SCAN'
#!/usr/bin/env bash
set -euo pipefail
count="$(cat "$MOCK_SCAN_COUNT_FILE" 2>/dev/null || printf '0')"
count="$((count + 1))"
printf '%s\n' "$count" >"$MOCK_SCAN_COUNT_FILE"
if [ "$count" -lt "${MOCK_SCAN_SUCCEED_AT:-3}" ]; then
  exit 1
fi
printf '|1|host-hash|salt ssh-ed25519 AAAATESTHOSTKEY\n'
MOCK_SCAN
chmod 700 "$mock_scan"

MOCK_SCAN_COUNT_FILE="$mock_count" \
MOCK_SCAN_SUCCEED_AT=3 \
SSH_KEYSCAN_BIN="$mock_scan" \
SSH_KEYSCAN_MAX_ATTEMPTS=3 \
SSH_KEYSCAN_TIMEOUT_SECONDS=1 \
SSH_KEYSCAN_RETRY_DELAY_SECONDS=0 \
  bash "$scan_script" 191.252.120.96 "$known_hosts"

test "$(cat "$mock_count")" = "3"
test "$(grep -Fc 'AAAATESTHOSTKEY' "$known_hosts")" = "1"
test "$(stat -c '%a' "$known_hosts")" = "600"

printf '0\n' >"$mock_count"
if MOCK_SCAN_COUNT_FILE="$mock_count" \
  MOCK_SCAN_SUCCEED_AT=4 \
  SSH_KEYSCAN_BIN="$mock_scan" \
  SSH_KEYSCAN_MAX_ATTEMPTS=3 \
  SSH_KEYSCAN_TIMEOUT_SECONDS=1 \
  SSH_KEYSCAN_RETRY_DELAY_SECONDS=0 \
  bash "$scan_script" 191.252.120.96 "$known_hosts" >/dev/null 2>&1; then
  echo "O helper aceitou falha persistente do ssh-keyscan." >&2
  exit 1
fi

test "$(cat "$mock_count")" = "3"
echo "Retry limitado do ssh-keyscan validado."
