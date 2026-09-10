const { chromium, devices, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const base=process.env.VEGA_CONTEXT_WEB_URL || 'http://127.0.0.1:15173';
const out=process.env.VEGA_CONTEXT_EVIDENCE || 'artifacts/vega-cycle2/browser';
fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 for(const [name,settings] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
  const context=await browser.newContext({...settings,browserName:undefined});
  const errors=[],writes=[];
  await context.route('**/*',async route=>{
   const req=route.request(),url=new URL(req.url());
   if(!['127.0.0.1','localhost',new URL(base).hostname].includes(url.hostname)) throw new Error('Destino externo recusado: '+url.hostname);
   if(req.method()!=='GET'&&req.method()!=='OPTIONS') writes.push(req.url());
   if(url.pathname.startsWith('/api/')) {
    if(req.method()==='OPTIONS') return route.fulfill({status:204,headers:{'access-control-allow-origin':'*','access-control-allow-methods':'GET,OPTIONS','access-control-allow-headers':'*'}});
    if(['/api/facebook/configuration-status','/api/creatives/video-review','/api/ops-monitor/v1/modules/availability'].includes(url.pathname)) return route.fulfill({json:[],headers:{'access-control-allow-origin':'*'}});
    const response=await route.fetch({url:'http://127.0.0.1:18091'+url.pathname+url.search});
    return route.fulfill({response,headers:{...response.headers(),'access-control-allow-origin':'*'}});
   }
   return route.continue();
  });
  const page=await context.newPage(); page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base+'/products/91001/value-chain-history/processes/2/activities',{waitUntil:'networkidle'});
  await expect(page.getByRole('heading',{name:'2º ciclo de vendas · Experimento #91002'})).toBeVisible();
  await expect(page.getByText('Uso parcial não comprova conversão')).toBeVisible();
  await expect(page.getByText(/Amostra pequena; sem causa comprovada/)).toBeVisible();
  await expect(page.getByRole('link',{name:'Abrir próxima atividade'})).toHaveAttribute('href',/processes\/3\/activities\?learningCycleId=92002&chainId=.*#activity-rework/);
  await page.screenshot({path:path.join(out,name+'-process2.png'),fullPage:true});
  await page.getByRole('link',{name:'Abrir próxima atividade'}).click();
  await expect(page).toHaveURL(/processes\/3\/activities\?learningCycleId=92002/);
  await expect(page.getByRole('heading',{name:'2º ciclo de vendas · Experimento #91002'})).toBeVisible();
  await page.screenshot({path:path.join(out,name+'-process3.png'),fullPage:true});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.goto(base+'/products/91002/value-chain-history/processes/3/activities?learningCycleId=92002',{waitUntil:'networkidle'});
  await expect(page.getByText(/Não foi possível identificar o ciclo desta atividade/)).toBeVisible({timeout:20000});
  expect(await page.getByRole('button',{name:'Executar atividade',exact:true}).count()).toBe(0);
  expect(errors).toEqual([]); expect(writes).toEqual([]);
  console.log('PASS',name,'identidade, memória, continuidade, isolamento, layout e navegação sem comandos');
  await context.close();
 }
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
