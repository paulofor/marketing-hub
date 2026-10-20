import { completeVegaFixture, fixtureModel } from './vega-deterministic-fixture.mjs';
import { isTerminalResultConsistent } from './private-result-state-checks.mjs';
import { completeTerminalMatrix, validTerminalResult } from './private-terminal-result-evidence.mjs';
import { verifyVideoIdentity, verifyIntegratedPage, videoBrowserOptions } from './learning-cycle-video-checks.mjs';
import {chromium,devices} from 'playwright-core';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import assert from 'node:assert/strict';
const [inputPath,outputPath,evidenceDirectory]=process.argv.slice(2);
const input=JSON.parse(await readFile(inputPath,'utf8'));
const base='/api/pde/vega/private/v1';
const internal=process.env.PDE_INTERNAL_API_TOKEN;
if(!internal || !input.cycleId)throw new Error('Credencial e ciclo explícitos são necessários para o harness do Vega.');
const api=async(path,body)=>{
 const response=await fetch(new URL(base+path,input.sourceUrl),{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json','X-PDE-Internal-Token':internal},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(30000)});
 if(!response.ok)throw new Error(`Vega harness ${path}: HTTP ${response.status}`);
 const bodyText=await response.text();return bodyText?JSON.parse(bodyText):null;
};
const profiles={DESKTOP_1440:{viewport:{width:1440,height:900}},IPHONE_15_PRO:{...devices['iPhone 15 Pro'],defaultBrowserType:'chromium'},PIXEL_7:{...devices['Pixel 7']}};
const plans=input.mode==='TECHNICAL'?[['ADHERENT','DESKTOP_1440'],['ADHERENT','IPHONE_15_PRO'],['ADHERENT','PIXEL_7'],['RECOVERY','IPHONE_15_PRO'],['SAFETY','PIXEL_7']]:[[input.scenarioCode,input.scenarioCode==='SAFETY'?'PIXEL_7':input.scenarioCode==='RECOVERY'?'IPHONE_15_PRO':'DESKTOP_1440']];
const browser=await chromium.launch(videoBrowserOptions());
await mkdir(evidenceDirectory,{recursive:true});
const startedAt=new Date();const scenarios=[],artifacts=[];
const terminalCases=[];
const terminalProofRequired=input.mode==='TECHNICAL' && Number(input.prototypeVersion.match(/-v(\d+)-/)?.[1])>=14;

// Encerra pela interface e compara o estado persistido, a mensagem e o cartão após recarregar.
async function observeTerminal(page,session,deviceProfile,generationStatus,savedResultConfirmed=null){
 const finish=page.getByRole('button',{name:'Encerrar leitura',exact:true});
 if(await finish.count()){
  const response=page.waitForResponse(r=>r.url().endsWith(base+'/finish')&&r.request().method()==='POST'&&r.ok());
  await finish.click();await response;
 }
 await page.locator('footer [role="status"]').waitFor();
 const state=await (await page.request.get(new URL(base+'/session',input.sourceUrl).href,{headers:{'X-Vega-Session':session.sessionToken}})).json();
 const terminalMessage=await page.locator('footer [role="status"]').innerText();
 const renderedResultPresent=await page.locator('.card-result').count()===1;
 await page.reload({waitUntil:'domcontentloaded'});await page.locator('footer [role="status"]').waitFor();
 const reloaded=await (await page.request.get(new URL(base+'/session',input.sourceUrl).href,{headers:{'X-Vega-Session':session.sessionToken}})).json();
 const messages={COMPLETED:/Seu ajuste continua disponível/,BLOCKED:/pedido foi bloqueado e nenhum ajuste foi gerado/i,FAILED:/Não foi possível gerar um ajuste/,NOT_STARTED:/Nenhum ajuste foi gerado/i};
 const item={deviceProfile,generationStatus,sessionState:state.state,reloadedSessionState:reloaded.state,
  resultPresent:!!state.card,renderedResultPresent,reloadedResultPresent:await page.locator('.card-result').count()===1,
  savedResultConfirmed,terminalMessage,reloadedMessage:await page.locator('footer [role="status"]').innerText(),
  messageMatchesResult:messages[generationStatus].test(terminalMessage)&& (generationStatus==='COMPLETED'||!/Seu ajuste continua disponível/.test(terminalMessage)),
  persistedGenerationStatus:state.generationStatus||'NOT_STARTED',evidenceId:session.id,prototypeVersion:state.prototypeVersion};
 assert.equal(item.persistedGenerationStatus,generationStatus);
 assert.equal(reloaded.card?.cardId,state.card?.cardId);
 assert.equal(validTerminalResult(item),true,'O encerramento precisa corresponder ao estado e resultado realmente persistidos');
 return {...item,status:'PASS',screenshotEvidenceKeys:[`vega-${session.id}`]};
}
let videoIdentity=null;
const videoResults=[];
try {
videoIdentity=input.videoIntegration?await verifyVideoIdentity(input.videoIntegration):null;
for(const [scenarioCode,deviceProfile] of plans){
 const session=await api('/internal/sessions',{cycleId:input.cycleId,prototypeVersion:input.prototypeVersion,origin:'AGENT_VALIDATION'});
 assert.equal(session.prototypeVersion,input.prototypeVersion);assert.equal(session.productId,input.productId);assert.equal(session.experimentId,Number(input.sourceReference.split(':')[1]));
 const context=await browser.newContext(profiles[deviceProfile]);const page=await context.newPage();const failures=[];
 page.on('pageerror',error=>failures.push(error.message));
 await context.addInitScript(token=>localStorage.setItem('vega-private-session-v1',token),session.sessionToken);
 await page.goto(input.sourceUrl,{waitUntil:'domcontentloaded'});
 if(input.videoIntegration)videoResults.push(await verifyIntegratedPage(page,input.videoIntegration,session));
 const start=page.getByRole('button',{name:'Começar',exact:true});
 await start.focus();
 const accessibilityBasic=(await page.locator('html').getAttribute('lang'))==='pt-BR' && await start.evaluate(element=>document.activeElement===element);
 assert.ok(accessibilityBasic,'Idioma e foco de teclado são necessários');
 await page.keyboard.press('Enter');
 await page.getByLabel('Ocasião').selectOption({label:'Almoço informal'});
 await page.getByLabel('Qual roupa').fill('Camisa branca e calça preta que já possuo');
 if(scenarioCode==='SAFETY')await page.getByLabel('Algo que gostaria').fill('Quero comprar outra roupa para conseguir esse resultado');
 let recovered=false,resumed=false;
 if(scenarioCode==='RECOVERY'){
  await page.route('**'+base+'/generate',route=>route.abort('failed'),{times:1});
  await page.getByRole('button',{name:'Criar meu ajuste',exact:true}).click();
  await page.getByRole('alert').waitFor();assert.match(await page.getByLabel('Qual roupa').inputValue(),/Camisa branca/);recovered=true;
 }
 const generatedAt=Date.now();
 const generated=page.waitForResponse(r=>r.url().endsWith(base+'/generate')&&r.request().method()==='POST'&&r.ok());
 await page.getByRole('button',{name:'Criar meu ajuste',exact:true}).click();
 const queued=await (await generated).json();
 await completeVegaFixture(api,input,session,queued.executionId);
 let first;
 if(scenarioCode==='SAFETY'){
  await page.getByRole('heading',{name:'Podemos ajustar o caminho'}).waitFor({timeout:360000});
  assert.match(await page.locator('main').innerText(),/apenas a roupa que você já possui/);
  assert.equal(await page.locator('.card-result').count(),0);
  await page.getByRole('button',{name:'Encerrar leitura'}).click();
  await page.getByText('Leitura encerrada.').waitFor();
  assert.match(await page.locator('main').innerText(),/Não recomenda compras/);
 }else{
  await page.locator('.card-result').waitFor({timeout:360000});
  first=await page.locator('.card-result h2').innerText();assert.ok(first.length>10);
  await page.getByRole('button',{name:'Entendi e consigo aplicar'}).click();
  await page.getByLabel('Depois de aplicar').fill('A aplicação ficou clara e confortável para esta ocasião. Resposta sintética do harness.');
  await page.getByRole('button',{name:'Apliquei este ajuste',exact:true}).click();
  await page.getByRole('button',{name:'Usar um cartão como este',exact:true}).click();
  await page.getByText('Sua preferência foi registrada.').waitFor();
  await page.getByRole('button',{name:'Explorar simulação sem cobrança'}).click();
  await page.getByRole('heading',{name:'Simulação concluída'}).waitFor();
  await page.reload({waitUntil:'domcontentloaded'});await page.locator('.card-result').waitFor();
  assert.equal(await page.locator('.card-result h2').innerText(),first);resumed=true;
  await page.getByRole('button',{name:'Meu ajuste salvo'}).click();
  await page.getByText('Este é o mesmo ajuste').waitFor();
 }
 if(terminalProofRequired && scenarioCode!=='RECOVERY')terminalCases.push(await observeTerminal(page,session,deviceProfile,scenarioCode==='SAFETY'?'BLOCKED':'COMPLETED',scenarioCode==='SAFETY'?null:true));
 const report=await api(`/internal/cycles/${input.cycleId}/report`);const evidence=report.readings.find(r=>r.id===session.id);assert.ok(evidence);
 const terminalResultConsistent=isTerminalResultConsistent(evidence,await page.locator('footer').innerText(),{resultAvailable:/Seu ajuste continua disponível/,noResult:/nenhum ajuste foi gerado|Não foi possível gerar um ajuste/i});
 const scenarioInput=evidence.executions[0]?.context?.input;
 assert.ok(scenarioInput,'O cenário precisa preservar a entrada realmente executada');
 const expected=scenarioCode==='SAFETY'?['EXPERIENCE_STARTED']:['EXPERIENCE_STARTED','VALUE_MOMENT','READY_RESULT_USED','PREFERRED_OVER_FREE','CHECKOUT_STARTED'];
 assert.deepEqual(Object.keys(evidence.events).sort(),expected.sort());assert.equal(evidence.origin,'AGENT_VALIDATION');assert.equal(evidence.paymentEnabled,false);assert.equal(evidence.published,false);assert.equal(evidence.mediaSpendBrl,0);
 assert.equal(evidence.executions.length,1);assert.equal(evidence.executions[0].status,scenarioCode==='SAFETY'?'BLOCKED':'COMPLETED');
 assert.equal(failures.length,0);const noHorizontalOverflow=await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1);assert.ok(noHorizontalOverflow);
 const privacyPreserved=!page.url().includes(session.sessionToken)&&!page.url().includes('#');assert.ok(privacyPreserved);
 const screenshotPath=resolve(evidenceDirectory,`${scenarioCode}-${deviceProfile}.png`);await page.screenshot({path:screenshotPath,fullPage:true});const viewport=page.viewportSize();
 const evidenceKey=`vega-${session.id}`;
 artifacts.push({captureSessionId:input.captureSessionId,evidenceKey,evidenceType:'FULL_PAGE',deviceProfile,pageNumber:1,foldNumber:null,viewportWidth:viewport.width,viewportHeight:viewport.height,pageHeightPx:await page.evaluate(()=>document.documentElement.scrollHeight),scrollY:0,sourceUrl:input.sourceUrl,finalUrl:page.url(),capturedAt:new Date().toISOString(),localPath:screenshotPath});
 scenarios.push({scenarioCode,deviceProfile,status:terminalResultConsistent?'PASS':'BLOCKED',prototypeVersion:input.prototypeVersion,evidenceId:session.id,screenshotEvidenceKeys:[evidenceKey],input:scenarioInput,card:evidence.card,events:evidence.events,trafficClass:'AGENT_VALIDATION',mhInternalTest:true,resultReadySeconds:Math.ceil((Date.now()-generatedAt)/1000),resumed,recovered,safetyBlocked:scenarioCode==='SAFETY',terminalResultConsistent,accessibilityBasic,noHorizontalOverflow,privacyPreserved,humanEvidenceClaimed:false,commercialEvidenceClaimed:false,sideEffects:{paymentEnabled:false,published:false,campaignCreated:false,mediaSpendBrl:0}});
 await context.close();
}
if(terminalProofRequired){
 for(const deviceProfile of Object.keys(profiles))for(const generationStatus of ['BLOCKED','FAILED','NOT_STARTED']){
  if(terminalCases.some(item=>item.deviceProfile===deviceProfile&&item.generationStatus===generationStatus))continue;
  const session=await api('/internal/sessions',{cycleId:input.cycleId,prototypeVersion:input.prototypeVersion,origin:'AGENT_VALIDATION'});
  assert.equal(session.productId,input.productId);assert.equal(session.experimentId,Number(input.sourceReference.split(':')[1]));assert.equal(session.prototypeVersion,input.prototypeVersion);
  const context=await browser.newContext(profiles[deviceProfile]);const page=await context.newPage();
  await context.addInitScript(token=>localStorage.setItem('vega-private-session-v1',token),session.sessionToken);
  await page.goto(input.sourceUrl,{waitUntil:'domcontentloaded'});
  await page.getByRole('button',{name:'Começar',exact:true}).click();
  if(generationStatus!=='NOT_STARTED'){
   await page.getByLabel('Ocasião').selectOption({label:'Almoço informal'});
   await page.getByLabel('Qual roupa').fill('Camisa branca e calça preta que já possuo');
   if(generationStatus==='BLOCKED')await page.getByLabel('Algo que gostaria').fill('Quero comprar outra roupa');
   const response=page.waitForResponse(r=>r.url().endsWith(base+'/generate')&&r.request().method()==='POST'&&r.ok());
   await page.getByRole('button',{name:'Criar meu ajuste',exact:true}).click();const queued=await(await response).json();
   const status=page.waitForResponse(async r=>r.url().endsWith(base+'/session')&&r.ok()&&(await r.json()).generationStatus===generationStatus);
   await completeVegaFixture(api,input,session,queued.executionId,{fail:generationStatus==='FAILED'});
   await status;await page.getByRole('button',{name:'Encerrar leitura',exact:true}).waitFor();
  }
  const terminal=await observeTerminal(page,session,deviceProfile,generationStatus);
  const report=await api(`/internal/cycles/${input.cycleId}/report`);const reading=report.readings.find(r=>r.id===session.id);
  assert.equal(reading.state,'FINISHED');assert.equal(reading.card??null,null);
  assert.deepEqual(Object.keys(reading.events),['EXPERIENCE_STARTED']);
  assert.equal(reading.executions.length,generationStatus==='NOT_STARTED'?0:1);
  const screenshotPath=resolve(evidenceDirectory,`TERMINAL-${generationStatus}-${deviceProfile}.png`);await page.screenshot({path:screenshotPath,fullPage:true});
  const viewport=page.viewportSize();artifacts.push({captureSessionId:input.captureSessionId,evidenceKey:`vega-${session.id}`,evidenceType:'FULL_PAGE',deviceProfile,pageNumber:1,foldNumber:null,viewportWidth:viewport.width,viewportHeight:viewport.height,pageHeightPx:await page.evaluate(()=>document.documentElement.scrollHeight),scrollY:0,sourceUrl:input.sourceUrl,finalUrl:page.url(),capturedAt:new Date().toISOString(),localPath:screenshotPath});
  terminalCases.push(terminal);await context.close();
 }
 assert.equal(completeTerminalMatrix(terminalCases),true,'A matriz terminal precisa comprovar quatro estados nos três dispositivos');
}
}finally{await browser.close();}
const deviceResults=[...new Set(plans.map(p=>p[1]))].map(deviceProfile=>({deviceProfile,viewportWidth:profiles[deviceProfile].viewport.width,viewportHeight:profiles[deviceProfile].viewport.height,status:'PASS',screenshotEvidenceKeys:artifacts.filter(a=>a.deviceProfile===deviceProfile).map(a=>a.evidenceKey)}));
const checks={sameVersion:scenarios.every(s=>s.prototypeVersion===input.prototypeVersion),desktopAndMobile:input.mode!=='TECHNICAL'||deviceResults.length===3,happyResultWithinTenMinutes:scenarios.filter(s=>s.scenarioCode==='ADHERENT').every(s=>s.resultReadySeconds<=600),recoveryPreserved:input.mode!=='TECHNICAL'||scenarios.some(s=>s.recovered&&s.resumed),safetyBlocked:input.mode!=='TECHNICAL'||scenarios.some(s=>s.safetyBlocked),accessibilityBasic:scenarios.every(s=>s.accessibilityBasic),responsiveLayout:scenarios.every(s=>s.noHorizontalOverflow),privacyPreserved:scenarios.every(s=>s.privacyPreserved),internalTrafficSegregated:scenarios.every(s=>s.trafficClass==='AGENT_VALIDATION'),paymentDisabled:true,publicationDisabled:true,campaignDisabled:true,zeroMediaSpend:true};
if(input.videoIntegration)Object.assign(checks,{videoIdentity:videoIdentity.length===2,videoPlayback:videoResults.length===plans.length,videoOptional:videoResults.every(r=>r.optional),videoFailureRecovery:videoResults.every(r=>r.failureRecovery)});
checks.terminalResultConsistency=scenarios.every(s=>s.terminalResultConsistent);
if(terminalProofRequired)checks.terminalResultCoverage=completeTerminalMatrix(terminalCases);
const finishedAt=new Date();
await writeFile(outputPath,JSON.stringify({cycleId:input.cycleId,generationMode:"DETERMINISTIC_FIXTURE",fixtureModel,modelFunctionalBehaviorClaimed:false,...(terminalProofRequired?{terminalResultEvidence:{contractVersion:'PDE_TERMINAL_RESULT_MATRIX_V1',cases:terminalCases}}:{}),...(input.videoIntegration?{videoIntegrationFingerprint:input.videoIntegration.integrationFingerprint,videoIdentity,videoResults}:{}),contractVersion:'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',mode:input.mode,decision:Object.values(checks).every(Boolean)?'APPROVED':'BLOCKED',sourceReference:input.sourceReference,productId:input.productId,productSlug:input.productSlug,publicUrl:input.sourceUrl,prototypeVersion:input.prototypeVersion,trafficClass:'AGENT_VALIDATION',internalMarker:'mh_internal_test',startedAt:startedAt.toISOString(),finishedAt:finishedAt.toISOString(),durationSeconds:Math.ceil((finishedAt-startedAt)/1000),devices:deviceResults,scenarios,checks,artifacts,sideEffects:{paymentEnabled:false,published:false,campaignCreated:false,mediaSpendBrl:0},humanEvidenceClaimed:false,commercialEvidenceClaimed:false,evidence:scenarios.map(s=>s.evidenceId)}));
