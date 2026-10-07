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

# Executa também a publicação Maven com ferramentas locais, antes de qualquer deploy.
python3 - "${WORKFLOW_FILE}" <<'PY'
#!/usr/bin/env python3
"""Executa a publicação do workflow com Maven local e falhas históricas do registry."""
import json
import os
import sys
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest

WORKFLOW = Path(sys.argv[1]).resolve()


def publication_script():
    """Extrai o bloco operacional real; o teste não mantém uma cópia da classificação."""
    workflow = WORKFLOW.read_text()
    block = workflow.split('      - name: Publish backend library for dependent workers\n', 1)[1]
    body = block.split('        run: |\n', 1)[1].split('\n      - ', 1)[0]
    return textwrap.dedent(body).strip() + '\n'


class GitHubPackagePublicationTest(unittest.TestCase):
    """Confere recuperação limitada, falha permanente e publicação única com ferramentas locais."""

    def execute(self, failures):
        """Simula só Maven e espera; bash, pipefail, tee, logs e decisões são os reais."""
        with tempfile.TemporaryDirectory(prefix='packages-contract-') as temporary:
            directory = Path(temporary)
            tools = directory / 'bin'; tools.mkdir()
            (directory / 'failures.json').write_text(json.dumps(failures))
            (tools / 'mvn').write_text('''#!/usr/bin/env python3
import json,os,pathlib,sys
root=pathlib.Path(os.environ['PACKAGES_TEST_ROOT'])
count=root/'calls.json';calls=json.loads(count.read_text()) if count.exists() else []
calls.append(sys.argv[1:]);count.write_text(json.dumps(calls))
failures=json.loads((root/'failures.json').read_text())
if len(calls)<=len(failures):print(failures[len(calls)-1]);sys.exit(1)
print('BUILD SUCCESS')
''')
            (tools / 'sleep').write_text('''#!/usr/bin/env python3
import json,os,pathlib,sys
root=pathlib.Path(os.environ['PACKAGES_TEST_ROOT']);path=root/'sleeps.json'
values=json.loads(path.read_text()) if path.exists() else []
values.append(sys.argv[1:]);path.write_text(json.dumps(values))
''')
            for executable in tools.iterdir():executable.chmod(0o755)
            environment = dict(os.environ, PATH=str(tools)+os.pathsep+os.environ['PATH'],
                               PACKAGES_TEST_ROOT=str(directory), TMPDIR=str(directory))
            result = subprocess.run(['bash', '-c', publication_script()], cwd=directory,
                                    env=environment, capture_output=True, text=True, timeout=15)
            calls = json.loads((directory/'calls.json').read_text())
            sleeps = json.loads((directory/'sleeps.json').read_text()) if (directory/'sleeps.json').exists() else []
            leftover = [p.name for p in directory.iterdir() if p.name.startswith('tmp.')]
            self.assertEqual(leftover, [], 'Logs temporários devem ser removidos em sucesso/falha.')
            for call in calls:
                self.assertIn('org.apache.maven.plugins:maven-deploy-plugin:3.1.1:deploy-file',call)
                self.assertIn('-Dfile=target/app.jar',call)
                self.assertIn('-Dversion=0.0.1-SNAPSHOT',call)
            return result, calls, sleeps

    def test_actual_maven_http_status_500_recovers_without_another_release(self):
        failure='[\x1b[31;1mERROR\x1b[0m] Could not transfer artifact from/to github (https://maven.pkg.github.com/fixture/one): HTTP Status: 500 -> Help 1'
        result,calls,sleeps=self.execute([failure])
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual(len(calls),2);self.assertEqual(calls[0],calls[1])
        self.assertEqual(sleeps,[['20']])

    def test_other_artifact_and_legacy_status_format_have_the_same_rule(self):
        for failure in ['Failed to deploy fixture/two: HTTP Status: 502',
                        'Failed to deploy fixture/three: status code: 503',
                        'Failed to deploy fixture/four: HTTP status: 504']:
            with self.subTest(failure=failure):
                result,calls,sleeps=self.execute([failure])
                self.assertEqual(result.returncode,0,result.stdout+result.stderr)
                self.assertEqual(len(calls),2);self.assertEqual(sleeps,[['20']])

    def test_persistent_server_failure_stops_after_three_attempts(self):
        result,calls,sleeps=self.execute(['HTTP Status: 500']*3)
        self.assertNotEqual(result.returncode,0)
        self.assertEqual(len(calls),3);self.assertEqual(sleeps,[['20'],['40']])
        self.assertIn('após 3 tentativas',result.stderr)

    def test_credentials_and_functional_errors_are_not_retried(self):
        for failure in ['HTTP Status: 401','HTTP Status: 403','status code: 403',
                        'COMPILATION ERROR','Could not transfer artifact: invalid configuration']:
            with self.subTest(failure=failure):
                result,calls,sleeps=self.execute([failure])
                self.assertNotEqual(result.returncode,0)
                self.assertEqual(len(calls),1);self.assertEqual(sleeps,[])
                self.assertIn('Falha permanente',result.stderr)

    def test_transport_failure_recovers_and_valid_publish_runs_once(self):
        for failures in [[],['Connection reset'],['Connection refused'],['Read timed out']]:
            with self.subTest(failures=failures):
                result,calls,sleeps=self.execute(failures)
                self.assertEqual(result.returncode,0,result.stdout+result.stderr)
                self.assertEqual(len(calls),len(failures)+1)
                self.assertEqual(sleeps,[['20']] if failures else [])


if __name__ == '__main__':unittest.main(argv=[sys.argv[0]])

PY

printf 'Contrato de deploy transacional validado.\n'
