"""Emite suplemento vinculado ao parecer somente a partir de provas locais completas."""
import datetime
import hashlib
import json
from pathlib import Path
import subprocess
import sys
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[3]
SUITE = "com.marketinghub.pde.vega.consultor.v1.MusaConsultorOperationalControlsMysql57Test"
IMPLEMENTATION = [
    "backend/ads-service/src/main/java/com/marketinghub/pde/vega/consultor/v1/service/MusaConsultorService.java",
    "backend/ads-service/src/main/java/com/marketinghub/pde/vega/consultor/v1/controller/MusaConsultorController.java",
    "backend/ads-service/src/main/java/com/marketinghub/pde/vega/style/v1/service/VegaContextualStyleRules.java",
    "pde-platform/frontend/src/MusaContinuousApp.tsx",
    "pde-platform/frontend/src/musaContinuous.css",
    "pde-platform/backend/src/main/java/com/marketinghub/pde/vega/privatevalidation/v1/MusaConsultorGatewayController.java",
]


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode()


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def emit(evidence_dir, binding_file, output):
    binding = json.loads(binding_file.read_text())
    original = json.loads(binding["resultJson"])
    for field in ("productId", "sourceReference", "prototypeVersion"):
        if binding[field] != original[field]:
            raise ValueError("Identidade do vínculo difere do parecer persistido.")
    if binding["activityId"] != "commercialIntegrityReview" or original["decision"] != "BLOCKED":
        raise ValueError("O suplemento não corresponde a uma revisão bloqueada de integridade.")
    controls = json.loads((evidence_dir / "musa-controls.json").read_text())
    terminal = json.loads((evidence_dir / "terminal" / "terminal-result.json").read_text())
    suite = ElementTree.parse(ROOT / "backend/ads-service/target/surefire-reports" / ("TEST-" + SUITE + ".xml")).getroot()
    if int(suite.get("tests", "0")) != 14 or any(int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")):
        raise ValueError("Os quatorze casos reais de controle precisam passar, sem skip.")
    required = set(json.loads((ROOT / "backend/ads-service/src/main/resources/contracts/pde-contextual-execution-controls-v1.json").read_text())["requiredCriteria"])
    grouped = {}
    for item in controls["results"]:
        if item["status"] != "PASS" or not item["databaseVersion"].startswith("5.7."):
            raise ValueError("O resultado precisa vir de execução aprovada no MySQL 5.7.")
        grouped.setdefault(item["code"], []).append(item)
    if not required.issubset(grouped) or not {"IDEMPOTENCY_CONFLICT", "REVISION_CONFLICT", "PREFERENCE_REVISION_EFFECT"}.issubset(grouped):
        raise ValueError("Faltam controles ou efeito da preferência revisada.")
    criteria = []
    for code, values in sorted(grouped.items()):
        if len(values) != 2 or len({item["productId"] for item in values}) != 2:
            raise ValueError("Cada controle exige duas identidades independentes.")
        result = {"status": "PASS", "contexts": [{"productId": item["productId"], "cycleId": item["cycleId"],
                  "experimentId": item["experimentId"], "status": "PASS", "observed": item["observed"]} for item in values]}
        criteria.append({"code": code, "status": "PASS", "verificationType": "EXECUTION", "testMethod": values[0]["testMethod"],
                         "result": result, "resultSha256": digest(encoded(result))})
    expected = {(device, state) for device in ("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")
                for state in ("NOT_STARTED", "COMPLETED", "BLOCKED", "RECOVERABLE_TRANSPORT_FAILURE")}
    actual = {(item["device"], item["state"]) for item in terminal["results"]}
    if len(terminal["results"]) != 12 or actual != expected:
        raise ValueError("Cobertura terminal incompleta.")
    for item in terminal["results"]:
        if item["status"] != "PASS" or not item.get("persistedAfterClose", {}).get("homologationClosed"):
            raise ValueError("Falta comprovação persistida do encerramento.")
        if item["persistedAfterClose"]["savedChoices"] != len(item["persistedBeforeClose"]["choices"]):
            raise ValueError("O encerramento não preservou as escolhas confirmadas.")
        value = {key: val for key, val in item.items() if key != "resultSha256"}
        value["screenshots"] = [{"path": filename, "sha256": digest((evidence_dir / "terminal" / filename).read_bytes())}
                                for filename in item["screenshots"]]
        criteria.append({"code": "TERMINAL_" + item["device"] + "_" + item["state"], "status": "PASS",
                         "verificationType": "EXECUTION", "testMethod": "terminal-supplement.cjs", "result": value,
                         "resultSha256": digest(encoded(value))})
    for filename in IMPLEMENTATION:
        committed = subprocess.check_output(["git", "show", "HEAD:" + filename], cwd=ROOT)
        if committed != (ROOT / filename).read_bytes():
            raise ValueError("Mudança da implementação exige contrato de revalidação próprio: " + filename)
    fingerprint = subprocess.check_output(["node", "pde-platform/frontend/scripts/source-fingerprint.mjs", "pde-platform/frontend"], cwd=ROOT, text=True).strip()
    source_files = IMPLEMENTATION + ["infra/testing/musa-consultor/terminal-supplement.cjs", "infra/testing/musa-consultor/emit-supplement.py",
        "backend/ads-service/src/test/java/" + SUITE.replace(".", "/") + ".java"]
    report = {
        "contractVersion": "PDE_OPERATIONAL_CONTROLS_EVIDENCE_V1", "productSlug": original["productSlug"],
        "prototypeVersion": original["prototypeVersion"], "status": "PASS", "origin": "LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES",
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(), "providerCalls": 0,
        "commercialSideEffects": False, "agentApprovalClaimed": False,
        "operationalControlsContract": "PDE_CONTEXTUAL_EXECUTION_CONTROLS_V1", "frontendSourceFingerprint": fingerprint,
        "testReceiptSha256": digest(encoded({"controls": controls, "terminal": terminal})),
        "implementationBaselineCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "sourceFiles": [{"path": name, "sha256": digest((ROOT / name).read_bytes())} for name in source_files],
        "requiredCriteria": [item["code"] for item in criteria], "criteria": criteria,
        "reviewReevaluation": {"contractVersion": "PDE_REVIEW_EVIDENCE_SUPPLEMENT_V1", "reasonCode": "MISSING_EXECUTION_EVIDENCE",
            "implementationChanged": False, "independentReviewRequired": True, "productId": binding["productId"],
            "sourceReference": binding["sourceReference"], "blockedTaskId": binding["taskId"], "activityId": binding["activityId"],
            "blockedResultSha256": digest(binding["resultJson"].encode())},
        "reusedEvidence": {"technicalTaskId": binding["technicalTaskId"], "scenarioTaskIds": binding["scenarioTaskIds"],
            "policy": "Preservar horários, custos e pareceres compatíveis; exigir novo aceite independente de Têmis."},
        "boundaries": ["Backend, gateway, navegador e MySQL 5.7 reais na sandbox; ciclos e produtos sintéticos.",
            "Falha de transporte induzida no navegador, sem inventar estado FAILED persistido no contrato síncrono.",
            "Chromium com emulação mobile; não comprova Safari ou aparelhos físicos.",
            "Mesma implementação funcional; não houve execução de mutações nos ciclos publicados.",
            "Zero provedor, cobrança, campanha, mídia, vídeo, evidência humana ou comercial."],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"criteria": len(criteria), "status": "PASS", "sha256": digest(output.read_bytes())}))


if __name__ == "__main__":
    emit(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
