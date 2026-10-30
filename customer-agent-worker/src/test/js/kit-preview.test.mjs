import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile, mkdir } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
let library; try { library = require('playwright'); } catch { library = require('playwright-core'); }
const { chromium, devices } = library;
const root = fileURLToPath(new URL('../../../../backend/ads-service/src/main/resources/private-kit-v1/', import.meta.url));
const names = {nails:'Unhas', barber:'Barbearia', hair:'Cabelo', makeup:'Maquiagem', brows:'Sobrancelhas e cílios', aesthetics:'Estética não clínica'};
const specialties = Object.entries(names).map(([code,label]) => ({code:code+'-v2',label,serviceExample:label}));
const presentation = {title:'Conheça seu kit de divulgação',introduction:'Escolha uma especialidade para conferir o acabamento.',statusMessage:''};

test('prévia real: desktop/mobile, troca de especialidade, falha honesta e versão histórica', async () => {
  let legacy = false;
  const server = createServer(async (req,res) => {
    if(req.url === '/session') {
      res.setHeader('Content-Type','application/json');
      res.end(JSON.stringify({id:'fixture',status:'INPUT',productName:'Capella · demonstração',presentation,specialties:legacy?[{code:'barber-v1',label:'Barbearia',serviceExample:'Corte e barba'}]:specialties,events:[]}));return;
    }
    const path = req.url === '/prototype' ? 'prototype.html' : req.url.slice(1);
    if(!/^(prototype\.(html|js|css)|samples\/[a-z]+-v2\.png)$/.test(path)) {res.writeHead(404);res.end();return;}
    try {res.setHeader('Content-Type',path.endsWith('.js')?'text/javascript':path.endsWith('.css')?'text/css':path.endsWith('.png')?'image/png':'text/html');res.end(await readFile(root+path));}
    catch {res.writeHead(404);res.end();}
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const executable=process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH||'/usr/bin/chromium';
  const browser=await chromium.launch({...(existsSync(executable)?{executablePath:executable}:{}),args:['--no-sandbox']});
  const base=`http://127.0.0.1:${server.address().port}`;
  try {
    for(const [name,profile] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
      const context=await browser.newContext(profile);const page=await context.newPage();
      await page.addInitScript(()=>sessionStorage.setItem('private-kit-session-v1','a'.repeat(64)));
      await page.goto(base+'/prototype');
      await page.locator('#sample-image').waitFor({state:'visible'});
      await page.locator('#specialty').selectOption('barber-v2');
      await page.waitForFunction(()=>document.querySelector('#sample-image').getAttribute('alt').includes('barbearia'));
      assert.equal(await page.locator('#sample-image').evaluate(e=>e.naturalWidth),1080);
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
      assert.equal(await page.locator('.sample-art').count(),0);
      await page.getByRole('button',{name:'Ampliar amostra',exact:true}).click();
      assert.equal(await page.locator('#art-dialog').isVisible(),true);
      await page.locator('#enlarged-art').evaluate(e=>e.decode());
      if(name!=='desktop') await page.setViewportSize({width:393,height:659});
      await page.locator('#enlarged-art').evaluate(async image=>{
        image.src='data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1920"><rect width="1080" height="1920" fill="purple"/></svg>');
        await image.decode();
      });
      const box=await page.locator('#enlarged-art').evaluate(image=>{
        const rect=image.getBoundingClientRect();return {left:rect.left,top:rect.top,right:rect.right,bottom:rect.bottom,width:innerWidth,height:innerHeight};
      });
      assert.ok(box.left>=0&&box.top>=0&&box.right<=box.width&&box.bottom<=box.height,JSON.stringify(box));
      await page.getByRole('button',{name:'Fechar ampliação',exact:true}).click();
      if(process.env.KIT_VISUAL_SCREENSHOTS) {await mkdir(process.env.KIT_VISUAL_SCREENSHOTS,{recursive:true});await page.screenshot({path:process.env.KIT_VISUAL_SCREENSHOTS+'/preview-'+name+'.png',fullPage:true});}
      await page.route('**/samples/hair-v2.png',async route=>{await new Promise(resolve=>setTimeout(resolve,250));await route.continue();});
      await page.locator('#specialty').selectOption('hair-v2');
      await page.locator('#specialty').selectOption('makeup-v2');
      await page.waitForFunction(()=>document.querySelector('#sample-image').getAttribute('alt').includes('maquiagem'));
      await page.waitForTimeout(300);
      assert.match(await page.locator('#sample-image').getAttribute('src'),/makeup-v2.png$/);
      await page.route('**/samples/brows-v2.png',route=>route.fulfill({status:404,body:''}));
      await page.locator('#specialty').selectOption('brows-v2');
      await page.getByText('A amostra não carregou.',{exact:false}).waitFor();
      assert.equal(await page.locator('#sample-image').isHidden(),true);
      legacy=true;await page.reload();
      await page.getByText('Esta versão usa ilustrações de demonstração.',{exact:false}).waitFor();
      assert.equal(await page.locator('#sample-image').isHidden(),true);
      legacy=false;await context.close();
    }
  } finally {await browser.close();await new Promise(resolve=>server.close(resolve));}
});
