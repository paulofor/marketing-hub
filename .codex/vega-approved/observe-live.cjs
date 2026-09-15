const {chromium,devices}=require('playwright');
const fs=require('node:fs');
(async()=>{
 const label=process.argv[2]||'live';if(!/^[a-z0-9-]+$/.test(label))throw Error('label');
 const root='.codex/vega-approved/'+label;fs.mkdirSync(root,{recursive:true});
 const b=await chromium.launch({headless:true,executablePath:'/usr/bin/chromium',args:['--no-sandbox']});
 try{
  for(const [name,options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']]]){
   const ctx=await b.newContext({...options,defaultBrowserType:'chromium'});const p=await ctx.newPage();
   await p.route('**/*',r=>['GET','HEAD','OPTIONS'].includes(r.request().method())?r.continue():r.abort());
   await p.goto('http://191.252.181.168:5173/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2',{waitUntil:'networkidle',timeout:60000});
   await p.getByText('Ciclos de aprendizado e vendas',{exact:true}).first().waitFor();
   await p.waitForTimeout(1500);await p.screenshot({path:root+'/'+name+'.png',fullPage:true});fs.writeFileSync(root+'/'+name+'.txt',await p.locator('body').innerText());await ctx.close();
  }
 }finally{await b.close()}
 for(const [name,path] of [['cycle','/api/business-process-chains/learning-cycles/v1/products/4?chainId=14'],['run','/api/business-processes/75/products/4/automation/v1?chainId=14&learningCycleId=2&sourceReference=experiment%3A92']]){
  const r=await fetch('http://191.252.181.168'+path);if(!r.ok)throw Error(r.status);const j=await r.json();fs.writeFileSync(root+'/'+name+'.json',JSON.stringify(j,null,2));
  const c=name==='cycle'?j.find(x=>x.id===2):j;console.log(JSON.stringify({name,id:c.id,status:c.status,stage:c.stage,reason:c.reason,nextAction:c.nextAction,automaticContinuation:c.automaticContinuation}));
 }
})().catch(e=>{console.error(e);process.exit(1)});
