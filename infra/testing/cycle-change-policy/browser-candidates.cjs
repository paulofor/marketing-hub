/** Homologa as 13 edições contra fotografias oficiais, com rede estritamente local. */
const { chromium, devices } = require('playwright');
const fs = require('fs');
const assert = require('assert/strict');
const { editProcess } = require('./edit-process.cjs');
const candidates = require('./candidates.json');
const catalogPath = process.argv[2];
if (!catalogPath) throw Error('Informe o catálogo de origem obtido somente por leitura.');
const sources = JSON.parse(fs.readFileSync(catalogPath, 'utf8'));
const base = 'http://127.0.0.1:15173';
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).filter(([,v]) => v !== null && v !== undefined).map(([k,v]) => [k,canonical(v)]));
  return value;
}
(async () => {
  const browser = await chromium.launch({ executablePath: '/usr/bin/chromium', headless: true, args: ['--no-sandbox'] });
  try {
    for (const [name, device] of [['desktop',{viewport:{width:1440,height:1050}}], ['iphone',devices['iPhone 15 Pro']], ['pixel',devices['Pixel 7']]]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const records = structuredClone(sources);
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      let mismatch;
      await page.route('**/*', async route => {
        const request = route.request();
        const u = new URL(request.url());
        if (!['127.0.0.1','localhost'].includes(u.hostname)) return route.abort();
        if (!u.pathname.startsWith('/api/')) return route.continue();
        const send = (json, status=200) => route.fulfill({status,contentType:'application/json',body:JSON.stringify(json)});
        if (u.pathname === '/api/business-processes' && request.method() === 'GET') return send(records);
        if (u.pathname === '/api/business-processes' && request.method() === 'POST') {
          const data = request.postDataJSON();
          const candidate = candidates.find(c => c.processCode === data.processCode);
          try { assert.deepEqual(canonical(data), canonical(candidate)); }
          catch (e) { mismatch = e; return send({message:'Contrato editado difere da candidata'},422); }
          const saved = {...data,id:98000+records.length,status:'DRAFT',activities:[]};
          records.push(saved); return send(saved,201);
        }
        if (u.pathname.endsWith('/composition')) {
          const id=Number(u.pathname.split('/')[3]);
          return send({process:records.find(p=>p.id===id),subprocesses:[],subprocessCount:0});
        }
        if (request.method() !== 'GET') return send({message:'Mutação fora do escopo'},409);
        return send([]);
      });
      const cases = name === 'desktop' ? candidates : candidates.filter(p => p.processCode === 'value-chain-learning-sales-cycle');
      for (const candidate of cases) {
        const source = sources.filter(p => p.processCode===candidate.processCode).sort((a,b)=>b.versionNumber-a.versionNumber)[0];
        await page.goto(base+'/business-processes?processId='+source.id);
        await page.getByRole('button',{name:'Criar versão editável',exact:true}).click();
        await editProcess(page,candidate);
        const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/business-processes' && r.request().method()==='POST');
        await page.getByRole('button',{name:'Salvar rascunho',exact:true}).click();
        const result = await response;
        if (mismatch) throw mismatch;
        assert.equal(result.status(),201);
        await page.getByText('Rascunho salvo com todos os elementos do processo.').last().waitFor();
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+2),false,'overflow '+name);
        console.log('PASS',name,candidate.processCode,'contrato integral preservado pela UI');
      }
      assert.deepEqual(errors,[]);
      await page.screenshot({path:'.tmp/cycle-change/local-'+name+'-process.png',fullPage:true});
      await context.close();
    }
  } finally { await browser.close(); }
})().catch(e => { console.error(String(e).slice(0,4500)); process.exitCode=1; });
