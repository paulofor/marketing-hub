import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const script = fileURLToPath(new URL('../../main/resources/browser/musa-continuous-harness.mjs', import.meta.url));

// Comprova a recusa anterior ao navegador/HTTP, sem emitir acesso ou usar credencial real.
for (const field of ['productId', 'cycleId', 'sourceReference', 'prototypeVersion', 'sourceUrl', 'mode', 'scenarioCode', 'captureSessionId']) {
  test(`checkpoint de outro ${field} não reutiliza estado nem credencial`, async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), 'musa-scope-'));
    try {
      const input = { productId: 97004, cycleId: 97022, sourceReference: 'experiment:97115',
        prototypeVersion: 'musa-consultor-pwa-v1', sourceUrl: 'http://127.0.0.1:9/isolated',
        mode: 'TECHNICAL', scenarioCode: '', captureSessionId: '00000000-0000-4000-9000-000000097022' };
      const scope = { ...input, [field]: typeof input[field] === 'number' ? 1 : 'outro' };
      const evidence = path.join(directory, 'evidence');
      await mkdir(evidence);
      await writeFile(path.join(directory, 'input.json'), JSON.stringify(input));
      await writeFile(path.join(evidence, 'access-private.json'), JSON.stringify({ scope, slots: {} }));
      const child = spawn(process.execPath, [script,
        path.join(directory, 'input.json'), path.join(directory, 'output.json'), evidence],
        { env: { ...process.env, PDE_INTERNAL_API_TOKEN: 'synthetic-only' } });
      let log = '';
      child.stdout.on('data', chunk => log += chunk);
      child.stderr.on('data', chunk => log += chunk);
      const status = await new Promise(resolve => child.on('exit', resolve));
      assert.notEqual(status, 0);
      assert.match(log, /Checkpoint privado de outro contexto recusado/);
      assert.doesNotMatch(log, /ECONNREFUSED|browserType.launch/);
    } finally { await rm(directory, { recursive: true, force: true }); }
  });
}
