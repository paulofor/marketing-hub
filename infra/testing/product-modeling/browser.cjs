const { chromium, devices } = require('@playwright/test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const out=path.resolve('artifacts/product-modeling/local');fs.mkdirSync(out,{recursive:true});
const base='http://127.0.0.1:5179';
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',headless:true,args:['--no-sandbox']});
 try {
  for (const [name,options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
   const context=await browser.newContext(options);const page=await context.newPage();const errors=[];
   page.on('pageerror',e=>errors.push(e.message));
   await context.route('**/*',async route=>{const u=new URL(route.request().url());if(!['127.0.0.1','localhost'].includes(u.hostname))return route.abort();return route.continue();});
   // Exercita o fallback necessário para a URL administrativa HTTP.
   await page.addInitScript(()=>{delete Crypto.prototype.randomUUID;});
   await page.goto(base+'/product-modeling?productId=99101');
   await page.getByRole('heading',{name:'Modelar por referências',exact:true}).waitFor().catch(async e=>{await page.screenshot({path:path.join(out,'failure.png'),fullPage:true});console.error(errors,await page.locator('body').innerText());throw e;});
   const beforeAudit=(await (await fetch('http://127.0.0.1:18101/fixture/health')).json()).audit;
   await page.getByLabel('O que a cliente deseja conseguir?').fill('Resultado próprio '+name);
   await page.getByLabel('Links das referências').fill('http://reference.example/');
   await page.getByRole('button',{name:'Salvar preparação'}).click();
   await page.getByRole('alert').filter({hasText:'Não foi possível salvar'}).waitFor();
   await page.getByLabel('Links das referências').fill('https://reference.example/');
   const request=page.waitForRequest(r=>r.url().endsWith('/api/product-modeling/v1')&&r.method()==='POST');
   await page.getByRole('button',{name:'Salvar preparação'}).click();
   assert.match((await request).postDataJSON().requestKey,/^[0-9a-f-]{36}$/);
   await page.getByRole('status').filter({hasText:'Preparação salva'}).waitFor();
   const card=page.locator('article').filter({hasText:'Resultado próprio '+name});
   await card.getByText('Preparada',{exact:true}).waitFor();
   await card.getByText('Referências internacionais → Brasil',{exact:true}).waitFor();
   assert.equal((await (await fetch('http://127.0.0.1:18101/fixture/health')).json()).audit,beforeAudit);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
   await page.screenshot({path:path.join(out,name+'.png'),fullPage:true});assert.deepEqual(errors,[]);
   const models=await (await fetch('http://127.0.0.1:18101/api/product-modeling/v1')).json();
   const current=models.find(m=>m.desiredOutcome==='Resultado próprio '+name);
   await card.getByRole('button',{name:'Preparar início da pesquisa'}).click();
   await card.getByRole('button',{name:'Iniciar pesquisa com Argos'}).click();
   await card.getByText('Na fila de Argos',{exact:true}).waitFor();
   await fetch(`http://127.0.0.1:18101/fixture/fail-collection/${current.discoveryCycleId}`,{method:'POST'});
   await page.reload();
   await card.getByText(/Plano de Argos preservado/).waitFor();
   await card.getByRole('button',{name:'Retomar pesquisa com Argos'}).click();
   await card.getByRole('button',{name:'Confirmar retomada da pesquisa'}).click();
   await card.getByText('Na fila de Argos',{exact:true}).waitFor();
   assert.match(await card.innerText(),/PENDING/);
   await page.screenshot({path:path.join(out,name+'-recovery.png'),fullPage:true});
   await context.close();
  }
  console.log(JSON.stringify({desktop:'PASS',iphone:'PASS',pixel:'PASS',httpUuid:'PASS',draftNoTasks:'PASS',recovery:'PASS'}));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
