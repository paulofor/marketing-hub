#!/usr/bin/env python3
"""Confere os pacotes e reproduz H.264/AAC na imagem real, somente na engine da sandbox."""
import base64
import hashlib
import io
import json
import pathlib
import subprocess
import zipfile

root = pathlib.Path('.codex/vega-approved')
manifest = json.loads((root / 'source-manifest.json').read_text())
digest = manifest['sourceDigest']
names = ['backend', 'frontend', 'customer-agent-worker', 'meta-ad-approver-worker', 'pde-platform-frontend-vega']
services = {name: {'image': f'marketing-hub/{name}:vega-approved-{digest[:12]}', 'mem_limit': '1g', 'shm_size': '256m', 'read_only': True, 'init': True, 'security_opt': ['no-new-privileges:true'], 'tmpfs': ['/tmp:size=256m,noexec,nosuid,mode=1777']} for name in names}
compose = root / 'verified-images.compose.json'
compose.write_text(json.dumps({'services': services}))
command = ['docker', 'compose', '-p', 'aihub-36e8935e-c4cc-43ea-8bd1-8b9d6b1922ec-6ff58094f8', '-f', str(compose)]


def run(name, entrypoint, *args, data=None):
    """Executa um container temporário sem mounts, rede de host ou dados produtivos."""
    return subprocess.run(command + ['run', '--rm', '-T', '--pull', 'never', '--entrypoint', entrypoint, name, *args], input=data, capture_output=True, check=True).stdout


for name in names:
    image = services[name]['image']
    actual = json.loads(subprocess.check_output(['docker', 'image', 'inspect', image]))[0]
    assert actual['Config']['Labels']['com.marketinghub.source-digest'] == digest, name

backend = run('backend', 'cat', '/app/app.jar')
assert hashlib.sha256(backend).digest() == hashlib.sha256(pathlib.Path('backend/ads-service/target/app-exec.jar').read_bytes()).digest()
print('PASS imagem backend corresponde ao JAR verificado')
for name, resource in [
    ('customer-agent-worker', 'prompts/bpm/v5/pde-agent-validation-scenario-review.md'),
    ('meta-ad-approver-worker', 'prompts/bpm/pde-agent-validation-review-v4.md'),
]:
    archive = zipfile.ZipFile(io.BytesIO(run(name, 'cat', '/app/app.jar')))
    assert archive.read('BOOT-INF/classes/' + resource) == pathlib.Path(name, 'src/main/resources', resource).read_bytes()
    print('PASS prompt empacotado', name)
for resource in ['vega-agent-validation-harness.mjs', 'learning-cycle-video-checks.mjs']:
    assert run('customer-agent-worker', 'cat', '/app/browser/' + resource) == pathlib.Path('customer-agent-worker/src/main/resources/browser', resource).read_bytes()
for name, local, remote in [
    ('frontend', 'frontend/dist/index.html', '/usr/share/nginx/html/index.html'),
    ('pde-platform-frontend-vega', 'pde-platform/frontend/dist-vega/index.html', '/usr/share/nginx/html/vega-private/index.html'),
]:
    assert run(name, 'cat', remote) == pathlib.Path(local).read_bytes(), name
    for asset in pathlib.Path(local).parent.joinpath('assets').glob('*'):
        if asset.suffix in ['.js', '.css']:
            assert run(name, 'cat', str(pathlib.PurePosixPath(remote).parent / 'assets' / asset.name)) == asset.read_bytes(), asset.name
    print('PASS bundle empacotado', name)
video = base64.b64encode((root / 'media/ad.mp4').read_bytes()).decode()
script = """
import { chromium } from '/app/node_modules/playwright-core/index.mjs';
import { verifyPlayback, videoBrowserOptions } from '/app/browser/learning-cycle-video-checks.mjs';
const browser = await chromium.launch(videoBrowserOptions());
try {
  const page = await browser.newPage();
  await page.setContent('<video controls src="data:video/mp4;base64,' + VIDEO + '"></video>');
  const result = await verifyPlayback(page.locator('video'), 3);
  console.log(JSON.stringify({browser: browser.version(), playback: result}));
} finally { await browser.close(); }
""".replace('VIDEO', json.dumps(video))
print(run('customer-agent-worker', 'sh', '-c', 'cat > /tmp/codec-check.mjs; node /tmp/codec-check.mjs', data=script.encode()).decode())
print('PASS cinco imagens e reprodução real com áudio')
