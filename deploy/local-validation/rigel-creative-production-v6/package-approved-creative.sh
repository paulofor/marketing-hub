#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
STAGING_DIR=$(mktemp -d "${TMPDIR:-/tmp}/rigel-approved-package.XXXXXX")
OUTPUT_FILE="${SCRIPT_DIR}/evidence/rigel-approved-creative-package.zip"

cleanup() {
  rm -rf "${STAGING_DIR}"
}
trap cleanup EXIT

node "${SCRIPT_DIR}/prepare-approved-package.mjs" "${SCRIPT_DIR}" "${STAGING_DIR}"
jar --create --no-manifest --file "${OUTPUT_FILE}" -C "${STAGING_DIR}" .
printf '{"status":"PACKAGED","file":"%s"}\n' "${OUTPUT_FILE}"
