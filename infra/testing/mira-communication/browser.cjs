// Confere a interface com contratos produzidos pelo backend local, sem conexões produtivas.
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const { chromium, devices, expect } = require('@playwright/test');
(async () => {
  const contract = JSON.parse(fs.readFileSync(process.env.MIRA_UI_CONTRACT, 'utf8'));
  const output = process.env.MIRA_UI_OUTPUT;
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ executablePath:'/usr/bin/chromium', args:['--no-sandbox','--disable-dev-shm-usage'] });
  const profiles = [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]];
  const results = [];
  try {
    for (const [name, profile] of profiles) {
      const context = await browser.newContext({...profile, permissions:['clipboard-read','clipboard-write']});
      const page = await context.newPage();
      let phase = 'ready', writes = 0;
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/*', async route => {
        const req=route.request(), url=new URL(req.url());
        if (url.pathname.startsWith('/api/')) {
          let data;
          if (url.pathname.includes('/automation/v1')) {
            if (req.method()==='POST') {
              assert.equal(phase,'ready');
              assert.deepEqual(req.postDataJSON(), {chainId:contract.chainId,sourceReference:contract.automationReady.sourceReference});
              writes++; phase='waiting';
            }
            data=phase==='ready'?contract.automationReady:phase==='waiting'?contract.automationWaiting:contract.automationComplete;
          } else if (url.pathname.endsWith('/activity-executions')) {
            data=phase==='complete'?contract.historyComplete:contract.historyReady;
          } else if (url.pathname.endsWith('/process-context')) data=null;
          else if (url.pathname.includes('/value-chain-positions/')) data={productId:contract.productId,chainDefinitionId:contract.chainId,chainName:'Cadeia local',chainVersion:14,processDefinitionId:contract.processId,sequenceNumber:4,processCount:6,processMeasurements:[]};
          else if (url.pathname.endsWith('/configuration-status')) data={accounts:[]};
          else data=[];
          assert.ok(req.method()==='GET'||url.pathname.includes('/automation/v1'),'Escrita fora do comando de processo');
          return route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
        }
        if (url.origin==='http://127.0.0.1:4173') return route.continue();
        return route.abort();
      });
      const url=`http://127.0.0.1:4173/products/${contract.productId}/value-chain-history/processes/${contract.processId}/activities?chainId=${contract.chainId}`;
      await page.goto(url);
      const start=page.getByRole('button',{name:'Executar processo',exact:true});
      await expect(start).toBeEnabled();
      await page.screenshot({path:path.join(output,`${name}-ready.png`),fullPage:true});
      await start.click();
      await expect(page.getByRole('button',{name:'Pausar',exact:true})).toBeVisible();
      assert.equal(writes,1);
      await page.getByRole('button',{name:'Copiar contexto do processo',exact:true}).click();
      const copied=await page.evaluate(()=>navigator.clipboard.readText());
      assert.ok(copied.includes(contract.automationReady.sourceReference));
      assert.ok(!copied.includes('experiment:'));
      phase='complete';
      await page.reload();
      await expect(page.getByRole('progressbar',{name:'Objetivos comprovados'})).toHaveAttribute('aria-valuenow','100');
      await expect(page.getByRole('button',{name:'Executar processo',exact:true})).toHaveCount(0);
      const width=await page.evaluate(()=>({doc:document.documentElement.scrollWidth,view:innerWidth}));
      assert.ok(width.doc<=width.view+1,JSON.stringify(width));
      assert.deepEqual(errors,[]);
      await page.screenshot({path:path.join(output,`${name}-complete.png`),fullPage:true});
      results.push({profile:name,initialCommand:true,onlyOneWrite:true,privateReferenceCopied:true,completionFromBackend:true,noOverflow:true,noJavascriptErrors:true});
      await context.close();
    }
  } finally { await browser.close(); }
  fs.writeFileSync(path.join(output,'results.json'),JSON.stringify(results,null,2));
  console.log(JSON.stringify(results));
})().catch(e=>{console.error(e);process.exit(1);});
