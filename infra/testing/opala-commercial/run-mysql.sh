#!/usr/bin/env bash
# Valida a migração Opala em MySQL local descartável e limpa somente o projeto informado.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${OPALA_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox ou do job}"
case "$OPALA_COMPOSE_PROJECT" in aihub-*) ;; *) echo 'Projeto de teste deve iniciar por aihub-' >&2; exit 2;; esac
opala_db_host="${OPALA_DB_HOST:-127.0.0.1}"
case "$opala_db_host" in 127.0.0.1|sandbox-docker) ;; *) echo 'Host deve ser local' >&2; exit 2;; esac
compose=(docker compose -p "$OPALA_COMPOSE_PROJECT" -f backend/ads-service/docker-compose.learning-cycles-local.yml)
cleanup() { "${compose[@]}" down --volumes --remove-orphans; }
trap cleanup EXIT
docker version
docker buildx version
docker compose version
"${compose[@]}" up -d --wait learning-cycles-mysql
"${compose[@]}" exec -T learning-cycles-mysql mysql -uroot -pcycles-root-local-only -e 'CREATE DATABASE IF NOT EXISTS opala_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
for attempt in $(seq 1 30); do
  if timeout 1 bash -c "</dev/tcp/${opala_db_host}/18307" 2>/dev/null; then
    break
  fi
  [[ "${attempt}" -lt 30 ]] || {
    echo 'A porta publicada do MySQL 5.7 não ficou acessível à JVM de teste.' >&2
    exit 1
  }
  sleep 1
done
OPALA_MYSQL_URL="jdbc:mysql://${opala_db_host}:18307/opala_test?useSSL=false&allowPublicKeyRetrieval=true" \
  mvn -B -f backend/ads-service/pom.xml '-Dtest=OpalaCommercial*Test,QuartzoCommercialMigrationTest' test
