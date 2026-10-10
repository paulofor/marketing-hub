#!/usr/bin/env bash
# Inicia somente a imagem já testada e confirma revisão e saúde antes de liberar o consumidor.
set -euo pipefail
repository_dir="${1:?Informe o diretório versionado}"
validated_revision="${2:?Informe o SHA validado}"
export EXPERIMENT_STRATEGIST_CODEX_HOME=/opt/growth-operator/agents/experiment-strategist/codex-home
install -d -o 10001 -g 10001 "$EXPERIMENT_STRATEGIST_CODEX_HOME"
cd "$repository_dir/experiment-strategist-worker"
export BACKEND_URL=http://191.252.181.168
export MARKETING_HUB_REPOSITORY_HOST="$repository_dir"
export CODEX_MODEL=gpt-6.1-sol
export AGENT_BUILD_REFERENCE="$validated_revision"
export EXPERIMENT_STRATEGIST_IMAGE="marketing-hub/experiment-strategist-worker:$validated_revision"
docker compose up -d --no-build --pull never --remove-orphans
container=experiment-strategist-worker-experiment-strategist-worker-1
for attempt in $(seq 1 24); do
  status=$(docker inspect --format '{{.State.Status}}' "$container" 2>/dev/null || true)
  image=$(docker inspect --format '{{.Config.Image}}' "$container" 2>/dev/null || true)
  codex_ready=false
  if [[ "$status" == running ]] && docker exec "$container" sh -lc 'test -w /home/strategist/.codex && codex login status' >/dev/null 2>&1; then
    codex_ready=true
  fi
  health_body=$(curl -fsS http://127.0.0.1:8096/ops-experiment-strategist-observability-v1/health 2>/dev/null || true)
  logfile_status=$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8096/ops-experiment-strategist-observability-v1/logfile 2>/dev/null || true)
  printf 'readiness attempt=%s container=%s codex=%s logfile=%s\n' "$attempt" "$status" "$codex_ready" "$logfile_status"
  if [[ "$status" == running && "$image" == "$EXPERIMENT_STRATEGIST_IMAGE" && "$codex_ready" == true && "$logfile_status" == 200 ]] \
    && docker exec "$container" node /app/agent-health-report.mjs \
    && printf '%s' "$health_body" | grep -Eq 'status.*UP'; then
    exit 0
  fi
  sleep 5
done
printf '%s\n' 'Falha: imagem não confirmou revisão e saúde; consumidor permanece protegido.' >&2
docker compose logs --tail=150 experiment-strategist-worker
exit 1
