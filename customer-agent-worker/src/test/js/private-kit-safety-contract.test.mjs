import assert from 'node:assert/strict';
import test from 'node:test';
import { validateKitSafety, validatePreservedKitSafety } from '../../main/resources/browser/private-kit-safety-contract.mjs';

function fixture(productId=8007,cycleId=7007,safetyCase='UNVERIFIED_VISUAL_ORIGIN') {
  const presentation={title:'A preparação deste kit foi bloqueada',introduction:'Nenhum pacote foi gerado nesta tentativa.',reasonCode:safetyCase,nextStep:'A verificação cabe à equipe do produto. Seus dados estão guardados e esta tentativa permanece bloqueada.',nextActionLabel:'Entender a verificação das imagens',nextActionPath:'#kit-continuation'};
  const session={id:'synthetic-safety-session',productId,cycleId,prototypeVersion:'synthetic-v2',status:'BLOCKED_SAFE',reason:'O limite registrado interrompeu esta tentativa.',presentation,input:{email:'teste+seguranca@sandbox.local'},events:[{code:'SAFETY_LIMIT_BLOCKED'}],manifest:null};
  const sourceUrl=productId===8007?'https://private.example/mh-api/pde/kit/private/v1/prototype':'http://127.0.0.1:57282/api/pde/kit/private/v1/prototype';
  const observed={title:presentation.title,introduction:presentation.introduction,reason:session.reason,nextStep:presentation.nextStep,actionLabel:presentation.nextActionLabel,nextActionUrl:sourceUrl+presentation.nextActionPath,actionVisible:true,continuationVisible:true,resultHidden:true};
  return {session,observed,context:{productId,cycleId,safetyCase,sourceUrl}};
}

test('aprova orientação e reabertura de dois contextos sem gerar resultado ou efeito externo',()=>{
  for(const args of [[8007,7007,'UNVERIFIED_VISUAL_ORIGIN'],[8018,7018,'EXTERNAL_ACTION']]){
    const {session,observed,context}=fixture(...args);
    const proof=validatePreservedKitSafety(session,structuredClone(session),observed,context);
    assert.equal(proof.persistedAfterReload,true);assert.equal(proof.resultGenerated,false);assert.equal(proof.providerCalled,false);assert.equal(proof.code,args[2]);
  }
});

test('recusa resultado inexistente, instrução administrativa e continuação fora da experiência',()=>{
  for(const corrupt of [
    f=>{f.observed.title='Seu próximo post, pronto para usar';},
    f=>{f.observed.introduction='Confira o resultado e guarde seu pacote.';},
    f=>{f.session.reason='';f.observed.reason='';},
    f=>{f.session.presentation.nextStep='';f.observed.nextStep='';},
    f=>{f.observed.actionVisible=false;},
    f=>{f.observed.continuationVisible=false;},
    f=>{f.session.presentation.nextStep='Confira a revisão deste ciclo e prepare uma entrada compatível.';f.observed.nextStep=f.session.presentation.nextStep;},
    f=>{f.session.presentation.nextStep='Confira as imagens antes de tentar de novo.';f.observed.nextStep=f.session.presentation.nextStep;},
    f=>{f.observed.nextActionUrl='http://191.252.181.168:5173/business-process-chains/learning-cycles?productId=999&cycleId=888';},
    f=>{f.observed.nextActionUrl='https://private.example/other#kit-continuation';},
    f=>{f.observed.nextActionUrl=f.context.sourceUrl+'?sessionToken=synthetic-must-not-appear#kit-continuation';},
    f=>{f.observed.nextActionUrl=f.context.sourceUrl+'#other';},
    f=>{f.session.manifest={files:[]};},
    f=>{f.session.events.push({code:'VALUE_MOMENT'});},
  ]){
    const f=fixture();corrupt(f);assert.throws(()=>validateKitSafety(f.session,f.observed,f.context),/SAFETY/);
  }
});

test('recusa perda de bloqueio, entrada ou orientação ao reabrir a mesma sessão',()=>{
  for(const corrupt of [
    f=>{f.session.status='READY';},
    f=>{f.session.input.email='teste+outra@sandbox.local';},
    f=>{f.session.presentation.nextStep='Outra orientação';f.observed.nextStep='Outra orientação';},
    f=>{f.session.events.push({code:'EXPERIENCE_STARTED'});},
    f=>{f.session.id='other-session';},
  ]){
    const f=fixture();const before=structuredClone(f.session);corrupt(f);assert.throws(()=>validatePreservedKitSafety(before,f.session,f.observed,f.context),/SAFETY/);
  }
});
