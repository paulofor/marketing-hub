const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, devices } = require('playwright');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
fs.mkdirSync(path.join(root,'.local/fichas-evidence'),{recursive:true});
const dir = path.join(root, 'backend/ads-service/src/main/resources/fichas/v1');
const fichas = ['exemplo-bloqueado.json','exemplo-recuperado.json'].flatMap(n=>JSON.parse(fs.readFileSync(path.join(dir,n))));
test('piloto de fichas: histórico, provas e lacunas em desktop e mobile sem despacho', async () => {
  const server = spawn(path.join(root,'frontend/node_modules/.bin/vite'), ['--host','127.0.0.1','--port','5179'], {cwd:path.join(root,'frontend'),env:{...process.env,NODE_ENV:'development'},stdio:'ignore'});
  let browser;
  try {
    for(let i=0;i<60;i++){try{if((await fetch('http://127.0.0.1:5179')).ok)break;}catch{} await new Promise(r=>setTimeout(r,500));}
    browser=await chromium.launch({executablePath:'/usr/bin/chromium',headless:true});
    for (const [name, options] of [['desktop',{viewport:{width:1440,height:1000}}],['mobile',devices['iPhone 15 Pro']]]) {
      const ctx=await browser.newContext(options); const page=await ctx.newPage(); page.on('pageerror',e=>console.error('PAGEERROR',e.message)); let writes=0;
      await page.route(url => url.pathname.startsWith('/api/'),async route=>{
        if(route.request().method()!=='GET')writes++;
        const url=route.request().url();
        await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(url.includes('/fichas/v1')?{ambiente:'HOMOLOGACAO_HISTORICA',despachoPermitido:false,fichas}:[])});
      });
      await page.goto('http://127.0.0.1:5179/products/7/fichas?learningCycleId=20');
      await page.getByRole('heading',{name:'Fichas v1.0 · Homologação'}).waitFor().catch(async e=>{console.error((await page.locator('body').innerText()).slice(0,3000));throw e;});
      await page.getByText('Destino #127 v12: PUBLISHED').waitFor();
      assert.equal(await page.locator('article').count(),4);
      assert.ok((await page.getByRole('region', {name:'Fichas históricas'}).innerText()).includes('WAITING_INPUT'));
      await page.getByRole('link',{name:'capella-c20-e97-passagem · revisão 1',exact:true}).click();
      assert.ok(page.url().endsWith('#capella-c20-e97-passagem-r1'));
      assert.equal(await page.getByRole('region', {name:'Fichas históricas'}).evaluate(el=>el.scrollWidth>el.clientWidth+1),false);
      assert.equal(writes,0);
      await page.screenshot({path:path.join(root,`.local/fichas-evidence/${name}.png`),fullPage:true});
      await ctx.close();
    }
  } finally {if(browser)await browser.close();server.kill('SIGTERM');}
});
