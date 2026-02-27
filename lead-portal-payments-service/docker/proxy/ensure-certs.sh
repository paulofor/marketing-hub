#!/bin/sh
set -euo pipefail

PRIMARY_DOMAIN="${CERTBOT_DOMAIN:-pagamentopalf.site}"
SSL_DOMAINS="${SSL_DOMAINS:-${PRIMARY_DOMAIN} vitrineproduto.online}"

for DOMAIN in $SSL_DOMAINS; do
  TARGET_CERT_DIR="/etc/nginx/certs/live/${DOMAIN}"
  LE_CERT_DIR="/etc/letsencrypt/live/${DOMAIN}"
  FALLBACK_CERT_DIR="/etc/nginx/dev-certs/live/${DOMAIN}"

  mkdir -p "${TARGET_CERT_DIR}"

  if [ -f "${TARGET_CERT_DIR}/fullchain.pem" ] && [ -f "${TARGET_CERT_DIR}/privkey.pem" ]; then
    echo "[proxy] SSL certificate for ${DOMAIN} already present in ${TARGET_CERT_DIR}, skipping copy"
    continue
  fi

  if [ -f "${LE_CERT_DIR}/fullchain.pem" ] && [ -f "${LE_CERT_DIR}/privkey.pem" ]; then
    SRC_DIR="${LE_CERT_DIR}"
    echo "[proxy] Using Let's Encrypt certificate for ${DOMAIN}"
  elif [ -f "${FALLBACK_CERT_DIR}/fullchain.pem" ] && [ -f "${FALLBACK_CERT_DIR}/privkey.pem" ]; then
    SRC_DIR="${FALLBACK_CERT_DIR}"
    echo "[proxy] Let's Encrypt certificate not found; using fallback development certificate for ${DOMAIN}" >&2
  else
    echo "[proxy] ERROR: no SSL certificate available for ${DOMAIN} (checked ${LE_CERT_DIR} and fallback ${FALLBACK_CERT_DIR})" >&2
    exit 1
  fi

  cp "${SRC_DIR}/fullchain.pem" "${TARGET_CERT_DIR}/fullchain.pem"
  cp "${SRC_DIR}/privkey.pem" "${TARGET_CERT_DIR}/privkey.pem"
  chmod 600 "${TARGET_CERT_DIR}/privkey.pem"
done
