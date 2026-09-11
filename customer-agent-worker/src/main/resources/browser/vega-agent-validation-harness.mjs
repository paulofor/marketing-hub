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
 if(!response.ok)throw new Error(`Vega harness ${path}: HTTP ${response.status}`);return await response.json();
};
const profiles={DESKTOP_1440:{viewport:{width:1440,height:900}},IPHONE_15_PRO:{...devices['iPhone 15 Pro'],defaultBrowserType:'chromium'},PIXEL_7:{...devices['Pixel 7']}};
const plans=input.mode==='TECHNICAL'?[['ADHERENT','DESKTOP_1440'],['ADHERENT','IPHONE_15_PRO'],['ADHERENT','PIXEL_7'],['RECOVERY','IPHONE_15_PRO'],['SAFETY','PIXEL_7']]:[[input.scenarioCode,input.scenarioCode==='SAFETY'?'PIXEL_7':input.scenarioCode==='RECOVERY'?'IPHONE_15_PRO':'DESKTOP_1440']];
const executablePath=process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH||process.env.CHROMIUM_BIN||process.env.CHROME_BIN;
const browser=await chromium.launch({headless:true,...(executablePath?{executablePath}:{}),args:['--no-sandbox']});
await mkdir(evidenceDirectory,{recursive:true});
const startedAt=new Date();const scenarios=[],artifacts=[];
try {
for(const [scenarioCode,deviceProfile] of plans){
 const session=await api('/internal/sessions',{cycleId:input.cycleId,prototypeVersion:input.prototypeVersion,origin:'AGENT_VALIDATION'});
 assert.equal(session.prototypeVersion,input.prototypeVersion);assert.equal(session.productId,input.productId);assert.equal(session.experimentId,Number(input.sourceReference.split(':')[1]));
 const context=await browser.newContext(profiles[deviceProfile]);const page=await context.newPage();const failures=[];
 page.on('pageerror',error=>failures.push(error.message));
 await context.addInitScript(token=>localStorage.setItem('vega-private-session-v1',token),session.sessionToken);
 await page.goto(input.sourceUrl,{waitUntil:'domcontentloaded'});
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
 await page.getByRole('button',{name:'Criar meu ajuste',exact:true}).click();
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
 const report=await api(`/internal/cycles/${input.cycleId}/report`);const evidence=report.readings.find(r=>r.id===session.id);assert.ok(evidence);
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
 scenarios.push({scenarioCode,deviceProfile,status:'PASS',prototypeVersion:input.prototypeVersion,evidenceId:session.id,screenshotEvidenceKeys:[evidenceKey],input:scenarioInput,card:evidence.card,events:evidence.events,trafficClass:'AGENT_VALIDATION',mhInternalTest:true,resultReadySeconds:Math.ceil((Date.now()-generatedAt)/1000),resumed,recovered,safetyBlocked:scenarioCode==='SAFETY',accessibilityBasic,noHorizontalOverflow,privacyPreserved,humanEvidenceClaimed:false,commercialEvidenceClaimed:false,sideEffects:{paymentEnabled:false,published:false,campaignCreated:false,mediaSpendBrl:0}});
 await context.close();
}
}finally{await browser.close();}
const deviceResults=[...new Set(plans.map(p=>p[1]))].map(deviceProfile=>({deviceProfile,viewportWidth:profiles[deviceProfile].viewport.width,viewportHeight:profiles[deviceProfile].viewport.height,status:'PASS',screenshotEvidenceKeys:artifacts.filter(a=>a.deviceProfile===deviceProfile).map(a=>a.evidenceKey)}));
const checks={sameVersion:scenarios.every(s=>s.prototypeVersion===input.prototypeVersion),desktopAndMobile:input.mode!=='TECHNICAL'||deviceResults.length===3,happyResultWithinTenMinutes:scenarios.filter(s=>s.scenarioCode==='ADHERENT').every(s=>s.resultReadySeconds<=600),recoveryPreserved:input.mode!=='TECHNICAL'||scenarios.some(s=>s.recovered&&s.resumed),safetyBlocked:input.mode!=='TECHNICAL'||scenarios.some(s=>s.safetyBlocked),accessibilityBasic:scenarios.every(s=>s.accessibilityBasic),responsiveLayout:scenarios.every(s=>s.noHorizontalOverflow),privacyPreserved:scenarios.every(s=>s.privacyPreserved),internalTrafficSegregated:scenarios.every(s=>s.trafficClass==='AGENT_VALIDATION'),paymentDisabled:true,publicationDisabled:true,campaignDisabled:true,zeroMediaSpend:true};
const finishedAt=new Date();
await writeFile(outputPath,JSON.stringify({contractVersion:'PDE_AGENT_TECHNICAL_HOMOLOGATION_V1',mode:input.mode,decision:Object.values(checks).every(Boolean)?'APPROVED':'BLOCKED',sourceReference:input.sourceReference,productId:input.productId,productSlug:input.productSlug,publicUrl:input.sourceUrl,prototypeVersion:input.prototypeVersion,trafficClass:'AGENT_VALIDATION',internalMarker:'mh_internal_test',startedAt:startedAt.toISOString(),finishedAt:finishedAt.toISOString(),durationSeconds:Math.ceil((finishedAt-startedAt)/1000),devices:deviceResults,scenarios,checks,artifacts,sideEffects:{paymentEnabled:false,published:false,campaignCreated:false,mediaSpendBrl:0},humanEvidenceClaimed:false,commercialEvidenceClaimed:false,evidence:scenarios.map(s=>s.evidenceId)}));
