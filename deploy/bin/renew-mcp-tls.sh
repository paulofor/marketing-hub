#!/usr/bin/env bash
# Renova somente o certificado do MCP e recarrega seu proxy pelo host.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/marketinghub/containers}"
MCP_TLS_LOCK="${MCP_TLS_LOCK:-/tmp/mcp-certbot-renew.lock}"
CERTBOT_IMAGE="certbot/certbot:v5.8.0"
DOMAIN="mcpserverdigi.shop"
PROXY="marketinghub-mcp-nginx"
CERT_ROOT="${DEPLOY_DIR}/volumes/mcp/certbot"
CONF_DIR="${CERT_ROOT}/conf"
WWW_DIR="${CERT_ROOT}/www"
CERT_FILE="${CONF_DIR}/live/${DOMAIN}/fullchain.pem"
DRY_RUN=false
if [[ "${1:-}" == --dry-run && "$#" == 1 ]]; then
  DRY_RUN=true
elif [[ "$#" != 0 ]]; then
  echo 'Uso: renew-mcp-tls.sh [--dry-run]' >&2
  exit 2
fi

umask 077
exec 9>"${MCP_TLS_LOCK}"
flock -w 30 9
test -s "${CERT_FILE}"
test -s "${CONF_DIR}/live/${DOMAIN}/privkey.pem"
test -f "${CONF_DIR}/renewal/${DOMAIN}.conf"
test "$(docker inspect --format '{{.State.Running}}' "${PROXY}")" = true

# Confirma a linhagem realmente montada pelo proxy; não lê configuração/secrets do app.
mounts="$(docker inspect --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}' "${PROXY}")"
grep -Fxq "${CONF_DIR} -> /etc/letsencrypt" <<<"${mounts}"
grep -Fxq "${WWW_DIR} -> /var/www/certbot" <<<"${mounts}"
docker exec "${PROXY}" nginx -t

# Um desafio descartável confirma os dois nomes antes de solicitar a renovação.
install -d -m 755 "${WWW_DIR}/.well-known/acme-challenge"
challenge="$(mktemp "${WWW_DIR}/.well-known/acme-challenge/mcp-renew-XXXXXXXX")"
cleanup() { rm -f -- "${challenge}"; }
trap cleanup EXIT
challenge_name="$(basename "${challenge}")"
printf '%s' "${challenge_name}" > "${challenge}"
chmod 644 "${challenge}"
for domain in "${DOMAIN}" "www.${DOMAIN}"; do
  received="$(curl --noproxy '*' --fail --silent --show-error --connect-timeout 10 --max-time 30 \
    "http://${domain}/.well-known/acme-challenge/${challenge_name}")"
  test "${received}" = "${challenge_name}"
done

install -d -m 700 "${CERT_ROOT}/logs"
flags=()
if [[ "${DRY_RUN}" == true ]]; then
  flags+=(--dry-run)
fi

# O container Certbot não recebe Docker CLI/socket. O reload é responsabilidade do host.
docker run --rm \
  --mount "type=bind,src=${CONF_DIR},dst=/etc/letsencrypt" \
  --mount "type=bind,src=${WWW_DIR},dst=/var/www/certbot" \
  --mount "type=bind,src=${CERT_ROOT}/logs,dst=/var/log/letsencrypt" \
  "${CERTBOT_IMAGE}" renew --cert-name "${DOMAIN}" --webroot -w /var/www/certbot \
  --non-interactive --no-random-sleep-on-renew "${flags[@]}"

if [[ "${DRY_RUN}" == true ]]; then
  echo '[MCP TLS] Renovação de teste concluída; certificado produtivo e proxy preservados.'
  exit 0
fi

# Recupera também o caso de emissão concluída com reload anterior interrompido.
openssl x509 -in "${CERT_FILE}" -noout -checkend 1209600
for domain in "${DOMAIN}" "www.${DOMAIN}"; do
  # x509 -checkhost retorna zero até em divergência; conferir também a resposta literal.
  LC_ALL=C openssl x509 -in "${CERT_FILE}" -noout -checkhost "${domain}" \
    | grep -Fx "Hostname ${domain} does match certificate"
done
docker exec "${PROXY}" nginx -t
docker exec "${PROXY}" nginx -s reload
echo '[MCP TLS] Renovação e reload concluídos; a validação HTTPS externa continua obrigatória.'
