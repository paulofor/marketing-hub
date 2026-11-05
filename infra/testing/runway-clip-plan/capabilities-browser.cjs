// Valida o componente real e seu controller local sem permitir requests a produção ou providers.
const {chromium,devices,expect}=require('@playwright/test');
const fs=require('node:fs');
(async()=>{
 const out=process.argv[2]||'.codex/studio/browser';fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 try {for(const [name,options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
  const context=await browser.newContext(options);const page=await context.newPage();const errors=[];const requests=[];
  page.on('pageerror',error=>errors.push(error.message));
  await page.route('**/*',async route=>{const url=new URL(route.request().url());
   if(url.pathname.startsWith('/api/')) {
    requests.push({method:route.request().method(),path:url.pathname});
    if(url.pathname==='/api/sales-videos/studio/clip-plan'||url.pathname==='/api/sales-videos/provider-models'){
     const response=await route.fetch({url:'http://127.0.0.1:18080'+url.pathname});return route.fulfill({response});
    }
    if(route.request().method()!=='GET')throw Error('Mutação inesperada: '+url.pathname);
    return route.fulfill({json:url.pathname.endsWith('/studio/catalog')?{characters:[],captionPresets:[]}:[]});
   }
   if(url.origin==='http://127.0.0.1:4173')return route.continue();return route.abort();
  });
  await page.goto('http://127.0.0.1:4173/e2e/studio-capabilities.html',{waitUntil:'networkidle'});
  await page.getByRole('button',{name:'Prever tomadas sem gerar'}).click();
  await expect(page.getByText('US$ 3.84',{exact:false})).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount(4);
  await expect(page.getByText('Previsão integral restante: incompleta',{exact:false})).toBeVisible();
  await page.screenshot({path:`${out}/${name}-gen45.png`,fullPage:true});
  await page.getByLabel('Modelo',{exact:true}).selectOption('RUNWAY_VEO_3_1_FAST');
  await page.getByLabel('Duração final',{exact:true}).fill('15');
  await page.getByRole('checkbox').check();
  await expect(page.locator('tbody')).toHaveCount(0);
  await page.getByRole('button',{name:'Prever tomadas sem gerar'}).click();
  await expect(page.getByText('US$ 2.40',{exact:false})).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount(2);
  await expect(page.getByText(/Excedente a cortar na montagem: 1s/)).toBeVisible();
  await page.screenshot({path:`${out}/${name}-veo.png`,fullPage:true});
  await page.getByLabel('Modelo',{exact:true}).selectOption('RUNWAY_KLING_4');
  await page.getByRole('button',{name:'Prever tomadas sem gerar'}).click();
  await expect(page.getByText(/Integração em cadastro\/homologação/)).toBeVisible();
  if((await page.locator('body').innerText()).includes('MUSA'))throw Error('Contexto de produto indevido');
  if(errors.length)throw Error(errors.join('\n'));
  await page.goto('http://127.0.0.1:4173/audio-video-studio',{waitUntil:'networkidle'});
  await expect(page.getByLabel(/^CTA$/i)).toHaveValue('');
  await expect(page.getByRole('button',{name:/Kling 4.0 via Runway/})).toBeVisible();
  fs.writeFileSync(`${out}/${name}-requests.json`,JSON.stringify(requests,null,2));
  console.log(`PASS ${name}: controller real, 32s, Veo discreto, custo, modelo DRAFT, contexto neutro e nenhuma geração`);
  await context.close();
 }}finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1});
