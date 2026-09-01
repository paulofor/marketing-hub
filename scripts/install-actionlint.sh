#!/usr/bin/env bash
set -euo pipefail

ACTIONLINT_REPOSITORY="https://github.com/rhysd/actionlint.git"
ACTIONLINT_PULL_REQUEST_REF="refs/pull/654/head"
ACTIONLINT_REVISION="644076a59742c2d1540ebd4686eab3c308f0e562"
ACTIONLINT_DISPLAY_VERSION="v1.7.12+queue.pr654.644076a"
ACTIONLINT_GO_IMAGE="golang:1.26@sha256:e30143be198ab04cf7ba25fba83ab3a692ca584c994aad0bf131fa0eb32dd8c1"

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
DEFAULT_INSTALL_DIRECTORY="${REPOSITORY_ROOT}/codex-cache/actionlint/${ACTIONLINT_REVISION}"
DESTINATION="${1:-${DEFAULT_INSTALL_DIRECTORY}/actionlint}"
SOURCE_DIRECTORY=""
BUILDER_CONTAINER=""

cleanup() {
  if [[ -n "${SOURCE_DIRECTORY}" && -d "${SOURCE_DIRECTORY}" ]]; then
    rm -rf -- "${SOURCE_DIRECTORY}"
  fi
  if [[ -n "${BUILDER_CONTAINER}" ]] && command -v docker >/dev/null 2>&1; then
    docker rm -f "${BUILDER_CONTAINER}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

is_expected_binary() {
  [[ -x "${DESTINATION}" ]] && "${DESTINATION}" -version 2>&1 | grep -Fq "${ACTIONLINT_DISPLAY_VERSION}"
}

checkout_pinned_source() {
  local source_directory="$1"
  git init -q --initial-branch=main "${source_directory}"
  git -C "${source_directory}" fetch --depth=1 "${ACTIONLINT_REPOSITORY}" "${ACTIONLINT_PULL_REQUEST_REF}"
  local fetched_revision
  fetched_revision="$(git -C "${source_directory}" rev-parse FETCH_HEAD)"
  if [[ "${fetched_revision}" != "${ACTIONLINT_REVISION}" ]]; then
    echo "Revisão inesperada do Actionlint: ${fetched_revision}" >&2
    exit 1
  fi
  git -C "${source_directory}" checkout -q --detach FETCH_HEAD
}

build_with_go() {
  SOURCE_DIRECTORY="$(mktemp -d)"
  checkout_pinned_source "${SOURCE_DIRECTORY}"
  install -d -m 0755 "$(dirname "${DESTINATION}")"
  local temporary_destination="${DESTINATION}.tmp.$$"
  (
    cd "${SOURCE_DIRECTORY}"
    go build \
      -trimpath \
      -ldflags "-s -w -X github.com/rhysd/actionlint.version=${ACTIONLINT_DISPLAY_VERSION} -X github.com/rhysd/actionlint.installedFrom=Marketing-Hub-pinned-upstream-patch" \
      -o "${temporary_destination}" \
      ./cmd/actionlint
  )
  chmod 0755 "${temporary_destination}"
  mv -f "${temporary_destination}" "${DESTINATION}"
}

build_with_docker() {
  BUILDER_CONTAINER="marketinghub-actionlint-builder-${PPID}-$$"
  docker run \
    --name "${BUILDER_CONTAINER}" \
    --label "com.docker.compose.project=${ACTIONLINT_DOCKER_PROJECT:-marketinghub-local-tools}" \
    --env "ACTIONLINT_REPOSITORY=${ACTIONLINT_REPOSITORY}" \
    --env "ACTIONLINT_PULL_REQUEST_REF=${ACTIONLINT_PULL_REQUEST_REF}" \
    --env "ACTIONLINT_REVISION=${ACTIONLINT_REVISION}" \
    --env "ACTIONLINT_DISPLAY_VERSION=${ACTIONLINT_DISPLAY_VERSION}" \
    "${ACTIONLINT_GO_IMAGE}" \
    sh -euc '
      git init -q --initial-branch=main /tmp/actionlint-src
      git -C /tmp/actionlint-src fetch --depth=1 "${ACTIONLINT_REPOSITORY}" "${ACTIONLINT_PULL_REQUEST_REF}"
      git -C /tmp/actionlint-src rev-parse FETCH_HEAD | grep -Fx "${ACTIONLINT_REVISION}" >/dev/null
      git -C /tmp/actionlint-src checkout -q --detach FETCH_HEAD
      install -d -m 0755 /out
      cd /tmp/actionlint-src
      go build \
        -trimpath \
        -ldflags "-s -w -X github.com/rhysd/actionlint.version=${ACTIONLINT_DISPLAY_VERSION} -X github.com/rhysd/actionlint.installedFrom=Marketing-Hub-pinned-upstream-patch" \
        -o /out/actionlint \
        ./cmd/actionlint
    '
  install -d -m 0755 "$(dirname "${DESTINATION}")"
  local temporary_destination="${DESTINATION}.tmp.$$"
  docker cp "${BUILDER_CONTAINER}:/out/actionlint" "${temporary_destination}"
  chmod 0755 "${temporary_destination}"
  mv -f "${temporary_destination}" "${DESTINATION}"
}

if is_expected_binary; then
  exit 0
fi

if command -v go >/dev/null 2>&1 && command -v git >/dev/null 2>&1; then
  build_with_go
elif command -v docker >/dev/null 2>&1; then
  build_with_docker
else
  echo "Go 1.26 ou Docker é obrigatório para instalar o Actionlint compatível com queue: max." >&2
  exit 1
fi

if ! is_expected_binary; then
  echo "O Actionlint instalado não corresponde à revisão fixada." >&2
  exit 1
fi
