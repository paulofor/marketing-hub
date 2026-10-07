"""Converte resultados reais dos controles em prova sanitizada; não publica propriedades do JVM."""
import datetime
import hashlib
import json
from pathlib import Path
import subprocess
import sys
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[3]
SUITE = "com.marketinghub.pde.mira.privateprototype.v1.MiraPrivateControlsMysql57Test"
SOURCE = "backend/ads-service/src/test/java/com/marketinghub/pde/mira/privateprototype/v1/MiraPrivateControlsMysql57Test.java"
METHODS = {
    "concurrentGenerationConsumesOnce": "CONCURRENT_SINGLE_CONSUMPTION",
    "twoOrganizationsRejectThird": "ORGANIZATION_LIMIT",
    "twelveItemsAcceptedThirteenthRejected": "PRODUCT_ITEM_LIMIT",
    "expiredCredentialRejected": "EXPIRATION",
    "revokedCredentialRejected": "REVOCATION",
    "closedContextRejectsMutationsAndPreservesResult": "CLOSED_CONTEXT",
    "incompatibleContextRejectsMutations": "INCOMPATIBLE_CONTEXT",
}


def digest(raw):
    """Calcula integridade dos bytes, sem incluir credenciais ou propriedades do ambiente."""
    return hashlib.sha256(raw).hexdigest()


def serialize(value):
    """Gera representação canônica para os hashes de cada resultado."""
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def emit(xml_file, output):
    """Exige sete critérios executados e aprovados na classe atual antes de emitir cobertura."""
    raw = xml_file.read_bytes()
    suite = ElementTree.fromstring(raw)
    if suite.get("name") != SUITE or any(int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")):
        raise ValueError("A suíte contém falha, erro, skip ou identidade incompatível.")
    compiled = ROOT / "backend/ads-service/target/test-classes" / (SUITE.replace(".", "/") + ".class")
    if not compiled.exists() or compiled.stat().st_mtime < (ROOT / SOURCE).stat().st_mtime:
        raise ValueError("Execute novamente a classe alterada antes de emitir prova.")
    cases = suite.findall("testcase")
    if len(cases) != len(METHODS) or {case.get("name") for case in cases} != set(METHODS):
        raise ValueError("Cobertura incompleta ou nomes de critérios incompatíveis.")
    receipt = []
    for case in cases:
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")) or case.get("classname") != SUITE:
            raise ValueError("O caso deve ter resultado aprovado, sem erro ou skip.")
        receipt.append({"class": SUITE, "method": case.get("name"), "status": "PASS", "durationSeconds": float(case.get("time", "0"))})
    criteria = [{"code": METHODS[item["method"]], "status": "PASS", "testMethod": item["method"],
                 "resultSha256": digest(serialize(item)), "source": SOURCE, "result": item} for item in receipt]
    source_files = [SOURCE,
        "backend/ads-service/src/test/java/com/marketinghub/pde/mira/privateprototype/v1/MiraPrivateLocalApplication.java",
        "backend/ads-service/src/main/java/com/marketinghub/pde/mira/privateprototype/v1/service/MiraPrivateService.java",
        "backend/ads-service/src/main/java/com/marketinghub/pde/mira/privateprototype/v1/controller/MiraPrivateController.java",
        "backend/ads-service/src/main/java/com/marketinghub/pde/mira/privateprototype/v1/service/contract/MiraPrivateContract.java",
        "backend/ads-service/src/main/java/com/marketinghub/pde/mira/privateprototype/v1/service/MiraRoutinePolicy.java"]
    fingerprint = subprocess.check_output(["node", str(ROOT / "pde-platform/frontend/scripts/source-fingerprint.mjs"), str(ROOT / "pde-platform/frontend")], text=True).strip()
    report = {"contractVersion": "PDE_OPERATIONAL_CONTROLS_EVIDENCE_V1", "productSlug": "pde-planejado-36",
        "prototypeVersion": "mira-private-candidate-v3", "status": "PASS",
        "origin": "LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES",
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "frontendSourceFingerprint": fingerprint, "providerCalls": 0, "commercialSideEffects": False,
        "requiredCriteria": list(METHODS.values()), "criteria": criteria,
        "testReceiptSha256": digest(serialize(receipt)), "rawJunitSha256": digest(raw),
        "testedProductIds": [8006, 8017], "testedCycleIds": [7006, 7017], "testedExperimentIds": [9006, 9017],
        "sourceFiles": [{"path": path, "sha256": digest((ROOT / path).read_bytes())} for path in source_files],
        "limits": {"organizations": 2, "productsPerInput": 12, "credentialDays": 7},
        "boundaries": ["HTTP, validação e transações de sessão reais em MySQL 5.7 isolado.",
          "Cadastros de ciclo são doubles locais; expiração altera somente sessão sintética local.",
          "Não houve mutação de ciclo publicado, cliente humano, pagamento, mídia ou provedor externo.",
          "Prova técnica da implementação; não é parecer independente nem evidência de mercado."]}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({"criteria": len(criteria), "status": "PASS", "reportSha256": digest(output.read_bytes())}))


if __name__ == "__main__":
    emit(Path(sys.argv[1]), Path(sys.argv[2]))
