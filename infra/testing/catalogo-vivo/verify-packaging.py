#!/usr/bin/env python3
"""Confere o pacote do piloto antes de commit, incluindo a retirada explícita dos textos migrados."""
import importlib.util
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[3]
MODULES = ['landing-generator-agent-worker', 'financial-agent-worker', 'customer-agent-worker', 'meta-ad-approver-worker']
REMOVED = {f'landing-generator-agent-worker/src/main/resources/prompts/opala-commercial/v1/{name}.md'
           for name in ['entry', 'creative', 'checkout', 'targeting']}
REMOVED.add('financial-agent-worker/src/main/resources/prompts/opala-commercial/v1/economics.md')


def main():
    """Verifica recursos remanescentes e removidos e inicializa o harness no JAR executável real."""
    spec = importlib.util.spec_from_file_location('packaging', ROOT/'scripts/verify-backend-packaged-resources.py')
    check = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(check)
    jar = ROOT/'backend/ads-service/target/app-exec.jar'
    print('Classes do backend:', check.verify_compiled_backend(ROOT, jar))
    paths = set(subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT, text=True).split('\0'))
    paths.discard('')
    for removed in REMOVED:
        if (ROOT/removed).exists():
            raise ValueError('Texto operacional migrado voltou ao arquivo: '+removed)
    total = 0
    with ZipFile(jar) as archive:
        entries = set(archive.namelist())
        for resource in ET.parse(ROOT/'backend/ads-service/pom.xml').findall('m:build/m:resources/m:resource', check.NS):
            target = resource.findtext('m:targetPath', namespaces=check.NS)
            if not target:
                continue
            patterns = [node.text for node in resource.findall('m:includes/m:include', check.NS)]
            expected = sorted(path for path in paths-REMOVED if any(check.matches(path,p) for p in patterns))
            if not expected:
                raise ValueError('Recurso externo sem fonte: '+target)
            for path in expected:
                source = ROOT/path
                entry = f'BOOT-INF/classes/{target}/{path}'
                if not source.is_file() or entry not in entries or archive.read(entry) != source.read_bytes():
                    raise ValueError('Recurso ausente ou divergente: '+path)
                total += 1
        for path in REMOVED:
            if 'BOOT-INF/classes/agent-behavior-files/'+path in entries:
                raise ValueError('Texto substituído permaneceu no backend: '+path)
    print('Recursos externos íntegros:', total)
    for module in MODULES:
        with ZipFile(ROOT/module/'target/app.jar') as archive:
            entries = set(archive.namelist())
            if not any(path.endswith('/CatalogPromptInput.class') for path in entries):
                raise ValueError('Leitor do catálogo não empacotado: '+module)
            resources = ROOT/module/'src/main/resources'
            for source in resources.rglob('*'):
                if not source.is_file():
                    continue
                entry = 'BOOT-INF/classes/'+source.relative_to(resources).as_posix()
                if entry not in entries or archive.read(entry) != source.read_bytes():
                    raise ValueError('Recurso do executor ausente ou divergente: '+str(source))
            for path in REMOVED:
                if path.startswith(module+'/') and 'BOOT-INF/classes/'+path.split('/src/main/resources/')[1] in entries:
                    raise ValueError('Texto substituído permaneceu no worker: '+path)
        print('Executor íntegro:', module)
    check.smoke(ROOT, jar)


if __name__ == '__main__':
    main()
