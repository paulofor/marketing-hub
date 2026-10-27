import { createRequire } from 'node:module';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';
import { validateKitSafety, validatePreservedKitSafety } from './private-kit-safety-contract.mjs';
import { validateFirstKitActions } from './private-kit-first-actions-contract.mjs';
import { checkCustomerSurface } from './customer-surface-contract.mjs';
import { verifyVideoIdentity, verifyIntegratedPage, videoBrowserOptions } from './learning-cycle-video-checks.mjs';
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
const verifiedVideos=input.videoIntegration?await verifyVideoIdentity(input.videoIntegration):null;
const browser=await chromium.launch(videoBrowserOptions());
async function api(path,{method='GET',body,session}={}) {
  const response=await fetch(base+path,{method,headers:session?{'X-Kit-Session':session,'Content-Type':'application/json'}:{'X-PDE-Internal-Token':token,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  if(!response.ok)throw new Error('Contrato privado recusou '+path+' · HTTP '+response.status);
  return response.status===204?null:response.json();
}
async function observeSafety(page) {
  return {title:await page.locator('#title').innerText(),introduction:await page.locator('#introduction').innerText(),reason:await page.locator('#status').innerText(),nextStep:await page.locator('#next-step').innerText(),actionLabel:await page.locator('#review-cycle').innerText(),nextActionUrl:await page.locator('#review-cycle').getAttribute('href'),actionVisible:await page.locator('#review-cycle').isVisible(),resultHidden:await page.locator('#result').isHidden()};
}
// Mede a tela realmente exibida, sem rolar o navegador para fazer o critério passar.
async function checkFirstActions(page) {
  validateFirstKitActions(await page.evaluate(() => ({
    viewport: { width: innerWidth, height: innerHeight },
    testControlsClosed: !document.querySelector('#test-controls'),
    actions: ['copy-caption', 'download'].map(id => {
      const element = document.getElementById(id);
      if (!element) return { id, hidden: true };
      const rect = element.getBoundingClientRect();
      return { id, label: element.textContent, x: rect.x, y: rect.y, width: rect.width, height: rect.height,
        hidden: element.getClientRects().length === 0, disabled: element.disabled };
    }),
  })));
}
// Preserva momentos reais do percurso; recortes editoriais posteriores mantêm os bytes originais.
async function captureMoment(page, deviceProfile, key) {
  if(artifacts.some(a=>a.evidenceKey===key))throw new Error('Momento já capturado nesta sessão: '+key);
  const path=resolve(directory,key+'.png');
  const height=await page.evaluate(()=>document.documentElement.scrollHeight);
  await page.screenshot({path,fullPage:true});
  artifacts.push({captureSessionId:input.captureSessionId,evidenceKey:key,evidenceType:'FULL_PAGE',deviceProfile,pageNumber:1,foldNumber:null,viewportWidth:profiles[deviceProfile].viewport.width,viewportHeight:profiles[deviceProfile].viewport.height,pageHeightPx:height,scrollY:0,sourceUrl:input.sourceUrl,finalUrl:page.url(),capturedAt:new Date().toISOString(),localPath:path});
  return key;
}
try {
  for(const [scenarioCode,deviceProfile] of plans){
    const safetyCase=scenarioCode==='SAFETY'?(deviceProfile==='IPHONE_15_PRO'?'EXTERNAL_ACTION':'UNVERIFIED_VISUAL_ORIGIN'):undefined;
    const created=await api('/internal/sessions',{method:'POST',body:{requestKey:crypto.randomUUID(),productId:input.productId,cycleId:input.cycleId,prototypeVersion:input.prototypeVersion,scenarioCode,deviceProfile,...(safetyCase?{safetyCase}:{})}});
    const context=await browser.newContext({...profiles[deviceProfile],acceptDownloads:true});
    const page=await context.newPage();const begin=Date.now(),momentKeys=[];
    try {
      await page.addInitScript(secret=>sessionStorage.setItem('private-kit-session-v1',secret),created.sessionToken);
      await page.goto(input.sourceUrl);await page.locator('#briefing').waitFor({state:'visible'});
      await checkCustomerSurface(page);
      const videoPlayback=input.videoIntegration?await verifyIntegratedPage(page,input.videoIntegration,created.session,{
        startName:'Preparar meu kit de exemplo',videoLabel:'Demonstração do kit de divulgação',
        failureMessage:'O vídeo não abriu. Você pode continuar e usar seu kit normalmente.',
      }):null;
      if(['beauty-v1','beauty-v2'].includes(input.profileCode) && input.specialtyCode) await page.locator('#specialty').selectOption(input.specialtyCode);
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
      if(input.mode==='TECHNICAL'&&scenarioCode==='ADHERENT')momentKeys.push(await captureMoment(page,deviceProfile,scenarioCode+'-'+deviceProfile+'-BRIEFING'));
      await page.locator('#prepare').click();let recovered=false,resumed=false,safetyBlocked=false,safetyOutcome=null,downloadedSha256=null;
      if(scenarioCode==='SAFETY'){
        await page.locator('#status').filter({hasText:'bloqueada antes da composição'}).waitFor();
        await page.locator('#blocked').waitFor({state:'visible'});
        const blocked=await api('/session',{session:created.sessionToken});
        const safetyContext={productId:input.productId,cycleId:input.cycleId,safetyCase};
        validateKitSafety(blocked,await observeSafety(page),safetyContext);
        await page.reload();await page.locator('#blocked').waitFor({state:'visible'});
        const preserved=await api('/session',{session:created.sessionToken});
        safetyOutcome=validatePreservedKitSafety(blocked,preserved,await observeSafety(page),safetyContext);safetyBlocked=true;
      }else{
        if(scenarioCode==='RECOVERY') { await page.locator('#reload').waitFor({state:'visible',timeout:600000}); await page.locator('#reload').click(); }
        await page.locator('#result').waitFor({state:'visible',timeout:600000});
        await page.locator('#post').evaluate(e=>e.decode());await page.locator('#story').evaluate(e=>e.decode());
        await checkFirstActions(page);
        if(input.profileCode?.endsWith('-v2')) {
          const proof=await api('/session',{session:created.sessionToken});
          const origin=proof.manifest?.visualReferences;
          if(origin?.contractVersion!=='KIT_LICENSED_REFERENCE_V1'||origin.profileCode!==proof.input.specialtyCode&&origin.profileCode!==proof.profileCode||origin.commercialApproval!==false||origin.assets?.length!==5)throw new Error('Origem visual não comprovada na demonstração.');
          await page.locator('#enlarge-post').click();
          await page.locator('#art-dialog').waitFor({state:'visible'});
          await page.locator('#enlarged-art').evaluate(e=>e.decode());
          momentKeys.push(await captureMoment(page,deviceProfile,scenarioCode+'-'+deviceProfile+'-ART'));
          await page.locator('#close-art').click();
          if(await page.locator('#art-dialog').isVisible())throw new Error('Ampliação não fechou.');
        }
        if(!(await page.locator('#caption').inputValue()).includes('Cidade Exemplo'))throw new Error('A primeira aplicação perdeu a personalização.');
        // Clipboard é observado no navegador; o stub local só resolve a permissão de mobile Chromium.
        await page.evaluate(()=>{window.__copiedText='';Object.defineProperty(navigator,'clipboard',{configurable:true,value:{writeText:async text=>{window.__copiedText=text;}}});});
        await page.locator('#copy-caption').click();await page.locator('#download-status').filter({hasText:'Texto copiado'}).waitFor();
        if(!(await page.evaluate(()=>window.__copiedText)))throw new Error('A ação não copiou o texto.');
        const download=page.waitForEvent('download');await page.locator('#download').click();const artifact=await download;if(await artifact.failure())throw new Error('Download falhou.');
        await page.locator('#download-status').filter({hasText:'Kit recebido'}).waitFor();
        downloadedSha256=createHash('sha256').update(await readFile(await artifact.path())).digest('hex');
        if(input.mode==='TECHNICAL'&&scenarioCode==='ADHERENT') {
          await page.locator('details').filter({has:page.locator('#calendar')}).locator('summary').click();
          if(!(await page.locator('#calendar').innerText()).trim()||!(await page.locator('#message').inputValue()).trim())throw new Error('Conteúdo narrado não ficou disponível para captura.');
          momentKeys.push(await captureMoment(page,deviceProfile,scenarioCode+'-'+deviceProfile+'-CONTENT'));
          await page.locator('details').filter({has:page.locator('#calendar')}).locator('summary').click();
        }
        // Os sinais sintéticos são exercitados pelo executor; não viram botões da cliente.
        for(const code of ['PREFERRED_OVER_FREE','CHECKOUT_STARTED'])await api('/events',{method:'POST',session:created.sessionToken,body:{eventId:crypto.randomUUID(),code}});
        const before=await api('/session',{session:created.sessionToken});
        await page.locator('#exit').click();await page.locator('#resume').waitFor({state:'visible'});await page.locator('#return').click();await page.locator('#result').waitFor({state:'visible'});
        await page.reload();await page.locator('#result').waitFor({state:'visible'});
        await checkFirstActions(page);
        const after=await api('/session',{session:created.sessionToken});
        resumed=before.manifest.zipSha256===after.manifest.zipSha256&&after.events.some(e=>e.code==='SESSION_RETURNED');recovered=scenarioCode==='RECOVERY'&&resumed;
        if(!resumed)throw new Error('Retomada perdeu o pacote ou criou uma nova composição.');
      }
      const current=await api('/session',{session:created.sessionToken});
      const customerSurface=await checkCustomerSurface(page);
      if(scenarioCode!=='SAFETY'&&(current.manifest?.packageContractVersion!==packageContractVersion||JSON.stringify(current.manifest.files.map(f=>f.name).sort())!==JSON.stringify(packageNames)||current.manifest.zipSha256!==downloadedSha256))throw new Error('O pacote recebido não comprovou os 36 arquivos aprovados e seu hash.');
      const expected=scenarioCode==='SAFETY'?['EXPERIENCE_STARTED','SAFETY_LIMIT_BLOCKED']:['EXPERIENCE_STARTED','VALUE_MOMENT','READY_RESULT_USED','PREFERRED_OVER_FREE','CHECKOUT_STARTED','DOWNLOAD_COMPLETED'];
      if(!expected.every(code=>current.events.some(e=>e.code===code))||current.prototypeVersion!==input.prototypeVersion||current.productId!==input.productId||current.cycleId!==input.cycleId)throw new Error('Sinais ou identidade da sessão não foram preservados.');
      if(scenarioCode==='SAFETY'&&(current.manifest||current.events.some(e=>e.code==='CHECKOUT_STARTED')))throw new Error('SAFETY criou um pacote ou continuidade indevida.');
      const layout=await page.evaluate(()=>({height:document.documentElement.scrollHeight,noHorizontalOverflow:document.documentElement.scrollWidth<=innerWidth,controlsNamed:[...document.querySelectorAll('button,input,textarea,select')].every(e=>e.textContent?.trim()||e.getAttribute('aria-label')||[...(e.labels||[])].some(l=>l.textContent.trim()))}));
      if(!layout.noHorizontalOverflow||!layout.controlsNamed)throw new Error('Layout ou acessibilidade básica reprovados.');
      const key=await captureMoment(page,deviceProfile,scenarioCode+'-'+deviceProfile);
      scenarios.push({scenarioCode,deviceProfile,status:'PASS',prototypeVersion:current.prototypeVersion,evidenceId:current.id,screenshotEvidenceKeys:[...momentKeys,key],events:current.events.map(e=>e.code),visualReferences:current.manifest?.visualReferences||null,packageFileCount:current.manifest?.files.length||0,zipSha256:current.manifest?.zipSha256||null,resultReadySeconds:(Date.now()-begin)/1000,resumed,recovered,safetyBlocked,safetyOutcome,videoPlayback,customerSurface,accessibilityBasic:layout.controlsNamed,noHorizontalOverflow:layout.noHorizontalOverflow,privacyPreserved:current.input?.email?.endsWith('@sandbox.local')&&current.input?.whatsapp==='00000000000',providerCalls:0,trafficClass:'AGENT_VALIDATION',mhInternalTest:true,sideEffects,humanEvidenceClaimed:false,commercialEvidenceClaimed:false});
      await writeFile(outputFile+'.partial',JSON.stringify({status:'IN_PROGRESS',scenarios,artifacts},null,2));
    }finally{await context.close();}
  }
}finally{await browser.close();}
const checks={sameVersion:scenarios.every(s=>s.prototypeVersion===input.prototypeVersion),desktopAndMobile:input.mode!=='TECHNICAL'||Object.keys(profiles).every(d=>scenarios.some(s=>s.deviceProfile===d)),happyResultWithinTenMinutes:scenarios.every(s=>s.resultReadySeconds<=600),recoveryPreserved:scenarios.filter(s=>s.scenarioCode==='RECOVERY').every(s=>s.recovered),safetyBlocked:scenarios.filter(s=>s.scenarioCode==='SAFETY').every(s=>s.safetyBlocked),accessibilityBasic:scenarios.every(s=>s.accessibilityBasic),responsiveLayout:scenarios.every(s=>s.noHorizontalOverflow),privacyPreserved:scenarios.every(s=>s.privacyPreserved),internalTrafficSegregated:true,paymentDisabled:true,publicationDisabled:true,campaignDisabled:true,zeroMediaSpend:true};
if(input.videoIntegration)Object.assign(checks,{videoIdentity:verifiedVideos.length===2,videoPlayback:scenarios.every(s=>s.videoPlayback?.hero&&s.videoPlayback?.campaign),videoOptional:scenarios.every(s=>s.videoPlayback?.optional),videoFailureRecovery:scenarios.every(s=>s.videoPlayback?.failureRecovery)});
await writeFile(outputFile,JSON.stringify({contractVersion:'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',fixtureContract:'PDE_PRIVATE_KIT_FIXTURES_V1',packageContractVersion,...(input.mode==='TECHNICAL'?{captureMomentsContract:'PDE_KIT_CAPTURE_MOMENTS_V1'}:{}),...(input.videoIntegration?{videoIntegrationFingerprint:input.videoIntegration.integrationFingerprint,verifiedVideos}:{}),mode:input.mode,decision:Object.values(checks).every(Boolean)?'APPROVED':'BLOCKED',sourceReference:input.sourceReference,productId:input.productId,cycleId:input.cycleId,productSlug:input.productSlug,profileCode:input.profileCode,publicUrl:input.sourceUrl,prototypeVersion:input.prototypeVersion,trafficClass:'AGENT_VALIDATION',internalMarker:'mh_internal_test',providerCalls:0,startedAt:startedAt.toISOString(),finishedAt:new Date().toISOString(),durationSeconds:(Date.now()-startedAt)/1000,devices:Object.keys(profiles).filter(d=>plans.some(p=>p[1]===d)).map(deviceProfile=>({deviceProfile,status:'PASS',screenshotEvidenceKeys:artifacts.filter(a=>a.deviceProfile===deviceProfile).map(a=>a.evidenceKey)})),scenarios,checks,artifacts,sideEffects,humanEvidenceClaimed:false,commercialEvidenceClaimed:false,evidence:scenarios.map(s=>s.evidenceId)},null,2));
