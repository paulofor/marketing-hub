#!/usr/bin/env bash
set -euo pipefail

show_help() {
  cat <<'USAGE'
Usage: provision-letsencrypt.sh --domain <example.com> --email <admin@example.com> [options]

Provision or renew Let's Encrypt certificates for the Lead Portal proxy running via Docker Compose.

Options:
  -d, --domain DOMAIN        Fully-qualified domain name to secure. Repeatable for SANs.
  -e, --email EMAIL          Email used for Let's Encrypt registration and renewal notices.
      --staging              Use Let's Encrypt's staging environment (recommended for testing).
      --force-renewal        Request certificate renewal even if not yet due.
      --compose-file PATH    Custom docker-compose file (default: lead-portal/docker-compose.yml).
      --certbot-image IMAGE  Docker image to run Certbot (default: certbot/certbot:latest).
  -h, --help                 Show this help message and exit.

Environment variables:
  CERTBOT_STATE_DIR   Location to persist /etc/letsencrypt data (default: /etc/letsencrypt).
  CERTBOT_LIB_DIR     Location for /var/lib/letsencrypt (default: /var/lib/letsencrypt).
  CERTBOT_LOG_DIR     Location for /var/log/letsencrypt (default: /var/log/letsencrypt).

Example (production):
  ./lead-portal/scripts/provision-letsencrypt.sh \
      --domain portal.suaempresa.com \
      --email infra@suaempresa.com

Example (test run against staging CA):
  ./lead-portal/scripts/provision-letsencrypt.sh \
      --domain portal.suaempresa.com \
      --email infra@suaempresa.com \
      --staging
USAGE
}

error() {
  echo "[ERROR] $*" >&2
}

docker_compose() {
  if command -v docker >/dev/null 2>&1; then
    if docker compose version >/dev/null 2>&1; then
      docker compose "$@"
      return
    fi
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi

  error "docker compose (v2) or docker-compose (v1) is required"
  exit 1
}

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
LEAD_PORTAL_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
DEFAULT_COMPOSE_FILE="${LEAD_PORTAL_DIR}/docker-compose.yml"
DEFAULT_CERTBOT_IMAGE="certbot/certbot:latest"

COMPOSE_FILE="${DEFAULT_COMPOSE_FILE}"
CERTBOT_IMAGE="${DEFAULT_CERTBOT_IMAGE}"
CERTBOT_STATE_DIR="${CERTBOT_STATE_DIR:-/etc/letsencrypt}"
CERTBOT_LIB_DIR="${CERTBOT_LIB_DIR:-/var/lib/letsencrypt}"
CERTBOT_LOG_DIR="${CERTBOT_LOG_DIR:-/var/log/letsencrypt}"
WEBROOT_DIR="${LEAD_PORTAL_DIR}/docker/proxy/html"
CERTS_DIR="${LEAD_PORTAL_DIR}/docker/proxy/certs"
FORCE_RENEWAL=false
STAGING=false
DOMAINS=()
EMAIL=""

cd "${LEAD_PORTAL_DIR}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -d|--domain)
      [[ $# -lt 2 ]] && { error "Missing value for $1"; exit 1; }
      DOMAINS+=("$2")
      shift 2
      ;;
    -e|--email)
      [[ $# -lt 2 ]] && { error "Missing value for $1"; exit 1; }
      EMAIL="$2"
      shift 2
      ;;
    --staging)
      STAGING=true
      shift
      ;;
    --force-renewal)
      FORCE_RENEWAL=true
      shift
      ;;
    --compose-file)
      [[ $# -lt 2 ]] && { error "Missing value for $1"; exit 1; }
      if [[ "$2" = /* ]]; then
        COMPOSE_FILE="$2"
      else
        COMPOSE_FILE="${LEAD_PORTAL_DIR}/$2"
      fi
      shift 2
      ;;
    --certbot-image)
      [[ $# -lt 2 ]] && { error "Missing value for $1"; exit 1; }
      CERTBOT_IMAGE="$2"
      shift 2
      ;;
    -h|--help)
      show_help
      exit 0
      ;;
    *)
      error "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
done

if [[ ${#DOMAINS[@]} -eq 0 ]]; then
  error "At least one --domain must be provided"
  show_help
  exit 1
fi

if [[ -z "${EMAIL}" ]]; then
  error "--email is required to comply with Let's Encrypt terms of service"
  show_help
  exit 1
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  error "Docker Compose file not found: ${COMPOSE_FILE}"
  exit 1
fi

PRIMARY_DOMAIN="${DOMAINS[0]}"
DOM_ARGS=()
for DOMAIN in "${DOMAINS[@]}"; do
  DOM_ARGS+=("-d" "$DOMAIN")
done

mkdir -p "${WEBROOT_DIR}" "${CERTS_DIR}" "${CERTBOT_STATE_DIR}" "${CERTBOT_LIB_DIR}" "${CERTBOT_LOG_DIR}"

echo "[INFO] Ensuring proxy service is running so ACME challenge can be served"
docker_compose -f "${COMPOSE_FILE}" up -d proxy

CERTBOT_CMD=(
  docker run --rm
  -v "${CERTBOT_STATE_DIR}:/etc/letsencrypt"
  -v "${CERTBOT_LIB_DIR}:/var/lib/letsencrypt"
  -v "${CERTBOT_LOG_DIR}:/var/log/letsencrypt"
  -v "${WEBROOT_DIR}:/var/www/certbot"
  "${CERTBOT_IMAGE}"
  certonly --webroot
  -w /var/www/certbot
  --email "${EMAIL}"
  --agree-tos
  --no-eff-email
)

if ${STAGING}; then
  CERTBOT_CMD+=(--staging)
fi

if ${FORCE_RENEWAL}; then
  CERTBOT_CMD+=(--force-renewal)
fi

CERTBOT_CMD+=("${DOM_ARGS[@]}")

echo "[INFO] Requesting certificates from Let's Encrypt for: ${DOMAINS[*]}"
"${CERTBOT_CMD[@]}"

FULLCHAIN_PATH="${CERTBOT_STATE_DIR}/live/${PRIMARY_DOMAIN}/fullchain.pem"
PRIVKEY_PATH="${CERTBOT_STATE_DIR}/live/${PRIMARY_DOMAIN}/privkey.pem"

if [[ ! -f "${FULLCHAIN_PATH}" || ! -f "${PRIVKEY_PATH}" ]]; then
  error "Certificate files were not generated at ${CERTBOT_STATE_DIR}/live/${PRIMARY_DOMAIN}"
  exit 1
fi

umask 077
install -m 0644 "${FULLCHAIN_PATH}" "${CERTS_DIR}/dev.crt"
install -m 0600 "${PRIVKEY_PATH}" "${CERTS_DIR}/dev.key"
umask 022

echo "[INFO] Certificates copied to ${CERTS_DIR}/dev.crt and dev.key"

PROXY_CONTAINER=$(docker_compose -f "${COMPOSE_FILE}" ps -q proxy || true)
if [[ -n "${PROXY_CONTAINER}" ]]; then
  echo "[INFO] Reloading Nginx to pick up the new certificates"
  docker_compose -f "${COMPOSE_FILE}" exec proxy nginx -s reload >/dev/null 2>&1 || {
    echo "[WARN] Could not reload proxy container automatically. Restarting instead."
    docker_compose -f "${COMPOSE_FILE}" restart proxy
  }
else
  echo "[WARN] Proxy container is not running. Start it with: docker compose -f ${COMPOSE_FILE} up -d proxy"
fi

echo "[SUCCESS] Let's Encrypt provisioning completed for ${DOMAINS[*]}"
