import test from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {verifyVideoIdentity,videoBrowserOptions} from '../../main/resources/browser/learning-cycle-video-checks.mjs';

test('usa o navegador com codecs sem ignorar a configuração explícita da sandbox',()=>{
  assert.equal(videoBrowserOptions({PDE_VIDEO_BROWSER_EXECUTABLE:'/opt/chrome-linux64/chrome'}).executablePath,'/opt/chrome-linux64/chrome');
  assert.equal(videoBrowserOptions({PDE_VIDEO_BROWSER_EXECUTABLE:'/opt/chrome-linux64/chrome',PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH:'/fixture/chromium'}).executablePath,'/fixture/chromium');
});

// Usa bytes segregados; nenhuma URL externa é acessada nos testes unitários.
function fixture() {
  const buffers = {AD:Buffer.from('Anúncio local'), LANDING_HERO:Buffer.from('Demonstração local')};
  const media = role => ({assetId:role==='AD'?91041:91042,role,reviewStatus:'APPROVED',captionsBurnedIn:true,captions:'Legenda',assetUrl:`https://fixture.invalid/${role}.mp4`,sha256:createHash('sha256').update(buffers[role]).digest('hex')});
  const binding = {evidenceType:'LEARNING_CYCLE_VIDEO_INTEGRATION_V1',integrationFingerprint:'c'.repeat(64),campaignVideo:media('AD'),heroVideo:media('LANDING_HERO')};
  const fetcher = async url => new Response(buffers[url.pathname.includes('LANDING_HERO')?'LANDING_HERO':'AD'],{status:200});
  return {binding,fetcher};
}
test('confere os bytes das peças distintas sem assumir sucesso pelo cadastro', async()=>{
  const {binding,fetcher}=fixture();const result=await verifyVideoIdentity(binding,fetcher);
  assert.equal(result.length,2);assert.equal(result[1].role,'LANDING_HERO');assert.ok(result.every(r=>r.bytes>0));
});
for(const field of ['sha256','role','reviewStatus','captions','captionsBurnedIn','assetUrl']) test(`recusa divergência de ${field}`,async()=>{
  const {binding,fetcher}=fixture();binding.heroVideo[field]=field==='captionsBurnedIn'?false:field==='assetUrl'?'http://fixture.invalid/file':field==='captions'?'':'invalid';
  await assert.rejects(verifyVideoIdentity(binding,fetcher));
});
test('recusa resposta sem arquivo, falha HTTP e redirecionamento', async()=>{
  for(const status of [200,302,404]){
    const {binding}=fixture();await assert.rejects(verifyVideoIdentity(binding,async()=>new Response('',{status})));
  }
});
test('respeita o limite antes de baixar conteúdo sem tamanho confiável',async()=>{
  const {binding}=fixture();await assert.rejects(verifyVideoIdentity(binding,async()=>new Response('x',{headers:{'content-length':String(65*1024*1024)}})));
});
