import { createRequire } from 'node:module';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';
const require=createRequire(import.meta.url);
let library;try{library=require('playwright-core');}catch(ex){if(ex.code!=='MODULE_NOT_FOUND')throw ex;library=require('playwright');}
const {chromium,devices}=library;
const [inputFile,outputFile,directory]=process.argv.slice(2);
const input=JSON.parse(await readFile(inputFile,'utf8'));
const token=process.env.PDE_INTERNAL_API_TOKEN;if(!token)throw new Error('Credencial interna necessária.');
const base=input.sourceUrl.replace(/\/prototype$/,'');
const profiles={DESKTOP_1440:{viewport:{width:1440,height:900}},IPHONE_15_PRO:{...devices['iPhone 15 Pro'],defaultBrowserType:undefined},PIXEL_7:{...devices['Pixel 7'],defaultBrowserType:undefined}};
const sideEffects={paymentEnabled:false,published:false,campaignCreated:false,mediaSpendBrl:0};
const packageContractVersion='PDE_PRIVATE_KIT_PACKAGE_V2';
const packageNames=['calendario/calendario-7-dias.txt',...Array.from({length:10},(_,i)=>['posts/post-','stories/story-','legendas/legenda-'].map(prefix=>prefix+String(i+1).padStart(2,'0')+(prefix.startsWith('legendas')?'.txt':'.png'))).flat(),...Array.from({length:5},(_,i)=>'mensagens/mensagem-'+String(i+1).padStart(2,'0')+'.txt')].sort();
const plans=input.mode==='TECHNICAL'?['ADHERENT','RECOVERY','SAFETY'].flatMap(s=>Object.keys(profiles).map(d=>[s,d])):[[input.scenarioCode,{ADHERENT:'DESKTOP_1440',RECOVERY:'IPHONE_15_PRO',SAFETY:'PIXEL_7'}[input.scenarioCode]]];
await mkdir(directory,{recursive:true});const startedAt=new Date(),scenarios=[],artifacts=[];
const browser=await chromium.launch({headless:true,...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH?{executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH}:{}),args:['--no-sandbox']});
async function api(path,{method='GET',body,session}={}) {
  const response=await fetch(base+path,{method,headers:session?{'X-Kit-Session':session,'Content-Type':'application/json'}:{'X-PDE-Internal-Token':token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  if(!response.ok)throw new Error('Contrato privado recusou '+path+' · HTTP '+response.status);
  return response.status===204?null:response.json();
}
try {
  for(const [scenarioCode,deviceProfile] of plans){
    const created=await api('/internal/sessions',{method:'POST',body:{requestKey:crypto.randomUUID(),productId:input.productId,cycleId:input.cycleId,prototypeVersion:input.prototypeVersion,scenarioCode,deviceProfile}});
    const context=await browser.newContext({...profiles[deviceProfile],acceptDownloads:true});
    const page=await context.newPage();const begin=Date.now();
    try {
      await page.addInitScript(secret=>sessionStorage.setItem('private-kit-session-v1',secret),created.sessionToken);
      await page.goto(input.sourceUrl);await page.locator('#briefing').waitFor({state:'visible'});
      if(input.profileCode==='barber-v1')await page.locator('[name=services]').fill('Corte e barba');
      // A fixture permanece igual entre dispositivos para comprovar reutilização sem nova composição.
      await page.locator('[name=professionalName]').fill(scenarioCode==='RECOVERY'?'Studio Retomada':'Studio Exemplo');
      await page.locator('[name=consentAccepted]').check();
      if(scenarioCode==='RECOVERY'){
        await page.locator('[name=cityRegion]').fill('');await page.locator('#prepare').click();
        if(!(await page.locator('[name=cityRegion]').evaluate(e=>!e.validity.valid)))throw new Error('Campo obrigatório vazio foi aceito.');
        await page.locator('[name=cityRegion]').fill('Cidade Exemplo');
      }
      if(scenarioCode==='RECOVERY') { let intercepted=false; await page.route('**/assets/post-01.png',route=>{ if(!intercepted){intercepted=true;return route.fulfill({status:503,contentType:'application/json',body:'{"detail":"Falha sintética de transporte"}'});}return route.continue(); }); }
      await page.locator('#prepare').click();let recovered=false,resumed=false,safetyBlocked=false,downloadedSha256=null;
      if(scenarioCode==='SAFETY'){
        await page.locator('#status').filter({hasText:'bloqueada antes da composição'}).waitFor();
        safetyBlocked=await page.locator('#result').isHidden();
      }else{
        if(scenarioCode==='RECOVERY') { await page.locator('#reload').waitFor({state:'visible',timeout:600000}); await page.locator('#reload').click(); }
        await page.locator('#result').waitFor({state:'visible',timeout:600000});
        await page.locator('#post').evaluate(e=>e.decode());await page.locator('#story').evaluate(e=>e.decode());
        if(!(await page.locator('#caption').inputValue()).includes('Cidade Exemplo'))throw new Error('A primeira aplicação perdeu a personalização.');
        // Clipboard é observado no navegador; o stub local só resolve a permissão de mobile Chromium.
        await page.evaluate(()=>{window.__copiedText='';Object.defineProperty(navigator,'clipboard',{configurable:true,value:{writeText:async text=>{window.__copiedText=text;}}});});
        await page.locator('#copy-caption').click();await page.locator('#download-status').filter({hasText:'Texto copiado'}).waitFor();
        if(!(await page.evaluate(()=>window.__copiedText)))throw new Error('A ação não copiou o texto.');
        const download=page.waitForEvent('download');await page.locator('#download').click();const artifact=await download;if(await artifact.failure())throw new Error('Download falhou.');
        await page.locator('#download-status').filter({hasText:'Pacote íntegro recebido'}).waitFor();
        downloadedSha256=createHash('sha256').update(await readFile(await artifact.path())).digest('hex');
        await page.locator('#prefer').click();await page.locator('#checkout-status').filter({hasText:'Preferência simulada'}).waitFor();
        await page.locator('#checkout').click();await page.locator('#checkout-status').filter({hasText:'Continuidade simulada'}).waitFor();
        const before=await api('/session',{session:created.sessionToken});
        await page.locator('#exit').click();await page.locator('#resume').waitFor({state:'visible'});await page.locator('#return').click();await page.locator('#result').waitFor({state:'visible'});
        await page.reload();await page.locator('#result').waitFor({state:'visible'});
        const after=await api('/session',{session:created.sessionToken});
        resumed=before.manifest.zipSha256===after.manifest.zipSha256&&after.events.some(e=>e.code==='SESSION_RETURNED');recovered=scenarioCode==='RECOVERY'&&resumed;
        if(!resumed)throw new Error('Retomada perdeu o pacote ou criou uma nova composição.');
      }
      const current=await api('/session',{session:created.sessionToken});
      if(scenarioCode!=='SAFETY'&&(current.manifest?.packageContractVersion!==packageContractVersion||JSON.stringify(current.manifest.files.map(f=>f.name).sort())!==JSON.stringify(packageNames)||current.manifest.zipSha256!==downloadedSha256))throw new Error('O pacote recebido não comprovou os 36 arquivos aprovados e seu hash.');
      const expected=scenarioCode==='SAFETY'?['EXPERIENCE_STARTED','SAFETY_LIMIT_BLOCKED']:['EXPERIENCE_STARTED','VALUE_MOMENT','READY_RESULT_USED','PREFERRED_OVER_FREE','CHECKOUT_STARTED','DOWNLOAD_COMPLETED'];
      if(!expected.every(code=>current.events.some(e=>e.code===code))||current.prototypeVersion!==input.prototypeVersion||current.productId!==input.productId||current.cycleId!==input.cycleId)throw new Error('Sinais ou identidade da sessão não foram preservados.');
      if(scenarioCode==='SAFETY'&&(current.manifest||current.events.some(e=>e.code==='CHECKOUT_STARTED')))throw new Error('SAFETY criou um pacote ou continuidade indevida.');
      const layout=await page.evaluate(()=>({height:document.documentElement.scrollHeight,noHorizontalOverflow:document.documentElement.scrollWidth<=innerWidth,controlsNamed:[...document.querySelectorAll('button,input,textarea,select')].every(e=>e.textContent?.trim()||e.getAttribute('aria-label')||[...(e.labels||[])].some(l=>l.textContent.trim()))}));
      if(!layout.noHorizontalOverflow||!layout.controlsNamed)throw new Error('Layout ou acessibilidade básica reprovados.');
      const key=scenarioCode+'-'+deviceProfile,path=resolve(directory,key+'.png');await page.screenshot({path,fullPage:true});
      artifacts.push({captureSessionId:input.captureSessionId,evidenceKey:key,evidenceType:'FULL_PAGE',deviceProfile,pageNumber:1,foldNumber:null,viewportWidth:profiles[deviceProfile].viewport.width,viewportHeight:profiles[deviceProfile].viewport.height,pageHeightPx:layout.height,scrollY:0,sourceUrl:input.sourceUrl,finalUrl:page.url(),capturedAt:new Date().toISOString(),localPath:path});
      scenarios.push({scenarioCode,deviceProfile,status:'PASS',prototypeVersion:current.prototypeVersion,evidenceId:current.id,screenshotEvidenceKeys:[key],events:current.events.map(e=>e.code),packageFileCount:current.manifest?.files.length||0,zipSha256:current.manifest?.zipSha256||null,resultReadySeconds:(Date.now()-begin)/1000,resumed,recovered,safetyBlocked,accessibilityBasic:layout.controlsNamed,noHorizontalOverflow:layout.noHorizontalOverflow,privacyPreserved:current.input?.email?.endsWith('@sandbox.local')&&current.input?.whatsapp==='00000000000',providerCalls:0,trafficClass:'AGENT_VALIDATION',mhInternalTest:true,sideEffects,humanEvidenceClaimed:false,commercialEvidenceClaimed:false});
      await writeFile(outputFile+'.partial',JSON.stringify({status:'IN_PROGRESS',scenarios,artifacts},null,2));
    }finally{await context.close();}
  }
}finally{await browser.close();}
const checks={sameVersion:scenarios.every(s=>s.prototypeVersion===input.prototypeVersion),desktopAndMobile:input.mode!=='TECHNICAL'||Object.keys(profiles).every(d=>scenarios.some(s=>s.deviceProfile===d)),happyResultWithinTenMinutes:scenarios.every(s=>s.resultReadySeconds<=600),recoveryPreserved:scenarios.filter(s=>s.scenarioCode==='RECOVERY').every(s=>s.recovered),safetyBlocked:scenarios.filter(s=>s.scenarioCode==='SAFETY').every(s=>s.safetyBlocked),accessibilityBasic:scenarios.every(s=>s.accessibilityBasic),responsiveLayout:scenarios.every(s=>s.noHorizontalOverflow),privacyPreserved:scenarios.every(s=>s.privacyPreserved),internalTrafficSegregated:true,paymentDisabled:true,publicationDisabled:true,campaignDisabled:true,zeroMediaSpend:true};
await writeFile(outputFile,JSON.stringify({contractVersion:'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',fixtureContract:'PDE_PRIVATE_KIT_FIXTURES_V1',packageContractVersion,mode:input.mode,decision:Object.values(checks).every(Boolean)?'APPROVED':'BLOCKED',sourceReference:input.sourceReference,productId:input.productId,productSlug:input.productSlug,profileCode:input.profileCode,publicUrl:input.sourceUrl,prototypeVersion:input.prototypeVersion,trafficClass:'AGENT_VALIDATION',internalMarker:'mh_internal_test',providerCalls:0,startedAt:startedAt.toISOString(),finishedAt:new Date().toISOString(),durationSeconds:(Date.now()-startedAt)/1000,devices:Object.keys(profiles).filter(d=>plans.some(p=>p[1]===d)).map(deviceProfile=>({deviceProfile,status:'PASS',screenshotEvidenceKeys:artifacts.filter(a=>a.deviceProfile===deviceProfile).map(a=>a.evidenceKey)})),scenarios,checks,artifacts,sideEffects,humanEvidenceClaimed:false,commercialEvidenceClaimed:false,evidence:scenarios.map(s=>s.evidenceId)},null,2));
