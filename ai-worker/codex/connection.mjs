import { mkdir, readFile, readdir, rename, unlink, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { health, login } from './responses.mjs';

const ROOT = '/api/ai/worker-connection/internal';

// Limita o tempo de rede e repete apenas callbacks idempotentes, nunca a autenticação.
async function callback(base, path, body) {
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const response = await fetch(`${base}${ROOT}${path}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body), signal: AbortSignal.timeout(10000),
      });
      if (response.ok || response.status === 409) return response.status;
      if (response.status < 500) throw new Error('CALLBACK_REJECTED');
    } catch (error) {
      if (error.message === 'CALLBACK_REJECTED') throw error;
    }
    if (attempt < 2) await new Promise(resolve => setTimeout(resolve, 250 * (attempt + 1)));
  }
  throw new Error('CALLBACK_UNAVAILABLE');
}

// Reenvia a confirmação já preservada, inclusive após reinício do processo Java.
export async function replay(base, options) {
  const directory = join(options.cacheDirectory, 'connection');
  await mkdir(directory, { recursive: true, mode: 0o700 });
  for (const name of await readdir(directory)) {
    if (!/^\d+\.pending\.json$/.test(name)) continue;
    const file = join(directory, name);
    const receipt = JSON.parse(await readFile(file, 'utf8'));
    const status = await callback(base, `/${receipt.id}/completion`, receipt.body);
    if (status === 409) await rename(file, file.replace('.pending.json', '.expired.json'));
    else await unlink(file);
  }
}

// Executa somente o comando reservado pelo backend, usando a sessão individual já existente.
export async function poll(base, options) {
  await replay(base, options);
  const response = await fetch(`${base}${ROOT}/pending`, { signal: AbortSignal.timeout(10000) });
  if (response.status === 204) return;
  if (!response.ok) throw new Error('PENDING_UNAVAILABLE');
  const job = await response.json();
  if (!Number.isSafeInteger(job.id) || job.id <= 1) throw new Error('INVALID_CONNECTION_ID');
  let authenticated = false;
  try {
    const result = await login(options, async code => {
      if (code.verificationUrl !== 'https://auth.openai.com/codex/device'
          || !/^[A-Z0-9][A-Z0-9-]{3,25}$/.test(code.userCode)) throw new Error('INVALID_DEVICE_CODE');
      const status = await callback(base, `/${job.id}/device-code`, code);
      if (status === 409) throw new Error('CONNECTION_EXPIRED');
    });
    authenticated = result.authenticated === true;
  } catch {
    // O provedor pode emitir dados da conta no erro; somente o estado seguro segue ao painel.
    process.stderr.write(`Worker IA: conexão ${job.id} não confirmada; sessão anterior preservada.\n`);
  }
  const file = join(options.cacheDirectory, 'connection', `${job.id}.pending.json`);
  const receipt = { id: job.id, body: { authenticated } };
  await writeFile(`${file}.tmp`, JSON.stringify(receipt), { mode: 0o600 });
  await rename(`${file}.tmp`, file);
  await replay(base, options);
}

// Informa disponibilidade sem iniciar turno, consultar conteúdo ou revelar a identidade da conta.
export async function reportSession(base, options) {
  let authenticated = false;
  try { authenticated = (await health(options)).authenticated === true; }
  catch { /* Ausência de sessão é um estado esperado e não transporta o erro bruto. */ }
  await callback(base, '/session', { authenticated });
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(new URL(import.meta.url).pathname)) {
  const options = {
    home: process.env.CODEX_HOME,
    workspace: process.env.AI_WORKER_CODEX_WORKSPACE || '/tmp/ai-worker-codex',
    cacheDirectory: process.env.AI_WORKER_CODEX_RECEIPTS || '/var/lib/ai-worker/codex-receipts',
  };
  const base = process.env.BACKEND_BASE_URL || 'http://191.252.181.168';
  try {
    await mkdir(options.workspace, { recursive: true, mode: 0o700 });
    if (process.argv.includes('--health')) await reportSession(base, options);
    else await poll(base, options);
  } catch {
    process.stderr.write('Worker IA: conexão sem confirmação do backend; recibo preservado para recuperação.\n');
    process.exitCode = 1;
  }
}
