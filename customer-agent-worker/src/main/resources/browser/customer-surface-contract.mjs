// Confere todo texto da experiência, inclusive detalhes recolhidos, antes do parecer por modelo.
const internalText = /AGENT_VALIDATION|mh_internal_test|commercialEvidenceEligible|providerCalls|prototypeVersion|sessionId|\bfixture\b|\b(?:productId|cycleId|experimentId)\b|EXPERIENCE_STARTED|SAFETY_LIMIT_BLOCKED|PREFERRED_OVER_FREE|CHECKOUT_STARTED|VALUE_MOMENT|\bINPUT_CONFIRMED\b|\bBLOCKED_SAFE\b|\b(?:produto|ciclo|experimento)\s*#\s*\d|capella-private-v\d|Dados desta prova|Limites técnicos|\bstack\s*trace\b|\bSQL(?:Exception|State)\b|\b(?:debugInfo|test-controls)\b|\b\d+\s+bytes\b/i;

export function validateCustomerSurface(observed) {
  if (typeof observed.text !== 'string' || !observed.text.trim()) {
    throw new Error('A revisão da interface não recebeu o texto da experiência.');
  }
  if (observed.internalElements?.length || internalText.test(observed.text) || /\b(?:posts|stories|legendas|mensagens|calendario)\/[^\s|]+\.(?:png|txt)\b/i.test(observed.text)) {
    throw new Error('A experiência expõe dados ou controles internos. Corrija a interface antes da revisão e da publicação.');
  }
  return { contractVersion: 'PDE_CUSTOMER_SURFACE_V1', internalInformationAbsent: true, collapsedContentChecked: true };
}

// Examina texto no DOM, não apenas innerText: fechar um acordeão não remove o vazamento.
export async function checkCustomerSurface(page) {
  const observed = await page.evaluate(() => {
    const root = document.querySelector('main') || document.body;
    const copy = root.cloneNode(true);
    copy.querySelectorAll('script,style').forEach(element => element.remove());
    const values = [...root.querySelectorAll('textarea[readonly]')].map(element => element.value);
    return { text: copy.textContent + '\n' + values.join('\n'),
      internalElements: [...root.querySelectorAll('#audit,.audit,#test-controls,#prefer,#checkout')].map(element => element.id || element.className) };
  });
  return validateCustomerSurface(observed);
}
