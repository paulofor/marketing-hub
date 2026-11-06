#!/usr/bin/env node
import { readFile, writeFile, mkdir, rename } from 'node:fs/promises';
import { join, dirname } from 'node:path';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

export const protocol = 'SINGLE_VIDEO_BOUNDED_V1';
export const header = 'X-Single-Video-Spending-Contract';

// O mesmo request contado e reservado é enviado uma única vez; não há SDK com retry oculto.
export async function runBounded({ args, env = process.env, fetcher = fetch, input }) {
  const agent = env.MH_AGENT_KEY;
  const taskId = env.MH_AGENT_TASK_ID;
  if (!agent || !/^[1-9][0-9]*$/.test(taskId ?? '')) throw new Error('Identidade de tarefa incompleta.');
  const backend = new URL(env.MH_SPENDING_BACKEND_URL);
  const path = `/api/internal/agent-tasks/${encodeURIComponent(agent)}/stage-executions/${taskId}`;
  const call = async (method, suffix, body) => {
    const response = await fetcher(new URL(path + suffix, backend), {
      method, redirect: 'error', signal: AbortSignal.timeout(120000),
      headers: { 'Content-Type': 'application/json', [header]: protocol },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    if (!response.ok) throw new Error(`Autoridade de gasto indisponível: HTTP ${response.status}.`);
    return response.json();
  };
  const context = await call('GET', '/spending-context');
  if (typeof context.required !== 'boolean') throw new Error('Autoridade de gasto sem resposta válida.');
  if (!context.required) {
    if (env.MH_EXPECT_SHARED_SPENDING === 'true') throw new Error('Tarefa protegida sem autoridade financeira; fallback recusado.');
    return { legacy: true };
  }
  if (context.blocker && !context.reservation) throw new Error(context.blocker);
  const value = flag => {
    const index = args.indexOf(flag);
    if (index < 0 || !args[index + 1]) throw new Error(`Contrato limitado sem ${flag}.`);
    return args[index + 1];
  };
  const model = value('--model');
  const schema = JSON.parse(await readFile(value('--output-schema'), 'utf8'));
  const output = value('--output-last-message');
  const effort = args.find(x => /^model_reasoning_effort=/.test(x))?.split('=')[1]?.replaceAll('"', '');
  if (!effort) throw new Error('Esforço do modelo ausente; substituição de configuração recusada.');
  const prompt = await input();
  const content = [{ type: 'input_text', text: prompt }];
  for (let i = 0; i < args.length; i++) {
    if (args[i] !== '--image') continue;
    const file = args[++i];
    const bytes = await readFile(file);
    const type = file.toLowerCase().endsWith('.png') ? 'image/png' :
      /\.jpe?g$/i.test(file) ? 'image/jpeg' : /\.webp$/i.test(file) ? 'image/webp' : null;
    if (!type) throw new Error('Evidência visual sem tipo permitido.');
    content.push({ type: 'input_image', image_url: `data:${type};base64,${bytes.toString('base64')}` });
  }
  let request = { model, service_tier: 'flex', store: false, max_output_tokens: 32768,
    reasoning: { effort }, input: [{ role: 'user', content }],
    text: { format: { type: 'json_schema', name: 'agent_result', strict: true, schema } } };
  // Têmis não recebe a chave do produtor de imagem. Seu acesso deve ser exclusivo de revisão textual.
  const independentReview = agent === 'meta-ad-approver';
  const nativeKeyFile = independentReview ? env.MH_BOUNDED_REVIEW_KEY_FILE : env.MH_BOUNDED_OPENAI_API_KEY_FILE;
  let key;
  if (nativeKeyFile) {
    try { key = (await readFile(nativeKeyFile, 'utf8')).trim(); }
    catch (error) { if (error.code !== 'ENOENT' || !context.reservation) throw new Error('Credencial limitada não está disponível no volume privado.'); }
  }
  if (!key && !independentReview) key = env.OPENAI_API_KEY?.trim();
  if (!key && !independentReview && env.OPENAI_API_KEY_FILE) key = (await readFile(env.OPENAI_API_KEY_FILE, 'utf8')).trim();
  if (!key && !context.reservation) throw new Error('Credencial de API do agente ausente; nenhuma inferência iniciada.');
  const provider = new URL(env.MH_BOUNDED_OPENAI_URL ?? 'https://api.openai.com/v1/');
  if (provider.href !== 'https://api.openai.com/v1/' && !['127.0.0.1', 'localhost', '[::1]'].includes(provider.hostname))
    throw new Error('Endpoint limitado deve ser OpenAI global ou double local.');
  const send = async (suffix, body) => fetcher(new URL(suffix, provider), {
    method: 'POST', redirect: 'error', signal: AbortSignal.timeout(600000),
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  });
  // Reserva já existente conserva a entrada original mesmo quando o envio ainda não começou.
  if (context.reservation?.request) request = context.reservation.request;
  const counted = { ...request };
  delete counted.store; delete counted.service_tier; delete counted.max_output_tokens;
  let reserved = context.reservation;
  if (!reserved || (reserved.state === 'RESERVED' && Date.parse(reserved.quoteExpiresAt) <= Date.now())) {
    const countResponse = await send('responses/input_tokens', counted);
    if (!countResponse.ok) throw new Error(`Contagem recusada: HTTP ${countResponse.status}; nenhuma inferência iniciada.`);
    const countedInput = await countResponse.json();
    reserved = await call('POST', '/spend-reservations', { request, inputTokenCount: countedInput });
  }
  // Replay usa a entrada original aceita, não timestamps e capturas recompostas na retomada.
  if (context.reservation && reserved.state !== 'RESERVED') request = reserved.request;
  const reservationPath = `/spend-reservations/${reserved.id}`;
  const state = env.MH_BOUNDED_STATE_DIRECTORY ?? join(env.CODEX_HOME ?? '', 'marketing-hub-bounded');
  if (!env.MH_BOUNDED_STATE_DIRECTORY && !env.CODEX_HOME) throw new Error('Volume persistente de resultados não configurado.');
  await mkdir(state, { recursive: true, mode: 0o700 });
  const saved = join(state, `${context.receiptId}-${reserved.id}.json`);
  const requestHash = createHash('sha256').update(JSON.stringify(reserved.request)).digest('hex');
  // O hash do backend é preservado como identidade; a comparação estrutural evita diferenças de serialização.
  if (JSON.stringify(reserved.request) !== JSON.stringify(request)) throw new Error('Request persistido diverge da reserva.');
  let receipt, pendingReceipt = false;
  try { receipt = JSON.parse(await readFile(saved, 'utf8')); }
  catch (error) {
    if (error.code !== 'ENOENT') throw error;
    try { receipt = JSON.parse(await readFile(saved + '.pending', 'utf8')); pendingReceipt = true; }
    catch (pendingError) { if (pendingError.code !== 'ENOENT') throw pendingError; }
  }
  let raw = reserved.rawResponse;
  if (receipt) {
    if (receipt.receiptId !== context.receiptId || receipt.requestHash !== requestHash || receipt.taskId !== taskId)
      throw new Error('Resultado local pertence a outro contexto.');
    if (pendingReceipt) await rename(saved + '.pending', saved);
    await call('POST', `${reservationPath}/result`, receipt.result);
    raw = receipt.result.rawResponse;
  }
  if (!raw) {
    if (!key) throw new Error('Credencial ausente; nenhum envio novo realizado, reserva preservada.');
    const lease = await call('POST', `${reservationPath}/begin`, {});
    raw = lease.rawResponse;
    if (!raw && !lease.mayInvoke) throw new Error('Envio anterior incerto; reserva preservada e nova inferência recusada.');
    if (!raw) {
      if (JSON.stringify(lease.request) !== JSON.stringify(request)) throw new Error('Request alterado após a reserva.');
      // SUBMITTED já está persistido antes da rede; qualquer erro mantém a reserva integral.
      if (!key) throw new Error('Credencial ausente; envio não realizado, reserva preservada.');
      const response = await send('responses', lease.request);
      raw = (await response.text()).replaceAll(key, '[CREDENCIAL_REMOVIDA]');
      const result = { httpStatus: response.status, rawResponse: raw, providerRequestId: response.headers.get('x-request-id') };
      const pending = { receiptId: context.receiptId, taskId, requestHash, result };
      await writeFile(saved + '.pending', JSON.stringify(pending), { mode: 0o600 });
      await rename(saved + '.pending', saved);
      await call('POST', `${reservationPath}/result`, result);
      if (!response.ok) throw new Error(`Inferência recusada: HTTP ${response.status}; resultado e reserva preservados.`);
    }
  }
  const response = JSON.parse(raw);
  if (response.status !== 'completed' || !response.usage || !Number.isSafeInteger(response.usage.input_tokens)
      || !Number.isSafeInteger(response.usage.output_tokens)) throw new Error('Resposta incompleta; sem retry automático.');
  const text = (response.output ?? []).flatMap(x => x.content ?? []).filter(x => x.type === 'output_text').map(x => x.text).join('');
  JSON.parse(text);
  await mkdir(dirname(output), { recursive: true });
  await writeFile(output + '.pending', text, { mode: 0o600 });
  await rename(output + '.pending', output);
  return { legacy: false, event: { type: 'turn.completed', usage: {
    input_tokens: response.usage.input_tokens,
    cached_input_tokens: response.usage.input_tokens_details?.cached_tokens ?? 0,
    output_tokens: response.usage.output_tokens,
  } }, serviceTier: response.service_tier ?? 'UNKNOWN', receiptId: context.receiptId, reservationId: reserved.id };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const result = await runBounded({ args: process.argv.slice(2), input: async () => {
      let value = ''; for await (const chunk of process.stdin) value += chunk; return value;
    } });
    if (result.legacy) process.exitCode = 125;
    else process.stdout.write(JSON.stringify(result.event) + '\n' + JSON.stringify({ type: 'bounded_spend.completed',
      serviceTier: result.serviceTier, receiptId: result.receiptId, reservationId: result.reservationId }) + '\n');
  } catch (error) {
    // Não imprimir corpo do provedor, header, segredo ou erro que possa repetir uma chave inválida.
    process.stderr.write(`Transporte limitado bloqueado: ${error.message}\n`);
    process.exitCode = 1;
  }
}
