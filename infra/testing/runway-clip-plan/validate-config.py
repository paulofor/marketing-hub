"""Valida localmente a configuração candidata e os payloads, sem acessar a Runway."""
import copy
import json
import pathlib
import sys

import jsonschema

api = json.loads(pathlib.Path(sys.argv[1]).read_text())
config = json.loads(pathlib.Path(sys.argv[2]).read_text())
requests = json.loads(pathlib.Path(sys.argv[3]).read_text())
contract = json.loads(pathlib.Path(sys.argv[4]).read_text())
config_schema = api["/v1/routers"]["post"]["requestBody"]["content"]["application/json"]["schema"]
video_schema = api["/v1/generate/video"]["post"]["requestBody"]["content"]["application/json"]["schema"]
jsonschema.Draft202012Validator(config_schema).validate(config)
assert config["settings"]["models"] == {"mode": "allowlist_only", "ids": ["gen4.5"]}
assert config["settings"]["optimizeFor"] == "quality"
assert config["settings"]["fallback"]["onCapacity"] is False
assert config["settings"]["maxCreditsPerGeneration"]["video"] == 400
assert len(requests) == contract["cycle"]["generationClipCount"] == 2
assert sum(r["input"]["duration"] for r in requests) == 15
assert 400 * len(requests) <= contract["pending"]["maxCredits"]
for request in requests:
    jsonschema.Draft202012Validator(video_schema).validate(request)
    assert request["configId"] == config["slug"]
    assert request["input"]["duration"] <= 10
    dry_run = copy.deepcopy(request)
    dry_run["dryRun"] = True
    jsonschema.Draft202012Validator(video_schema).validate(dry_run)
    del dry_run["dryRun"]
    assert dry_run == request
print("PASS: schemas oficiais, lista fechada, duração integral, teto de 800 créditos e dry run sem drift.")
