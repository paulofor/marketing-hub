const {chromium,devices}=require('playwright');
const {readFileSync,mkdirSync,writeFileSync}=require('node:fs');
const {resolve}=require('node:path');
const assert=require('node:assert/strict');

// Usa a tela real com o backend local e verifica que somente navegar não executa comandos.
(async()=>{
  const output=resolve('.codex/vega-approved/browser');mkdirSync(output,{recursive:true});
  const cycle=JSON.parse(readFileSync('.codex/vega-approved/local-cycle.json','utf8'));
  const browser=await chromium.launch({headless:true,executablePath:resolve('.codex/vega-approved/media/chromium-fixture.sh'),args:['--no-sandbox']});
  const results=[];
  try {
    for(const [name,profile] of Object.entries({desktop:{viewport:{width:1440,height:1000}},iphone:devices['iPhone 15 Pro'],pixel:devices['Pixel 7']})){
      const context=await browser.newContext({...profile,defaultBrowserType:'chromium'});const page=await context.newPage();
      const writes=[],errors=[];page.on('request',r=>{if(r.method()==='POST')writes.push(r.url());});page.on('pageerror',e=>errors.push(e.message));
      await page.goto(`http://127.0.0.1:15173/business-process-chains/learning-cycles?chainId=${cycle.chainDefinitionId}&productId=${cycle.productId}&cycleId=${cycle.id}`);
      await page.getByRole('heading',{name:'Próximas etapas automáticas'}).waitFor();
      assert.equal(await page.getByLabel('Criativo de campanha aprovado',{exact:true}).count(),0);
      assert.equal(await page.getByRole('link',{name:'Ver aprovações dos vídeos'}).getAttribute('href'),'/videos');
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
      await page.screenshot({path:output+`/${name}-admin.png`,fullPage:true});
      assert.deepEqual(writes,[]);assert.deepEqual(errors,[]);
      const response=await fetch('http://127.0.0.1:18091/api/pde/vega/private/v1/internal/sessions',{method:'POST',headers:{'Content-Type':'application/json','X-PDE-Internal-Token':'vega-local-internal-only'},body:JSON.stringify({cycleId:cycle.id,prototypeVersion:cycle.productVersion,origin:'QA_INTERNAL'})});
      assert.equal(response.status,200);const session=await response.json();
      await context.addInitScript(token=>localStorage.setItem('vega-private-session-v1',token),session.sessionToken);
      await page.goto('https://localhost:18443/vega-private');
      await page.getByRole('button',{name:'Começar',exact:true}).waitFor();
      const demo=page.locator('details.video-demo');assert.equal(await demo.getAttribute('open'),null);
      await demo.locator('summary').first().click();
      await page.getByLabel('Como funciona seu primeiro ajuste').evaluate(v=>v.play());
      await page.screenshot({path:output+`/${name}-private.png`,fullPage:true});
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
      assert.ok(await page.getByRole('button',{name:'Começar',exact:true}).isEnabled());
      assert.deepEqual(errors,[]);results.push({device:name,admin:true,optionalVideo:true,sideEffects:false});await context.close();
    }
  }finally{await browser.close();}
  writeFileSync(output+'/results.json',JSON.stringify(results,null,2));console.log('PASS navegação e mídia em desktop, iPhone e Pixel');
})().catch(error=>{console.error(error);process.exit(1);});
