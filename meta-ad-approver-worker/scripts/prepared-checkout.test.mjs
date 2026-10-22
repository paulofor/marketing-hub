import assert from 'node:assert/strict';
import test from 'node:test';
import {preparedCheckoutVerified} from '../src/main/resources/mcp/landing-evidence.mjs';
const landing='https://candidate.sandbox.local';
const checkout='https://checkout.sandbox.local/offer';
const evidence={contractVersion:'PDE_PREPARED_CHECKOUT_REVIEW_V1',verificationStatus:'VERIFIED',reviewMode:'PLANNED_PDE_PREPARATION',activationAuthorized:false,landingUrl:landing,checkoutUrl:checkout,experimentId:106,productVersion:'fixture-v14',priceBrl:67,billingModel:'ONE_TIME'};
const offer={checkoutUrl:checkout,productVersion:'fixture-v14',experimentId:106,priceBrl:67,bindingPriceBrl:67,billingModel:'ONE_TIME',bindingBillingModel:'ONE_TIME'};
test('configuração candidata conciliada não depende de link comercial já ativo',()=>{
  assert.equal(preparedCheckoutVerified(evidence,landing,checkout,[offer],true),true);
  assert.equal(preparedCheckoutVerified({...evidence,experimentId:922},landing,checkout,[{...offer,experimentId:922}],true),true);
});
test('checkout isolado, preço, versão, experimento ou cobrança divergentes continuam bloqueados',()=>{
  for(const change of [{productVersion:'old'},{experimentId:922},{priceBrl:79},{bindingPriceBrl:79},{billingModel:'RECURRING'},{bindingBillingModel:'RECURRING'},{checkoutUrl:'https://other.sandbox.local'}])
    assert.equal(preparedCheckoutVerified(evidence,landing,checkout,[{...offer,...change}],true),false,JSON.stringify(change));
  assert.equal(preparedCheckoutVerified(evidence,landing,checkout,[offer],false),false);
  assert.equal(preparedCheckoutVerified(null,landing,checkout,[offer],true),false);
});
test('a exceção não concede ativação nem se aplica a outro modo ou estado',()=>{
  for(const change of [{activationAuthorized:true},{verificationStatus:'INCOMPLETE'},{reviewMode:'LIVE'},{landingUrl:'https://other.sandbox.local'},{contractVersion:'UNKNOWN'}])
    assert.equal(preparedCheckoutVerified({...evidence,...change},landing,checkout,[offer],true),false);
});

// A inspeção lê página, contrato carregado em rede e checkout, sem realizar compra.
import {createServer} from 'node:http';
import {createRequire} from 'node:module';
import {captureCommercialLanding} from '../src/main/resources/mcp/landing-evidence.mjs';
const require=createRequire(import.meta.url);
const {chromium}=require('playwright-core');

test('inspeção real do navegador preserva link falso e comprova somente a configuração candidata',async t=>{
  let base,experimentId=106,posts=0;
  const server=createServer((req,res)=>{
    if(req.method!=='GET')posts++;
    if(req.url==='/product') {
      res.writeHead(200,{'content-type':'application/json'});
      return res.end(JSON.stringify({experienceVersion:'fixture-v14',commercialBinding:{experimentId,priceBrl:67,billingModel:'ONE_TIME'},commercialCheckout:{checkoutUrl:`${base}/checkout`,priceBrl:67,billingModel:'ONE_TIME',provider:'PEPPER',currency:'BRL'}}));
    }
    res.writeHead(200,{'content-type':'text/html; charset=utf-8'});
    res.end(req.url==='/checkout'
      ? '<html><body><h1>Compra de demonstração — R$ 67</h1><p>Pagamento único, sem assinatura. Esta página é um test double local do checkout, sem compra, gateway ou chamada externa.</p></body></html>'
      : '<html><body><h1>Primeiro ajuste gratuito</h1><p>Informe ocasião e peças para experimentar. A continuidade possui sete dias e materiais de apoio, com acesso de noventa dias. O checkout permanece bloqueado nesta candidata enquanto a homologação comercial ainda não terminou. Esta página é uma fixture local.</p><script>fetch("/product");</script></body></html>');
  });
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  base=`http://127.0.0.1:${server.address().port}`;
  const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_BIN?{executablePath:process.env.CHROMIUM_BIN}:{}),args:['--disable-dev-shm-usage']});
  t.after(async()=>{await browser.close();await new Promise(r=>server.close(r));});
  for(const id of [106,922]) {
    experimentId=id;
    const proof={...evidence,experimentId:id,landingUrl:base,checkoutUrl:`${base}/checkout`};
    const inspected=await captureCommercialLanding(browser,base,proof.checkoutUrl,proof);
    assert.equal(inspected.checkoutLinkedFromLanding,false);
    assert.equal(inspected.preparedCheckoutVerified,true);
    assert.equal(Boolean(inspected.checkout),true);
    assert.equal(inspected.checkout.interactionPerformed,false);
    assert.equal(inspected.screenshots.length,2);
    assert.ok(inspected.checkout.screenshot.length>100);
  }
  const unverified=await captureCommercialLanding(browser,base,`${base}/checkout`);
  assert.equal(unverified.checkoutLinkedFromLanding,false);
  assert.equal(unverified.preparedCheckoutVerified,false);
  assert.equal(posts,0);
});
