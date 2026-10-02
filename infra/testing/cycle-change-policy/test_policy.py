"""Verifica o contrato operacional com identidades sintéticas e sem dados produtivos."""
import copy
import unittest
from policy import revise, PROCESS, POLICY


class PolicyTest(unittest.TestCase):
    def source(self, code):
        return {"id": 8001, "processCode": code, "versionNumber": 31, "status": "PUBLISHED", "purpose": "Valor verificável.", "diagram": {"nodes": [{"id": "work", "type": "TASK", "owner": "Agente", "description": "Prova existente.", "executionResourceCode": "existing", "subprocessRoutes": [{"productTypeCode": "PDE", "subprocessVersion": 8}]}], "flows": []}}

    def test_preserves_sources_contracts_and_unrelated_identities(self):
        for code in PROCESS:
            with self.subTest(code=code):
                original = self.source(code)
                snapshot = copy.deepcopy(original)
                candidate = revise(original)
                self.assertEqual(original, snapshot)
                self.assertEqual(candidate["versionNumber"], 32)
                node = candidate["diagram"]["nodes"][0]
                self.assertEqual(node["executionResourceCode"], "existing")
                self.assertEqual(node["subprocessRoutes"], snapshot["diagram"]["nodes"][0]["subprocessRoutes"])
                for phrase in [POLICY, "novo ciclo e novo experimento", "predecessor", "custos", "não misturar métricas", "gasto"]:
                    self.assertIn(phrase, node["description"])

    def test_cycle_routes_changes_to_new_occurrence_and_preserves_measurement(self):
        source = self.source("value-chain-learning-sales-cycle")
        source["diagram"]["nodes"].append({"id": "end", "type": "END", "label": "Fim"})
        source["diagram"]["flows"] = [
            {"from": "commercialDecision", "to": "ADJUSTMENT", "label": "Mesmo experimento"},
            {"from": "commercialDecision", "to": "SCALE_AUTHORIZATION"},
            {"from": "SCALE_AUTHORIZATION", "to": "MEASUREMENT"},
            {"from": "commercialDecision", "to": "MEASUREMENT", "label": "Coleta inalterada"}]
        result = revise(source)["diagram"]
        self.assertEqual(result["experimentChangePolicy"], POLICY)
        self.assertEqual([f["to"] for f in result["flows"]], ["end", "MEASUREMENT"])
        self.assertTrue(any(n["id"] == "end" and "novo ciclo" in n["label"] for n in result["nodes"]))

    def test_unknown_process_fails_before_any_change(self):
        with self.assertRaises(ValueError):
            revise(self.source("unrelated-process"))


if __name__ == "__main__":
    unittest.main()
