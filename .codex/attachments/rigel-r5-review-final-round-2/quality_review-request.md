# Etapa: Quality Review comercial e visual (landing-page-quality-review)

template_id: landing-page-quality-review

Você é o avaliador final de qualidade comercial e visual do GeraLanding.

Seu papel é avaliar screenshots renderizados da landing page como se você fosse um visitante real vindo de tráfego pago: frio, cético, distraído e com pouca paciência.

A landing do GeraLanding não deve apenas parecer bonita. Ela precisa cumprir uma função comercial específica:

1. explicar rapidamente a dor do público;
2. tornar desejável o resultado prometido;
3. mostrar um mecanismo plausível para chegar ao resultado;
4. oferecer uma microprova concreta do valor da solução;
5. fazer o visitante sentir que vale a pena avançar para a ação principal: checkout em `SALES` ou e-mail em `LEADS`;
6. conduzir visualmente para a ação principal sem confusão.
7. reduzir carga cognitiva e tornar facil imaginar a melhoria concreta depois da acao.

Use as imagens como evidência principal. Avalie o que aparece na tela, não o que provavelmente estava no briefing. Não recompense intenção invisível.

Além dos screenshots, você receberá o HTML final consolidado `htmlGeraLanding` e, quando disponível, o contrato de promessa única. Use os screenshots como evidência principal do que o visitante vê, use o HTML para confirmar problemas técnicos/textuais observáveis no artefato final e use o contrato para verificar coerência comercial entre dor, prova/recompensa, promessa e CTA.

## Contrato de promessa única recebido

- Dor única: Você responde um orçamento no WhatsApp, o cliente some e você fica sem saber qual mensagem mandar depois sem parecer insistente — aí a conversa morre e você perde o timing do “fechamos ou não?”.
- Prova/preview ou recompensa única: Demonstração real na página: uma sequência completa para retomar um orçamento sem parecer insistente, com três follow-ups respeitosos e perguntas de qualificação. A demonstração prova o método sem prometer conversão nem entregar gratuitamente a implantação completa.
- Promessa do funil: Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.
- CTA principal: Quero meu atendimento sob medida
- Objetivo da campanha: SALES

Se esses campos estiverem preenchidos, penalize qualquer landing que troque a prova/recompensa por diagnóstico, prévia genérica, material vago, consultoria ou sistema completo fora do contrato. Se `campaignObjective` for `SALES`, penalize formulário/captura competindo com checkout e CTA principal que prometa receber amostra gratuita.

## Arquivo enviado para avaliação de causa-raiz

### HTML final do GeraLanding (`htmlGeraLanding`)

```html
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Atendimento de WhatsApp personalizado e revisado, com respostas, perguntas, follow-ups e regras prontas em até 48 horas após pagamento e briefing mínimo.">
  <title>Kit WhatsApp Pronto — atendimento sob medida</title>
  <style>
    :root{color-scheme:light;--ink:#17201c;--muted:#59665f;--paper:#fbfaf6;--surface:#ffffff;--soft:#eef4ee;--line:#dce4dd;--brand:#176b4b;--brand-dark:#0d4f37;--accent:#e6b85c;--wa:#25d366;--shadow:0 18px 50px rgba(23,32,28,.10);--radius:24px;--max:1160px}
    *{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--paper);color:var(--ink);font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;font-size:17px;line-height:1.65;-webkit-font-smoothing:antialiased}img{display:block;max-width:100%;height:auto}a{color:inherit}:focus-visible{outline:3px solid var(--accent);outline-offset:4px}.skip{position:absolute;left:16px;top:-80px;z-index:20;padding:10px 14px;background:var(--ink);color:#fff;border-radius:8px}.skip:focus{top:16px}.shell{width:min(calc(100% - 40px),var(--max));margin-inline:auto}.eyebrow{display:inline-flex;align-items:center;gap:8px;margin:0 0 18px;padding:7px 12px;border:1px solid #c9dbcd;border-radius:999px;background:#f3f8f3;color:var(--brand-dark);font-size:.78rem;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.dot{width:8px;height:8px;border-radius:50%;background:var(--wa);box-shadow:0 0 0 5px rgba(37,211,102,.12)}h1,h2,h3{margin:0;line-height:1.08;letter-spacing:-.035em;text-wrap:balance}h1{font-size:clamp(2.5rem,6.7vw,5.35rem);max-width:850px}h2{font-size:clamp(2rem,4.4vw,3.55rem)}h3{font-size:clamp(1.15rem,2vw,1.45rem)}p{margin:0}.lead{font-size:clamp(1.08rem,2vw,1.28rem);color:var(--muted);max-width:690px}.fine{font-size:.86rem;color:var(--muted)}.nav{border-bottom:1px solid rgba(220,228,221,.85);background:rgba(251,250,246,.94)}.nav-inner{min-height:72px;display:flex;align-items:center;justify-content:space-between;gap:18px}.brand{font-size:.98rem;font-weight:900;letter-spacing:-.02em}.brand span{color:var(--brand)}.nav-note{font-size:.84rem;color:var(--muted);text-align:right}.hero{padding:clamp(56px,9vw,110px) 0 72px;overflow:hidden}.hero-grid{display:grid;grid-template-columns:minmax(0,1.08fr) minmax(340px,.72fr);gap:60px;align-items:center}.hero-copy strong{color:var(--brand)}.hero .lead{margin-top:24px}.hero-actions{display:flex;flex-wrap:wrap;align-items:center;gap:16px;margin-top:32px}.button{display:inline-flex;min-height:56px;align-items:center;justify-content:center;padding:14px 22px;border-radius:14px;background:var(--brand);color:#fff;text-decoration:none;font-weight:850;box-shadow:0 10px 24px rgba(23,107,75,.19);transition:background .2s ease,transform .2s ease}.button:hover{background:var(--brand-dark);transform:translateY(-1px)}.button:active{transform:translateY(0)}.micro{display:flex;align-items:center;gap:9px;color:var(--muted);font-size:.9rem}.micro svg{flex:0 0 auto;color:var(--brand)}.hero-card{position:relative;padding:16px;border:1px solid var(--line);border-radius:30px;background:var(--surface);box-shadow:var(--shadow)}.hero-card:before{content:"";position:absolute;z-index:-1;inset:-18px -24px auto auto;width:180px;height:180px;border-radius:50%;background:#dff1e5;filter:blur(2px)}.hero-card img{width:100%;border-radius:19px;border:1px solid #e6ebe7}.proof-caption{display:flex;justify-content:space-between;gap:14px;padding:14px 5px 2px;font-size:.8rem;color:var(--muted)}.proof-caption strong{color:var(--brand-dark)}.trust-row{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;margin-top:42px;border:1px solid var(--line);border-radius:18px;overflow:hidden;background:var(--line);max-width:760px}.trust-item{padding:16px 18px;background:var(--surface)}.trust-item strong{display:block;font-size:.98rem}.trust-item span{display:block;color:var(--muted);font-size:.78rem}section{padding:clamp(72px,9vw,112px) 0}.section-head{display:grid;grid-template-columns:minmax(0,.8fr) minmax(320px,.55fr);gap:70px;align-items:end;margin-bottom:48px}.section-head .lead{justify-self:end}.demo{background:var(--ink);color:#fff}.demo .eyebrow{background:rgba(255,255,255,.08);border-color:rgba(255,255,255,.18);color:#ccebd8}.demo .lead,.demo .fine{color:#bac5bf}.demo-grid{display:grid;grid-template-columns:minmax(300px,.72fr) minmax(0,1.1fr);gap:64px;align-items:start}.sticky{position:sticky;top:28px}.demo-intro .lead{margin-top:22px}.demo-note{margin-top:30px;padding:18px;border-left:3px solid var(--accent);background:rgba(255,255,255,.055);border-radius:0 12px 12px 0}.sequence{display:grid;gap:14px}.message{width:min(100%,620px);padding:19px 21px;border:1px solid rgba(255,255,255,.12);border-radius:18px;background:#25312b;box-shadow:0 10px 26px rgba(0,0,0,.13)}.message:nth-child(even){margin-left:auto;background:#194c37}.message-tag{display:flex;align-items:center;gap:9px;margin-bottom:8px;color:#a7d8bb;font-size:.75rem;font-weight:800;text-transform:uppercase;letter-spacing:.08em}.message p{font-size:.97rem}.message small{display:block;margin-top:8px;color:#aeb9b3;font-size:.76rem}.sequence-result{margin-top:16px;padding:22px;border:1px solid rgba(230,184,92,.35);border-radius:18px;background:rgba(230,184,92,.09)}.sequence-result strong{color:#f1d79f}.gallery{background:var(--soft)}.gallery-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(280px,.8fr);gap:18px;align-items:start}.proof{margin:0;padding:12px;border:1px solid var(--line);border-radius:22px;background:var(--surface);box-shadow:0 12px 28px rgba(23,32,28,.06)}.proof:first-child{grid-column:1;grid-row:1}.proof:nth-child(2){grid-column:1;grid-row:2}.proof:nth-child(3){grid-column:2;grid-row:1/span 2}.proof img{width:100%;border:1px solid #e8ece9;border-radius:14px}.proof figcaption{padding:14px 6px 6px}.proof figcaption strong{display:block}.proof figcaption span{color:var(--muted);font-size:.86rem}.scope-grid{display:grid;grid-template-columns:minmax(0,.86fr) minmax(330px,.62fr);gap:70px;align-items:start}.scope-copy .lead{margin-top:22px}.check-list{display:grid;gap:13px;margin:32px 0 0;padding:0;list-style:none}.check-list li{display:grid;grid-template-columns:26px 1fr;gap:12px}.check{width:24px;height:24px;display:grid;place-items:center;border-radius:50%;background:#dff2e5;color:var(--brand-dark);font-weight:900;font-size:.78rem}.scope-card{padding:28px;border:1px solid var(--line);border-radius:var(--radius);background:var(--surface);box-shadow:var(--shadow)}.scope-card .label{font-size:.74rem;font-weight:850;letter-spacing:.09em;text-transform:uppercase;color:var(--brand)}.scope-card h3{margin-top:8px}.scope-card p{margin-top:12px;color:var(--muted)}.not-kit{margin-top:22px;padding:18px;border-radius:15px;background:#fff8e7;border:1px solid #eed9a4;color:#5e4b24;font-size:.92rem}.process{background:#f2eee6}.steps{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:46px}.step{padding:26px;border:1px solid #ded8cc;border-radius:var(--radius);background:rgba(255,255,255,.65)}.step-number{display:grid;place-items:center;width:38px;height:38px;border-radius:12px;background:var(--ink);color:#fff;font-weight:900;margin-bottom:22px}.step p{margin-top:11px;color:var(--muted);font-size:.94rem}.deadline{margin-top:24px;padding:18px 22px;border-radius:16px;background:var(--surface);border:1px solid #ded8cc;text-align:center;font-weight:750}.offer{background:var(--brand-dark);color:#fff}.offer-grid{display:grid;grid-template-columns:minmax(0,.9fr) minmax(330px,.58fr);gap:70px;align-items:center}.offer .lead{margin-top:22px;color:#c8ddd2}.truth{margin-top:26px;color:#b8cfc3;font-size:.9rem;max-width:620px}.price-card{padding:32px;border-radius:28px;background:#fff;color:var(--ink);box-shadow:0 25px 70px rgba(0,0,0,.23)}.price-card .overline{font-size:.76rem;font-weight:850;letter-spacing:.09em;text-transform:uppercase;color:var(--brand)}.price{display:flex;align-items:flex-start;gap:5px;margin:10px 0 2px;font-weight:950;letter-spacing:-.06em}.price .currency{font-size:1.1rem;margin-top:14px}.price .amount{font-size:4.6rem;line-height:1}.price-note{color:var(--muted);font-size:.88rem}.price-card .button{width:100%;margin-top:24px}.secure{display:flex;align-items:center;justify-content:center;gap:8px;margin-top:14px;color:var(--muted);font-size:.78rem;text-align:center}.faq-list{display:grid;gap:12px;margin-top:44px}details{border:1px solid var(--line);border-radius:16px;background:var(--surface)}summary{cursor:pointer;padding:20px 22px;font-weight:820;list-style:none}summary::-webkit-details-marker{display:none}summary:after{content:"+";float:right;color:var(--brand);font-size:1.35rem;line-height:1}details[open] summary:after{content:"−"}details p{padding:0 22px 22px;color:var(--muted);max-width:850px}.final{padding-top:40px}.final-card{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:32px;align-items:center;padding:clamp(28px,5vw,52px);border-radius:30px;background:var(--ink);color:#fff}.final-card p{margin-top:12px;color:#bdc8c2}footer{padding:30px 0 42px;color:var(--muted);font-size:.8rem}.footer-row{display:flex;justify-content:space-between;gap:28px;align-items:flex-start}.footer-copy{max-width:680px}.footer-copy strong{display:block;color:var(--ink);margin-bottom:5px}.footer-links{display:flex;flex-wrap:wrap;gap:10px 18px}.footer-links a{color:var(--brand-dark);font-weight:750}
    @media(max-width:900px){.hero-grid,.demo-grid,.scope-grid,.offer-grid{grid-template-columns:1fr;gap:42px}.section-head{grid-template-columns:1fr;gap:18px}.section-head .lead{justify-self:start}.sticky{position:static}.gallery-grid{grid-template-columns:minmax(0,1.2fr) minmax(260px,.8fr)}.steps{grid-template-columns:1fr}.final-card{grid-template-columns:1fr}}
    @media(max-width:620px){body{font-size:16px}.shell{width:min(calc(100% - 28px),var(--max))}.nav-inner{min-height:64px}.nav-note{max-width:150px;font-size:.72rem}.hero{padding-top:46px}.hero-actions{align-items:stretch}.hero-actions .button{width:100%}.trust-row{grid-template-columns:1fr}.trust-item{display:grid;grid-template-columns:1fr 1fr;align-items:center;gap:12px}.trust-item span{text-align:right}.gallery-grid{grid-template-columns:1fr}.proof:first-child,.proof:nth-child(2),.proof:nth-child(3){grid-column:auto;grid-row:auto}.proof:nth-child(3){width:min(100%,290px);justify-self:center}.price-card{padding:25px 20px}.price .amount{font-size:4rem}.final-card .button{width:100%}.proof-caption{flex-direction:column;gap:4px}.footer-row{flex-direction:column}.footer-links{display:grid;gap:8px}h1{font-size:clamp(2.35rem,13vw,3.7rem)}}
    @media(prefers-reduced-motion:reduce){html{scroll-behavior:auto}*,*:before,*:after{transition:none!important}}
  </style>
</head>
<body>
  <a class="skip" href="#conteudo">Pular para o conteúdo</a>
  <header class="nav" aria-label="Cabeçalho"><div class="shell nav-inner"><div class="brand">Kit WhatsApp <span>Pronto</span></div><div class="nav-note">Implantação personalizada · R$ 349</div></div></header>
  <main id="conteudo">
    <section class="hero" aria-labelledby="hero-title"><div class="shell hero-grid"><div class="hero-copy"><p class="eyebrow"><span class="dot" aria-hidden="true"></span>Conversa que avança</p><h1 id="hero-title">Seu orçamento ficou no vácuo. <strong>O próximo passo não precisa ficar.</strong></h1><p class="lead">Se você presta serviços e envia orçamentos pelo WhatsApp, receba um atendimento feito para o seu negócio — com respostas, perguntas, follow-ups e regras revisadas para saber o que mandar sem soar insistente.</p><div class="hero-actions"><a class="button" id="checkout-cta-primary" href="https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081">Quero meu atendimento sob medida</a><span class="micro">◷ Prévia em até 12h · entrega em até 48h*</span></div><div class="trust-row" aria-label="Resumo da oferta"><div class="trust-item"><strong>Sob medida</strong><span>não é pacote genérico</span></div><div class="trust-item"><strong>Revisado</strong><span>antes da entrega</span></div><div class="trust-item"><strong>R$ 349</strong><span>pagamento no checkout</span></div></div><p class="fine" style="margin-top:14px">*Prazo contado após o pagamento confirmado e o briefing mínimo completo. O briefing leva cerca de 15 a 25 minutos e deve usar exemplos anonimizados.</p></div><figure class="hero-card"><img src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/182deed413e5-rigel-tasting-response.png" alt="Recorte demonstrativo de uma resposta de atendimento no WhatsApp" decoding="async" fetchpriority="high"><figcaption class="proof-caption"><strong>Recorte demonstrativo</strong><span>Amostra ilustrativa e não clicável</span></figcaption></figure></div></section>
    <section class="demo" aria-labelledby="demo-title"><div class="shell demo-grid"><div class="demo-intro sticky"><p class="eyebrow">Veja antes de decidir</p><h2 id="demo-title">Uma retomada respeitosa tem ritmo, contexto e saída.</h2><p class="lead">A lógica não é cobrar resposta. É facilitar uma decisão clara — inclusive quando a resposta for “agora não”.</p><div class="demo-note"><strong>O que esta demonstração prova:</strong><p class="fine">como uma sequência pode retomar contexto, qualificar a necessidade e propor um próximo passo sem prometer conversão.</p></div></div><div><div class="sequence" aria-label="Exemplo de sequência de follow-up"><article class="message"><div class="message-tag">Follow-up 1 · contexto</div><p>Oi, Ana. Passei para confirmar se você conseguiu ver o orçamento que enviei ontem. Ficou alguma dúvida sobre o que está incluído?</p><small>Abre espaço para uma objeção real, sem pressão.</small></article><article class="message"><div class="message-tag">Pergunta de qualificação</div><p>Para eu te orientar melhor: hoje pesa mais para você o prazo, a forma de pagamento ou ajustar o escopo?</p><small>Troca “e aí?” por uma pergunta fácil de responder.</small></article><article class="message"><div class="message-tag">Follow-up 2 · próximo passo</div><p>Se ainda fizer sentido, posso separar duas opções mais objetivas para você comparar. Quer que eu envie?</p><small>Pede permissão antes de avançar.</small></article><article class="message"><div class="message-tag">Follow-up 3 · encerramento respeitoso</div><p>Vou encerrar por aqui para não ocupar seu WhatsApp. Se quiser retomar depois, me diga “quero rever” e eu continuo do ponto em que paramos.</p><small>Preserva a relação e deixa uma saída simples.</small></article></div><div class="sequence-result"><strong>Na sua entrega, essa lógica é adaptada.</strong> O tom, as perguntas, os intervalos e os próximos passos consideram o seu atendimento e o tipo de cliente que você recebe.</div></div></div></section>
    <section class="gallery" aria-labelledby="gallery-title"><div class="shell"><div class="section-head"><div><p class="eyebrow">Amostras do formato</p><h2 id="gallery-title">Veja recortes demonstrativos da entrega.</h2></div><p class="lead">As amostras abaixo são ilustrativas e não clicáveis. Elas demonstram respostas, perguntas, follow-ups e organização; a implantação completa é personalizada depois do briefing.</p></div><div class="gallery-grid"><figure class="proof"><img src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/17c1af1709af-rigel-tasting-question.png" alt="Recorte demonstrativo de pergunta de qualificação para WhatsApp" decoding="async"><figcaption><strong>Perguntas que dão direção</strong><span>Recorte ilustrativo e não clicável para descobrir o que impede o próximo passo.</span></figcaption></figure><figure class="proof"><img src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/f25cb72bf5e3-rigel-tasting-followups.png" alt="Recorte demonstrativo de sequência de três follow-ups respeitosos" decoding="async"><figcaption><strong>Follow-ups com função</strong><span>Recorte ilustrativo e não clicável; cada contato tem objetivo e saída.</span></figcaption></figure><figure class="proof"><img src="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/commercial-plans/visual-assets/2026/08/25/misc/61511a55e5ee-rigel-offer-proof.png" alt="Recorte demonstrativo da organização da oferta personalizada" decoding="async"><figcaption><strong>Oferta explicada com clareza</strong><span>Recorte ilustrativo e não clicável do escopo e do próximo passo.</span></figcaption></figure></div></div></section>
    <section aria-labelledby="scope-title"><div class="shell scope-grid"><div class="scope-copy"><p class="eyebrow">O que você recebe</p><h2 id="scope-title">Um sistema de atendimento aplicado ao seu jeito de vender.</h2><p class="lead">Após entender o seu negócio, organizamos as mensagens e as regras que ajudam você a conduzir conversas comerciais com menos improviso.</p><ul class="check-list"><li><span class="check" aria-hidden="true">✓</span><span><strong>10 a 20 respostas personalizadas</strong><br><span class="fine">Para dúvidas e momentos recorrentes do seu atendimento.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>5 a 10 perguntas de qualificação</strong><br><span class="fine">Para entender prioridade, objeção e intenção antes do próximo passo.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>3 a 5 follow-ups manuais</strong><br><span class="fine">Com objetivo, contexto e encerramento — sem disparo automático.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Regras de escalonamento</strong><br><span class="fine">Quando responder, esperar, encerrar ou tratar o caso individualmente.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Guia, checklist e revisão humana</strong><br><span class="fine">Pacote editável em área privada, acompanhado por sete materiais de apoio.</span></span></li></ul></div><aside class="scope-card" aria-label="Diferença entre um kit genérico e a oferta personalizada"><span class="label">A diferença central</span><h3>Você não compra um arquivo igual para todo mundo.</h3><p>O briefing guiado leva cerca de 15 a 25 minutos e reúne serviços, dúvidas, políticas, tom e exemplos anonimizados. A entrega fica disponível para download e edição em uma área privada ligada ao e-mail da compra.</p><div class="not-kit"><strong>Não é software, bot, integração, disparo ou assinatura.</strong> Também não inclui promessa de resposta, conversão, faturamento ou agenda cheia.</div></aside></div></section>
    <section class="process" aria-labelledby="process-title"><div class="shell"><div class="section-head"><div><p class="eyebrow">Do pagamento à entrega</p><h2 id="process-title">Quatro passos, sem mistério.</h2></div><p class="lead">Depois do pagamento, você acessa o briefing, valida uma prévia e recebe o pacote completo para uso manual.</p></div><div class="steps"><article class="step"><span class="step-number">1</span><h3>Briefing guiado</h3><p>Após o pagamento, acesse com o e-mail da compra e complete em 15 a 25 minutos, usando exemplos sem dados pessoais.</p></article><article class="step"><span class="step-number">2</span><h3>Prévia para validar o tom</h3><p>Com a entrada completa, você recebe uma primeira sequência em até 12 horas para confirmar a direção.</p></article><article class="step"><span class="step-number">3</span><h3>Entrega completa</h3><p>Em até 48 horas, o pacote editável e revisado fica disponível para download na área privada.</p></article><article class="step"><span class="step-number">4</span><h3>Primeira aplicação</h3><p>Escolha um bloco pequeno, revise e use manualmente no atendimento real durante a primeira semana.</p></article></div><div class="deadline">Prazo: até 48 horas após o pagamento confirmado <em>e</em> o briefing mínimo completo.</div></div></section>
    <section class="offer" aria-labelledby="offer-title"><div class="shell offer-grid"><div><p class="eyebrow">Implantação personalizada</p><h2 id="offer-title">Menos “o que eu mando agora?”. Mais próximo passo claro.</h2><p class="lead">Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.</p><p class="truth">O serviço organiza sua comunicação. Não garante resposta, conversão, faturamento ou agenda cheia.</p></div><aside class="price-card" aria-label="Preço e compra"><span class="overline">Kit WhatsApp Pronto · sob medida</span><div class="price"><span class="currency">R$</span><span class="amount">349</span></div><p class="price-note">Pagamento único, sem recorrência. Pacote personalizado, editável e revisado.</p><a class="button" data-analytics-role="primary-checkout" href="https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081">Quero meu atendimento sob medida</a><div class="secure">▣ Checkout canônico do Mercado Pago</div><p class="fine" style="margin-top:12px;text-align:center">Ao comprar, você recebe o acesso ao briefing pelo e-mail da compra. Seus dados são usados somente para personalizar e entregar o serviço conforme a Política de Privacidade.</p></aside></div></section>
    <section aria-labelledby="faq-title"><div class="shell"><p class="eyebrow">Antes de comprar</p><h2 id="faq-title">Dúvidas importantes</h2><div class="faq-list"><details><summary>É um pacote de mensagens genéricas?</summary><p>Não. Os exemplos da página demonstram o método. A entrega é montada após o briefing mínimo e revisada para o contexto informado por você.</p></details><details><summary>Quando começa o prazo de até 48 horas?</summary><p>Quando as duas condições estiverem concluídas: pagamento confirmado e briefing mínimo completo. Se faltar informação essencial, o prazo ainda não começou.</p></details><details><summary>Isso garante que o cliente vai responder ou comprar?</summary><p>Não. O atendimento ajuda você a formular respostas, perguntas e próximos passos com mais clareza, mas não controla a decisão de outra pessoa e não promete conversão ou faturamento.</p></details><details><summary>O que preciso informar no briefing?</summary><p>O contexto mínimo do seu negócio e do atendimento: o que você oferece, quem costuma pedir orçamento, dúvidas recorrentes e como as conversas normalmente avançam ou travam.</p></details><details><summary>Preciso usar as mensagens palavra por palavra?</summary><p>Não. Você recebe uma base organizada e regras de uso para adaptar naturalmente ao contexto de cada conversa.</p></details><details><summary>Como recebo e edito o material?</summary><p>O pacote completo fica disponível para download em uma área privada ligada ao e-mail da compra. Os materiais são editáveis e foram feitos para aplicação manual no WhatsApp.</p></details><details><summary>Como meus dados são usados?</summary><p>Use exemplos anonimizados no briefing. As informações comerciais fornecidas são usadas somente para personalizar, revisar e entregar o serviço, conforme os termos e a política de privacidade.</p></details></div></div></section>
    <section class="final" aria-labelledby="final-title"><div class="shell final-card"><div><h2 id="final-title">Seu próximo follow-up pode começar com uma regra clara.</h2><p>Implantação personalizada e revisada por R$ 349, entregue em até 48 horas após pagamento e briefing mínimo.</p></div><a class="button" data-analytics-role="primary-checkout" href="https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081">Quero meu atendimento sob medida</a></div></section>
  </main>
  <footer><div class="shell footer-row"><div class="footer-copy"><strong>PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA · CNPJ 25.215.414/0001-69</strong><span>Rua Antonio Basilio, 204, apto 805 · Tijuca · Rio de Janeiro/RJ · CEP 20511-190<br>Suporte: <a href="mailto:contato@digicomdigital.com.br">contato@digicomdigital.com.br</a></span></div><nav class="footer-links" aria-label="Informações legais"><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/terms" target="_blank" rel="noopener">Termos</a><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/privacy" target="_blank" rel="noopener">Privacidade</a><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy" target="_blank" rel="noopener">Cancelamento e reembolso</a></nav></div></footer>
</body>
</html>

```

### Screenshots renderizados enviados como imagens

```json
[
  {
    "viewport": "desktop",
    "role": "full-page",
    "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-desktop-full.jpg"
  },
  {
    "viewport": "desktop",
    "role": "proof-section",
    "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-desktop-proof.jpg"
  },
  {
    "viewport": "mobile",
    "role": "full-page",
    "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-iphone15pro-full.jpg"
  },
  {
    "viewport": "mobile",
    "role": "proof-section",
    "path": "/root/ai-hub/src/ai-hub-b3b98477-9dae-4483-b305-094906c24189-f486lm/repo/.codex/attachments/rigel-r5-iphone15pro-proof.jpg"
  }
]
```

Os screenshots `*-full-page` preservam a composição e a sequência completas. Quando houver
screenshots `*-proof-section`, use-os para julgar legibilidade, densidade e força das provas sem
inferir vazio ou baixa qualidade apenas pela redução necessária da página inteira. As duas visões
são complementares e devem representar o mesmo HTML e a mesma execução auditável.

Ao preencher `blockingIssues` e `recommendedRegeneration`, cite apenas problemas observáveis no `htmlGeraLanding` e nos screenshots renderizados. Quando recomendar regeneração, diferencie pela evidência final observada:

- `LANDING_PAGE_WIREFRAME`: quando a falha principal estiver na ordem, estrutura, promessa, prova, CTA ou conteúdo planejado;
- `LANDING_PAGE_DESIGN_PRESET`: quando a estrutura estiver correta, mas a percepção visual, contraste, hierarquia, espaçamento, responsividade ou acabamento estiver ruim;
- `LANDING_PAGE_HTML`: quando o problema for montagem/renderização final, HTML/CSS, overflow, corte, conteúdo visível indevido ou aplicação incorreta do preset.

Não use intenção interna de etapas anteriores para perdoar uma falha visível na landing. Se a evidência visual estiver ruim, a nota deve refletir a experiência final do visitante.

## O que a landing precisa provar

A página deve sustentar a sequência comercial:

**Dor → Resultado → Mecanismo → Prova → Oferta → Ação**

A landing deve vender primeiro a transformação percebida pelo visitante. O material gratuito, diagnóstico, checklist, plano, template, preview ou amostra deve funcionar como prova de valor e redução de risco, não como um item genérico sem desejo. Em `SALES`, essa prova deve preparar o clique no checkout, não substituir a compra.

Tambem avalie se a pagina aplica psicologia comercial de forma saudavel: reconhecimento imediato da dor, cena concreta da rotina, alivio de esforco, identidade desejada plausivel e futuro facil de visualizar.

## Critérios de avaliação

Avalie com rigor os pontos abaixo.

### 1. Primeira dobra

A primeira dobra deve deixar claro, em poucos segundos:

- para quem é a página;
- qual dor concreta está sendo tratada;
- qual resultado desejável será alcançado;
- qual mecanismo torna esse resultado plausível;
- qual ação o usuário deve tomar agora.

Penalize fortemente se a primeira dobra parecer genérica, decorativa, abstrata, sem promessa forte ou sem direção clara para o CTA.

### 2. Força da promessa

A promessa deve vender transformação, não apenas um material.

Não basta dizer que o usuário receberá um PDF, guia, checklist, diagnóstico, plano, template ou amostra. A página precisa mostrar por que isso melhora a vida, o trabalho, o negócio ou a decisão do visitante.

Penalize se a oferta estiver centrada no formato do entregável em vez do resultado prático que ele gera.

### 3. Mecanismo

A página deve explicar, de forma simples e visualmente compreensível, por que a solução pode gerar o resultado prometido.

Procure sinais como:

- etapas claras;
- processo explicado;
- método próprio;
- personalização;
- diagnóstico;
- antes/depois;
- transformação de uma entrada em uma entrega útil.

Penalize se a solução parecer mágica, vaga, genérica ou apenas uma promessa sem caminho.

### 4. Prova e amostra de valor

A landing deve mostrar uma pequena amostra do poder da solução antes de pedir a ação principal: checkout em `SALES` ou e-mail em `LEADS`.

Essa prova pode ser:

- preview do material;
- exemplo preenchido;
- mini diagnóstico;
- antes/depois;
- trecho realista da entrega;
- mockup funcional;
- demonstração do método;
- visualização concreta do resultado.

Penalize fortemente se a prova for decorativa, pequena demais, genérica, escondida, abstrata ou incapaz de aumentar confiança.

### 5. Oferta de entrada e ação principal

Em `SALES`, o CTA de checkout deve parecer o ponto natural de avanço, com baixo risco percebido, preço/valor claros quando disponíveis e prova suficiente antes do clique.

Em `LEADS`, o formulário deve parecer um ponto natural de avanço, não uma interrupção.

Verifique se fica claro:

- o que o visitante recebe ou compra ao avançar;
- por que vale a pena receber;
- qual benefício imediato ele terá;
- se há baixo risco percebido;
- se o botão vende o benefício, e não apenas a ação técnica.

Penalize CTAs genéricos como “Enviar”, “Cadastrar”, “Saiba mais” ou “Receber material” quando não estiverem conectados ao benefício imediato.

### 6. Hierarquia visual e percepção premium

A página deve parecer final, confiável e suficientemente premium para receber tráfego pago.

Avalie:

- contraste;
- espaçamento;
- alinhamento;
- ritmo visual;
- escaneabilidade;
- destaque do hero;
- destaque da prova;
- destaque do formulário;
- consistência visual;
- qualidade dos cards;
- aparência mobile e desktop.

Penalize se a página parecer wireframe, template cru, layout monótono, tela genérica, página sem acabamento ou composição visual sem intenção.

### 7. Reconhecimento psicologico e baixo esforço

A pagina deve fazer o visitante pensar rapidamente: "isso fala de mim", "esse problema me custa algo", "esse caminho parece possivel" e "o proximo passo e simples".

Penalize se a pagina exigir muito esforco mental para entender a oferta, usar linguagem abstrata demais, nao mostrar cena concreta da rotina ou nao conectar dor atual a uma melhoria futura imaginavel.

### 8. Especificidade do público

A página deve parecer feita para um público real.

Penalize se os textos e blocos poderiam servir para qualquer nicho. A dor, a promessa, a prova e a oferta devem conter sinais específicos do mercado, da situação e do desejo do público.

### 9. Coerência entre promessa, prova e CTA

Quando houver contrato de promessa única, a landing deve preservar a mesma `singlePain`, `freeReward`, `funnelPromise` e `primaryCta` do contrato.

A promessa, a microprova e o CTA precisam estar alinhados.

Penalize se:

- a headline promete uma coisa, mas a prova mostra outra;
- o CTA pede e-mail sem reforçar o valor;
- a prova não sustenta a promessa;
- a oferta parece menor do que a promessa;
- o formulário aparece antes de existir desejo suficiente.

### 10. Responsividade e ausência de falhas técnicas visíveis

Verifique desktop e mobile:

- layout quebrado;
- corte de texto;
- overflow horizontal;
- botões desalinhados;
- formulário difícil de usar;
- imagem distorcida;
- seção vazia;
- metadado técnico visível;
- texto provisório;
- debug;
- comentário interno;
- classes ou tokens aparentes;
- título provisório.

Qualquer artefato técnico visível deve pesar muito na nota.

## Escala de score calibrada

Use a escala abaixo com rigor:

- 85-100: pode estar pronta para tráfego pago quando não houver bloqueio de publicação e todos os critérios essenciais atingirem o piso definido abaixo.
- 80-84: boa landing, mas ainda com ajustes relevantes antes de escalar tráfego.
- 70-79: funcional, porém comercialmente fraca ou visualmente comum. Não deveria ser publicada sem revisão.
- 60-69: estrutura existe, mas falta força de promessa, prova, mecanismo ou acabamento.
- 40-59: bloqueio forte. Parece rascunho, genérica, pouco confiável ou confusa.
- 0-39: quebrada, incompleta, provisória, incoerente ou incapaz de converter.

## Regras de aprovação

Recomende `APPROVE_FOR_PUBLICATION` somente se todas as condições abaixo forem verdadeiras:

- `score` maior ou igual a 85;
- todos os itens de `criteriaScores` têm nota maior ou igual a 8;
- `targetAudienceSpecificity` é `medium` ou `high`;
- `commercialReadiness` é `strong` ou `excellent`;
- `blockingIssues` está vazio;
- `recommendedRegeneration` está vazio;
- primeira dobra comunica dor, resultado, mecanismo e CTA;
- existe prova ou amostra concreta do valor da solução;
- o formulário/CTA deixa claro o benefício de enviar o e-mail;
- a pagina reduz carga cognitiva e ajuda o visitante a visualizar a transformacao concreta;
- a página parece final, confiável e premium;
- não há artefato técnico, metadado, texto provisório ou layout quebrado;
- mobile e desktop estão visualmente corretos.

Em qualquer outro caso, recomende `REGENERATE_BEFORE_PUBLICATION`.

Não transforme refinamentos opcionais em bloqueios. Registre em `improvementOpportunities` ajustes que podem elevar conversão ou acabamento, mas cuja ausência não impede checkout/formulário, compreensão, confiança, responsividade ou entrega. Uma landing pode ser aprovada com oportunidades de melhoria.

Considere `blockingIssues` somente falhas que tornam inseguro receber tráfego: checkout/formulário/link inoperante, promessa ou oferta incoerente, ausência de prova suficiente, baixa legibilidade, falha responsiva, artefato técnico, baixa confiança ou critério essencial abaixo de 8.

Se a página estiver bonita, mas não vender bem a transformação, não aprove.

Se a página estiver clara, mas sem prova concreta suficiente, não aprove.

Se a página parecer genérica para qualquer público, não aprove.

Se o formulário não parecer uma troca valiosa pelo e-mail, não aprove.

## Como preencher `targetAudienceSpecificity`

- `low`: a página poderia servir para quase qualquer público ou nicho.
- `medium`: há alguns sinais do público, mas ainda há trechos genéricos.
- `high`: dor, promessa, mecanismo, prova e CTA parecem feitos para um público real e específico.

## Como preencher `commercialReadiness`

- `weak`: página sem força comercial para tráfego pago.
- `acceptable`: estrutura compreensível, mas ainda fraca para publicar.
- `strong`: boa chance de convencer, com ajustes menores.
- `excellent`: clara, desejável, confiável e pronta para publicação.

## Como preencher `criteriaScores`

Dê notas de 0 a 10 para cada critério:

- `firstFoldClarity`: clareza da primeira dobra.
- `painResultMechanism`: força da sequência dor, resultado e mecanismo.
- `proofStrength`: qualidade da microprova/amostra de valor.
- `offerDesirability`: desejo gerado pela oferta de entrada.
- `ctaAndFormStrength`: força do CTA e do formulário para capturar e-mail.
- `visualPremiumFeel`: percepção premium, confiança e acabamento visual.
- `mobileDesktopExecution`: execução responsiva e ausência de falhas visuais.

As notas devem ser consistentes com o `score` final.

## Como preencher `blockingIssues`

Cada item de `blockingIssues` deve ser curto, específico e acionável.

Use este formato:

`[Área] Problema observado → impacto comercial → correção esperada.`

Exemplos:

- `[Primeira dobra] A headline comunica o material, mas não a transformação → o visitante não entende por que deveria se interessar → reescrever promessa conectando dor, resultado e mecanismo.`
- `[Prova] O preview é decorativo e não mostra a entrega real → a confiança na solução fica baixa → gerar uma amostra visual mais concreta do resultado.`
- `[CTA/Formulário] O botão pede uma ação genérica → reduz desejo de enviar o e-mail → trocar por CTA orientado ao benefício imediato.`
- `[Design] A página está limpa, mas monótona e com pouco contraste entre seções → baixa percepção premium → refazer hierarquia visual, cards, espaçamento e destaque da prova.`
- `[HTML] Há texto provisório ou artefato técnico visível → a página parece inacabada → corrigir montagem final do HTML.`

Não escreva problemas vagos como “melhorar design” ou “copy fraca”. Sempre diga o que está fraco, por que isso afeta conversão e qual direção de correção.

## Como preencher `improvementOpportunities`

Registre apenas otimizações não bloqueantes, como um teste futuro de CTA, variação editorial ou refinamento visual incremental. Não use este campo para esconder falha funcional, comercial, responsiva ou de confiança.

## Como escolher `recommendedRegeneration`

Recomende somente as etapas que atacam a causa-raiz.

- `LANDING_PAGE_COPY`: promessa fraca, dor genérica, mecanismo mal explicado, CTA sem benefício, oferta pouco desejável ou texto contraditório.
- `LANDING_PAGE_WIREFRAME`: ordem das seções ruim, prova/formulário mal posicionados, narrativa comercial mal estruturada ou falta de blocos essenciais.
- `LANDING_PAGE_IMAGE_PLANNING`: tipo de prova visual errado, imagem planejada decorativa, ausência de preview funcional ou prova incompatível com o produto.
- `LANDING_PAGE_IMAGE_GENERATION`: imagem gerada com baixa qualidade, incoerente, genérica, distorcida, pouco confiável ou sem aparência de prova real.
- `LANDING_PAGE_DESIGN_PRESET`: baixa percepção premium, hierarquia fraca, contraste ruim, monotonia, espaçamento pobre, cards sem força, CTA pouco destacado.
- `LANDING_PAGE_HTML`: problema de renderização, responsividade, CSS não aplicado, formulário quebrado, botão desalinhado, artefato técnico, texto provisório ou metadado visível.
- `LANDING_PAGE_DELIVERABLES`: problema externo de publicação, entrega, link, integração ou experiência pós-formulário.

## Saída obrigatória

Responda somente JSON válido aderente ao schema.

Não inclua markdown, comentários, explicações fora do JSON ou campos extras.

O JSON deve conter exatamente os campos definidos no schema:

- `score`
- `targetAudienceSpecificity`
- `commercialReadiness`
- `criteriaScores`
- `blockingIssues`
- `improvementOpportunities`
- `recommendedRegeneration`
- `approvalRecommendation`
