const assert=require('node:assert/strict');const crypto=require('node:crypto');
const base='http://127.0.0.1:18101';
async function call(method,path,body,status=200){const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});const t=await r.text();assert.equal(r.status,status,t);return t?JSON.parse(t):null;}
(async()=>{
 for(const id of [99101,99102]){
  const input={requestKey:crypto.randomUUID(),productId:id,desiredOutcome:'Resultado específico '+id,referenceUrls:['https://reference.example/']};
  const m=await call('POST','/api/product-modeling/v1',input);assert.equal(m.status,'DRAFT');
  const replay=await call('POST','/api/product-modeling/v1',input);assert.equal(m.id,replay.id);
  await call('POST','/api/product-modeling/v1',{...input,desiredOutcome:'Outra intenção'},409);
  assert.equal((await call('GET','/fixture/health')).audit,id===99101?0:1);
  await call('POST',`/api/product-modeling/v1/${m.id}/adopt`,{cycleId:id+100},409);
  await Promise.all([call('POST',`/api/product-modeling/v1/${m.id}/start`),call('POST',`/api/product-modeling/v1/${m.id}/start`)]);
  assert.equal((await call('GET','/fixture/health')).audit,id===99101?1:2);
  await call('POST',`/fixture/complete/${m.discoveryCycleId}`);
  await call('POST',`/api/product-modeling/v1/${m.id}/adopt`,{cycleId:id===99101?99202:99201},409);
  const adopted=await call('POST',`/api/product-modeling/v1/${m.id}/adopt`,{cycleId:id+100});
  assert.equal(adopted.learningCycleId,id+100);assert.match(adopted.chainUrl,new RegExp('productId='+id));
  await call('POST',`/api/product-modeling/v1/${m.id}/adopt`,{cycleId:id+100});
  const context=await call('GET',`/fixture/context/${id+200}`);assert.equal(context.productId,id);assert.equal(context.factualSources.length,1);assert.equal(context.commercialAuthorizationInherited,false);
 }
 const fresh=await call('POST','/api/product-modeling/v1',{requestKey:crypto.randomUUID(),newProductName:'Nova estrela',desiredOutcome:'Outro resultado',referenceUrls:['https://reference.example/']});
 assert.equal(fresh.productId,null);assert.equal(fresh.status,'DRAFT');
 console.log(JSON.stringify({prepare:'PASS',replay:'PASS',concurrentStart:'PASS',adoption:'PASS',identity:'PASS',factualContext:'PASS',newProduct:'PASS',externalSpend:0}));
})().catch(e=>{console.error(e);process.exitCode=1});
