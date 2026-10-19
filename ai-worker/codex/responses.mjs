import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { mkdir, readFile, writeFile, rename, appendFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { join, resolve } from 'node:path';
import Ajv from 'ajv';
import Ajv2020 from 'ajv/dist/2020.js';

const COMPATIBILITY_REASON = 'Codex OAuth usa default; Flex não é suportado neste transporte. Sem fallback para API paga.';
let activeClose = () => {};

// Traduz somente entradas suportadas; nunca descarta silenciosamente parte do contrato.
export function translate(body) {
  if (!body.model || !body.input) throw new Error('Modelo e input são obrigatórios');
  if (body.background || body.stream || body.previous_response_id || body.conversation)
    throw new Error('Codex requer turno completo, isolado e sem estado anterior');
  if (body.tools?.length) throw new Error('Ferramentas Responses não são executáveis neste transporte Codex');
  const supported = new Set(['model', 'input', 'instructions', 'metadata', 'text', 'service_tier', 'reasoning', 'store', 'tools', 'stream', 'background', 'previous_response_id', 'conversation']);
  for (const key of Object.keys(body)) if (!supported.has(key)) throw new Error(`Parâmetro Responses sem equivalente Codex: ${key}`);
  const input = [];
  const developer = body.instructions ? [body.instructions] : [];
  const messages = typeof body.input === 'string' ? [{ role: 'user', content: body.input }] : body.input;
  for (const message of messages) {
    if (!['user', 'system', 'developer', 'assistant'].includes(message.role))
      throw new Error('Tipo de mensagem não suportado pelo transporte Codex');
    const privileged = ['system', 'developer'].includes(message.role);
    if (!privileged) input.push({ type: 'text', text: `[${message.role}]` });
    const contents = typeof message.content === 'string' ? [{ type: 'input_text', text: message.content }] : message.content;
    for (const part of contents || []) {
      if (part.type === 'input_text' || part.type === 'output_text') {
        if (privileged) developer.push(part.text); else input.push({ type: 'text', text: part.text });
      }
      else if (!privileged && part.type === 'input_image' && part.image_url) input.push({ type: 'image', url: part.image_url });
      else throw new Error(`Entrada não suportada: ${part.type}`);
    }
  }
  const format = body.text?.format;
  if (format && !['json_schema', 'text', 'json_object'].includes(format.type)) throw new Error('Formato não suportado');
  return { input, developerInstructions: developer.join('\n\n'), schema: format?.schema, json: format?.type?.startsWith('json_') };
}

// Mantém os segredos de APIs fora do processo Codex e usa somente a sessão gerenciada.
export function codexEnvironment(env) {
  return Object.fromEntries(Object.entries(env).filter(([key]) =>
    /^(PATH|HOME|LANG|LC_ALL|TMPDIR|CODEX_HOME|SSL_CERT_FILE|SSL_CERT_DIR|HTTPS_PROXY|HTTP_PROXY|NO_PROXY)$/.test(key)));
}

// Ordena chaves para manter a identidade do recibo após reiniciar a JVM ou serializar metadados.
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  return value;
}

// Compila o schema antes da inferência e na recuperação, sem resolver referências pela rede.
function schemaValidator(schema) {
  const Validator = schema.$schema?.includes('2020-12') ? Ajv2020 : Ajv;
  return new Validator({ strict: false, allErrors: true, validateFormats: false }).compile(schema);
}

// Abre JSON-RPC por stdio sob o mesmo lock usado pela reconexão de sessão.
async function connect(options) {
  const home = resolve(options.home);
  await mkdir(home, { recursive: true, mode: 0o700 });
  const command = options.command || 'codex';
  const args = options.noLock ? [] : ['--no-fork', '-w', '60', join(home, '.oauth-session.lock'), command];
  const child = spawn(options.noLock ? command : 'flock', [...args, 'app-server', '--listen', 'stdio://'], {
    cwd: options.workspace,
    env: { ...codexEnvironment(process.env), CODEX_HOME: home },
    stdio: ['pipe', 'pipe', 'pipe'],
  });
  let sequence = 0;
  const pending = new Map();
  const events = [];
  const listeners = new Set();
  const fail = (error) => { for (const p of pending.values()) p.reject(error); pending.clear(); for (const fn of listeners) fn(null, error); };
  child.on('error', fail);
  child.on('exit', code => fail(new Error(`Codex App Server encerrou (${code})`)));
  // stderr pode conter dados da conta; a saída pública contém somente erros estruturados.
  child.stderr.resume();
  createInterface({ input: child.stdout }).on('line', line => {
    let message;
    try { message = JSON.parse(line); } catch { fail(new Error('JSON-RPC inválido no App Server')); return; }
    if (message.id !== undefined && !message.method) {
      const p = pending.get(message.id);
      if (p) {
        pending.delete(message.id);
        if (['thread/start', 'turn/start'].includes(p.method)) events.push({ method: `rpc/${p.method}`, params: { response: message } });
        message.error ? p.reject(new Error(message.error.message)) : p.resolve(message.result);
      }
    } else if (message.method && message.id !== undefined) {
      // O worker não concede autorização de shell, arquivos, rede ou intervenção humana.
      child.stdin.write(`${JSON.stringify({ id: message.id, error: { code: -32601, message: 'Ferramenta não autorizada pelo Worker IA' } })}\n`);
      fail(new Error(`App Server solicitou ferramenta não autorizada: ${message.method}`));
    } else if (message.method) {
      events.push(message);
      for (const fn of listeners) fn(message);
    }
  });
  const request = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++sequence;
    pending.set(id, { resolve, reject, method });
    child.stdin.write(`${JSON.stringify({ id, method, params })}\n`);
  });
  const send = (method, params = {}) => child.stdin.write(`${JSON.stringify({ method, params })}\n`);
  const close = () => { child.stdin.end(); child.kill('SIGTERM'); };
  activeClose = close;
  const terminate = () => { close(); process.exitCode = 2; };
  process.once('SIGTERM', terminate);
  child.once('exit', () => { process.removeListener('SIGTERM', terminate); activeClose = () => {}; });
  try {
    await request('initialize', { clientInfo: { name: 'marketing_hub_ai_worker', title: 'Marketing Hub Worker IA', version: '1.0.0' } });
    send('initialized');
    const account = await request('account/read', { refreshToken: false });
    if (options.requireAuth !== false && account.account?.type !== 'chatgpt') throw new Error('CODEX_AUTH_REQUIRED: conecte uma sessão ChatGPT própria do Worker IA');
    return { request, send, close, events, listeners };
  } catch (error) { close(); throw error; }
}

// Executa uma inferência isolada e preserva seu recibo para replay sem novo consumo.
export async function execute(body, options) {
  const translated = translate(body);
  if (translated.schema) schemaValidator(translated.schema);
  const fingerprint = createHash('sha256').update(JSON.stringify(canonical(body))).digest('hex');
  const scoped = body.metadata?.marketingHubJobId || body.metadata?.idJob;
  const receipt = options.cacheDirectory && scoped ? join(options.cacheDirectory, `${fingerprint}.json`) : null;
  if (receipt) {
    try { return JSON.parse(await readFile(receipt, 'utf8')); } catch (error) { if (error.code !== 'ENOENT') throw error; }
    await mkdir(options.cacheDirectory, { recursive: true, mode: 0o700 });
  }
  const server = await connect(options);
  let threadParams, turnParams, threadId, turnId, usage;
  let inferenceStarted = false;
  try {
    const models = await server.request('model/list', { includeHidden: true });
    if (!models.data?.some(model => model.model === body.model || model.id === body.model))
      throw new Error(`Modelo ${body.model} indisponível nesta sessão Codex; configure um modelo autorizado, sem substituição automática`);
    threadParams = {
      model: body.model, cwd: options.workspace, approvalPolicy: 'never', sandbox: 'read-only', ephemeral: true,
      config: { service_tier: 'default', web_search: 'disabled', features: { shell_tool: false, apply_patch_tool: false }, mcp_servers: {} },
    };
    if (translated.developerInstructions) threadParams.developerInstructions = translated.developerInstructions;
    const thread = await server.request('thread/start', threadParams);
    threadId = thread.thread.id;
    turnParams = { threadId, input: translated.input };
    if (translated.schema) turnParams.outputSchema = translated.schema;
    if (body.reasoning?.effort) turnParams.effort = body.reasoning.effort;
    const messages = new Map();
    const completed = new Promise((resolve, reject) => {
      server.listeners.add((event, error) => {
        if (error) { reject(error); return; }
        if (event.params?.threadId !== threadId) return;
        if (event.method === 'item/completed' && event.params.item?.type === 'agentMessage')
          messages.set(event.params.item.id, event.params.item);
        if (event.method === 'thread/tokenUsage/updated') usage = event.params.tokenUsage?.total;
        if (event.method === 'turn/completed') resolve(event.params.turn);
      });
    });
    // Evita rejeição não observada quando o servidor encerra durante turn/start.
    completed.catch(() => {});
    // Só registra despacho depois da prontidão. Uma falha de autenticação não consumiu inferência.
    if (receipt) await writeFile(`${receipt}.started`, JSON.stringify({ fingerprint, threadId, at: new Date().toISOString() }), { flag: 'wx', mode: 0o600 });
    inferenceStarted = true;
    const started = await server.request('turn/start', turnParams);
    turnId = started.turn.id;
    const turn = await completed;
    if (turn.id !== started.turn.id || turn.status !== 'completed')
      throw new Error(`Turno Codex não concluído: ${turn.status}`);
    const finals = [...messages.values()].filter(item => !item.phase || item.phase === 'final_answer');
    const text = finals.at(-1)?.text;
    if (!text?.trim()) throw new Error('Codex concluiu sem resposta final');
    const response = {
      id: `codex:${threadId}:${turn.id}`, status: 'completed', model: body.model, service_tier: 'default', output_text: text,
      output: [{ type: 'message', role: 'assistant', content: [{ type: 'output_text', text }] }],
      usage: usage ? { input_tokens: usage.inputTokens, output_tokens: usage.outputTokens, total_tokens: usage.totalTokens } : null,
      codex_app_server: { version: 1, threadId, turnId: turn.id, metadata: body.metadata || {}, request: { thread: threadParams, turn: turnParams }, requestedServiceTier: body.service_tier || 'flex', effectiveServiceTier: 'default', compatibilityReason: COMPATIBILITY_REASON, events: structuredClone(server.events) },
    };
    // Salva a saída crua antes da validação: resposta inválida também é recuperável e auditável.
    if (receipt) {
      await writeFile(`${receipt}.tmp`, JSON.stringify(response), { mode: 0o600 });
      await rename(`${receipt}.tmp`, receipt);
    }
    return response;
  } catch (error) {
    const failure = {
      status: 'failed', model: body.model, at: new Date().toISOString(), inferenceStarted,
      error: { message: error.message }, usage: usage || null,
      codex_app_server: { version: 1, threadId, turnId, metadata: body.metadata || {}, request: { thread: threadParams, turn: turnParams }, events: structuredClone(server.events) },
    };
    error.receipt = failure;
    if (receipt) {
      try { await appendFile(`${receipt}.failures.jsonl`, `${JSON.stringify(failure)}\n`, { mode: 0o600 }); }
      catch (persistenceError) { failure.auditPersistenceError = { code: persistenceError.code, message: persistenceError.message }; }
    }
    throw error;
  } finally { server.close(); }
}

// Valida localmente o contrato original, inclusive em respostas recuperadas do recibo.
export function validateResponse(body, response) {
  const { schema, json } = translate(body);
  if (json) {
    const parsed = JSON.parse(response.output_text);
    if (schema) {
      const validate = schemaValidator(schema);
      if (!validate(parsed)) throw new Error(`Resposta Codex viola schema: ${validate.errors.length} inconsistência(s); recibo completo preservado para diagnóstico`);
    }
  }
  return response;
}

// Prova autenticação sem iniciar inferência, revelar conta ou consumir orçamento do produto.
export async function health(options) {
  const server = await connect(options);
  try { return { transport: 'codex-app-server', authenticated: true }; } finally { server.close(); }
}

// Conecta uma sessão própria pelo fluxo oficial de device code, preservando a sessão anterior.
export async function login(options, showCode) {
  const server = await connect({ ...options, requireAuth: false });
  try {
    let loginId;
    const confirmed = new Promise((resolve, reject) => {
      server.listeners.add((event, error) => {
        if (error) { reject(error); return; }
        if (event.method === 'account/login/completed' && event.params?.loginId === loginId) {
          event.params.success ? resolve() : reject(new Error('A conexão Codex não foi confirmada'));
        }
      });
    });
    confirmed.catch(() => {});
    const started = await server.request('account/login/start', { type: 'chatgptDeviceCode' });
    loginId = started.loginId;
    if (!loginId || !started.verificationUrl || !started.userCode) throw new Error('App Server não forneceu device code válido');
    showCode({ verificationUrl: started.verificationUrl, userCode: started.userCode });
    await confirmed;
    const account = await server.request('account/read', { refreshToken: false });
    if (account.account?.type !== 'chatgpt') throw new Error('A conta conectada não é uma sessão ChatGPT');
    return { transport: 'codex-app-server', authenticated: true };
  } finally { server.close(); }
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(new URL(import.meta.url).pathname)) {
  const timeout = setTimeout(() => {
    process.stderr.write('CODEX_TIMEOUT: recibo incerto preservado; não repetir automaticamente\n');
    activeClose();
    process.exitCode = 2;
    setTimeout(() => process.exit(2), 1000).unref();
  }, Number(process.env.AI_WORKER_CODEX_TIMEOUT_MS || 900000));
  try {
    const options = { home: process.env.CODEX_HOME, workspace: process.env.AI_WORKER_CODEX_WORKSPACE || '/tmp/ai-worker-codex', cacheDirectory: process.env.AI_WORKER_CODEX_RECEIPTS || '/var/lib/ai-worker/codex-receipts' };
    await mkdir(options.workspace, { recursive: true, mode: 0o700 });
    if (process.argv.includes('--health')) process.stdout.write(JSON.stringify(await health(options)));
    else if (process.argv.includes('--login')) process.stdout.write(`${JSON.stringify(await login(options, code => process.stdout.write(`${JSON.stringify(code)}\n`)))}\n`);
    else {
      let raw = '';
      for await (const chunk of process.stdin) { raw += chunk; if (raw.length > 32 * 1024 * 1024) throw new Error('Request Codex excede limite de 32 MiB'); }
      const body = JSON.parse(raw);
      const response = await execute(body, options);
      // O Java recebe o recibo integral e aplica a validação antes de entregar ao consumidor.
      process.stdout.write(JSON.stringify(response));
      validateResponse(body, response);
    }
  } catch (error) {
    if (error.receipt) process.stdout.write(JSON.stringify(error.receipt));
    process.stderr.write(`${String(error.message).replace(/(?:sk-[\w-]+|gh[pousr]_[\w]+|github_pat_[\w]+)/g, '[REDACTED]')}\n`);
    process.exitCode = 1;
  } finally { clearTimeout(timeout); }
}
