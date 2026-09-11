#!/usr/bin/env python3
"""Coordena a pausa e a retomada de publicadores existentes para intervenções autorizadas."""

import argparse
import base64
import contextlib
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import selectors
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parent.parent
SCOPES = json.loads((ROOT / "scripts/deploy-intervention-scopes.json").read_text())
REPOSITORY = "paulofor/marketing-hub"
CONTROL_HOST = "root@191.252.181.168"
CONTROL_DIRECTORY = "/var/lib/marketinghub/deploy-coordination"
LIVE_STATUSES = ("requested", "queued", "pending", "waiting", "in_progress")
DISABLED_STATES = {"disabled_manually", "disabled_inactivity", "disabled_fork"}


def now():
    """Retorna um horário UTC para auditoria; o tempo não autoriza expiração automática."""
    return datetime.now(timezone.utc).isoformat()


class CoordinationError(RuntimeError):
    """Representa uma condição que impede operar ou liberar a proteção."""


class GitHub:
    """Usa a autenticação existente do gh, sem ler, transportar ou registrar tokens."""

    def api(self, path, method="GET"):
        """Executa uma chamada limitada e só publica diagnóstico sem credenciais."""
        try:
            result = subprocess.run(
                ["gh", "api", "--method", method, "-H", "Accept: application/vnd.github+json",
                 f"repos/{REPOSITORY}/{path}"],
                capture_output=True, text=True, timeout=45, check=False,
            )
        except subprocess.TimeoutExpired as error:
            raise CoordinationError(f"Timeout do GitHub em {method} {path}; pausa mantida.") from error
        if result.returncode:
            # Não reproduzir stderr de ferramentas autenticadas.
            status = re.search(r"HTTP (\d{3})", result.stderr)
            detail = f"HTTP {status[1]}" if status else f"exit {result.returncode}"
            raise CoordinationError(f"GitHub recusou {method} {path} ({detail}); pausa mantida.")
        try:
            return json.loads(result.stdout) if result.stdout.strip() else None
        except ValueError as error:
            raise CoordinationError(f"Resposta inválida do GitHub em {path}.") from error

    def live_runs(self, workflow_id):
        """Consulta todos os estados não terminais com paginação e recusa truncamento."""
        found = {}
        for status in LIVE_STATUSES:
            for page in range(1, 11):
                data = self.api(
                    f"actions/workflows/{workflow_id}/runs?status={status}&per_page=100&page={page}"
                )
                if not isinstance(data, dict) or not isinstance(data.get("workflow_runs"), list):
                    raise CoordinationError("Consulta de runs incompleta; proteção não liberada.")
                if data.get("total_count", 1001) >= 1000:
                    raise CoordinationError("Fila excede limite de consulta; proteção não liberada.")
                for run in data["workflow_runs"]:
                    if run["status"] != "completed":
                        found[run["id"]] = {key: run[key] for key in ("id", "status", "head_sha", "html_url")}
                if len(data["workflow_runs"]) < 100:
                    break
            else:
                raise CoordinationError("Paginação de runs incompleta; proteção não liberada.")
        return list(found.values())


class RemoteStore:
    """Mantém uma conexão SSH com lock real durante cada operação de coordenação."""

    def __init__(self, command=None):
        source = base64.b64encode((ROOT / "scripts/deploy_intervention_store.py").read_bytes()).decode()
        remote_code = f"import base64; exec(compile(base64.b64decode('{source}'), '<deploy-control>', 'exec'))"
        self.command = command or ["sandbox-ssh", CONTROL_HOST, "python3", "-c", remote_code, CONTROL_DIRECTORY]
        self.process = None

    def __enter__(self):
        """Inicia somente o controle operacional; nenhum arquivo de aplicação é publicado."""
        self.process = subprocess.Popen(self.command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE, text=True, bufsize=1)
        return self

    def request(self, request):
        """Troca um registro por stdin e recusa transporte interrompido ou silencioso."""
        try:
            self.process.stdin.write(json.dumps(request) + "\n")
            self.process.stdin.flush()
            with selectors.DefaultSelector() as selector:
                selector.register(self.process.stdout, selectors.EVENT_READ)
                if not selector.select(30):
                    raise CoordinationError("Timeout do controle SSH; intervenção não liberada.")
            line = self.process.stdout.readline()
            if not line:
                raise CoordinationError("Controle SSH indisponível; intervenção não liberada.")
            reply = json.loads(line)
            if "error" in reply:
                raise CoordinationError(reply["error"])
            return reply
        except (BrokenPipeError, ValueError) as error:
            raise CoordinationError("Falha no protocolo do controle SSH; intervenção não liberada.") from error

    def load(self):
        """Lê o registro, distinguindo ausência de falha de transporte."""
        return self.request({"op": "load"})["state"]

    def save(self, state):
        """Confirma a persistência antes da próxima mutação no GitHub."""
        self.request({"op": "save", "state": state})

    def __exit__(self, *_):
        """Encerra o transporte sem apagar o registro ou reativar publicadores."""
        with contextlib.suppress(BrokenPipeError):
            self.process.stdin.close()
        try:
            self.process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait()
        self.process.stdout.close()
        self.process.stderr.close()


class Coordinator:
    """Controla o ciclo da intervenção, sem publicar imagens nem alterar tarefas de produto."""

    def __init__(self, github, store, settle=lambda: time.sleep(5)):
        self.github, self.store, self.settle = github, store, settle

    def checkpoint(self, state, event, **details):
        """Persiste a decisão e sua evidência antes de continuar a operação."""
        state["updated_at"] = now()
        state["history"].append({"at": state["updated_at"], "event": event, **details})
        self.store.save(state)

    def current(self, identifier):
        """Impede que outro identificador opere uma intervenção ativa."""
        state = self.store.load()
        if not state or state["id"] != identifier or state["repository"] != REPOSITORY:
            raise CoordinationError("Intervenção não encontrada ou identificador divergente.")
        return state

    def begin(self, scopes, owner, reason, authorization, version):
        """Registra escopo e estado anterior antes de pausar qualquer workflow."""
        for value in (owner, reason, authorization, version):
            if not isinstance(value, str) or not value.strip() or len(value) > 1000:
                raise CoordinationError("Informe responsável, motivo, autorização e versão (até 1000 caracteres).")
        if not scopes or any(scope not in SCOPES for scope in scopes):
            raise CoordinationError("Selecione um escopo de publicação conhecido.")
        previous = self.store.load()
        if previous and previous["phase"] != "RELEASED":
            raise CoordinationError(f"Intervenção {previous['id']} já está aberta ({previous['phase']}).")
        workflows = []
        files = list(dict.fromkeys(file for scope in scopes for file in SCOPES[scope]))
        for file in files:
            workflow = self.github.api(f"actions/workflows/{file}")
            if workflow["state"] not in DISABLED_STATES | {"active"}:
                raise CoordinationError(f"Workflow {file} em estado desconhecido; nenhuma pausa iniciada.")
            if workflow["path"] != f".github/workflows/{file}":
                raise CoordinationError(f"Identidade inesperada do publicador {file}.")
            workflows.append({"file": file, "id": workflow["id"], "previous_state": workflow["state"]})
        state = {"schema": 1, "repository": REPOSITORY, "id": uuid.uuid4().hex,
                 "phase": "DRAINING", "created_at": now(), "owner": owner, "reason": reason,
                 "authorization": authorization, "protected_version": version, "scopes": scopes,
                 "initial_sha": self.github.api("git/ref/heads/main")["object"]["sha"],
                 "workflows": workflows, "pending_runs": [], "history": []}
        self.checkpoint(state, "requested")
        return self.protect(state["id"])

    def inspect(self, state):
        """Confere publicadores e filas reais sem tratar ausência de resposta como fila vazia."""
        enabled, pending = [], []
        for workflow in state["workflows"]:
            current = self.github.api(f"actions/workflows/{workflow['id']}")
            if current["path"] != f".github/workflows/{workflow['file']}":
                raise CoordinationError("Publicador renomeado durante a intervenção; reconciliar o escopo.")
            if current["state"] == "active":
                enabled.append(workflow["file"])
            elif current["state"] not in DISABLED_STATES:
                raise CoordinationError(f"Estado inesperado de {workflow['file']}; proteção não confirmada.")
            pending.extend(self.github.live_runs(workflow["id"]))
        return enabled, pending

    def protect(self, identifier):
        """Pausa produtores e continuações, aguardando as execuções existentes sem cancelá-las."""
        state = self.current(identifier)
        if state["phase"] == "RELEASED":
            raise CoordinationError("Intervenção encerrada; abra outra com autorização própria.")
        if state["phase"] == "OPERATING":
            raise CoordinationError("Comando interrompido sem resultado; conferir o host e usar reconcile-command.")
        state["phase"] = "DRAINING"
        self.checkpoint(state, "protecting")
        for workflow in state["workflows"]:
            current = self.github.api(f"actions/workflows/{workflow['id']}")
            if current["state"] == "active":
                self.github.api(f"actions/workflows/{workflow['id']}/disable", "PUT")
                self.checkpoint(state, "workflow_paused", workflow=workflow["file"])
        enabled, pending = self.inspect(state)
        if not enabled and not pending:
            # Duas observações independentes evitam liberar pela primeira leitura transitória.
            self.settle()
            enabled, pending = self.inspect(state)
        state["pending_runs"] = pending
        if not enabled and not pending:
            state["phase"] = "ACTIVE"
        self.checkpoint(state, "protection_checked", enabled=enabled, pending=pending)
        return state

    def reconcile_command(self, identifier, evidence, confirmed_stopped):
        """Registra a conferência explícita de um comando interrompido, sem presumir que ele terminou."""
        if not confirmed_stopped or not evidence.strip():
            raise CoordinationError("Confirme o término do comando no host e informe a evidência.")
        state = self.current(identifier)
        if state["phase"] != "OPERATING":
            raise CoordinationError("Não existe comando interrompido a reconciliar.")
        enabled, pending = self.inspect(state)
        if enabled or pending:
            raise CoordinationError("Publicadores ou execuções ativos; reconciliação não liberada.")
        state["phase"] = "ACTIVE"
        self.checkpoint(state, "command_reconciled", evidence=evidence)
        return state

    def discard_unstarted(self, identifier, keep_run=None):
        """Retira só runs sem jobs, ainda na fila global do APP; mantém qualquer execução iniciada."""
        state = self.current(identifier)
        if state["phase"] != "DRAINING":
            raise CoordinationError("Descarte de fila permitido somente durante a drenagem.")
        enabled, pending = self.inspect(state)
        if enabled:
            raise CoordinationError("Pause todos os publicadores antes de retirar itens da fila.")
        app = next((w for w in state["workflows"] if w["file"] == "deploy-containers.yml"), None)
        if not app:
            raise CoordinationError("Esta intervenção não inclui a fila global do APP.")
        if keep_run is not None:
            retained = self.github.api(f"actions/runs/{keep_run}")
            current_sha = self.github.api("git/ref/heads/main")["object"]["sha"]
            if (retained["workflow_id"] != app["id"] or retained["head_sha"] != current_sha
                    or retained.get("head_branch") != "main"
                    or retained.get("event") not in {"push", "workflow_dispatch"}):
                raise CoordinationError("Run preservado deve ser a publicação existente da main atual.")
            self.checkpoint(state, "current_main_run_retained", run_id=keep_run, sha=current_sha)
        for item in pending:
            run_id = item["id"]
            if run_id == keep_run:
                continue
            run = self.github.api(f"actions/runs/{run_id}")
            if run["workflow_id"] != app["id"] or run["status"] not in {"pending", "queued"}:
                continue
            jobs = self.github.api(f"actions/runs/{run_id}/jobs?per_page=100")
            if jobs.get("total_count") != 0 or jobs.get("jobs") != []:
                continue
            # Reconfirma a fila depois de consultar os jobs. Nunca usar force-cancel.
            run = self.github.api(f"actions/runs/{run_id}")
            if run["status"] not in {"pending", "queued"}:
                continue
            self.checkpoint(state, "unstarted_cancel_requested", run_id=run_id, sha=run["head_sha"])
            self.github.api(f"actions/runs/{run_id}/cancel", "POST")
        # O aceite do cancelamento não equivale a término; protect consulta novamente a fila.
        return self.protect(identifier)

    def check(self, identifier):
        """Exige proteção atual imediatamente antes de qualquer intervenção no runtime."""
        state = self.current(identifier)
        enabled, pending = self.inspect(state)
        if state["phase"] != "ACTIVE" or enabled or pending:
            if state["phase"] == "ACTIVE":
                state["phase"] = "DRAINING"
                state["pending_runs"] = pending
                self.checkpoint(state, "protection_lost", enabled=enabled, pending=pending)
            raise CoordinationError("Intervenção ainda não protegida; execute protect e aguarde ACTIVE.")
        return state

    def resume(self, identifier, integrated_commit, evidence):
        """Restaura estados anteriores só após comprovar integração e fila vazia; não dispara deploy."""
        if not re.fullmatch(r"[a-f0-9]{40}", integrated_commit or "") or not evidence.strip():
            raise CoordinationError("Retomada exige commit completo integrado e evidência de validação.")
        state = self.current(identifier)
        if state["phase"] == "RELEASED":
            return state
        if state["phase"] == "RESUMING":
            # Uma tentativa parcial volta a pausar antes de reconciliar a retomada.
            state = self.protect(identifier)
        self.check(identifier)
        current_sha = self.github.api("git/ref/heads/main")["object"]["sha"]
        for commit in (state["initial_sha"], integrated_commit):
            comparison = self.github.api(f"compare/{commit}...{current_sha}")
            if comparison.get("status") not in {"ahead", "identical"}:
                raise CoordinationError("Correção ausente da main ou histórico divergente; pausa mantida.")
        state["phase"] = "RESUMING"
        state["integrated_commit"] = integrated_commit
        state["resume_sha"] = current_sha
        state["validation_evidence"] = evidence
        self.checkpoint(state, "resume_authorized")
        # Habilita consumidores antes do produtor central para não perder sua continuação.
        for workflow in state["workflows"]:
            if workflow["previous_state"] == "active":
                self.github.api(f"actions/workflows/{workflow['id']}/enable", "PUT")
                self.checkpoint(state, "workflow_resumed", workflow=workflow["file"])
        for workflow in state["workflows"]:
            live = self.github.api(f"actions/workflows/{workflow['id']}")
            valid = live["state"] == "active" if workflow["previous_state"] == "active" else live["state"] in DISABLED_STATES
            if not valid:
                raise CoordinationError("Retomada parcial; reconciliar com o mesmo identificador.")
        state["phase"] = "RELEASED"
        self.checkpoint(state, "released")
        return state

    def execute(self, identifier, scopes, command):
        """Mantém o lock do operador até o comando autorizado terminar, impedindo retomada concorrente."""
        state = self.check(identifier)
        protected = {workflow["file"] for workflow in state["workflows"]}
        if not scopes or any(scope not in SCOPES or not set(SCOPES[scope]) <= protected for scope in scopes):
            raise CoordinationError("Comando pede componentes fora da intervenção protegida.")
        if not command:
            raise CoordinationError("Informe o comando autorizado após --.")
        # Argumentos podem conter credenciais. O histórico registra somente o executável.
        state["phase"] = "OPERATING"
        self.checkpoint(state, "command_started", executable=Path(command[0]).name, scopes=scopes)
        try:
            result = subprocess.run(command, check=False)
        except OSError:
            state["phase"] = "ACTIVE"
            self.checkpoint(state, "command_failed_to_start")
            raise
        state["phase"] = "ACTIVE"
        self.checkpoint(state, "command_finished", exit_code=result.returncode)
        return result.returncode


def main():
    """Expõe comandos curtos e retomáveis; retorno 75 indica fila ainda em drenagem."""
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    begin = commands.add_parser("begin", help="Pausar publicadores antes da intervenção autorizada")
    begin.add_argument("--scope", action="append", choices=SCOPES, required=True)
    for argument in ("owner", "reason", "authorization", "protected-version"):
        begin.add_argument("--" + argument, required=True)
    commands.add_parser("status", help="Consultar o registro persistido e a situação real")
    for name in ("protect", "check", "resume", "discard-unstarted"):
        command = commands.add_parser(name)
        command.add_argument("--id", required=True)
        if name == "resume":
            command.add_argument("--integrated-commit", required=True)
            command.add_argument("--evidence", required=True)
        if name == "discard-unstarted":
            command.add_argument("--keep-run", type=int, help="Preservar um run existente da main atual")
    execute = commands.add_parser("execute", help="Executar comando autorizado sob a proteção e o lock")
    execute.add_argument("--id", required=True)
    execute.add_argument("--scope", action="append", choices=SCOPES, required=True)
    execute.add_argument("argv", nargs=argparse.REMAINDER)
    reconcile = commands.add_parser("reconcile-command", help="Registrar conferência de comando interrompido")
    reconcile.add_argument("--id", required=True)
    reconcile.add_argument("--evidence", required=True)
    reconcile.add_argument("--confirmed-stopped", action="store_true")
    args = parser.parse_args()
    try:
        with RemoteStore() as store:
            coordinator = Coordinator(GitHub(), store)
            if args.command == "begin":
                state = coordinator.begin(args.scope, args.owner, args.reason, args.authorization, args.protected_version)
            elif args.command == "status":
                state = store.load()
                if state:
                    enabled, pending = coordinator.inspect(state)
                    state = {**state, "live_enabled": enabled, "live_pending_runs": pending,
                             "safe_to_intervene": state["phase"] == "ACTIVE" and not enabled and not pending}
            elif args.command == "resume":
                state = coordinator.resume(args.id, args.integrated_commit, args.evidence)
            elif args.command == "execute":
                command = args.argv[1:] if args.argv[:1] == ["--"] else args.argv
                return coordinator.execute(args.id, args.scope, command)
            elif args.command == "reconcile-command":
                state = coordinator.reconcile_command(args.id, args.evidence, args.confirmed_stopped)
            elif args.command == "discard-unstarted":
                state = coordinator.discard_unstarted(args.id, args.keep_run)
            else:
                state = getattr(coordinator, args.command)(args.id)
            print(json.dumps(state, ensure_ascii=False, indent=2))
            return 75 if state and state["phase"] == "DRAINING" else 0
    except (CoordinationError, OSError, KeyError, TypeError) as error:
        print(json.dumps({"error": str(error), "automatic_resume": False}, ensure_ascii=False), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
