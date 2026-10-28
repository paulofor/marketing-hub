#!/usr/bin/env bash
# Renova a linhagem existente de Vega e recarrega somente seu certificado no proxy compartilhado.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/root/lead-portal-payments-service}"
CERT_ROOT="${CERT_ROOT:-/etc/letsencrypt}"
CERT_LOG_DIR="${CERT_LOG_DIR:-/var/log/letsencrypt}"
TLS_LOCK="${TLS_LOCK:-/tmp/clubemusa-certbot-renew.lock}"
DOMAIN=clubemusa.com.br
PROXY=lead-portal-payments-service-proxy-1
CERTBOT_IMAGE=certbot/certbot:v5.8.0
WWW_DIR="${DEPLOY_DIR}/docker/proxy/html"
CERT_FILE="${CERT_ROOT}/live/${DOMAIN}/fullchain.pem"

[[ "$#" == 0 ]] || { echo 'Uso: renew-clubemusa-tls.sh' >&2; exit 2; }
umask 077
exec 9>"${TLS_LOCK}"
flock -w 30 9
test -s "${CERT_FILE}"
test -s "${CERT_ROOT}/live/${DOMAIN}/privkey.pem"
test -f "${CERT_ROOT}/renewal/${DOMAIN}.conf"
test "$(docker inspect --format '{{.State.Running}}' "${PROXY}")" = true

# A montagem conferida impede renovar uma linhagem sem relação com o proxy publicado.
mounts="$(docker inspect --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}' "${PROXY}")"
grep -Fxq "${CERT_ROOT} -> /etc/letsencrypt" <<<"${mounts}"
grep -Fxq "${WWW_DIR} -> /usr/share/nginx/html" <<<"${mounts}"
docker exec "${PROXY}" nginx -t
mapfile -t domains < <(openssl x509 -in "${CERT_FILE}" -noout -ext subjectAltName \
  | grep -oE 'DNS:[^,[:space:]]+' | cut -d: -f2)
[[ "${#domains[@]}" -ge 2 ]]
printf '%s\n' "${domains[@]}" | grep -Fxq "${DOMAIN}"
printf '%s\n' "${domains[@]}" | grep -Fxq "www.${DOMAIN}"
for domain in "${domains[@]}"; do
  [[ "${domain}" =~ ^(www\.|v[0-9]+\.)?clubemusa\.com\.br$ ]] \
    || { echo 'Linhagem contém domínio fora do escopo Clube MUSA.' >&2; exit 1; }
done

# Confere cada nome da linhagem antes de consumir uma tentativa na autoridade certificadora.
install -d -m 755 "${WWW_DIR}/.well-known/acme-challenge"
challenge="$(mktemp "${WWW_DIR}/.well-known/acme-challenge/clubemusa-renew-XXXXXXXX")"
cleanup() { rm -f -- "${challenge}"; }
trap cleanup EXIT
challenge_name="$(basename "${challenge}")"
printf '%s' "${challenge_name}" > "${challenge}"
chmod 644 "${challenge}"
for domain in "${domains[@]}"; do
  received="$(curl --noproxy '*' --fail --silent --show-error --connect-timeout 10 --max-time 30 \
    "http://${domain}/.well-known/acme-challenge/${challenge_name}")"
  test "${received}" = "${challenge_name}"
done

# Reutiliza os volumes Certbot existentes; não acessa credenciais nem recria serviços da aplicação.
install -d -m 700 "${CERT_LOG_DIR}"
docker run --rm \
  --mount "type=bind,src=${CERT_ROOT},dst=/etc/letsencrypt" \
  --mount "type=bind,src=${WWW_DIR},dst=/var/www/certbot" \
  --mount "type=bind,src=${CERT_LOG_DIR},dst=/var/log/letsencrypt" \
  "${CERTBOT_IMAGE}" renew --cert-name "${DOMAIN}" --webroot -w /var/www/certbot \
  --non-interactive --no-random-sleep-on-renew

# O Certbot pode terminar sem renovar; a validade real e todos os nomes continuam obrigatórios.
openssl x509 -in "${CERT_FILE}" -noout -checkend 1209600
for domain in "${domains[@]}"; do
  LC_ALL=C openssl x509 -in "${CERT_FILE}" -noout -checkhost "${domain}" \
    | grep -Fx "Hostname ${domain} does match certificate"
done
# Reutiliza o instalador versionado do proxy, restringindo a cópia a esta linhagem.
docker exec -e "SSL_DOMAINS=${DOMAIN}" "${PROXY}" sh /docker-entrypoint.d/40-ensure-certs.sh
docker exec "${PROXY}" nginx -t
docker exec "${PROXY}" nginx -s reload
echo '[Clube MUSA TLS] Certificado válido instalado; conferir HTTPS externo antes de concluir.'
