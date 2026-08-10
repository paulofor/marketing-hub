import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

const transport = new StdioClientTransport({
  command: 'node',
  args: ['src/main/resources/mcp/meta-ad-approver.mjs'],
  env: {
    ...process.env,
    MCP_MARKETING_HUB_URL: 'http://127.0.0.1:9',
    MCP_CREATIVE_ID: '273',
    MCP_EXPERIMENT_ID: '88'
  }
});
const client = new Client({ name: 'meta-ad-approver-contract-test', version: '1.0.0' });

try {
  await client.connect(transport);
  const response = await client.listTools();
  const names = response.tools.map(tool => tool.name).sort();
  const expected = [
    'consultar_contexto',
    'inspecionar_landing',
    'inspecionar_midia',
    'recuperar_estrategias_promovidas',
    'recuperar_memoria_especializada',
    'registrar_aprendizado_candidato'
  ];
  if (JSON.stringify(names) !== JSON.stringify(expected)) {
    throw new Error(`Ferramentas MCP divergentes: ${JSON.stringify(names)}`);
  }
} finally {
  await client.close();
}
