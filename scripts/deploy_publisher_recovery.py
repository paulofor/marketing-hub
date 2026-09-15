"""Reconcilia publicadores após homologação encerrada e integração comprovada, sob o lock existente."""

from datetime import datetime
import json
from pathlib import Path

from deploy_coordination_errors import GitHubApiError

POLICY = json.loads(Path(__file__).with_name("deploy-publisher-recovery.json").read_text())
APP = "deploy-containers.yml"

# Exceções legadas exigem alteração versionada no repositório. Elas nunca são inferidas por tempo.
# Este registro específico autoriza substituir a revisão manual órfã pela main atual, conforme
# decisão operacional de 2026-09-15 de priorizar a versão mais recente em produção.
LEGACY_ORPHANED_VALIDATIONS = {
    "983c827dbf6c477daab5006788d1d55b": {
        "validated_commit": "12e2e35ab4db307990924062eba6e243cb054062",
        "action": "supersede_with_current_main",
        "authorized_by": "paulofor",
        "authorized_at": "2026-09-15T22:04:18Z",
        "reason": "Revisão manual não recuperável no GitHub; priorizar a main atual em produção.",
    }
}


def matching_runs(github, workflow, sha, since):
    """Confere identidade, evento e data mesmo quando a API oferece os mesmos filtros."""
    return sorted([
        run for run in github.runs_for(workflow, sha, since)
        if run.get("head_sha") == sha and run.get("head_branch") == "main"
        and run.get("event") in {"push", "workflow_dispatch", "workflow_run"}
        and datetime.fromisoformat(run["created_at"].replace("Z", "+00:00"))
        >= datetime.fromisoformat(since.replace("Z", "+00:00"))
    ], key=lambda run: (run["created_at"], run["id"]), reverse=True)


def successful_application(github, sha, since):
    """Exige sucesso da aplicação na mesma revisão; continuação ou PR não liberam agentes."""
    runs = [r for r in matching_runs(github, APP, sha, since) if r["event"] != "workflow_run"]
    return bool(runs and runs[0]["status"] == "completed" and runs[0].get("conclusion") == "success")


class PublisherRecovery:
    """Decide retomada e dispatch auditáveis; nunca executa comandos Docker ou SSH de publicação."""

    def __init__(self, coordinator):
        self.coordinator = coordinator
        self.github = coordinator.github

    def outcome(self, state, status, reason):
        """Persiste mudança de diagnóstico sem inflar o histórico em consultas idênticas."""
        prepared = state["automatic_resume"]
        result = {"status": status, "reason": reason, "intervention_id": state["id"],
                  "phase": state["phase"], "validated_commit": prepared["validated_commit"],
                  "effective_commit": prepared.get("effective_commit", prepared["validated_commit"])}
        if prepared.get("superseded_validated_commit"):
            result["superseded_validated_commit"] = prepared["superseded_validated_commit"]
        if state.get("recovery"):
            result.update(state["recovery"])
        if state.get("recovery_result") != result:
            state["recovery_result"] = result
            self.coordinator.checkpoint(state, "publishers_reconciled", result=result)
        return result

    def authorized_supersession(self, state, missing_commit, current_sha):
        """Permite superar revisão órfã somente por autorização explícita e versionada."""
        authorization = LEGACY_ORPHANED_VALIDATIONS.get(state["id"])
        if not authorization:
            return None
        if (authorization.get("validated_commit") != missing_commit
                or authorization.get("action") != "supersede_with_current_main"):
            return None
        prepared = state["automatic_resume"]
        if prepared.get("effective_commit") != current_sha:
            prepared["superseded_validated_commit"] = missing_commit
            prepared["effective_commit"] = current_sha
            prepared["supersession"] = {
                "authorized_by": authorization["authorized_by"],
                "authorized_at": authorization["authorized_at"],
                "reason": authorization["reason"],
            }
            self.coordinator.checkpoint(
                state,
                "orphaned_validation_superseded",
                missing_commit=missing_commit,
                replacement_commit=current_sha,
                authorization=prepared["supersession"],
            )
        return current_sha

    def observe_receipts(self, state, workflows):
        """Lê recibos pelo ID exato, inclusive se main avançou entre a conferência e o dispatch."""
        observed = {}
        recovery = state.get("recovery", {})
        for name, entry in recovery.get("publications", {}).items():
            identifier = entry.get("dispatch_run_id")
            if not identifier:
                continue
            run = self.github.api(f"actions/runs/{identifier}")
            if (run.get("id") != identifier or run.get("workflow_id") != workflows[name]["id"]
                    or run.get("event") != "workflow_dispatch" or run.get("head_branch") != "main"):
                raise ValueError("Recibo de publicação divergente; requer conciliação operacional.")
            observed[name] = run
            entry["run_id"] = identifier
            if run.get("head_sha") != recovery["sha"] and run["status"] == "completed":
                if run.get("conclusion") != "failure":
                    raise ValueError("Publicação de revisão divergente sem reprovação do guard; conferir o run.")
                excluded = state.setdefault("superseded_dispatch_ids", [])
                if identifier not in excluded:
                    excluded.append(identifier)
                    self.coordinator.checkpoint(state, "dispatch_revision_changed", run_id=identifier,
                                                expected_sha=recovery["sha"], observed_sha=run["head_sha"])
        return observed

    def reconcile(self):
        """Executa uma passagem curta, retomável e sem espera bloqueante pela fila dos Actions."""
        state = self.coordinator.store.load()
        if not state or not state.get("automatic_resume"):
            return {"status": "NO_ACTION", "reason": "Nenhuma homologação preparada para retomada automática."}
        if state["phase"] not in {"AWAITING_MERGE", "RESUMING", "RELEASED", "ACTIVE", "DRAINING"}:
            return self.outcome(state, "WAITING", "Intervenção ainda em andamento; proteção preservada.")
        if state.get("recovery", {}).get("completed"):
            return self.outcome(state, "COMPLETE", "Publicações recuperadas; próximos merges seguem os gatilhos normais.")

        prepared = state["automatic_resume"]
        if state["phase"] in {"RESUMING", "DRAINING"}:
            state = self.coordinator.protect(state["id"])
            if state["phase"] != "ACTIVE":
                return self.outcome(state, "WAITING", "Retomada parcial: aguardando a fila terminar com publicadores protegidos.")
        sha = self.github.api("git/ref/heads/main")["object"]["sha"]
        effective_commit = prepared.get("effective_commit", prepared["validated_commit"])
        checks = (("base protegida", state["initial_sha"]), ("revisão validada", effective_commit))
        for label, commit in checks:
            try:
                comparison = self.github.api(f"compare/{commit}...{sha}")
            except GitHubApiError as error:
                if error.status != 404:
                    raise
                if label == "revisão validada" and commit == prepared["validated_commit"]:
                    replacement = self.authorized_supersession(state, commit, sha)
                    if replacement:
                        effective_commit = replacement
                        comparison = {"status": "identical"}
                    else:
                        return self.outcome(
                            state, "BLOCKED",
                            f"Revisão validada {commit} não existe mais no GitHub (HTTP 404). "
                            "A homologação permanece protegida, mas esse estado exige correção explícita; "
                            "novas preparações recusam commits não recuperáveis.",
                        )
                else:
                    return self.outcome(
                        state, "BLOCKED",
                        f"{label.capitalize()} {commit} indisponível no GitHub (HTTP 404); "
                        "não é seguro liberar publicadores automaticamente.",
                    )
            if not isinstance(comparison, dict) or comparison.get("status") not in {
                "ahead", "identical", "behind", "diverged"
            }:
                raise ValueError("Comparação de revisões inválida; integração não comprovada e pausa preservada.")
            if comparison["status"] not in {"ahead", "identical"}:
                return self.outcome(state, "WAITING", "A revisão homologada ainda não está integrada à main.")
        if state["phase"] != "RELEASED":
            enabled, pending = self.coordinator.inspect(state)
            if pending:
                return self.outcome(state, "WAITING", "Execuções anteriores ainda em andamento; pausa preservada.")
            if enabled:
                return self.outcome(state, "BLOCKED", "Proteção alterada externamente antes da retomada.")
            # resume é a única autoridade que restaura estados anteriores e drena retomadas parciais.
            state = self.coordinator.resume(state["id"], effective_commit, prepared["evidence"])

        workflows = {w["file"]: w for w in state["workflows"] if w["previous_state"] == "active"}
        selected = [name for name in POLICY if name in workflows]
        receipts = self.observe_receipts(state, workflows)
        previous = state.get("recovery")
        if not previous or previous["sha"] != sha:
            if previous and any(entry.get("dispatch_requested_at") and not entry.get("run_id")
                                for entry in previous["publications"].values()):
                # Primeiro concilia o recibo da revisão anterior; avanço de main não autoriza repetir pedido incerto.
                for name, entry in previous["publications"].items():
                    if entry.get("dispatch_requested_at") and not entry.get("run_id"):
                        observed = matching_runs(self.github, name, previous["sha"], prepared["prepared_at"])
                        if observed:
                            entry["run_id"] = observed[0]["id"]
                if any(entry.get("dispatch_requested_at") and not entry.get("run_id")
                       for entry in previous["publications"].values()):
                    return self.outcome(state, "WAITING", "Dispatch anterior ainda sem recibo; main nova não autoriza duplicação.")
            if (any(run["status"] != "completed" for run in receipts.values())
                    or any(self.github.live_runs(workflows[name]["id"]) for name in selected)):
                return self.outcome(state, "WAITING", "Aguardando publicações já iniciadas antes de recuperar a main.")
            if previous:
                self.coordinator.checkpoint(state, "recovery_revision_superseded", previous=previous)
            state["recovery"] = {"sha": sha, "publications": {}, "completed": False}
            self.coordinator.checkpoint(state, "recovery_planned")
        recovery = state["recovery"]
        publications = recovery["publications"]
        for name in selected:
            workflow = workflows[name]
            live = self.github.api(f"actions/workflows/{workflow['id']}")
            if live.get("state") != "active" or live.get("path") != f".github/workflows/{name}":
                return self.outcome(state, "BLOCKED", f"Publicador {name} desativado ou renomeado após retomada.")
            runs = matching_runs(self.github, name, sha, prepared["prepared_at"])
            receipt = receipts.get(name)
            if receipt and receipt["head_sha"] == sha and not any(r["id"] == receipt["id"] for r in runs):
                runs.insert(0, receipt)
            runs = [run for run in runs if run["id"] not in state.get("superseded_dispatch_ids", [])]
            if not POLICY[name].get("requires_app"):
                runs = [r for r in runs if r["event"] != "workflow_run"]
            entry = publications.setdefault(name, {})
            if runs:
                # Runs vivos são preservados mesmo quando há uma tentativa anterior já verde.
                current = next((r for r in runs if r["status"] != "completed"), runs[0])
                entry.update(run_id=current["id"], url=current["html_url"])
                if current["status"] != "completed":
                    entry["status"] = "RUNNING"
                    continue
                if current.get("conclusion") != "success":
                    entry["status"] = "FAILED"
                    return self.outcome(state, "BLOCKED", f"{name} falhou; corrigir a causa antes de nova publicação.")
                if POLICY[name].get("requires_app"):
                    jobs = self.github.pages(f"actions/runs/{current['id']}/jobs", "jobs")
                    published = any(j["name"] == POLICY[name]["publication_job"]
                                    and j.get("conclusion") == "success" for j in jobs)
                    if published:
                        entry["status"] = "COMPLETE"
                        continue
                    if current["event"] == "push":
                        continuations = [r for r in runs if r["event"] == "workflow_run"]
                        for run in continuations:
                            jobs = self.github.pages(f"actions/runs/{run['id']}/jobs", "jobs")
                            if any(j["name"] == POLICY[name]["publication_job"]
                                   and j.get("conclusion") == "success" for j in jobs):
                                entry.update(status="COMPLETE", run_id=run["id"], url=run["html_url"])
                                break
                        if entry.get("status") == "COMPLETE":
                            continue
                    # Continuação sem publicação não comprova a entrega; dispatch recupera a origem ausente.
                    if current["event"] == "workflow_dispatch":
                        entry["status"] = "FAILED"
                        return self.outcome(state, "BLOCKED", f"{name} encerrou sem executar o job de publicação.")
                else:
                    entry["status"] = "COMPLETE"
                    continue
            if entry.get("dispatch_requested_at"):
                entry["status"] = "AWAITING_RECEIPT"
                continue
            if self.github.live_runs(workflow["id"]):
                entry["status"] = "WAITING_QUEUE"
                continue
            if POLICY[name].get("requires_app") and not successful_application(self.github, sha, prepared["prepared_at"]):
                entry["status"] = "WAITING_APP"
                continue
            if self.github.api("git/ref/heads/main")["object"]["sha"] != sha:
                return self.outcome(state, "WAITING", "Main avançou; a próxima passagem reconciliará a revisão atual.")
            entry["status"] = "REQUESTED"
            # A intenção vai ao disco antes da API: desconexão após aceite nunca duplica dispatch.
            entry["dispatch_requested_at"] = self.coordinator.store.load()["updated_at"]
            self.coordinator.checkpoint(state, "publisher_dispatch_requested", workflow=name, sha=sha)
            inputs = {**POLICY[name].get("inputs", {}), "recovery_sha": sha}
            try:
                receipt = self.github.api(f"actions/workflows/{workflow['id']}/dispatches", "POST",
                                          {"ref": "main", "inputs": inputs})
            except RuntimeError as error:
                self.coordinator.checkpoint(state, "publisher_dispatch_unconfirmed", workflow=name, error=str(error))
                raise
            if receipt:
                entry["dispatch_run_id"] = receipt["workflow_run_id"]
                entry["url"] = receipt["html_url"]
            self.coordinator.checkpoint(state, "publisher_dispatch_accepted", workflow=name)
        recovery["completed"] = all(publications.get(name, {}).get("status") == "COMPLETE" for name in selected)
        return self.outcome(state, "COMPLETE" if recovery["completed"] else "WAITING",
                            "Publicações recuperadas." if recovery["completed"] else "Aguardando filas, resultados ou confirmação do dispatch.")
