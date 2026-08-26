import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

const transport = new StdioClientTransport({
  command: 'node',
  args: ['src/main/resources/mcp/landing-generator.mjs'],
  env: { ...process.env, MCP_MARKETING_HUB_URL: 'http://127.0.0.1:9', MCP_EXECUTION_ID: 'job-88', MCP_EXPERIMENT_ID: '88' }
});
const client = new Client({ name: 'landing-generator-contract-test', version: '1.0.0' });
try {
  await client.connect(transport);
  const names = (await client.listTools()).tools.map(tool => tool.name).sort();
  const expected = ['auditar_jornada_funcional', 'consultar_contexto', 'inspecionar_landing_desktop_mobile', 'recuperar_estrategias_promovidas', 'recuperar_memoria_especializada', 'registrar_aprendizado_candidato'];
  if (JSON.stringify(names) !== JSON.stringify(expected)) throw new Error(`Ferramentas MCP divergentes: ${JSON.stringify(names)}`);
  const html = '<!doctype html><html><body><main>' + 'conteúdo '.repeat(30) + '</main><a id="checkout-cta-primary" href="https://checkout.example/rigel">Comprar</a></body></html>';
  const audit = await client.callTool({ name: 'auditar_jornada_funcional', arguments: { generatedHtml: html } });
  const result = JSON.parse(audit.content[0].text);
  if (result.eventsEmitted !== false || result.formSubmitted !== false) throw new Error('Auditoria local emitiu evento ou formulário');
  if (result.audits.length !== 3 || result.audits.some(item => item.horizontalOverflow)) throw new Error(`Auditoria responsiva divergente: ${JSON.stringify(result.audits)}`);
} finally { await client.close(); }
