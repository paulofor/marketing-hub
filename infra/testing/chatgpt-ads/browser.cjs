/* Homologa a tela real com contratos sintéticos; nunca envia autorização para produção. */
const {chromium,devices}=require('playwright');
const fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',headless:true,args:['--no-sandbox']});
 fs.mkdirSync('artifacts/chatgpt-ads-3783/browser',{recursive:true});
 for(const [name,device] of [['desktop',{viewport:{width:1365,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]){
  const context=await browser.newContext(device);const page=await context.newPage();let campaigns=[];let authorizations=0;
  await page.route('http://localhost:4187/api/**',async route=>{
   const request=route.request();const url=new URL(request.url());let result=[];
   if(url.pathname==='/api/experiments')result=[{id:991,name:'Capella sintética · ChatGPT Ads',status:'PLANNED',platform:'CHATGPT_ADS'}];
   if(url.pathname.endsWith('/ads-in-use'))result=[{id:771,headline:'Seu trabalho merece destaque',primaryText:'Veja uma divulgação pronta para usar.',imageUrl:'https://example.org/sample.png',destinationUrl:'https://example.org/sample',status:'READY',agentReviewStatus:'APPROVED'}];
   if(url.pathname==='/api/chatgpt-ads/v1')result={connection:{status:'active',name:'Conta sintética',currency_code:'BRL'},campaigns};
   if(url.pathname==='/api/chatgpt-ads/v1/campaigns'&&request.method()==='POST'){
    const draft=request.postDataJSON();if(draft.totalBudget!==25||draft.maxCpc!==1)throw Error('Orçamento alterado pela tela');
    result={id:331,experimentId:991,productName:'Capella sintética',status:'DRAFT',draft,fingerprint:'fixture-snapshot',authorized:false,blockers:['Homologação comercial pendente.'],canPublish:false,canActivate:false,canPause:false,remote:{},metrics:{},updatedAt:new Date().toISOString()};campaigns=[result];
   }
   if(url.pathname.endsWith('/authorize')){authorizations++;const payload=request.postDataJSON();if(payload.confirmation!=='AUTORIZO CHATGPT ADS'||payload.fingerprint!=='fixture-snapshot')throw Error('Decisão sem identidade');result={...campaigns[0],authorized:true};campaigns=[result];}
   await route.fulfill({contentType:'application/json',body:JSON.stringify(result)});
  });
  await page.route('https://example.org/**',r=>r.fulfill({contentType:'image/svg+xml',body:'<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640"><rect width="640" height="640" fill="#216658"/><text x="55" y="300" font-size="36" fill="white">Amostra de teste</text></svg>'}));
  await page.goto('http://localhost:4187/chatgpt-ads?experimentId=991');
  await page.getByLabel(/^Experimento \*/).selectOption('991');
  await page.getByLabel(/^Material do experimento \*/).selectOption('771');
  await page.getByLabel(/^Nome desta preparação \*/).fill('Piloto Capella sintético');
  await page.getByLabel(/^Situações em que o produto ajuda \*/).fill('Organizar a divulgação da semana');
  await page.getByLabel(/^Moeda da conta \*/).fill('BRL');
  await page.getByLabel(/^Teto total do teste \*/).fill('25');
  await page.getByLabel(/^Máximo por clique \*/).fill('1');
  await page.getByLabel(/^Início \*/).fill('2030-01-01T10:00');
  await page.getByLabel(/^Fim \*/).fill('2030-01-06T10:00');
  await page.getByRole('button',{name:'Salvar preparação sem publicar'}).click();
  await page.getByRole('heading',{name:'Piloto Capella sintético'}).waitFor();
  if(authorizations!==0)throw Error('Salvar rascunho autorizou gasto');
  if(!(await page.getByRole('button',{name:'Ativar teste autorizado'}).isDisabled()))throw Error('Ativação habilitada sem gates');
  await page.getByRole('button',{name:'Conferir autorização'}).click();
  await page.getByLabel(/^Responsável \*/).fill('Operadora sintética');
  await page.getByRole('checkbox').check();
  await page.getByRole('button',{name:'Registrar autorização'}).click();
  await page.getByRole('heading',{name:'Autorizar este teste no ChatGPT Ads'}).waitFor({state:'hidden'});
  if(authorizations!==1)throw Error('Autorização ausente ou duplicada');
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+2);if(overflow)throw Error('Layout excede viewport');
  await page.screenshot({path:`artifacts/chatgpt-ads-3783/browser/${name}.png`,fullPage:true});
  console.log(`${name}: preparar, autorizar, bloqueios e layout aprovados`);await context.close();
 }
 await browser.close();
})().catch(error=>{console.error(error);process.exit(1);});
