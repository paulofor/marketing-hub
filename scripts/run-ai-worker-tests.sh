#!/usr/bin/env bash
# Preserva a saída integral dos testes sem enviar payloads multimodais gigantes ao console do CI.
set -euo pipefail

mkdir -p target/test-evidence
test_exit=0
mvn -B -s settings.xml test > target/test-evidence/maven-test.log 2>&1 || test_exit=$?
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

reports = list(Path('target/surefire-reports').glob('TEST-*.xml'))
totals = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
for report in reports:
    root = ET.parse(report).getroot()
    for key in totals:
        totals[key] += int(root.get(key, 0))
    for case in root.findall('testcase'):
        for problem in list(case.findall('failure')) + list(case.findall('error')):
            print(f"Falha: {case.get('classname')}.{case.get('name')}: {problem.get('message', '')[:800]}")
print('Worker IA:', ', '.join(f'{key}={value}' for key, value in totals.items()))
print('Diagnóstico integral: target/test-evidence/maven-test.log e target/surefire-reports.')
PY
exit "$test_exit"
