#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT

mkdir -p "$test_tmp_dir/bin" "$test_tmp_dir/sessions"
cat >"$test_tmp_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >>"${DOCKER_DOUBLE_CALLS:?}"

if [[ "$1" == "info" ]]; then
  exit "${DOCKER_DOUBLE_INFO_STATUS:-0}"
fi

if [[ "$1" == "build" ]]; then
  exit 0
fi

if [[ "$1 $2 $3" == "image ls --all" ]]; then
  printf '%s\n' old-stale old-active recent in-use mixed-tag invalid-session
  exit 0
fi

if [[ "$1 $2" == "image inspect" ]]; then
  image_id="${!#}"
  case "$*" in
    *"{{.Created}}"*)
      if [[ "$image_id" == "recent" ]]; then
        echo "2999-01-01T00:00:00Z"
      else
        echo "2000-01-01T00:00:00Z"
      fi
      ;;
    *"com.marketinghub.homologation.session"*)
      case "$image_id" in
        old-stale) echo "stale" ;;
        old-active) echo "active" ;;
        recent) echo "recent" ;;
        in-use) echo "in-use" ;;
        mixed-tag) echo "mixed" ;;
        invalid-session) echo "../../unsafe" ;;
      esac
      ;;
    *".RepoTags"*)
      case "$image_id" in
        old-stale)
          printf '%s\n' \
            "aihub-homologation/stale/backend:r1" \
            "aihub-homologation/stale/backend:r2"
          ;;
        old-active) echo "aihub-homologation/active/backend:latest" ;;
        recent) echo "aihub-homologation/recent/backend:latest" ;;
        in-use) echo "aihub-homologation/in-use/backend:latest" ;;
        mixed-tag)
          printf '%s\n' \
            "aihub-homologation/mixed/backend:latest" \
            "marketinghub/backend:production"
          ;;
        invalid-session) echo "aihub-homologation/unsafe/backend:latest" ;;
      esac
      ;;
  esac
  exit 0
fi

if [[ "$1 $2" == "ps --all" ]]; then
  if [[ "$*" == *"ancestor=in-use"* ]]; then
    echo "container-in-use"
  fi
  exit 0
fi

if [[ "$1 $2" == "image rm" ]]; then
  exit 0
fi

echo "Chamada Docker inesperada: $*" >&2
exit 2
DOCKER_DOUBLE
chmod +x "$test_tmp_dir/bin/docker"

calls_file="$test_tmp_dir/calls"
: >"$calls_file"
PATH="$test_tmp_dir/bin:$PATH" \
  DOCKER_DOUBLE_CALLS="$calls_file" \
  AIHUB_HOMOLOGATION_SESSION="build-session" \
  AIHUB_HOMOLOGATION_IMAGE_TAG="round-1" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" \
    backend --file Dockerfile . >/dev/null

grep -F 'build --label com.marketinghub.homologation.temporary=true' "$calls_file" >/dev/null
grep -F -- '--label com.marketinghub.homologation.session=build-session' "$calls_file" >/dev/null
grep -F -- '--tag aihub-homologation/build-session/backend:round-1' "$calls_file" >/dev/null

for invalid_session in 'session-20260905T080631Z' 'bad..session' 'bad--session'; do
  if PATH="$test_tmp_dir/bin:$PATH" DOCKER_DOUBLE_CALLS="$calls_file" \
    AIHUB_HOMOLOGATION_SESSION="$invalid_session" \
    bash "$test_root/scripts/docker-build-temporary-image.sh" backend . >/dev/null 2>&1; then
    echo "O build aceitou sessão incompatível com nome de repositório Docker." >&2
    exit 1
  fi
done

printf '#!/usr/bin/env bash\nexit 0\n' > "$test_tmp_dir/cleanup-noop"
chmod +x "$test_tmp_dir/cleanup-noop"
# A expansão pertence ao shell filho, após o wrapper gerar a sessão.
# shellcheck disable=SC2016
env -u AIHUB_HOMOLOGATION_SESSION \
  AIHUB_DOCKER_CLEANUP_SCRIPT="$test_tmp_dir/cleanup-noop" \
  AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  bash "$test_root/scripts/run-docker-homologation.sh" bash -c \
    '[[ "$AIHUB_HOMOLOGATION_SESSION" =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]]' >/dev/null

if PATH="$test_tmp_dir/bin:$PATH" DOCKER_DOUBLE_CALLS="$calls_file" \
  AIHUB_HOMOLOGATION_SESSION="../unsafe" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" backend . \
  >/dev/null 2>&1; then
  echo "O build aceitou uma sessão insegura." >&2
  exit 1
fi

exec 9>"$test_tmp_dir/sessions/active.lock"
flock 9
: >"$calls_file"
cleanup_output="$(
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$calls_file" \
    AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
    AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
    AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=3600 \
    bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once
)"
flock -u 9

test "$(grep -Fc 'image rm ' "$calls_file")" -eq 3
grep -F 'image rm aihub-homologation/stale/backend:r1' "$calls_file" >/dev/null
grep -F 'image rm aihub-homologation/stale/backend:r2' "$calls_file" >/dev/null
grep -F 'image rm aihub-homologation/mixed/backend:latest' "$calls_file" >/dev/null
if grep -Eq 'image rm .*(active|recent|in-use|production|unsafe)' "$calls_file"; then
  echo "A limpeza tentou remover uma referência protegida." >&2
  exit 1
fi
grep -F 'candidatas=6 referênciasRemovidas=3 sessõesAtivas=1 recentes=1 emUso=1 referênciasProtegidas=1 inválidas=1 falhasRemoção=0 dryRun=false' \
  <<<"$cleanup_output" >/dev/null

exec 7>"$test_tmp_dir/contended-cleanup.lock"
flock 7
: >"$calls_file"
contended_output="$(
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$calls_file" \
    AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/contended-cleanup.lock" \
    bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once
)"
flock -u 7
grep -F 'já está em execução' <<<"$contended_output" >/dev/null
test ! -s "$calls_file"

: >"$calls_file"
dry_run_output="$(
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$calls_file" \
    AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
    AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/dry-run.lock" \
    AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=0 \
    AIHUB_DOCKER_CLEANUP_DRY_RUN=true \
    AIHUB_DOCKER_CLEANUP_SESSION=stale \
    bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once
)"
grep -F 'DRY-RUN removeria aihub-homologation/stale/backend:r1' <<<"$dry_run_output" >/dev/null
if grep -Fq 'image rm ' "$calls_file"; then
  echo "O modo dry-run alterou uma imagem." >&2
  exit 1
fi

if AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=-1 \
  bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once \
  >/dev/null 2>&1; then
  echo "A limpeza aceitou idade negativa." >&2
  exit 1
fi

: >"$calls_file"
if PATH="$test_tmp_dir/bin:$PATH" \
  DOCKER_DOUBLE_CALLS="$calls_file" \
  DOCKER_DOUBLE_INFO_STATUS=1 \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/unavailable.lock" \
  bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once \
  >/dev/null 2>&1; then
  echo "A limpeza aceitou um daemon Docker indisponível." >&2
  exit 1
fi
if grep -Fq 'image rm ' "$calls_file"; then
  echo "A limpeza alterou imagens com o daemon indisponível." >&2
  exit 1
fi

echo "Contrato da limpeza de imagens temporárias validado."
