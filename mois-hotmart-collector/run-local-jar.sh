#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/mois-hotmart-collector.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar não encontrado em $JAR_PATH. Execute: mvn clean package"
  exit 1
fi

exec "$JAR_PATH"
