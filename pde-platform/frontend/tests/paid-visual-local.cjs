const assert=require("node:assert/strict");
const fs=require("node:fs/promises");
const path=require("node:path");
const {chromium,devices}=require("playwright");
const {pathToFileURL}=require("node:url");
(async()=>{
 const root=path.resolve(__dirname,"../../..");
 const {VisualPersonalizationWorker}=await import(pathToFileURL(path.join(root,"pde-platform/pde-ai-worker/src/visual-personalization-v1.js")));
 const image=(await (await fetch("http://127.0.0.1:8189/_fixture/image")).json()).image;
 let calls=0;
 const fetchImpl=async(url,options)=>{
   if(url==='https://api.openai.com/v1/responses') {
     calls++;const request=JSON.parse(options.body);
     return new Response(JSON.stringify({id:'resp-local-'+calls,status:'completed',metadata:request.metadata,
       output:[{type:'image_generation_call',status:'completed',result:image}]}),{status:200});
   }
   return fetch(url,options);
 };
 const spoolDir=path.join(root,'artifacts','paid-visual-spool');
 const worker=new VisualPersonalizationWorker({backendUrl:'http://127.0.0.1:8189',internalToken:'local-worker',apiKey:'local-double',fetchImpl,spoolDir});
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_BIN||process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH||chromium.executablePath(),args:['--no-sandbox']});
 const output=path.join(root,'artifacts','paid-visual-local');await fs.mkdir(output,{recursive:true});
 try {
  for(const [name,options,id] of [['desktop',{viewport:{width:1365,height:900}},97],['iphone',devices['iPhone 15 Pro'],709],['pixel',devices['Pixel 7'],811]]) {
   const context=await browser.newContext(options);const page=await context.newPage();page.setDefaultTimeout(20000);
   const url='http://127.0.0.1:5189/paid-visual.html?experiment='+id;
   await page.goto(url);await page.getByLabel('E-mail da compra').fill('buyer@example.test');await page.getByRole('button',{name:'Receber meu acesso'}).click();
   await page.getByRole('status').filter({hasText:'enviaremos seu acesso'}).waitFor();
   const access=(await (await fetch('http://127.0.0.1:8189/_fixture/access')).json()).access;
   assert.ok(access.length>=32);await page.goto(url+'#access='+access);
   await page.getByRole('heading',{name:'Sua entrega',exact:true}).waitFor();assert.equal(new URL(page.url()).hash,'');
   await page.getByLabel('Qual é a ocasião?').fill('Jantar informal');
   await page.getByLabel('Quando será?').fill(new Date(Date.now()+2*86400000).toISOString().slice(0,16));
   await page.getByLabel('Quais peças você tem?',{exact:false}).fill('Camisa branca\nCalça preta\nTênis branco');
   await page.getByLabel('O que você prefere?').fill('Conforto');await page.getByLabel('Algum cuidado prático?').fill('Sem salto');
   await page.getByRole('button',{name:'Preparar minhas três combinações'}).click();
   await page.getByText('Estamos preparando suas combinações.',{exact:false}).waitFor();
   await worker.processNextPending();await page.getByAltText('Três combinações de roupas para Jantar informal').waitFor();
   const before=calls;await page.reload();await page.getByRole('link',{name:'Baixar minhas combinações'}).waitFor();assert.equal(calls,before);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
   await page.screenshot({path:path.join(output,name+'.png'),fullPage:true});
   await page.getByRole('button',{name:'Usar minha regeneração'}).click();await page.getByLabel('O que você prefere?').fill('Conforto, ambiente frio');
   await page.getByRole('button',{name:'Gerar meu ajuste'}).click();await page.getByText('Estamos preparando suas combinações.',{exact:false}).waitFor();
   await worker.processNextPending();await page.getByRole('link',{name:'Baixar minhas combinações'}).nth(1).waitFor();
   assert.equal(calls,before+1);assert.equal(await page.getByRole('button',{name:'Usar minha regeneração'}).count(),0);
   await context.close();console.log(name+': compra → e-mail → entrada → worker → resultado → recuperação → regeneração PASS');
  }
  assert.equal(calls,6);console.log('6 gerações locais simuladas; 0 chamadas externas.');
 } finally {await browser.close();await fs.rm(spoolDir,{recursive:true,force:true});}
})().catch(error=>{console.error(error);process.exit(1);});
