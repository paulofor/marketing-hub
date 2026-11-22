import { readFileSync, appendFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

const snapshot = JSON.parse(readFileSync(process.env.PDE_CONTEXT_FILE, 'utf8'));
if (process.argv.includes('--check-snapshot')) {
  for (const [hash, text] of Object.entries(snapshot.references)) {
    if (typeof text !== 'string' || createHash('sha256').update(text).digest('hex') !== hash)
      throw new Error('Integridade do snapshot inválida.');
  }
  process.exit(0);
}
const server = new McpServer({ name: 'pde-context', version: '1.0.0' });
server.registerTool('read_task_context', {
  description: 'Recupera integralmente uma referência do contexto desta tarefa, em páginas de texto JSON. Leia todas as páginas necessárias antes de concluir; histórico não constitui aceite atual.',
  inputSchema: { sha256: z.string().regex(/^[a-f0-9]{64}$/), offset: z.number().int().min(0).default(0) },
  annotations: { readOnlyHint: true, openWorldHint: false, destructiveHint: false },
}, async ({ sha256, offset }) => {
  if (!Object.hasOwn(snapshot.references, sha256)) throw new Error('Referência fora do alcance desta tarefa.');
  const text = snapshot.references[sha256];
  if (typeof text !== "string") throw new Error("Contrato da referência inválido.");
  if (createHash('sha256').update(text).digest('hex') !== sha256) throw new Error('Integridade da referência inválida.');
  if (offset > text.length) throw new Error('Posição inválida.');
  let end = Math.min(offset + 16000, text.length);
  if (end < text.length && /[\uD800-\uDBFF]/.test(text[end - 1])) end--;
  appendFileSync(`${process.env.PDE_CONTEXT_FILE}.reads`, JSON.stringify({taskId: snapshot.taskId, sha256, offset, end}) + '\n');
  return { content: [{type:'text', text:JSON.stringify({taskId:snapshot.taskId, sha256, offset, nextOffset:end < text.length ? end : null, totalChars:text.length, text:text.slice(offset,end)})}] };
});
await server.connect(new StdioServerTransport());
