// Confere a orientação observada do kit antes do parecer pago, sem deduzir causa no navegador.
export function validateKitSafety(session, observed, context) {
  const nextUrl = new URL(observed.nextActionUrl);
  const presentation = session.presentation;
  if (session.productId !== context.productId || session.cycleId !== context.cycleId ||
      session.status !== 'BLOCKED_SAFE' || session.manifest || !observed.resultHidden ||
      !observed.title.includes('bloqueada') || observed.title !== presentation?.title ||
      !observed.introduction.includes('Nenhum pacote foi gerado') || observed.introduction !== presentation?.introduction ||
      presentation?.reasonCode !== context.safetyCase || !session.reason?.trim() || observed.reason !== session.reason ||
      !presentation.nextStep?.trim() || observed.nextStep !== presentation.nextStep ||
      !observed.actionVisible || !presentation.nextActionLabel?.trim() || observed.actionLabel !== presentation.nextActionLabel ||
      nextUrl.origin !== 'http://191.252.181.168:5173' || nextUrl.pathname !== '/business-process-chains/learning-cycles' ||
      nextUrl.searchParams.get('productId') !== String(context.productId) || nextUrl.searchParams.get('cycleId') !== String(context.cycleId) ||
      nextUrl.searchParams.size !== 2 || nextUrl.hash || nextUrl.username || nextUrl.password ||
      presentation.nextActionPath !== nextUrl.pathname + nextUrl.search ||
      session.events.some(e => ['VALUE_MOMENT', 'CHECKOUT_STARTED', 'DOWNLOAD_COMPLETED'].includes(e.code))) {
    throw new Error('SAFETY não comprovou bloqueio, causa, ausência de resultado e revisão permitida do mesmo ciclo.');
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
