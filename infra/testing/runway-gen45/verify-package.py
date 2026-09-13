"""Confere presença e bytes das classes testadas e do prompt dentro da imagem candidata."""
import hashlib
import json
import pathlib
import sys
import zipfile

jar = pathlib.Path(sys.argv[1])
classes = pathlib.Path('video-management-service/target/classes')
with zipfile.ZipFile(jar) as archive:
    local = {file.relative_to(classes).as_posix(): file for file in classes.rglob('*.class')}
    packaged = {name.removeprefix('BOOT-INF/classes/') for name in archive.namelist()
                if name.startswith('BOOT-INF/classes/') and name.endswith('.class')}
    assert packaged == set(local), 'Presença de classes diverge da revisão testada'
    for name, file in local.items():
        assert archive.read('BOOT-INF/classes/' + name) == file.read_bytes(), name
    prompt = 'prompts/sales-video/runway-router-v1.md'
    assert archive.read('BOOT-INF/classes/' + prompt) == pathlib.Path(
        'video-management-service/src/main/resources', prompt).read_bytes()
receipt = {'classesMatched': len(local), 'promptMatched': True,
           'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
jar.with_suffix('.verification.json').write_text(json.dumps(receipt, indent=2))
print(json.dumps(receipt))
