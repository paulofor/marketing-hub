#!/usr/bin/env bash
# Prepara coletores suspensos ou ativa somente por comando explícito do publicador.
set -euo pipefail

operation="${1:?Informe prepare ou activate.}"
service="${2:?Informe o coletor.}"
case "${operation}" in prepare|activate) ;; *) echo 'Operação não permitida.' >&2; exit 2 ;; esac
case "${service}" in
  mois-hotmart-collector|mois-meta-ad-library-collector) ;;
  *) echo 'Coletor não permitido.' >&2; exit 2 ;;
esac

running="$(docker container inspect "${service}" --format '{{.State.Running}}' 2>/dev/null || true)"
if [[ "${operation}" == prepare && "${running}" == true ]]; then
  echo 'Coletor ativo: conferir trabalho e suspender antes de preparar outra versão.' >&2
  exit 1
fi

if [[ "${operation}" == prepare ]]; then
  docker compose -f docker-compose.deploy.yml create --no-build "${service}"
  [[ "$(docker container inspect "${service}" --format '{{.State.Running}}')" == false ]]
  [[ "$(docker container inspect "${service}" --format '{{.HostConfig.RestartPolicy.Name}}')" == no ]]
  echo "${service}: preparado parado, sem consumir fila."
else
  docker compose -f docker-compose.deploy.yml up -d --no-build --no-deps "${service}"
  for _attempt in $(seq 1 30); do
    health="$(docker container inspect "${service}" --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}')"
    if [[ "${health}" == healthy ]]; then
      echo "${service}: ativação explícita saudável."
      exit 0
    fi
    sleep 2
  done
  echo "${service}: saúde não confirmada; conferir logs preservados." >&2
  exit 1
fi
