#!/usr/bin/env bash
# shellcheck disable=SC2016 # Os contratos verificam expressões literais dos scripts e do workflow.
set -euo pipefail

APPLY_SCRIPT="${1:-deploy/bin/apply.sh}"
WORKFLOW_FILE="${2:-.github/workflows/deploy-containers.yml}"
BACKEND_DOCKERFILE="${3:-backend/ads-service/Dockerfile}"
FRONTEND_DOCKERFILE="${4:-frontend/Dockerfile}"

bash -n "${APPLY_SCRIPT}"

require_contract() {
  local pattern="$1"
  local description="$2"

  if ! grep -Fq "${pattern}" "${APPLY_SCRIPT}"; then
    printf '[ARQUITETURA] apply.sh não garante %s.\n' "${description}" >&2
    exit 1
  fi
}

require_contract 'BACKEND_HEALTH_ATTEMPTS=${BACKEND_HEALTH_ATTEMPTS:-60}' 'janela de saúde independente para o backend'
require_contract 'BACKEND_HEALTH_INTERVAL=${BACKEND_HEALTH_INTERVAL:-5}' 'sondagem frequente sem reduzir a janela de inicialização'
require_contract 'BACKEND_MAX_RESTARTS=${BACKEND_MAX_RESTARTS:-2}' 'falha antecipada em ciclo de reinício'
require_contract 'BACKEND_HEALTH_SUCCESSES_REQUIRED=${BACKEND_HEALTH_SUCCESSES_REQUIRED:-2}' 'confirmação de estabilidade antes do sucesso'
require_contract 'preserve_current_image "${BACKEND_IMAGE}:latest" "${BACKEND_IMAGE}:rollback"' 'preservação da imagem backend anterior'
require_contract 'rollback_app_stack || true' 'rollback quando a aplicação da nova versão falha'
require_contract 'wait_backend_container_http' 'validação do estado do container e da saúde HTTP'

bash "$(dirname "$0")/test-backend-health-wait.sh"
bash "$(dirname "$0")/test-read-frontend-build-revision.sh"

if ! grep -A6 '^concurrency:' "${WORKFLOW_FILE}" | grep -Fq 'cancel-in-progress: false'; then
  printf '[ARQUITETURA] workflow pode cancelar um deploy válido por causa de push posterior sem mudança operacional.\n' >&2
  exit 1
fi

if ! grep -A6 '^concurrency:' "${WORKFLOW_FILE}" | grep -Fq 'queue: max'; then
  printf '[ARQUITETURA] workflow deve preservar na fila os deploys pendentes de cada revisão.\n' >&2
  exit 1
fi

if [[ "$(grep -Fc 'mvn -B -q test' "${WORKFLOW_FILE}")" -ne 1 ]]; then
  printf '[ARQUITETURA] workflow deve executar a suíte completa do backend exatamente uma vez.\n' >&2
  exit 1
fi

if grep -Eq 'mvn .*package|npm run build' "${BACKEND_DOCKERFILE}" "${FRONTEND_DOCKERFILE}"; then
  printf '[ARQUITETURA] Dockerfiles de runtime não podem recompilar artefatos já validados pelo workflow.\n' >&2
  exit 1
fi

if ! grep -Fq "if: needs.detect-changes.outputs.backend == 'true'" "${WORKFLOW_FILE}" \
  || ! grep -Fq "if: needs.detect-changes.outputs.frontend == 'true'" "${WORKFLOW_FILE}"; then
  printf '[ARQUITETURA] imagens de backend e frontend devem respeitar detecção independente de módulos.\n' >&2
  exit 1
fi

if ! grep -Fq '.deployed-app-revision' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'scripts/detect-deployment-changes.sh' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'scripts/read-frontend-build-revision.sh' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'Require deployed frontend revision' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'Mark successful APP revision' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'abortando para não perder módulos pendentes' "${WORKFLOW_FILE}"; then
  printf '[ARQUITETURA] workflow deve detectar e confirmar revisões realmente publicadas em cada superfície APP.\n' >&2
  exit 1
fi

if ! grep -Fq 'Probe VIDEO VPS without blocking APP detection' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'VIDEO_SSH_READY=false' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'Reconciliação remota do VIDEO adiada' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'deployed_video_revision="${EVENT_BEFORE}"' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'Add VIDEO VPS to known_hosts' "${WORKFLOW_FILE}"; then
  printf '[ARQUITETURA] indisponibilidade do VPS de vídeo não pode bloquear APP, mas o deploy real de vídeo deve continuar estrito.\n' >&2
  exit 1
fi

# Replica cada sincronização real em diretórios temporários, sem SSH nem estado produtivo.
python3 - "${WORKFLOW_FILE}" <<'PY'
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile

workflow = Path(sys.argv[1]).read_text()
commands = [shlex.split(line.strip()) for line in workflow.splitlines()
            if 'retry rsync -az --delete' in line and ' deploy/ ' in line]
assert len(commands) == 3, '[ARQUITETURA] As três sincronizações centrais devem preservar estado operacional.'
for index, command in enumerate(commands):
    options = command[command.index('rsync') + 1:command.index('-e')]
    with tempfile.TemporaryDirectory(prefix='deploy-marker-contract-') as temporary:
        root = Path(temporary)
        source, destination = root / 'source', root / 'destination'
        source.mkdir()
        destination.mkdir()
        (source / 'descriptor.yml').write_text('image: validated-release\n')
        (destination / 'obsolete.yml').write_text('old descriptor\n')
        sentinels = {
            '.deployed-app-revision': 'a' * 40 + '\n',
            '.deployed-frontend-revision': 'b' * 40 + '\n',
            '.deployed-video-revision': 'c' * 40 + '\n',
            '.env': 'TEST_CREDENTIAL=fixture-only\n',
            'volumes/fixture.txt': 'persisted local fixture\n',
        }
        for name, value in sentinels.items():
            target = destination / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(value)
        for phase in ['delete', 'overwrite']:
            if phase == 'overwrite':
                for name in sentinels:
                    target = source / name
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_text('untrusted source value\n')
            subprocess.run(['rsync', *options, str(source) + '/', str(destination) + '/'], check=True)
            for name, value in sentinels.items():
                assert (destination / name).is_file(), f'[ARQUITETURA] Sincronização {index}: {name} foi apagado.'
                assert (destination / name).read_text() == value, f'[ARQUITETURA] Sincronização {index}: {name} foi substituído.'
            assert (destination / 'descriptor.yml').read_text() == 'image: validated-release\n', '[ARQUITETURA] Descritor válido não foi atualizado.'
            assert not (destination / 'obsolete.yml').exists(), '[ARQUITETURA] Descritor obsoleto não foi removido.'
print('[ARQUITETURA] Rsync real preservou revisões, credenciais locais e volumes nas três sincronizações.')
PY

printf 'Contrato de deploy transacional validado.\n'
