const {chromium,devices}=require('playwright');const assert=require('node:assert/strict');const fs=require('fs');
const fixture=require('./fixtures/admin.json');const dir=process.argv[2];assert.ok(dir);fs.mkdirSync(dir,{recursive:true});
(async()=>{const browser=await chromium.launch({headless:true,executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
for(const [profile,options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]){
 let cycle=structuredClone(fixture.cycle);const requests=[],errors=[];const context=await browser.newContext(options);const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api/**',async route=>{const req=route.request(),path=new URL(req.url()).pathname;let data=[];
 if(path==='/api/products')data=[{id:91004,internalName:'Vega QA',name:'MUSA QA'}];
 else if(path==='/api/business-process-chains')data=[{id:91014,name:'Cadeia PDE QA',versionNumber:14}];
 else if(path.endsWith('/learning-cycles/v1/catalog'))data=fixture.catalog;
 else if(path.includes('/learning-cycles/v1/')&&req.method()==='POST'){const payload=req.postDataJSON();requests.push(payload);cycle={...cycle,productVersion:payload.evidence.productVersion,revision:cycle.revision+1};data=cycle;}
 else if(path.includes('/learning-cycles/v1/products/91004'))data=[cycle];
 await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
 });
 await page.goto('http://sandbox-docker:18320/business-process-chains/learning-cycles?chainId=91014&productId=91004&cycleId=91002',{waitUntil:'networkidle'});
 fs.writeFileSync(dir+'/'+profile+'-before.txt',await page.locator('body').innerText());
 await page.getByRole('combobox',{name:/^Decisão/}).selectOption('REWORK');
 await page.getByLabel('A nova versão já foi implementada e testada').check();
 const values={operatorName:'QA local',summary:'Entrega privada verificada localmente',evidenceReference:'repo://homologacao/vega380/QA',rootCause:'Faltava o executável privado',productVersion:'musa-pde-entry-v9-primeiro-ajuste-aplicavel',privateAccessUrl:'https://private.invalid/vega-private',prototypeImage:'repo/vega:qa-v9',prototypeEvidence:'Geração, retomada, segurança e segregação testadas',prototypeObservedAt:'2026-09-11T01:00'};
 for(const [name,value]of Object.entries(values))await page.locator(`[name="${name}"]`).fill(value);
 await page.locator('[name="returnTarget"]').selectOption('70:prototypeCorrection');
 const button=page.getByRole('button',{name:'Devolver para correção',exact:true});await button.click();assert.equal(requests.length,0,'Confirmações essenciais são obrigatórias');
 for(const name of ['technicalOnly','desktopValidated','mobileValidated','firstResultValidated','resumeValidated','failuresValidated','testDataExcluded','noExternalSideEffects'])await page.locator(`[name="${name}"]`).check();
 await button.click();await page.waitForFunction(()=>document.body.innerText.includes('Versão: musa-pde-entry-v9-primeiro-ajuste-aplicavel'));
 assert.equal(requests.length,1);assert.equal(requests[0].evidence.privatePrototype.prototypeVersion,values.productVersion);assert.equal(requests[0].evidence.returnActivityId,'prototypeCorrection');
 assert.equal(errors.length,0,errors.join('\n'));assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
 await page.screenshot({path:dir+'/'+profile+'.png',fullPage:true});await context.close();console.log('PASS entrega privada administrativa',profile);
}
await browser.close();})();
