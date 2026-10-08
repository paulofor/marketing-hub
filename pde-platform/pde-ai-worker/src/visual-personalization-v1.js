import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const prompts = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../prompts/visual-personalization-v1');
const api = '/api/pde/visual-personalization/v1/internal/stage-executions';

/** Materializa request limitada usando somente contexto persistido pelo backend. */
export async function buildVisualRequest(execution) {
  const [system, template, schema] = await Promise.all([
    fs.readFile(path.join(prompts, 'system.md'), 'utf8'),
    fs.readFile(path.join(prompts, 'user.md'), 'utf8'),
    fs.readFile(path.join(prompts, 'response-schema.json'), 'utf8').then(JSON.parse),
  ]);
  if (schema.properties.output.maxContains !== 1 || execution.trafficClass !== 'AGENT_VALIDATION'
      || execution.contractVersion !== 'PDE_VISUAL_PERSONALIZATION_V1' || !execution.input || !execution.model) {
    throw new Error('Contrato visual privado incompleto; não enviar chamada paga.');
  }
  const user = template.replace('{{PRODUCT_NAME}}', execution.productName)
    .replace('{{PRODUCT_VERSION}}', execution.productVersion)
    .replace('{{JOB_ID}}', execution.jobId);
  return {
    model: execution.model,
    service_tier: 'flex',
    max_output_tokens: 4096,
    max_tool_calls: 1,
    input: [
      { role: 'system', content: system },
      { role: 'developer', content: user },
      { role: 'user', content: JSON.stringify(execution.input, null, 2) },
    ],
    metadata: { mh_job_id: execution.jobId, mh_input_hash: execution.inputHash },
    tool_choice: { type: 'image_generation' },
    tools: [{ type: 'image_generation', model: 'gpt-image-2.5-sunburst', action: 'generate',
      quality: 'high', size: '1024x1024', output_format: 'png' }],
  };
}

/** Consome a fila canônica e reutiliza callbacks persistidos antes de considerar nova inferência. */
export class VisualPersonalizationWorker {
  constructor({ backendUrl, internalToken, apiKey, fetchImpl = fetch,
    providerUrl = 'https://api.openai.com/v1/responses',
    spoolDir = process.env.PDE_VISUAL_CALLBACK_SPOOL_DIR || '/app/data/visual-callbacks' }) {
    this.backendUrl = backendUrl.replace(/\/+$/, '');
    this.internalToken = internalToken;
    this.apiKey = apiKey;
    this.fetch = fetchImpl;
    this.providerUrl = providerUrl;
    this.spoolDir = spoolDir;
    const provider = new URL(providerUrl);
    if (providerUrl !== 'https://api.openai.com/v1/responses'
        && !(process.env.PDE_VISUAL_ALLOW_LOCAL_PROVIDER === 'true'
          && provider.protocol === 'http:' && ['127.0.0.1', 'localhost', 'mock-provider'].includes(provider.hostname)
          && !provider.username && !provider.password))
      throw new Error('Provedor visual deve usar a origem oficial; servidor local é exclusivo de teste.');
  }

  /** Recupera callbacks por arquivo antes de descobrir novas pendências. */
  async processNextPending() {
    await fs.mkdir(this.spoolDir, { recursive: true, mode: 0o700 });
    for (const file of await fs.readdir(this.spoolDir)) {
      if (!/^pde-visual-v1-[a-f0-9-]{36}\.json$/.test(file)) continue;
      const saved = JSON.parse(await fs.readFile(path.join(this.spoolDir, file), 'utf8'));
      await this.callback(saved.jobId, saved.result);
      await fs.unlink(path.join(this.spoolDir, file));
      return;
    }
    const [execution] = await this.backend('GET', '/pending');
    if (!execution) return;
    if (execution.replayOnly) {
      await this.backend('POST', '/' + execution.jobId + '/replay');
      return;
    }
    if (!this.apiKey) return;
    let claimed = false;
    let providerCalled = false;
    let result;
    try {
      const reserved = await this.backend('POST', '/' + execution.jobId + '/claim');
      if (reserved.status !== 'PDE_RUNNING') return;
      claimed = true;
      const rawRequest = await buildVisualRequest(reserved);
      await this.backend('POST', '/' + execution.jobId + '/request', { rawRequest });
      providerCalled = true;
      console.log('PDE visual request jobId=' + execution.jobId + ' endpoint=' + this.providerUrl + ' model=' + rawRequest.model + ' serviceTier=flex');
      const response = await this.fetch(this.providerUrl, {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + this.apiKey, 'Content-Type': 'application/json',
          'X-Client-Request-Id': execution.jobId },
        body: JSON.stringify(rawRequest),
        signal: AbortSignal.timeout(900_000),
      });
      const rawBody = await response.text();
      let rawResponse;
      try { rawResponse = JSON.parse(rawBody); }
      catch { rawResponse = { providerRawBody: rawBody }; }
      result = { rawResponse, httpStatus: response.status, providerCalled: true };
      console.log('PDE visual response jobId=' + execution.jobId + ' endpoint=' + this.providerUrl + ' status=' + response.status + ' responseId=' + (rawResponse.id || 'unknown'));
    } catch (error) {
      console.error('Falha na homologação visual PDE jobId=' + execution.jobId + ' providerCalled=' + providerCalled, error);
      if (!claimed) return;
      result = { rawResponse: { error: { message: error instanceof Error ? error.message : String(error) } },
        httpStatus: 502, providerCalled };
    }
    const filename = path.join(this.spoolDir, execution.jobId + '.json');
    const temporary = filename + '.tmp';
    await fs.writeFile(temporary, JSON.stringify({ jobId: execution.jobId, result }), { mode: 0o600 });
    await fs.rename(temporary, filename);
    await this.callback(execution.jobId, result);
    await fs.unlink(filename);
  }

  /** Reenvia somente a mesma resposta registrada localmente, nunca uma chamada ao provedor. */
  async callback(jobId, result) {
    return this.backend('POST', '/' + jobId + '/result', result);
  }

  /** Acessa contratos PDE oficiais preservando status de recusa e correlação. */
  async backend(method, suffix, body) {
    const response = await this.fetch(this.backendUrl + api + suffix, {
      method,
      headers: { 'X-PDE-Internal-Token': this.internalToken, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(30_000),
    });
    if (!response.ok) throw new Error('Backend PDE recusou ' + method + ' ' + suffix + ': HTTP ' + response.status);
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }
}
