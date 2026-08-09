import { chromium } from 'playwright-core';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

const baseUrl = requiredEnv('MCP_MARKETING_HUB_URL').replace(/\/$/, '');
const creativeId = positiveInteger(requiredEnv('MCP_CREATIVE_ID'), 'MCP_CREATIVE_ID');
const experimentId = positiveInteger(requiredEnv('MCP_EXPERIMENT_ID'), 'MCP_EXPERIMENT_ID');

const server = new McpServer({ name: 'meta-ad-approver', version: '1.0.0' });
registerTool('consultar_contexto', 'Confirma no Marketing Hub os dados atuais do anúncio reservado.');
registerTool('inspecionar_midia', 'Retorna a imagem em alta definição ou três quadros do vídeo.');
registerTool('inspecionar_landing', 'Renderiza e retorna a página de destino em mobile e desktop.');
registerMemoryTool('recuperar_memoria_especializada', 'Recupera aprendizados de copy, estética e integração deste experimento.', false);
registerMemoryTool('registrar_aprendizado_candidato', 'Registra uma hipótese de melhoria do anúncio sem aprová-la automaticamente.', true);
await server.connect(new StdioServerTransport());

function registerTool(name, description) {
  server.registerTool(name, {
    description,
    inputSchema: {},
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      destructiveHint: false,
    },
  }, async () => callTool(name));
}

function registerMemoryTool(name, description, writable) {
  const inputSchema = writable ? {
    specialty: z.string().min(3).max(120), content: z.string().min(10).max(4000),
    evidence: z.string().min(10).max(4000), sourceReference: z.string().max(700).optional(),
    confidence: z.number().min(0).max(1)
  } : {};
  server.registerTool(name, { description, inputSchema, annotations: { readOnlyHint: !writable, openWorldHint: false, destructiveHint: false } }, async args => callMemory(writable ? 'POST' : 'GET', args));
}

async function callMemory(method, args) {
  const root = '/api/internal/agent-memory/v1/agents/meta-ad-approver';
  const path = method === 'GET' ? `${root}?${new URLSearchParams({ scopeType: 'EXPERIMENT', scopeId: String(experimentId), limit: '8' })}` : root;
  const body = method === 'POST' ? JSON.stringify({ ...args, scopeType: 'EXPERIMENT', scopeId: String(experimentId), sourceExecutionId: `creative-${creativeId}` }) : undefined;
  const response = await fetch(`${baseUrl}${path}`, { method, headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body, signal: AbortSignal.timeout(30000) });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} na memória do Aprovador`);
  return { content: [text({ audit: audit(method === 'GET' ? 'recuperar_memoria_especializada' : 'registrar_aprendizado_candidato', new Date().toISOString()), data: await response.json() })] };
}

async function callTool(name) {
  const startedAt = new Date().toISOString();
  const creative = await loadCreative();
  let content;
  if (name === 'consultar_contexto') {
    content = [text({ audit: audit(name, startedAt), data: creative })];
  } else if (name === 'inspecionar_midia') {
    content = await inspectMedia(creative, name, startedAt);
  } else if (name === 'inspecionar_landing') {
    content = await inspectLanding(creative, name, startedAt);
  } else {
    throw new Error(`Ferramenta não permitida: ${name}`);
  }
  process.stderr.write(`${JSON.stringify({ tool: name, creativeId, experimentId, startedAt, status: 200 })}\n`);
  return { content };
}

async function loadCreative() {
  const response = await fetch(`${baseUrl}/api/internal/creatives/${creativeId}/agent-review/context?experimentId=${experimentId}`, { headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(30000) });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} ao consultar o contexto do criativo`);
  return response.json();
}

async function inspectMedia(creative, toolName, startedAt) {
  const url = httpUrl(creative.mediaUrl ?? creative.imageUrl, 'mídia');
  if (String(creative.format ?? '').toUpperCase() !== 'VIDEO') {
    const response = await fetch(url, { signal: AbortSignal.timeout(30000) });
    if (!response.ok) throw new Error(`Mídia respondeu HTTP ${response.status}`);
    const mimeType = response.headers.get('content-type')?.split(';')[0] ?? 'image/jpeg';
    const data = Buffer.from(await response.arrayBuffer()).toString('base64');
    return [text({ audit: audit(toolName, startedAt), mediaType: 'IMAGE', source: url }), { type: 'image', data, mimeType }];
  }
  return withBrowser(async browser => {
    const page = await browser.newPage({ viewport: { width: 1080, height: 1080 } });
    await page.setContent(`<html><body style="margin:0;background:#000;display:grid;place-items:center;height:100vh"><video id="ad" crossorigin="anonymous" muted playsinline style="max-width:100%;max-height:100%" src="${escapeHtml(url)}"></video></body></html>`);
    await page.waitForFunction("document.querySelector('#ad').readyState >= 2", null, { timeout: 120000 });
    const duration = await page.locator('#ad').evaluate(video => video.duration);
    const result = [text({ audit: audit(toolName, startedAt), mediaType: 'VIDEO', source: url, duration })];
    for (const position of [0.1, 0.5, 0.9]) {
      await page.locator('#ad').evaluate((video, second) => { video.currentTime = second; }, Math.max(0, duration * position));
      await page.waitForTimeout(600);
      result.push({ type: 'image', data: (await page.screenshot({ type: 'jpeg', quality: 86 })).toString('base64'), mimeType: 'image/jpeg' });
    }
    return result;
  });
}

async function inspectLanding(creative, toolName, startedAt) {
  const url = httpUrl(creative.destinationUrl, 'URL de destino');
  return withBrowser(async browser => {
    const result = [text({ audit: audit(toolName, startedAt), source: url, views: ['mobile', 'desktop'] })];
    for (const viewport of [{ width: 390, height: 844 }, { width: 1440, height: 1000 }]) {
      const page = await browser.newPage({ viewport });
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 120000 });
      result.push({ type: 'image', data: (await page.screenshot({ fullPage: true, type: 'jpeg', quality: 82 })).toString('base64'), mimeType: 'image/jpeg' });
      await page.close();
    }
    return result;
  });
}

async function withBrowser(action) {
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'] });
  try { return await action(browser); } finally { await browser.close(); }
}

function audit(toolName, consultedAt) { return { tool: toolName, creativeId, experimentId, consultedAt, readOnly: true }; }
function text(value) { return { type: 'text', text: JSON.stringify(value) }; }
function httpUrl(value, name) { if (!/^https?:\/\//i.test(String(value ?? ''))) throw new Error(`${name} ausente ou inválida`); return String(value); }
function positiveInteger(value, name) { const parsed = Number(value); if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error(`${name} deve ser inteiro positivo`); return parsed; }
function requiredEnv(name) { const value = process.env[name]; if (!value) throw new Error(`Variável obrigatória ausente: ${name}`); return value; }
function escapeHtml(value) { return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'); }
