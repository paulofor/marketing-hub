import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';

const baseUrl = requiredEnv('MCP_MARKETING_HUB_URL').replace(/\/$/, '');
const assetId = positiveInteger(requiredEnv('MCP_VISUAL_ASSET_ID'), 'MCP_VISUAL_ASSET_ID');
const planId = positiveInteger(requiredEnv('MCP_COMMERCIAL_PLAN_ID'), 'MCP_COMMERCIAL_PLAN_ID');
const server = new McpServer({ name: 'temis-library-review', version: '1.0.0' });

server.registerTool('inspecionar_entregavel', {
  description: 'Consulta o snapshot vigente e retorna a imagem real em alta definição.',
  inputSchema: {},
  annotations: { readOnlyHint: true, openWorldHint: true, destructiveHint: false },
}, async () => {
  const response = await fetch(`${baseUrl}/api/internal/planning/image-studio/v1/reviews/${assetId}/context?planId=${planId}`, {
    headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(30000),
  });
  if (!response.ok) throw new Error(`Marketing Hub respondeu HTTP ${response.status} ao consultar o entregável`);
  const context = await response.json();
  const imageResponse = await fetch(httpUrl(context.assetUrl), { signal: AbortSignal.timeout(30000) });
  if (!imageResponse.ok) throw new Error(`Entregável respondeu HTTP ${imageResponse.status}`);
  const mimeType = imageResponse.headers.get('content-type')?.split(';')[0] ?? 'image/png';
  if (!mimeType.startsWith('image/')) throw new Error('Entregável não retornou uma imagem');
  const data = Buffer.from(await imageResponse.arrayBuffer()).toString('base64');
  return { content: [
    { type: 'text', text: JSON.stringify({ assetId, planId, inspectedAt: new Date().toISOString(), context }) },
    { type: 'image', data, mimeType },
  ] };
});

await server.connect(new StdioServerTransport());

function httpUrl(value) { if (!/^https?:\/\//i.test(String(value ?? ''))) throw new Error('URL do entregável ausente ou inválida'); return String(value); }
function positiveInteger(value, name) { const parsed = Number(value); if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error(`${name} deve ser inteiro positivo`); return parsed; }
function requiredEnv(name) { const value = process.env[name]; if (!value) throw new Error(`Variável obrigatória ausente: ${name}`); return value; }
