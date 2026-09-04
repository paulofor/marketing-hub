#!/bin/sh
set -euo pipefail

PRIMARY_DOMAIN="${CERTBOT_DOMAIN:-pagamentopalf.site}"
SSL_DOMAINS="${SSL_DOMAINS:-${PRIMARY_DOMAIN} vitrineproduto.online digicomdigital.com.br clubemusa.com.br kit-whatsapp-pronto.digicomdigital.com.br mkthub.api.br}"

for DOMAIN in $SSL_DOMAINS; do
  TARGET_CERT_DIR="/etc/nginx/certs/live/${DOMAIN}"
  LE_CERT_DIR="/etc/letsencrypt/live/${DOMAIN}"
  FALLBACK_CERT_DIR="/etc/nginx/dev-certs/live/${DOMAIN}"

  mkdir -p "${TARGET_CERT_DIR}"

  if [ -f "${LE_CERT_DIR}/fullchain.pem" ] && [ -f "${LE_CERT_DIR}/privkey.pem" ]; then
    SRC_DIR="${LE_CERT_DIR}"
    echo "[proxy] Using Let's Encrypt certificate for ${DOMAIN}"
  elif [ -f "${TARGET_CERT_DIR}/fullchain.pem" ] && [ -f "${TARGET_CERT_DIR}/privkey.pem" ]; then
    echo "[proxy] SSL certificate for ${DOMAIN} already present in ${TARGET_CERT_DIR}, keeping existing copy"
    continue
  elif [ -f "${FALLBACK_CERT_DIR}/fullchain.pem" ] && [ -f "${FALLBACK_CERT_DIR}/privkey.pem" ]; then
    SRC_DIR="${FALLBACK_CERT_DIR}"
    echo "[proxy] Let's Encrypt certificate not found; using fallback development certificate for ${DOMAIN}" >&2
  else
    if ! command -v openssl >/dev/null 2>&1; then
      echo "[proxy] ERROR: no SSL certificate available for ${DOMAIN} and openssl is unavailable" >&2
      exit 1
    fi
    echo "[proxy] Let's Encrypt certificate not found; generating temporary self-signed certificate for ${DOMAIN}" >&2
    openssl req -x509 -nodes -newkey rsa:2048 -days 7 \
      -keyout "${TARGET_CERT_DIR}/privkey.pem" \
      -out "${TARGET_CERT_DIR}/fullchain.pem" \
      -subj "/CN=${DOMAIN}" >/dev/null 2>&1
    chmod 600 "${TARGET_CERT_DIR}/privkey.pem"
    continue
  fi

  cp "${SRC_DIR}/fullchain.pem" "${TARGET_CERT_DIR}/fullchain.pem"
  cp "${SRC_DIR}/privkey.pem" "${TARGET_CERT_DIR}/privkey.pem"
  chmod 600 "${TARGET_CERT_DIR}/privkey.pem"
done
