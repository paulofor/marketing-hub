#!/usr/bin/env python3
"""Confere o conteúdo das imagens candidatas contra as classes e recursos testados localmente."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile


def digest(path):
    """Calcula SHA-256 sem carregar o JAR inteiro em memória."""
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    """Inspeciona containers parados e remove os arquivos temporários após cada conferência."""
    project = os.environ['PROCESS_COMPOSE_PROJECT']
    if not project.startswith('aihub-'):
        raise ValueError('Projeto exclusivo da sandbox obrigatório.')
    output, revision = Path(sys.argv[1]), sys.argv[2]
    output.mkdir(parents=True, exist_ok=True)
    results = []
    for module in ('backend', 'communication-agent-worker'):
        image = f'marketing-hub/{module}:mira-creative-review-v1-{revision[:12]}'
        metadata = json.loads(subprocess.check_output(['docker', 'image', 'inspect', image]))[0]
        assert metadata['Config']['Labels']['org.opencontainers.image.revision'] == revision
        container = subprocess.check_output(['docker', 'create', '--label',
            f'com.docker.compose.project={project}', image], text=True).strip()
        jar = output / f'{module}-image.jar'
        try:
            subprocess.run(['docker', 'cp', f'{container}:/app/app.jar', str(jar)], check=True)
            if module == 'backend':
                assert digest(jar) == digest(Path('backend/ads-service/target/app-exec.jar'))
            else:
                with zipfile.ZipFile(jar) as archive:
                    classes = Path(module) / 'target/classes'
                    for source in classes.rglob('*'):
                        if source.is_file() and source.suffix in {'.class', '.md', '.json', '.mjs'}:
                            packaged = archive.read('BOOT-INF/classes/' + source.relative_to(classes).as_posix())
                            assert packaged == source.read_bytes(), 'Divergência na imagem: ' + str(source)
            results.append({'module': module, 'image': image, 'imageId': metadata['Id'],
                'revision': revision, 'jarSha256': digest(jar), 'status': 'PASS'})
        finally:
            subprocess.run(['docker', 'rm', '-v', container], check=True, stdout=subprocess.DEVNULL)
            jar.unlink(missing_ok=True)
    (output / 'images.json').write_text(json.dumps(results, indent=2) + '\n')
    print(json.dumps(results))


if __name__ == '__main__':
    main()
