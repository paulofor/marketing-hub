import { chromium } from 'playwright-core';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

const baseUrl = required('MCP_MARKETING_HUB_URL').replace(/\/$/, '');
const executionId = required('MCP_EXECUTION_ID');
const experimentId = positive(required('MCP_EXPERIMENT_ID'));
const server = new McpServer({ name: 'landing-generator', version: '1.0.0' });

server.registerTool('consultar_contexto', { description: 'Consulta o snapshot congelado da landing e do Quality Review.', inputSchema: {}, annotations: { readOnlyHint: true, openWorldHint: false, destructiveHint: false } }, async () => ({ content: [text(await context())] }));
server.registerTool('inspecionar_landing_desktop_mobile', { description: 'Renderiza o rascunho HTML em desktop, iPhone e Android sem publicar.', inputSchema: {}, annotations: { readOnlyHint: true, openWorldHint: false, destructiveHint: false } }, async () => inspectLanding());
server.registerTool('recuperar_memoria_especializada', { description: 'Recupera aprendizados confirmados ou candidatos vigentes deste experimento.', inputSchema: {}, annotations: { readOnlyHint: true, openWorldHint: false, destructiveHint: false } }, async () => memory('GET', {}));
server.registerTool('registrar_aprendizado_candidato', { description: 'Registra uma hipótese de melhoria sem permitir autopromoção.', inputSchema: { specialty: z.string().min(3).max(120), content: z.string().min(10).max(4000), evidence: z.string().min(10).max(4000), sourceReference: z.string().max(700).optional(), confidence: z.number().min(0).max(1) }, annotations: { readOnlyHint: false, openWorldHint: false, destructiveHint: false } }, async args => memory('POST', args));
await server.connect(new StdioServerTransport());

async function context() {
  const response = await fetch(`${baseUrl}/api/internal/geralanding/agent/v1/stage-executions/${executionId}/context`, { headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(30000) });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} ao consultar contexto`);
  return { audit: audit('consultar_contexto'), data: await response.json() };
}

async function inspectLanding() {
  const snapshot = await context();
  const html = snapshot.data?.context?.landingHtml;
  if (typeof html !== 'string' || html.length < 100) throw new Error('Rascunho HTML ausente ou incompleto');
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'] });
  try {
    const result = [text({ audit: audit('inspecionar_landing_desktop_mobile'), views: ['desktop', 'iPhone 15 Pro', 'Pixel 7'] })];
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 393, height: 852 }, { width: 412, height: 915 }]) {
      const page = await browser.newPage({ viewport, isMobile: viewport.width < 500, hasTouch: viewport.width < 500 });
      await page.setContent(html, { waitUntil: 'domcontentloaded', timeout: 120000 });
      await page.evaluate(() => document.fonts?.ready);
      result.push({ type: 'image', data: (await page.screenshot({ fullPage: true, type: 'jpeg', quality: 84 })).toString('base64'), mimeType: 'image/jpeg' });
      await page.close();
    }
    return { content: result };
  } finally { await browser.close(); }
}

async function memory(method, args) {
  const root = '/api/internal/agent-memory/v1/agents/landing-generator';
  const path = method === 'GET' ? `${root}?${new URLSearchParams({ scopeType: 'EXPERIMENT', scopeId: String(experimentId), limit: '8' })}` : root;
  const body = method === 'POST' ? JSON.stringify({ ...args, scopeType: 'EXPERIMENT', scopeId: String(experimentId), sourceExecutionId: executionId }) : undefined;
  const response = await fetch(`${baseUrl}${path}`, { method, headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body, signal: AbortSignal.timeout(30000) });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} ao acessar memória`);
  return { content: [text({ audit: audit(method === 'GET' ? 'recuperar_memoria_especializada' : 'registrar_aprendizado_candidato'), data: await response.json() })] };
}

function audit(tool) { return { tool, executionId, experimentId, consultedAt: new Date().toISOString(), authority: 'DRAFT_ONLY_NO_PUBLICATION' }; }
function text(value) { return { type: 'text', text: JSON.stringify(value) }; }
function required(name) { const value = process.env[name]; if (!value) throw new Error(`Variável obrigatória ausente: ${name}`); return value; }
function positive(value) { const parsed = Number(value); if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error('MCP_EXPERIMENT_ID inválido'); return parsed; }
