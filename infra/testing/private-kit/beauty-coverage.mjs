import { createRequire } from 'node:module';
import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
const require=createRequire(import.meta.url);
const { chromium }=require('playwright');
const root='http://127.0.0.1:57282/api/pde/kit/private/v1';
if(process.env.PDE_INTERNAL_API_TOKEN!=='kit-local-internal-only')throw new Error('Apenas homologação local.');
const browser=await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH||'/usr/bin/chromium',args:['--no-sandbox']});
const results=[];
const family=process.env.KIT_TEST_REFERENCE==='true'?'beauty-v2':'beauty-v1';
const suffix=family.endsWith('v2')?'v2':'v1';
await mkdir(process.argv[2],{recursive:true});
async function api(path,method,body,session){
 const r=await fetch(root+path,{method,headers:{'Content-Type':'application/json',...(session?{'X-Kit-Session':session}:{'X-PDE-Internal-Token':process.env.PDE_INTERNAL_API_TOKEN})},body:body?JSON.stringify(body):undefined});
 assert.equal(r.status,200);return r.json();
}
try {
 for(const [productId,cycleId] of [[8037,7037],[8048,7048]]){
  const plan=await api('/admin/implementations?productId='+productId+'&cycleId='+cycleId,'GET');
  assert.equal(plan.boundProfileCode,null);
  const selection={requestKey:crypto.randomUUID(),productId,cycleId,expectedRevision:plan.revision,architectureTaskId:plan.architectureTaskId,architectureSha256:plan.architectureSha256,profileCode:family,operatorName:'QA local'};
  const bound=await api('/admin/implementations','POST',selection);
  assert.equal(bound.boundProfileCode,family);
  assert.equal((await api('/admin/implementations','POST',selection)).revision,bound.revision);
  for(const specialty of ['nails','barber','hair','makeup','brows','aesthetics'].map(p=>p+'-'+suffix)){
   const access=await api('/internal/sessions','POST',{requestKey:crypto.randomUUID(),productId,cycleId,prototypeVersion:'beauty-candidate-v1',scenarioCode:'ADHERENT',deviceProfile:'DESKTOP_1440'});
   const context=await browser.newContext({viewport:{width:1440,height:900},acceptDownloads:true});
   const page=await context.newPage();
   const browserErrors=[];page.on('pageerror',e=>browserErrors.push(e.message));
   await page.addInitScript(token=>sessionStorage.setItem('private-kit-session-v1',token),access.sessionToken);
   await page.goto(root+'/prototype');await page.locator('#specialty-field').waitFor({state:'visible'});
   await page.locator('#specialty').selectOption(specialty);
   const services=await page.locator('[name=services]').inputValue();
   assert.equal(await page.locator('#specialty-sample').isVisible(),true);
   assert.ok((await page.locator('#sample-caption').innerText()).includes(services));
   assert.ok(services.length>3);
   if(!specialty.startsWith('nails-'))assert.ok(!services.toLowerCase().includes('manicure'));
   await page.locator('[name=consentAccepted]').check();await page.locator('#prepare').click();
   try { await page.locator('#result').waitFor({state:'visible',timeout:60000}); }
   catch(ex){
     await page.screenshot({path:process.argv[2]+'/failure-'+productId+'-'+specialty+'.png',fullPage:true});
     const state=await api('/session','GET',null,access.sessionToken);
     console.error(JSON.stringify({productId,specialty,status:state.status,reason:state.reason,pageError:await page.locator('#error').textContent(),browserErrors}));
     throw ex;
   }
   const session=await api('/session','GET',null,access.sessionToken);
   assert.equal(session.input.specialtyCode,specialty);assert.equal(session.manifest.files.length,36);
   assert.ok(session.firstApplication.caption.includes(services));
   assert.equal(session.manifest.commercialEvidenceEligible,false);
   if(suffix==='v2'){
    assert.equal(session.manifest.visualReferences.profileCode,specialty);
    assert.equal(session.manifest.visualReferences.assets.length,5);
    assert.equal(session.manifest.visualReferences.commercialApproval,false);
    await page.locator('#enlarge-post').click();
    await page.locator('#art-dialog').waitFor({state:'visible'});
    await page.locator('#enlarged-art').evaluate(e=>e.decode());
    await page.screenshot({path:process.argv[2]+'/'+productId+'-'+specialty+'-ampliado.png'});
    await page.keyboard.press('Escape');
    assert.equal(await page.locator('#art-dialog').isVisible(),false);
   }
   const sha=session.manifest.zipSha256;
   await page.reload();await page.locator('#result').waitFor({state:'visible'});
   assert.equal((await api('/session','GET',null,access.sessionToken)).manifest.zipSha256,sha);
   await page.screenshot({path:process.argv[2]+'/'+productId+'-'+specialty+'.png',fullPage:true});
   results.push({productId,cycleId,specialty,services,files:36,sha256:sha,recovered:true});
   await context.close();
  }
 }
 await writeFile(process.argv[2]+'/report.json',JSON.stringify({providerCalls:0,commercialEvidenceEligible:false,results},null,2));
 console.log('Cobertura: seis especialidades em duas identidades, 36 arquivos por pacote e recuperação preservada.');
}finally{await browser.close();}
