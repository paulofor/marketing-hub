import { writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const root='http://127.0.0.1:57282/api/pde/kit/private/v1';
const admin=process.env.PDE_INTERNAL_API_TOKEN;if(admin!=='kit-local-internal-only')throw new Error('Este teste admite somente o backend local.');
const compositor='kit-local-payments-only';
async function request(path,{method='GET',body,session,payments=false,authenticated=true}={}){
  const response=await fetch(root+path,{method,headers:{'Content-Type':'application/json',...(authenticated?(session?{'X-Kit-Session':session}:payments?{'X-Payments-Auth':compositor}:{'X-PDE-Internal-Token':admin}):{})},body:body?JSON.stringify(body):undefined});
  if(path==='/download'||path.startsWith('/assets/'))return {status:response.status,bytes:(await response.arrayBuffer()).byteLength};
  const text=await response.text();return {status:response.status,data:text?JSON.parse(text):null};
}
async function create(productId,cycleId,scenarioCode='ADHERENT',extra={}){const body={requestKey:crypto.randomUUID(),productId,cycleId,scenarioCode,deviceProfile:'DESKTOP_1440',prototypeVersion:'v1',...extra};const result=await request('/internal/sessions',{method:'POST',body});return {body,...result};}
const proof=[];
assert.equal((await request('/stage-executions/pending',{authenticated:false})).status,401);proof.push('Fila exige identidade do compositor.');
assert.equal((await request('/session',{session:'a'.repeat(64)})).status,401);
assert.equal((await create(8018,7007)).status,409);assert.equal((await create(8007,7007,'ADHERENT',{prototypeVersion:'other-version'})).status,409);proof.push('Produto, ciclo e versão incompatíveis recusados.');
const primary=await create(8029,7029);assert.equal(primary.status,200);
const duplicate=await request('/internal/sessions',{method:'POST',body:primary.body});assert.equal(duplicate.data.sessionId,primary.data.sessionId);assert.equal(duplicate.data.sessionToken,primary.data.sessionToken);
assert.equal((await request('/internal/sessions',{method:'POST',body:{...primary.body,scenarioCode:'RECOVERY'}})).status,409);proof.push('Emissão idempotente mantém acesso e recusa conteúdo divergente.');
const token=primary.data.sessionToken;
const baseline={email:'teste+controle@sandbox.local',professionalName:'Studio Controle',cityRegion:'Cidade Exemplo',whatsapp:'00000000000',services:'Corte e barba',visualStyle:'elegante',weeklyGoal:'Apresentar serviço',preferredColors:'preto',notes:'',consentAccepted:true};
assert.equal((await create(8029,7029,'ADHERENT',{safetyCase:'EXTERNAL_ACTION'})).status,409);
assert.equal((await create(8029,7029,'SAFETY',{safetyCase:'UNSUPPORTED'})).status,400);
for(const safetyCase of ['UNVERIFIED_VISUAL_ORIGIN','EXTERNAL_ACTION',null]){
  const access=await create(8029,7029,'SAFETY',{safetyCase});assert.equal(access.status,200);
  const blocked=await request('/input',{method:'PUT',session:access.data.sessionToken,body:baseline});
  assert.equal(blocked.status,200);assert.equal(blocked.data.status,'BLOCKED_SAFE');assert.equal(blocked.data.manifest,null);
  assert.equal(blocked.data.presentation.reasonCode,safetyCase||'REVIEW_REQUIRED');
  assert.ok(blocked.data.presentation.title.includes('bloqueada'));assert.ok(blocked.data.presentation.introduction.includes('Nenhum pacote foi gerado'));
  assert.equal(blocked.data.presentation.nextActionPath,'/business-process-chains/learning-cycles?productId=8029&cycleId=7029');
  assert.deepEqual((await request('/session',{session:access.data.sessionToken})).data,blocked.data);
  assert.equal((await request('/download',{session:access.data.sessionToken})).status,409);
  assert.equal((await request('/events',{method:'POST',session:access.data.sessionToken,body:{eventId:crypto.randomUUID(),code:'VALUE_MOMENT'}})).status,409);
  assert.equal((await request('/internal/sessions',{method:'POST',body:{...access.body,safetyCase:safetyCase==='EXTERNAL_ACTION'?'UNVERIFIED_VISUAL_ORIGIN':'EXTERNAL_ACTION'}})).status,409);
}
proof.push('Bloqueio tem causa explícita ou desconhecida, orientação do mesmo ciclo e reabertura sem pacote, evento de valor ou troca silenciosa da fixture.');
assert.equal((await request('/input',{method:'PUT',session:token,body:{...baseline,professionalName:''}})).status,400);
assert.equal((await request('/input',{method:'PUT',session:token,body:{...baseline,consentAccepted:false}})).status,409);
assert.equal((await request('/input',{method:'PUT',session:token,body:{...baseline,email:'real@example.com'}})).status,409);
assert.equal((await request('/events',{method:'POST',session:token,body:{eventId:crypto.randomUUID(),code:'CHECKOUT_STARTED'}})).status,409);
assert.equal((await request('/download',{session:token})).status,409);proof.push('Validação, consentimento e contatos fictícios precedem reserva ou efeito.');
const creates=await Promise.all(Array.from({length:4},()=>create(8029,7029)));
const entries=await Promise.all(creates.map((c,i)=>request('/input',{method:'PUT',session:c.data.sessionToken,body:{...baseline,notes:'variante-'+i}})));
assert.equal(entries.filter(r=>r.status===200).length,2);assert.equal(entries.filter(r=>r.status===409).length,2);proof.push('Concorrência mantém no máximo duas composições por fixture.');
const queue=(await request('/stage-executions/pending',{payments:true})).data.filter(a=>a.cycleId===7029);assert.equal(queue.length,2);
const claimKey=crypto.randomUUID(),id=queue[0].id;
assert.equal((await request('/stage-executions/'+id+'/claim',{method:'POST',payments:true,body:{claimKey}})).status,200);
assert.equal((await request('/stage-executions/'+id+'/claim',{method:'POST',payments:true,body:{claimKey}})).status,200);
assert.equal((await request('/stage-executions/'+id+'/claim',{method:'POST',payments:true,body:{claimKey:crypto.randomUUID()}})).status,409);
assert.equal((await request('/stage-executions/'+id+'/result',{method:'POST',payments:true,body:{claimKey,zipBase64:'aGVsbG8=',zipSha256:'a'.repeat(64),providerCalls:1}})).status,400);
assert.equal((await request('/stage-executions/'+id+'/result',{method:'POST',payments:true,body:{claimKey,zipBase64:'aGVsbG8=',zipSha256:'a'.repeat(64),providerCalls:0}})).status,409);
assert.equal((await request('/stage-executions/'+id+'/failure',{method:'POST',payments:true,body:{claimKey,error:'Falha sintética preservada.'}})).status,200);
assert.equal((await request('/stage-executions/'+id+'/failure',{method:'POST',payments:true,body:{claimKey,error:'Falha sintética preservada.'}})).status,200);proof.push('Claims e falhas idempotentes; callback inválido não conclui a etapa nem renova quota.');
const report=(await request('/internal/cycles/7007/report')).data;assert.equal(report.providerCalls,0);assert.equal(report.commercialEvidenceEligible,false);
const ready=report.sessions.find(s=>s.status==='READY');assert.ok(ready);
const recovery=await create(8007,7007,ready.scenarioCode);const restored=await request('/input',{method:'PUT',session:recovery.data.sessionToken,body:ready.input});assert.equal(restored.data.manifest.zipSha256,ready.manifest.zipSha256);
const eventId=crypto.randomUUID();for(let i=0;i<2;i++)assert.equal((await request('/events',{method:'POST',session:recovery.data.sessionToken,body:{eventId,code:'READY_RESULT_USED'}})).status,200);
const session=(await request('/session',{session:recovery.data.sessionToken})).data;assert.equal(session.events.filter(e=>e.eventId===eventId).length,1);
for(let i=0;i<10;i++){const response=await request('/download',{session:recovery.data.sessionToken});assert.equal(response.status,200);assert.ok(response.bytes>50000);}
assert.equal((await request('/download',{session:recovery.data.sessionToken})).status,409);
assert.equal((await request('/assets/private-secret.png',{session:recovery.data.sessionToken})).status,409);
assert.equal((await request('/internal/sessions/'+recovery.data.sessionId,{method:'DELETE'})).status,200);
assert.equal((await request('/session',{session:recovery.data.sessionToken})).status,401);proof.push('Reutilização preserva hash; dez transferências; eventos sem duplicação e revogação bloqueia acesso.');
assert.ok(!JSON.stringify(report).includes(token));assert.ok(report.sessions.every(s=>s.events.every(e=>e.origin==='AGENT_VALIDATION'&&e.mh_internal_test===true)));proof.push('Auditoria sem credenciais e eventos técnicos inelegíveis como venda.');
await writeFile(process.argv[2],JSON.stringify({contractVersion:'PRIVATE_KIT_CONTROLS_V1',status:'PASS',observedAt:new Date().toISOString(),checks:proof,providerCalls:0,externalEffects:0},null,2));console.log('Controles privados: '+proof.length+' grupos aprovados.');
