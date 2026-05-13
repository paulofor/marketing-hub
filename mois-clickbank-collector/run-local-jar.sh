#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/mois-clickbank-collector.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar não encontrado em $JAR_PATH. Execute: mvn clean package"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java não encontrado no PATH. Instale Java 21+ para executar o coletor."
  exit 1
fi

exec java -jar "$JAR_PATH"
