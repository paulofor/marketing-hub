import assert from 'node:assert/strict';
import test from 'node:test';
import {createServer} from 'node:http';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {Client} from '@modelcontextprotocol/sdk/client/index.js';
import {StdioClientTransport} from '@modelcontextprotocol/sdk/client/stdio.js';

test('MCP confere os bytes observados e bloqueia captura estática adulterada',async t=>{
  const png=Buffer.from('pixels-locais-para-teste-do-hash');
  const digest=createHash('sha256').update(png).digest('hex');
  let base,expected=digest;
  const server=createServer((req,res)=>{
    if(req.url.startsWith('/api/internal/creatives/')) {
      res.writeHead(200,{'content-type':'application/json'});
      return res.end(JSON.stringify({format:'IMAGE',mediaUrl:`${base}/image.png`,staticUiProof:{contractVersion:'OPALA_COMMERCIAL_UI_PROOF_V1',sha256:expected}}));
    }
    res.writeHead(200,{'content-type':'image/png'});res.end(png);
  });
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  base=`http://127.0.0.1:${server.address().port}`;
  const transport=new StdioClientTransport({command:'node',args:[fileURLToPath(new URL('../src/main/resources/mcp/meta-ad-approver.mjs',import.meta.url))],env:{...process.env,MCP_MARKETING_HUB_URL:base,MCP_CREATIVE_ID:'542',MCP_EXPERIMENT_ID:'106'}});
  const client=new Client({name:'static-ui-proof-test',version:'1.0.0'});
  t.after(async()=>{await client.close();await new Promise(r=>server.close(r));});
  await client.connect(transport);
  const valid=await client.callTool({name:'inspecionar_midia',arguments:{}});
  assert.ok(valid.content.some(item=>item.type==='image'));
  const audit=JSON.parse(valid.content.find(item=>item.type==='text').text);
  assert.equal(audit.sha256,digest);assert.equal(audit.sha256MatchesGovernance,true);
  expected='b'.repeat(64);
  const invalid=await client.callTool({name:'inspecionar_midia',arguments:{}});
  assert.equal(invalid.isError,true);
  assert.equal(invalid.content.some(item=>item.type==='image'),false);
  assert.match(invalid.content.find(item=>item.type==='text').text,/Hash da captura estática diverge/);
});
