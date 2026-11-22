import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';
import { mkdtempSync, writeFileSync, rmSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
const dir=mkdtempSync(join(tmpdir(),'pde-context-test-'));
const file=join(dir,'context.json');
const original=JSON.stringify('prova😀\\"\n'.repeat(9000));
const sha256=createHash('sha256').update(original).digest('hex');
writeFileSync(file,JSON.stringify({taskId:9001,references:{[sha256]:original}}));
const script=process.env.PDE_CONTEXT_MCP_SCRIPT ?? new URL('../src/main/resources/mcp/pde-context.mjs', import.meta.url).pathname;
assert.equal(spawnSync('node',[script,'--check-snapshot'],{env:{...process.env,PDE_CONTEXT_FILE:file}}).status,0);
const client=new Client({name:'synthetic',version:'1'});
try {
 await client.connect(new StdioClientTransport({command:'node',args:[script],env:{...process.env,PDE_CONTEXT_FILE:file}}));
 assert.equal((await client.listTools()).tools[0].name,'read_task_context');
 let offset=0, restored='';
 do {
  const response=await client.callTool({name:'read_task_context',arguments:{sha256,offset}});
  assert.ok(!response.isError);
  const page=JSON.parse(response.content[0].text); restored+=page.text;offset=page.nextOffset;
 } while(offset!==null);
 assert.equal(restored,original);
 assert.equal((await client.callTool({name:'read_task_context',arguments:{sha256:'a'.repeat(64)}})).isError,true);
 assert.ok(readFileSync(file+'.reads','utf8').includes('9001'));
 console.log('MCP: recuperação integral Unicode, paginação, hash e isolamento aprovados.');
} finally {await client.close();rmSync(dir,{recursive:true,force:true});}
