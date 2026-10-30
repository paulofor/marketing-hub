// Confere a orientação observada do kit antes do parecer pago, sem deduzir causa no navegador.
export function validateKitSafety(session, observed, context) {
  const nextUrl = new URL(observed.nextActionUrl);
  const sourceUrl = new URL(context.sourceUrl);
  const presentation = session.presentation;
  if (session.productId !== context.productId || session.cycleId !== context.cycleId ||
      session.status !== 'BLOCKED_SAFE' || session.manifest || !observed.resultHidden ||
      !observed.title.includes('bloqueada') || observed.title !== presentation?.title ||
      !observed.introduction.includes('Nenhum pacote foi gerado') || observed.introduction !== presentation?.introduction ||
      presentation?.reasonCode !== context.safetyCase || !session.reason?.trim() || observed.reason !== session.reason ||
      !presentation.nextStep?.trim() || observed.nextStep !== presentation.nextStep ||
      !observed.actionVisible || !presentation.nextActionLabel?.trim() || observed.actionLabel !== presentation.nextActionLabel ||
      !observed.continuationVisible || !presentation.nextStep.includes('equipe do produto') ||
      /revisão (?:deste ciclo|interna)|entrada compatível/.test(presentation.nextStep + ' ' + presentation.nextActionLabel) ||
      nextUrl.origin !== sourceUrl.origin || nextUrl.pathname !== sourceUrl.pathname ||
      nextUrl.search || nextUrl.hash !== '#kit-continuation' || nextUrl.username || nextUrl.password ||
      presentation.nextActionPath !== '#kit-continuation' ||
      session.events.some(e => ['VALUE_MOMENT', 'CHECKOUT_STARTED', 'DOWNLOAD_COMPLETED'].includes(e.code))) {
    throw new Error('SAFETY não comprovou bloqueio, causa, responsabilidade e continuação visível na própria experiência.');
  }
  return {
    code: presentation.reasonCode, title: observed.title, reason: session.reason,
    noResultMessage: observed.introduction, safeAction: presentation.nextActionLabel,
    nextActionPath: presentation.nextActionPath, resultGenerated: false, providerCalled: false,
  };
}

// Reabrir a sessão deve preservar a explicação, os dados e o bloqueio sem nova composição ou evento.
export function validatePreservedKitSafety(before, after, observed, context) {
  const outcome = validateKitSafety(after, observed, context);
  if (before.id !== after.id || before.prototypeVersion !== after.prototypeVersion ||
      before.reason !== after.reason || JSON.stringify(before.presentation) !== JSON.stringify(after.presentation) ||
      JSON.stringify(before.input) !== JSON.stringify(after.input) || JSON.stringify(before.events) !== JSON.stringify(after.events)) {
    throw new Error('Reabrir SAFETY alterou o bloqueio, a entrada, a orientação ou os eventos preservados.');
  }
  return { ...outcome, persistedAfterReload: true };
}
