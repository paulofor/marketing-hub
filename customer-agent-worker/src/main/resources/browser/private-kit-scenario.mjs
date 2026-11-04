// Mantém um único prazo absoluto para navegação, renderização, retomada e callbacks do cenário.
export function remainingPrivateKitTime(startedAt) {
  const remaining = 600000 - (Date.now() - startedAt);
  if (!Number.isFinite(remaining) || remaining <= 0 || remaining > 600000)
    throw new Error('O prazo de dez minutos do cenário terminou.');
  return remaining;
}

export async function runPrivateKitScenario(context, startedAt, operation) {
  const remaining = remainingPrivateKitTime(startedAt);
  context.setDefaultTimeout(remaining);
  context.setDefaultNavigationTimeout(remaining);
  const controller = new AbortController();
  let rejectCrash;
  const crashed = new Promise((_, reject) => { rejectCrash = reject; });
  const onCrash = () => {
    const error = new Error('O navegador encerrou a página durante a homologação.');
    controller.abort(error); rejectCrash(error);
  };
  const pages = context.pages();
  for (const page of pages) page.on('crash', onCrash);
  let timer;
  const deadline = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const error = new Error('O prazo de dez minutos do cenário terminou.');
      controller.abort(error);
      reject(error);
    }, remaining);
  });
  try {
    return await Promise.race([operation(controller.signal), deadline, crashed]);
  } finally {
    clearTimeout(timer);
    for (const page of pages) page.off('crash', onCrash);
  }
}

// Não transforma um erro já mostrado à cliente em uma espera longa por um resultado impossível.
export async function waitForPrivateKitResult(page) {
  await page.locator('#result:visible, #error:visible').first().waitFor({ state: 'visible' });
  if (await page.locator('#error').isVisible())
    throw new Error('A experiência não abriu o kit: ' + await page.locator('#error').innerText());
}
