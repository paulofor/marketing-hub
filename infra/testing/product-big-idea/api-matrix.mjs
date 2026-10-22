import assert from 'node:assert/strict';
const base='http://127.0.0.1:18099';
async function call(path,method='GET',body,expected=200){
  const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  const text=await r.text();assert.equal(r.status,expected,`${path}: ${text}`);return text?JSON.parse(text):null;
}
const content={centralIdea:'A agenda real vira conteúdo pronto',audience:'Profissional autônoma',desire:'Publicar sem criar do zero',promise:'Kit pronto para usar',mechanism:'Posts adaptados à ocasião',evidence:'Hipótese sintética, sem vendas medidas',limits:'Não garante clientes'};
const path=id=>`/api/products/${id}/big-idea`;
assert.equal((await call(path(99101))).status,'MISSING');
const initial={expectedRevision:0,content,changeReason:'Hipótese inicial'};
const v1=await call(path(99101)+'/revisions','POST',initial);
assert.equal(v1.revision,1);
assert.equal((await call(path(99101)+'/revisions','POST',initial)).id,v1.id);
const pinned=await call('/fixture/pin/99101','POST',{reference:'experiment:99201'});
assert.equal(pinned.id,v1.id);
const v2=await call(path(99101)+'/revisions','POST',{expectedRevision:1,content:{...content,centralIdea:'A ocasião vira uma pauta utilizável'},changeReason:'Explicitar o mecanismo'});
assert.equal(v2.revision,2);
assert.equal((await call('/fixture/pin/99101','POST',{reference:'experiment:99201@agent-validation-v1'})).id,v1.id);
assert.equal((await call('/fixture/pin/99101','POST',{reference:'experiment:99202'})).id,v2.id);
await call(path(99101)+'/revisions','POST',{expectedRevision:1,content,changeReason:'Edição desatualizada'},409);
await call(path(99101)+'/revisions','POST',{expectedRevision:2,content:{...content,centralIdea:'  '},changeReason:'Inválido'},400);
await call(path(999999), 'GET',undefined,404);
const history=(await call(path(99101))).history;assert.equal(history.length,2);assert.deepEqual(history[1].content,content);
const concurrent=await Promise.all(['A','B'].map(async variant=>{
 const r=await fetch(base+path(99102)+'/revisions',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({expectedRevision:0,content:{...content,centralIdea:'Outra direção '+variant},changeReason:'Concorrência '+variant})});return r.status;
}));
assert.deepEqual(concurrent.sort(),[200,409]);
assert.equal((await call(path(99102))).history.length,1);
assert.equal((await call(path(99103))).status,'MISSING');
const chains=await call('/fixture/chain');assert.deepEqual(chains.map(c=>[c.version_number,c.status,Number(c.items)]),[[26,'RETIRED',6],[27,'PUBLISHED',6]]);
console.log(JSON.stringify({result:'PASS',checks:['create','evolve','history','idempotency','concurrency','validation','isolation','pinned-experiment','new-context','versioned-chain'],commercialEvents:0}));
