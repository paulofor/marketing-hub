#!/bin/sh
set -euo pipefail

DOMAIN="${CERTBOT_DOMAIN:-pagamentopalf.online}"
LE_CERT_DIR="/etc/letsencrypt/live/${DOMAIN}"
FALLBACK_CERT_DIR="/etc/nginx/dev-certs/live/${DOMAIN}"
TARGET_CERT_DIR="/etc/nginx/certs/live/${DOMAIN}"

mkdir -p "${TARGET_CERT_DIR}"

has_le_cert=false
if [ -f "${LE_CERT_DIR}/fullchain.pem" ] && [ -f "${LE_CERT_DIR}/privkey.pem" ]; then
  has_le_cert=true
fi

if [ "${has_le_cert}" = true ]; then
  SRC_DIR="${LE_CERT_DIR}"
  echo "[proxy] Using Let's Encrypt certificate for ${DOMAIN}"
else
  SRC_DIR="${FALLBACK_CERT_DIR}"
  if [ ! -f "${SRC_DIR}/fullchain.pem" ] || [ ! -f "${SRC_DIR}/privkey.pem" ]; then
    echo "[proxy] ERROR: no SSL certificate available in ${LE_CERT_DIR} or fallback ${FALLBACK_CERT_DIR}" >&2
    exit 1
  fi
  echo "[proxy] Let's Encrypt certificate not found; using fallback development certificate for ${DOMAIN}" >&2
fi

cp "${SRC_DIR}/fullchain.pem" "${TARGET_CERT_DIR}/fullchain.pem"
cp "${SRC_DIR}/privkey.pem" "${TARGET_CERT_DIR}/privkey.pem"
chmod 600 "${TARGET_CERT_DIR}/privkey.pem"
