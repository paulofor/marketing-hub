// Confere a leitura da continuação estática com API local e tráfego comercial segregado.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { chromium, devices } = require('playwright');
const base = 'http://127.0.0.1:15173';
const output = 'artifacts/static-route-browser';
fs.mkdirSync(output, { recursive: true });
(async () => {
  const cycles = await (await fetch('http://127.0.0.1:18091/api/business-process-chains/learning-cycles/v1/products/91001')).json();
  const cycle = cycles[0];
  assert.equal(cycle.stage, 'AUTHORIZATION');
  const browser = await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
  const result = [];
  try {
    for (const [name, options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const errors = [], posts = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('request', request => {if(request.method()==='POST')posts.push(request.url());});
      await page.route('**/*', route => {
        const url = new URL(route.request().url());
        if(url.origin!==base) return route.fulfill({status:200,body:''});
        if(url.pathname.startsWith('/api/') && !['/api/business-process','/api/products'].some(prefix=>url.pathname.startsWith(prefix))) return route.fulfill({status:200,contentType:'application/json',body:'[]'});
        return route.continue();
      });
      await page.goto(`${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`,{waitUntil:'domcontentloaded',timeout:60000});
      await page.getByText('Autorizar orçamento e janela',{exact:false}).first().waitFor({timeout:60000});
      const body = await page.locator('body').innerText();
      assert(!body.includes('Falta um teto próprio desta versão para o anúncio e a demonstração'));
      assert(!body.includes('Produzir no Estúdio'));
      assert(!body.includes('em RUNNING'));
      assert.deepEqual(posts, []);
      assert.deepEqual(errors, []);
      assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      await page.screenshot({path:`${output}/${name}.png`,fullPage:true});
      fs.writeFileSync(`${output}/${name}.txt`,body);
      result.push({device:name,stage:cycle.stage,posts:posts.length,pageErrors:errors.length});
      await context.close();
    }
  } finally { await browser.close(); }
  fs.writeFileSync(`${output}/results.json`,JSON.stringify(result,null,2));
  console.log(JSON.stringify(result));
})().catch(error=>{console.error(error.message);process.exit(1);});
