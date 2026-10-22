import assert from 'node:assert/strict';
import {createServer} from 'node:http';
import {createRequire} from 'node:module';
import {mkdtemp,readFile,rm,stat} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
import test from 'node:test';
const require=createRequire(import.meta.url);
const {chromium}=require('playwright-core');
const {captureUiProof}=await import(process.env.OPALA_PROOF_MODULE_PATH
  ? pathToFileURL(process.env.OPALA_PROOF_MODULE_PATH).href
  : new URL('../src/main/resources/proofs/opala/v1/capture-ui-proof.mjs',import.meta.url).href);

async function fixture(t, mode='LOCAL') {
  const state={posts:0,version:'fixture-v14'};
  const server=createServer(async(req,res)=>{
    if(req.method==='POST') {
      let raw='';for await(const c of req)raw+=c;
      const body=JSON.parse(raw);state.posts++;state.sentVersion=body.experienceVersion;
      res.writeHead(201,{'content-type':'application/json'});
      return res.end(JSON.stringify({status:'COMPLETED',model:mode==='LOCAL'?'MUSA_LOCAL_RULES_V1':'external-model',serviceTier:mode,inputTokens:0,outputTokens:0,costUsd:0}));
    }
    res.writeHead(200,{'content-type':'text/html; charset=utf-8'});
    res.end(`<html><body><section role="region" aria-label="Primeiro ajuste MUSA"><textarea></textarea><button id="next">Continuar</button><button id="submit">Descobrir meu primeiro ajuste</button></section><section class="public-diagnostic-result" style="display:none;width:420px;padding:20px;background:white;border:1px solid black;font:18px sans-serif"><h1>Primeiro ajuste gratuito</h1><div class="public-main-signal"><p>DEMONSTRAÇÃO — trabalho</p></div><p>camisa azul e calça preta</p><p>Ajuste o caimento e teste sentar, caminhar e levantar os braços.</p><button class="secondary-button"><svg viewBox="0 0 16 16"><path d="M1 1L9 9"/></svg>Baixar meu primeiro ajuste</button></section><script>document.querySelector('#next').onclick=()=>document.querySelector('textarea').value='';document.querySelector('#submit').onclick=async()=>{await fetch('/api/pde/public/presence-diagnostic',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({experienceVersion:'fixture-v14',answers:{occasion:'DEMONSTRAÇÃO — trabalho',existingSelection:'camisa azul e calça preta'}})});document.querySelector('.public-diagnostic-result').style.display='block';};</script></body></html>`);
  });
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  const directory=await mkdtemp(join(tmpdir(),'opala-proof-test-'));
  const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_BIN?{executablePath:process.env.CHROMIUM_BIN}:{}),args:['--disable-dev-shm-usage']});
  t.after(async()=>{await browser.close();await new Promise(r=>server.close(r));await rm(directory,{recursive:true,force:true});});
  return {browser,state,png:join(directory,'proof.png'),input:{contractVersion:'OPALA_COMMERCIAL_UI_PROOF_V1',adapter:'MUSA_LOCAL_FIRST_VALUE_V1',taskId:1,productId:4,cycleId:13,experimentId:106,productVersion:state.version,adapterExperienceVersion:state.version,adapterContractSha256:'a'.repeat(64),destinationUrl:`http://127.0.0.1:${server.address().port}`,copySourceTaskId:788,copySourceArtifactId:1118}};
}

test('captura completa preserva identidade, dados de teste e custo local em dois produtos',async t=>{
  const f=await fixture(t);
  for(const [productId,cycleId,experimentId] of [[4,13,106],[407,71,922]]) {
    const receipt=await captureUiProof(f.browser,{...f.input,productId,cycleId,experimentId},f.png);
    const png=await readFile(f.png);
    assert.equal(png.subarray(1,4).toString(),'PNG');
    assert.equal(receipt.productId,productId);assert.equal(receipt.experimentId,experimentId);
    assert.equal(receipt.testData,true);assert.equal(receipt.commercialEvidenceEligible,false);
    assert.equal(receipt.costUsd,0);assert.equal(receipt.sourceType,'OWN_PRODUCT_UI');
    assert.equal(receipt.captureMode,'DOM_REGION');assert.ok(receipt.pixelWidth>600);
    assert.equal(receipt.inputTokens,0);assert.equal(receipt.outputTokens,0);
  }
  assert.equal(f.state.posts,2);assert.equal(f.state.sentVersion,'fixture-v14');
});

test('resposta de provedor externo não vira prova local mesmo com custo declarado zero',async t=>{
  const f=await fixture(t,'FLEX');
  await assert.rejects(captureUiProof(f.browser,f.input,f.png),/primeiro valor local/);
  await assert.rejects(stat(f.png),{code:'ENOENT'});
});

test('contrato inválido falha antes de abrir a página ou gerar artefato',async t=>{
  const f=await fixture(t);
  await assert.rejects(captureUiProof(f.browser,{...f.input,adapter:'UNKNOWN'},f.png),/incompleto/);
  assert.equal(f.state.posts,0);
});
