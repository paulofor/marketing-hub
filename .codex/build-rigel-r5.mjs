import { readFileSync, writeFileSync } from "node:fs";

const source = JSON.parse(
  readFileSync(".codex/attachments/rigel-dedalo-decision-r4.json", "utf8"),
);

function replaceRequired(html, before, after) {
  if (!html.includes(before)) {
    throw new Error(`Trecho obrigatório ausente: ${before.slice(0, 100)}`);
  }
  return html.replace(before, after);
}

let html = source.generatedHtml;
html = replaceRequired(
  html,
  ".steps{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:46px}",
  ".steps{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:46px}",
);
html = replaceRequired(
  html,
  ".footer-row{display:flex;justify-content:space-between;gap:24px;align-items:flex-start}",
  ".footer-row{display:flex;justify-content:space-between;gap:28px;align-items:flex-start}.footer-copy{max-width:680px}.footer-copy strong{display:block;color:var(--ink);margin-bottom:5px}.footer-links{display:flex;flex-wrap:wrap;gap:10px 18px}.footer-links a{color:var(--brand-dark);font-weight:750}",
);
html = replaceRequired(
  html,
  ".final-card .button{width:100%}.proof-caption{flex-direction:column;gap:4px}h1",
  ".final-card .button{width:100%}.proof-caption{flex-direction:column;gap:4px}.footer-row{flex-direction:column}.footer-links{display:grid;gap:8px}h1",
);
html = replaceRequired(
  html,
  "Receba um atendimento de WhatsApp feito para o seu negócio — com respostas, perguntas, follow-ups e regras revisadas para você saber o que mandar sem soar insistente.",
  "Se você presta serviços e envia orçamentos pelo WhatsApp, receba um atendimento feito para o seu negócio — com respostas, perguntas, follow-ups e regras revisadas para saber o que mandar sem soar insistente.",
);
html = replaceRequired(
  html,
  "<span class=\"micro\">◷ Entrega em até 48 horas*</span>",
  "<span class=\"micro\">◷ Prévia em até 12h · entrega em até 48h*</span>",
);
html = replaceRequired(
  html,
  "<p class=\"fine\" style=\"margin-top:14px\">*Prazo contado após a confirmação do pagamento e o briefing mínimo completo.</p>",
  "<p class=\"fine\" style=\"margin-top:14px\">*Prazo contado após o pagamento confirmado e o briefing mínimo completo. O briefing leva cerca de 15 a 25 minutos e deve usar exemplos anonimizados.</p>",
);
html = replaceRequired(
  html,
  'alt="Exemplo real de resposta de atendimento no WhatsApp"',
  'alt="Recorte demonstrativo de uma resposta de atendimento no WhatsApp"',
);
html = replaceRequired(
  html,
  "<strong>Demonstração real do método</strong><span>Exemplo parcial da entrega</span>",
  "<strong>Recorte demonstrativo</strong><span>Amostra ilustrativa e não clicável</span>",
);
html = replaceRequired(
  html,
  "Não imagine o formato. Veja partes reais.",
  "Veja recortes demonstrativos da entrega.",
);
html = replaceRequired(html, '<p class="eyebrow">Partes reais</p>', '<p class="eyebrow">Amostras do formato</p>');
html = replaceRequired(
  html,
  "Os exemplos abaixo demonstram respostas, perguntas, follow-ups e organização da oferta. A implantação completa é personalizada depois do briefing.",
  "As amostras abaixo são ilustrativas e não clicáveis. Elas demonstram respostas, perguntas, follow-ups e organização; a implantação completa é personalizada depois do briefing.",
);
html = html.replaceAll('alt="Exemplo real de', 'alt="Recorte demonstrativo de');
html = html.replaceAll('alt="Exemplo real da', 'alt="Recorte demonstrativo da');
html = replaceRequired(
  html,
  "<strong>Perguntas que dão direção</strong><span>Para descobrir o que impede o próximo passo.</span>",
  "<strong>Perguntas que dão direção</strong><span>Recorte ilustrativo e não clicável para descobrir o que impede o próximo passo.</span>",
);
html = replaceRequired(
  html,
  "<strong>Follow-ups com função</strong><span>Cada contato tem um objetivo claro e uma saída.</span>",
  "<strong>Follow-ups com função</strong><span>Recorte ilustrativo e não clicável; cada contato tem objetivo e saída.</span>",
);
html = replaceRequired(
  html,
  "<strong>Oferta explicada com clareza</strong><span>O que entra, como funciona e qual é o próximo passo.</span>",
  "<strong>Oferta explicada com clareza</strong><span>Recorte ilustrativo e não clicável do escopo e do próximo passo.</span>",
);
html = replaceRequired(
  html,
  '<ul class="check-list"><li><span class="check" aria-hidden="true">✓</span><span><strong>Respostas prontas para adaptar</strong><br><span class="fine">Para dúvidas e momentos recorrentes do seu atendimento.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Perguntas de qualificação</strong><br><span class="fine">Para entender prioridade, objeção e intenção antes de oferecer o próximo passo.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Follow-ups respeitosos</strong><br><span class="fine">Com objetivo, contexto e encerramento — sem insistência automática.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Regras de uso</strong><br><span class="fine">Quando mandar, quando esperar e quando encerrar a sequência.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Revisão antes da entrega</strong><br><span class="fine">Para manter clareza, coerência e aderência ao briefing.</span></span></li></ul>',
  '<ul class="check-list"><li><span class="check" aria-hidden="true">✓</span><span><strong>10 a 20 respostas personalizadas</strong><br><span class="fine">Para dúvidas e momentos recorrentes do seu atendimento.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>5 a 10 perguntas de qualificação</strong><br><span class="fine">Para entender prioridade, objeção e intenção antes do próximo passo.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>3 a 5 follow-ups manuais</strong><br><span class="fine">Com objetivo, contexto e encerramento — sem disparo automático.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Regras de escalonamento</strong><br><span class="fine">Quando responder, esperar, encerrar ou tratar o caso individualmente.</span></span></li><li><span class="check" aria-hidden="true">✓</span><span><strong>Guia, checklist e revisão humana</strong><br><span class="fine">Pacote editável em área privada, acompanhado por sete materiais de apoio.</span></span></li></ul>',
);
html = replaceRequired(
  html,
  "<p>O briefing mínimo informa contexto, tipo de cliente, principais dúvidas e como você costuma atender. A partir dele, a estrutura é ajustada e revisada para o seu caso.</p>",
  "<p>O briefing guiado leva cerca de 15 a 25 minutos e reúne serviços, dúvidas, políticas, tom e exemplos anonimizados. A entrega fica disponível para download e edição em uma área privada ligada ao e-mail da compra.</p>",
);
html = replaceRequired(
  html,
  "<strong>Não inclui promessa de venda garantida.</strong> As mensagens ajudam a conduzir a conversa com clareza; a decisão continua sendo do cliente.",
  "<strong>Não é software, bot, integração, disparo ou assinatura.</strong> Também não inclui promessa de resposta, conversão, faturamento ou agenda cheia.",
);
html = replaceRequired(
  html,
  '<h2 id="process-title">Três passos, sem mistério.</h2>',
  '<h2 id="process-title">Quatro passos, sem mistério.</h2>',
);
html = replaceRequired(
  html,
  "Você compra, completa o contexto mínimo e recebe o atendimento organizado para usar e adaptar.",
  "Depois do pagamento, você acessa o briefing, valida uma prévia e recebe o pacote completo para uso manual.",
);
html = replaceRequired(
  html,
  '<div class="steps"><article class="step"><span class="step-number">1</span><h3>Pagamento confirmado</h3><p>O checkout registra sua compra de R$ 349.</p></article><article class="step"><span class="step-number">2</span><h3>Briefing mínimo completo</h3><p>Você informa os pontos essenciais do negócio e das conversas que precisa conduzir.</p></article><article class="step"><span class="step-number">3</span><h3>Personalização e revisão</h3><p>O atendimento é adaptado, organizado e revisado antes de ser entregue.</p></article></div>',
  '<div class="steps"><article class="step"><span class="step-number">1</span><h3>Briefing guiado</h3><p>Após o pagamento, acesse com o e-mail da compra e complete em 15 a 25 minutos, usando exemplos sem dados pessoais.</p></article><article class="step"><span class="step-number">2</span><h3>Prévia para validar o tom</h3><p>Com a entrada completa, você recebe uma primeira sequência em até 12 horas para confirmar a direção.</p></article><article class="step"><span class="step-number">3</span><h3>Entrega completa</h3><p>Em até 48 horas, o pacote editável e revisado fica disponível para download na área privada.</p></article><article class="step"><span class="step-number">4</span><h3>Primeira aplicação</h3><p>Escolha um bloco pequeno, revise e use manualmente no atendimento real durante a primeira semana.</p></article></div>',
);
html = replaceRequired(
  html,
  '<p class="price-note">Implantação personalizada e revisada.</p>',
  '<p class="price-note">Pagamento único, sem recorrência. Pacote personalizado, editável e revisado.</p>',
);
html = replaceRequired(
  html,
  '<div class="secure">▣ Checkout canônico do Mercado Pago</div>',
  '<div class="secure">▣ Checkout canônico do Mercado Pago</div><p class="fine" style="margin-top:12px;text-align:center">Ao comprar, você recebe o acesso ao briefing pelo e-mail da compra. Seus dados são usados somente para personalizar e entregar o serviço conforme a Política de Privacidade.</p>',
);
html = replaceRequired(
  html,
  '<details><summary>Preciso usar as mensagens palavra por palavra?</summary><p>Não. Você recebe uma base organizada e regras de uso para adaptar naturalmente ao contexto de cada conversa.</p></details>',
  '<details><summary>Preciso usar as mensagens palavra por palavra?</summary><p>Não. Você recebe uma base organizada e regras de uso para adaptar naturalmente ao contexto de cada conversa.</p></details><details><summary>Como recebo e edito o material?</summary><p>O pacote completo fica disponível para download em uma área privada ligada ao e-mail da compra. Os materiais são editáveis e foram feitos para aplicação manual no WhatsApp.</p></details><details><summary>Como meus dados são usados?</summary><p>Use exemplos anonimizados no briefing. As informações comerciais fornecidas são usadas somente para personalizar, revisar e entregar o serviço, conforme os termos e a política de privacidade.</p></details>',
);
html = replaceRequired(
  html,
  '<footer><div class="shell footer-row"><span>Kit WhatsApp Pronto · atendimento personalizado</span></div></footer>',
  '<footer><div class="shell footer-row"><div class="footer-copy"><strong>PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA · CNPJ 25.215.414/0001-69</strong><span>Rua Antonio Basilio, 204, apto 805 · Tijuca · Rio de Janeiro/RJ · CEP 20511-190<br>Suporte: <a href="mailto:contato@digicomdigital.com.br">contato@digicomdigital.com.br</a></span></div><nav class="footer-links" aria-label="Informações legais"><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/terms" target="_blank" rel="noopener">Termos</a><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/privacy" target="_blank" rel="noopener">Privacidade</a><a href="https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy" target="_blank" rel="noopener">Cancelamento e reembolso</a></nav></div></footer>',
);

source.generatedHtml = html;
source.summary =
  "A landing torna explícitos público, escopo mensurável, formato, fluxo pós-pagamento, privacidade, fornecedor e políticas sem alterar preço, promessa ou checkout.";
source.selectedGenerationApproach.justification =
  "A correção de causa-raiz usa o contexto comercial canônico para eliminar a ambiguidade pós-pagamento apontada por Psique, preservando oferta, criativos aprovados e checkout.";
source.selectedGenerationApproach.evaluationWindow =
  "Quality Review, Psique e Têmis independentes no ambiente local; somente depois, homologação produtiva sem publicação automática.";

writeFileSync(
  ".codex/attachments/rigel-dedalo-decision-r5.json",
  `${JSON.stringify(source, null, 2)}\n`,
);
writeFileSync(".codex/attachments/rigel-r5.html", `${html}\n`);
