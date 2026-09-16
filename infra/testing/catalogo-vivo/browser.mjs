// Exercita a UI real e a API/MySQL locais do catálogo; demais painéis usam contratos simulados.
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import vm from 'node:vm';
import { createRequire } from 'node:module';
import { chromium, devices, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs';
const require = createRequire(new URL('../../../frontend/package.json',import.meta.url));
const ts=require('typescript');
const root=new URL('../../../',import.meta.url);
const output=new URL('artifacts/catalogo-vivo-opala/browser/',root);
await mkdir(output,{recursive:true});
const source=await readFile(new URL('frontend/src/pages/learningCycle/LearningCyclesPage.test.tsx',root),'utf8');
const block=source.slice(source.indexOf('const catalog:'),source.indexOf('const decisionProposal'));
const fixtures=vm.runInNewContext(ts.transpile(block+'\nglobalThis.fixture={catalog,cycle};',{target:ts.ScriptTarget.ES2022})+'\nfixture');
const browser=await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium',headless:true,args:['--no-sandbox']});
const evidence=[];
const adoptionOnly=process.argv.includes("--adoption-only");
try {
  for(const [name,device] of (adoptionOnly ? [['desktop',{viewport:{width:1440,height:1000}}]] : [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]])) {
    const context=await browser.newContext(device);
    const page=await context.newPage();
    const errors=[];page.on('pageerror',error=>errors.push(error.message));
    await context.route('**/*',async route=>{
      const url=new URL(route.request().url());
      if(!['127.0.0.1','localhost'].includes(url.hostname)) return route.abort();
      if(url.pathname.startsWith('/api/catalogo-vivo/')) return route.continue();
      if(url.pathname.startsWith('/api/')) {
        let value=[];
        if(url.pathname.includes('learning-cycles/v1/catalog')) value=fixtures.catalog;
        else if(url.pathname.includes('learning-cycles/v1/products/900004')) value=[fixtures.cycle];
        else if(url.pathname.includes('business-process-chains')) value=[{id:fixtures.cycle.chainDefinitionId,name:'Cadeia histórica local'}];
        else if(url.pathname==='/api/products') value=[{id:900004,name:'Opala local',productType:'PDE'}];
        return route.fulfill({contentType:'application/json',body:JSON.stringify(value)});
      }
      if(url.pathname.startsWith('/ws')) return route.fulfill({status:204,body:''});
      return route.continue();
    });
    let changed;
    if(!adoptionOnly) {
    await page.goto('http://127.0.0.1:15173/catalogo-vivo/opala');
    await expect(page.getByText('Conjunto de instruções disponível para os agentes.')).toBeVisible();
    const catalog=await (await page.request.get('http://127.0.0.1:15173/api/catalogo-vivo/v1/opala')).json();
    const binding=catalog.items[0].binding;
    const section=page.getByRole('region',{name:binding.activityName,exact:true});
    await page.getByLabel('Responsável *',{exact:true}).fill('Homologação '+name);
    await page.getByLabel('Motivo ou parecer *').fill('Validação local segregada '+name);
    const textarea=section.getByLabel('Instrução da atividade *');
    await textarea.fill('Instrução de teste segregado '+name+': {{TASK_CONTEXT}}');
    await section.getByRole('button',{name:'Salvar como nova versão'}).click();
    await expect(section.getByRole('button',{name:'Registrar revisão desta versão'})).toBeVisible();
    await section.getByRole('button',{name:'Registrar revisão desta versão'}).click();
    await expect(page.getByText('Revisão registrada. O conjunto pode ser ativado após conferir as demais atividades.')).toBeVisible();
    await page.getByRole('button',{name:'Ativar conjunto selecionado'}).click();
    await expect(page.getByText('Conjunto ativado para novas tarefas. As tarefas existentes preservaram suas versões.')).toBeVisible();
    changed=await (await page.request.get('http://127.0.0.1:15173/api/catalogo-vivo/v1/opala')).json();
    const active=changed.items[0].versions.find(v=>v.id===changed.items[0].binding.activeVersionId);
    expect(active.text).toContain('teste segregado '+name);
    expect(changed.items).toHaveLength(7);
    await page.evaluate(()=>window.scrollTo(0,0));
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBeTruthy();
    await page.screenshot({path:new URL(name+'-catalog.png',output).pathname,fullPage:true});
    }
    // O painel de ciclos conserva dados da fixture; adesão e navegação são fornecidas pela API real.
    const adoption=await (await page.request.get('http://127.0.0.1:15173/api/catalogo-vivo/v1/opala/products/900004/cycles/900002/adoption')).json();
    const chainId=Number(new URL(adoption.preparationUrl||'/?chainId=1','http://local').searchParams.get('chainId'));
    Object.assign(fixtures.cycle,{id:900002,productId:900004,experimentId:900092,revision:14,chainDefinitionId:chainId,stage:'PUBLICATION',stageLabel:'Preparação comercial',status:'OPEN',productVersion:'fixture-v12',budgetLimitBrl:100,commands:[{action:'COMPLETE',label:'Concluir etapa',available:false,reason:'Preparação pendente'}],commercialPreparation:{catalogoVivoApplicable:true,readyForReview:false,guidance:'Preparar ativos com os agentes',experimentUrl:'/experiments/900092',requirements:[]}});
    if(fixtures.catalog.entry) Object.assign(fixtures.catalog.entry,{chainDefinitionId:chainId});
    await page.goto(`http://127.0.0.1:15173/business-process-chains/learning-cycles?chainId=${chainId}&productId=900004&cycleId=900002`);
    const panel=page.getByRole('region',{name:'Preparação Opala com os agentes'});
    await expect(panel).toBeVisible();
    if(adoption.canAdopt) {
      await panel.getByLabel('Responsável *').fill('Homologação '+name);
      await panel.getByRole('button',{name:'Integrar e iniciar preparação'}).click();
    }
    const link=panel.getByRole('link',{name:'Acompanhar preparação dos agentes'});
    await expect(link).toBeVisible();
    const target=await link.getAttribute('href');expect(target).toContain('learningCycleId=900002');expect(target).toContain('chainId='+chainId);
    await panel.scrollIntoViewIfNeeded();await page.screenshot({path:new URL(name+'-adoption.png',output).pathname,fullPage:true});
    expect(errors).toEqual([]);
    evidence.push({device:name,createReviewActivate:!adoptionOnly,activities:changed?.items.length,adoptionSubmitted:adoption.canAdopt,adoptionUrl:target,pageErrors:errors,realCatalogApi:true,otherPanels:'test doubles'});
    await context.close();
  }
} finally {await browser.close();}
await writeFile(new URL(adoptionOnly?'summary-adoption.json':'summary.json',output),JSON.stringify(evidence,null,2));
console.log(JSON.stringify(evidence,null,2));
