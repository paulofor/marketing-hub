import {createRequire} from 'node:module';
import {mkdir,readFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
const require=createRequire(new URL('../../../frontend/package.json',import.meta.url));
const {chromium,devices}=require('@playwright/test');
const folder='artifacts/product-big-idea/browser';await mkdir(folder,{recursive:true});
const browser=await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium',headless:true,args:['--no-sandbox']});
const results=[];
try {
for (const [index,device] of ['Desktop','iPhone 15 Pro','Pixel 7'].entries()) {
 const context=await browser.newContext(device==='Desktop'?{viewport:{width:1440,height:1000}}:devices[device]);
 await context.route('**/*',async route=>{
  const url=new URL(route.request().url());
  if(!['127.0.0.1','localhost'].includes(url.hostname)) return route.abort();
  if(url.pathname.startsWith('/api/products/')) {
    const response=await route.fetch({url:'http://127.0.0.1:18099'+url.pathname+url.search});
    return route.fulfill({response,headers:{...response.headers(),'access-control-allow-origin':'*'}});
  }
  if(url.pathname.startsWith('/api/') || url.pathname.startsWith('/ws')) return route.fulfill({status:200,contentType:'application/json',body:'[]',headers:{'access-control-allow-origin':'*'}});
  return route.continue();
 });
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(`http://127.0.0.1:15179/products/${99103+index}/big-idea`);
 await page.getByText('Big Idea ainda não definida').waitFor();
 assert.equal(await page.getByRole('button',{name:'Criar Big Idea'}).isDisabled(),true);
 const fields={
 'Ideia central *':'Sua ocasião vira um plano pronto para usar',
 'Para quem *':'Pessoa com um compromisso e pouco tempo',
 'Desejo reconhecido *':'Decidir com menos esforço',
 'Transformação prometida *':'Receber opções concretas e utilizáveis',
 'Como o produto torna isso possível *':'Relaciona ocasião e entradas reais a opções práticas',
 'Provas e hipóteses *':'Demonstração sintética; sem prova de vendas',
 'Limites da promessa *':'Não garante resultado social ou financeiro',
 'Motivo desta versão *':'Definição inicial sintética'
 };
 for(const [label,value] of Object.entries(fields)) await page.getByLabel(label,{exact:true}).fill(value);
 await page.getByRole('button',{name:'Criar Big Idea'}).click();await page.getByText('Versão vigente: v1').waitFor();
 await page.getByLabel('Ideia central *',{exact:true}).fill('Uma decisão simples para a ocasião real');
 await page.getByLabel('Motivo desta versão *',{exact:true}).fill('Explicitar benefício');
 await page.getByRole('button',{name:'Salvar nova versão'}).click();await page.getByText('Versão vigente: v2').waitFor();
 await page.getByText('Histórico de versões (2)').click();
 await page.getByText('Sua ocasião vira um plano pronto para usar',{exact:true}).waitFor();
 await page.getByRole('heading',{name:'Big Idea do produto',exact:true}).scrollIntoViewIfNeeded();
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),true,'Overflow '+device);
 assert.deepEqual(errors,[]);
 await page.screenshot({path:`${folder}/${device.replaceAll(' ','-')}.png`});
 const researchProduct=99106+index;
 await page.goto(`http://127.0.0.1:15179/products/${researchProduct}/big-idea`);
 await page.getByRole('button',{name:'Pedir Big Idea a Atena'}).waitFor();
 await page.getByText('Acrescentar contexto e fontes',{exact:true}).click();
 await page.getByLabel('Orientação para Atena (opcional)').fill('Pesquisa sintética sem mercado real');
 await page.getByRole('button',{name:'Pedir Big Idea a Atena'}).click();
 await page.getByText(/Na fila de Atena/).waitFor();
 assert.equal(await page.getByRole('button',{name:'Aplicar Big Idea de Atena'}).count(),0);
 const endpoint=`http://127.0.0.1:18099/api/products/${researchProduct}/big-idea/research`;
 const research=await (await page.request.get(endpoint)).json();
 const duplicate=await (await page.request.post(endpoint,{data:{brief:'Reenvio sintético'}})).json();
 assert.equal(duplicate.executionId,research.executionId);
 const result=JSON.parse(await readFile(new URL('./research-result.json',import.meta.url),'utf8'));result.productId=researchProduct;
 const wrong={...result,productId:researchProduct+200};
 assert.equal((await page.request.post(`http://127.0.0.1:18099/fixture/research/${research.executionId}/complete`,{data:wrong})).status(),400);
 const response=await page.request.post(`http://127.0.0.1:18099/fixture/research/${research.executionId}/complete`,{data:result});
 assert.equal(response.status(),200,await response.text());
 await page.reload();await page.getByRole('button',{name:'Aplicar Big Idea de Atena'}).click();
 await page.getByText('Versão vigente: v1').waitFor();
 assert.equal((await page.request.post(endpoint+`/${research.executionId}/adopt`)).status(),200);
 const saved=await (await page.request.get(`http://127.0.0.1:18099/api/products/${researchProduct}/big-idea`)).json();
 assert.equal(saved.history.length,1);assert.equal(saved.current.content.centralIdea,result.bigIdea.centralIdea);
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),true,'Overflow pesquisa '+device);
 assert.deepEqual(errors,[]);
 await page.screenshot({path:`${folder}/atena-${device.replaceAll(' ','-')}.png`});
 results.push({device,create:true,evolve:true,history:true,research:true,duplicateReused:true,crossProductBlocked:true,adoptPersisted:true,horizontalOverflow:false});await context.close();
}
console.log(JSON.stringify({result:'PASS',results,commercialEvents:0}));
} finally {await browser.close();}
