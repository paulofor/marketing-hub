#!/usr/bin/env python3
"""Simula o contrato de transporte observado, sem credencial nem inferência."""
import json
import pathlib
import re
import sys

root = pathlib.Path(__file__).parent
arguments = sys.argv[1:]
tiers = [value for value in arguments if value.startswith("service_tier=")]
if tiers != ['service_tier="default"']:
    print(json.dumps({"type": "error", "status": 400,
                      "message": "Unsupported service_tier: flex"}))
    sys.exit(1)
assert arguments[arguments.index("--sandbox") + 1] == "read-only"
assert "--model" in arguments
schema = json.loads(pathlib.Path(arguments[arguments.index("--output-schema") + 1]).read_text())
result = json.loads((root / "expected.json").read_text())


def validate(value, contract):
    """Exige saída aderente ao schema real para não ocultar outro bloqueio do provedor."""
    kind = contract.get("type")
    if kind == "object":
        assert isinstance(value, dict)
        assert set(contract.get("required", [])).issubset(value)
        if contract.get("additionalProperties") is False:
            assert set(value).issubset(contract.get("properties", {}))
        for key, item in value.items():
            validate(item, contract.get("properties", {}).get(key, {}))
    elif kind == "array":
        assert isinstance(value, list)
        assert len(value) >= contract.get("minItems", 0)
        for item in value:
            validate(item, contract.get("items", {}))
    elif kind == "boolean":
        assert type(value) is bool
    elif kind in ("integer", "number"):
        assert isinstance(value, (int, float)) and not isinstance(value, bool)
        assert value >= contract.get("minimum", float("-inf"))
    elif kind == "string":
        assert isinstance(value, str) and len(value) >= contract.get("minLength", 0)
        if "pattern" in contract:
            assert re.search(contract["pattern"], value)
    if "const" in contract:
        assert value == contract["const"]
    if "enum" in contract:
        assert value in contract["enum"]


validate(result, schema)
(root / "arguments.json").write_text(json.dumps(arguments))
(root / "prompt.txt").write_text(sys.stdin.read())
with (root / "attempts.txt").open("a") as attempts:
    attempts.write("request\n")
pathlib.Path(arguments[arguments.index("--output-last-message") + 1]).write_text(json.dumps(result))
print(json.dumps({"type": "turn.completed", "usage": {
    "input_tokens": 100, "cached_input_tokens": 20, "output_tokens": 30}}))
