#!/usr/bin/env python3
"""Confere pacotes em imagens locais sem iniciar serviços ou conectar integrações."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile


def digest(path):
    """Calcula a identidade do arquivo efetivamente testado e empacotado."""
    return hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()


def main():
    """Extrai os JARs de containers parados e remove somente os recursos desta conferência."""
    project = os.environ['PROCESS_COMPOSE_PROJECT']
    if not project.startswith('aihub-'):
        raise ValueError('Informe o projeto exclusivo da sandbox.')
    output, tag = Path(sys.argv[1]), sys.argv[2]
    checked = []
    for module in ['backend', 'communication-agent-worker', 'meta-ad-approver-worker']:
        image = f'{project}/mira-{module}:{tag}'
        container = subprocess.check_output(['docker', 'create', '--label',
            f'com.docker.compose.project={project}', image], text=True).strip()
        jar = output / f'{module}-image.jar'
        try:
            subprocess.run(['docker', 'cp', f'{container}:/app/app.jar', str(jar)], check=True)
            if module == 'backend':
                if digest(jar) != digest(Path('backend/ads-service/target/app-exec.jar')):
                    raise ValueError('A imagem não contém o JAR validado do backend.')
            else:
                with zipfile.ZipFile(jar) as archive:
                    resources = Path(module) / 'src/main/resources'
                    for source in resources.rglob('*'):
                        if source.is_file() and source.suffix in {'.md', '.json', '.mjs'}:
                            if archive.read('BOOT-INF/classes/' + str(source.relative_to(resources))) != source.read_bytes():
                                raise ValueError('Recurso da imagem diverge: ' + str(source))
            image_id = subprocess.check_output(['docker', 'image', 'inspect', '--format', '{{.Id}}', image], text=True).strip()
            checked.append({'module': module, 'image':image, 'imageId':image_id, 'jarSha256':digest(jar), 'status':'PASS'})
        finally:
            subprocess.run(['docker', 'rm', '-v', container], check=True, stdout=subprocess.DEVNULL)
            jar.unlink(missing_ok=True)
    (output / 'images.json').write_text(json.dumps(checked, indent=2) + '\n')
    print(json.dumps(checked))


if __name__ == '__main__':
    main()
