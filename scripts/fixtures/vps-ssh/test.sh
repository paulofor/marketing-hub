#!/usr/bin/env bash
set -euo pipefail

# Servidor e clientes reais comunicam somente pelo loopback de um container sem rede.
test_directory="$(mktemp -d)"
sshd_pid=""
cleanup_test() {
  local test_status="$?"
  if [[ "$test_status" != 0 ]]; then
    tail -n 20 "$test_directory/server.log" "$test_directory"/accepted-*/client.log 2>/dev/null || true
  fi
  if [[ -n "$sshd_pid" ]]; then
    kill "$sshd_pid" 2>/dev/null || true
    wait "$sshd_pid" 2>/dev/null || true
  fi
  rm -rf -- "$test_directory"
}
trap cleanup_test EXIT

for identity in host accepted rejected changed-host; do
  ssh-keygen -q -t ed25519 -N '' -f "$test_directory/$identity"
done
ssh-keygen -q -t ed25519 -N 'synthetic-passphrase' -f "$test_directory/encrypted"
mkdir -p /run/sshd
cp "$test_directory/accepted.pub" /run/sshd/authorized_keys
chmod 600 /run/sshd/authorized_keys
cat >"$test_directory/sshd_config" <<EOF
ListenAddress 127.0.0.1
Port 22
HostKey $test_directory/host
AuthorizedKeysFile /run/sshd/authorized_keys
PidFile $test_directory/sshd.pid
PermitRootLogin prohibit-password
PasswordAuthentication no
KbdInteractiveAuthentication no
UsePAM no
Subsystem sftp internal-sftp
EOF
/usr/sbin/sshd -D -e -f "$test_directory/sshd_config" >"$test_directory/server.log" 2>&1 &
sshd_pid="$!"
for attempt in {1..20}; do
  if ssh-keyscan -T 1 -t ed25519 127.0.0.1 >/dev/null 2>&1; then break; fi
  printf 'Aguardando servidor SSH local: tentativa=%s\n' "$attempt"
  sleep 0.1
done
kill -0 "$sshd_pid"
test "$(stat -c '%a' /app/ssh-keyscan-with-retry.sh)" = 644

for accepted_index in 0 1 2 3; do
  case_directory="$test_directory/accepted-$accepted_index"
  mkdir -p "$case_directory"
  touch "$case_directory/github-env"
  (
    export VPS_SSH_KEY_PRIMARY VPS_SSH_KEY_FALLBACK_1 VPS_SSH_KEY_FALLBACK_2 VPS_SSH_KEY_FALLBACK_3
    VPS_SSH_KEY_PRIMARY="$(cat "$test_directory/rejected")"
    VPS_SSH_KEY_FALLBACK_1="$VPS_SSH_KEY_PRIMARY"
    VPS_SSH_KEY_FALLBACK_2="$VPS_SSH_KEY_PRIMARY"
    VPS_SSH_KEY_FALLBACK_3="$VPS_SSH_KEY_PRIMARY"
    if [[ "$accepted_index" = 0 ]]; then
      VPS_SSH_KEY_PRIMARY="$(cat "$test_directory/accepted")"
    else
      printf -v "VPS_SSH_KEY_FALLBACK_$accepted_index" '%s' "$(cat "$test_directory/accepted")"
    fi
    VPS_SSH_RUNTIME_ROOT="$case_directory/runtime" \
      bash /app/configure-vps-ssh-fallback.sh 127.0.0.1 root "$case_directory/github-env"
  ) >"$case_directory/client.log" 2>&1
  grep -Fxq 'SSH_DEPLOY_READY=true' "$case_directory/github-env"
  ssh_config="$(sed -n 's/^SSH_COMMON_ARGS=-F //p' "$case_directory/github-env")"
  ssh -F "$ssh_config" root@127.0.0.1 true
  printf 'prova-local-%s\n' "$accepted_index" >"$case_directory/source"
  scp -q -F "$ssh_config" "$case_directory/source" "root@127.0.0.1:$case_directory/scp-copy"
  rsync -az -e "ssh -F $ssh_config" "$case_directory/source" "root@127.0.0.1:$case_directory/rsync-copy"
  cmp "$case_directory/source" "$case_directory/scp-copy"
  cmp "$case_directory/source" "$case_directory/rsync-copy"
  if grep -q 'PRIVATE KEY' "$case_directory/client.log"; then
    echo 'Credencial exposta no diagnóstico local.' >&2
    exit 1
  fi
  echo "OpenSSH real: identidade $accepted_index, SSH, SCP e rsync aprovados."
done

# Chave cifrada sem passphrase não deve bloquear um fallback válido.
touch "$test_directory/encrypted-env"
VPS_SSH_KEY_PRIMARY="$(cat "$test_directory/encrypted")" \
VPS_SSH_KEY_FALLBACK_1="$(cat "$test_directory/accepted")" \
VPS_SSH_RUNTIME_ROOT="$test_directory/encrypted-runtime" \
  bash /app/configure-vps-ssh-fallback.sh 127.0.0.1 root "$test_directory/encrypted-env" >/dev/null
grep -Fxq 'SSH_DEPLOY_READY=true' "$test_directory/encrypted-env"

# Recusa total não pode disponibilizar credenciais nem autorização para as próximas etapas.
touch "$test_directory/rejected-env"
if VPS_SSH_KEY_PRIMARY="$(cat "$test_directory/rejected")" \
  VPS_SSH_RUNTIME_ROOT="$test_directory/rejected-runtime" \
    bash /app/configure-vps-ssh-fallback.sh 127.0.0.1 root "$test_directory/rejected-env" \
    >"$test_directory/rejected.log" 2>&1; then
  echo 'Servidor aceitou identidade não autorizada.' >&2
  exit 1
fi
test ! -s "$test_directory/rejected-env"
test -z "$(find "$test_directory/rejected-runtime" -mindepth 1 -print -quit)"

# Uma chave de host divergente depois do preflight deve bloquear a operação seguinte.
known_hosts="$(awk '$1 == "UserKnownHostsFile" {print $2}' "$ssh_config")"
printf '127.0.0.1 %s\n' "$(cat "$test_directory/changed-host.pub")" >"$known_hosts"
if ssh -F "$ssh_config" root@127.0.0.1 true >"$test_directory/changed-host.log" 2>&1; then
  echo 'Cliente aceitou mudança de identidade do host.' >&2
  exit 1
fi
grep -Fq 'Host key verification failed' "$test_directory/changed-host.log"
echo 'OpenSSH real: chave cifrada, recusa total, limpeza e identidade divergente aprovadas.'
