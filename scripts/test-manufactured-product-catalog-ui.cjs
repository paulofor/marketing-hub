const { chromium, devices, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const definition = JSON.parse(fs.readFileSync(path.join(root, 'config/product-types/ai-designed-manufactured-product-v1.json'), 'utf8'));
const baseUrl = process.env.PRODUCT_TYPE_E2E_BASE_URL || 'http://127.0.0.1:4173';
assert.ok(['127.0.0.1', 'localhost'].includes(new URL(baseUrl).hostname), 'Test must use a local frontend');
const artifactDir = path.join(root, 'artifacts/jaspe-v1');
fs.mkdirSync(artifactDir, { recursive:true });

async function fillForm(page) {
  for (const field of ['name','internalName','code','description']) {
    const id = field === 'internalName' ? 'internal-name' : field;
    await page.locator(`#product-type-${id}`).fill(definition[field]);
  }
  await page.locator('#product-type-aliases').fill(definition.aliases.join('\n'));
  await page.locator('#product-type-status').selectOption(definition.status);
  const ids = {version:'blueprint-version', primaryChannel:'primary-channel', backendSdkModule:'backend-sdk', frontendSdkModule:'frontend-sdk'};
  for (const [key,value] of Object.entries(definition.blueprint)) await page.locator(`#product-type-${ids[key] || key}`).fill(value);
}

(async () => {
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_BIN || '/usr/bin/chromium', args:['--no-sandbox']});
  const results = [];
  try {
    for (const [name,options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      const saved = [];
      let failFirst = true;
      let posts = 0;
      await page.route(url => url.pathname.startsWith('/api/'), async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname === '/api/product-types') {
          if (request.method() === 'POST') {
            posts++;
            const actual = request.postDataJSON();
            assert.deepEqual(actual,definition);
            if (failFirst) { failFirst = false; return route.fulfill({status:409,json:{message:'Conflito sintético'}}); }
            const item = {...actual,id:999,constructionReady:true,missingBlueprintFields:[],productCount:0};
            saved.push(item);
            return route.fulfill({json:item});
          }
          const query = (url.searchParams.get('query') || '').toLowerCase();
          return route.fulfill({json:saved.filter(x=>JSON.stringify(x).toLowerCase().includes(query))});
        }
        return route.fulfill({json:[]});
      });
      await page.goto(`${baseUrl}/product-types`, {waitUntil:'networkidle'});
      await fillForm(page);
      await page.getByRole('button',{name:'Cadastrar tipo',exact:true}).click();
      await expect(page.getByRole('alert').filter({hasText:'Não foi possível salvar'})).toBeVisible();
      await expect(page.locator('#product-type-name')).toHaveValue(definition.name);
      await page.getByRole('button',{name:'Cadastrar tipo',exact:true}).click();
      await expect(page.locator('#product-type-name')).toHaveValue('');
      await page.getByPlaceholder('Nome, mineral, código ou apelido').fill('Jaspe');
      await expect(page.getByRole('heading',{name:definition.name,exact:true})).toBeVisible();
      assert.equal(saved.length,1);
      assert.equal(posts,2);
      assert.equal(saved[0].status,'PROPOSED');
      assert.deepEqual(errors,[]);
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth+2));
      await page.screenshot({path:path.join(artifactDir,`${name}.png`),fullPage:true});
      results.push({profile:name,passed:true,requests:posts,persistedInTestDouble:1,productionCalls:0});
      await context.close();
    }
    fs.writeFileSync(path.join(artifactDir,'ui-results.json'),JSON.stringify(results,null,2)+'\n');
    console.log(JSON.stringify(results));
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
