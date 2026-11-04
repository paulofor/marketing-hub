// Aguarda a transferência dentro do prazo do cenário, sem renovar prazo ou repetir o pedido.
export async function downloadPrivateKit(page, scenarioStartedAt) {
  const remaining = 600000 - (Date.now() - scenarioStartedAt);
  if (!Number.isFinite(remaining) || remaining <= 0 || remaining > 600000) {
    throw new Error('O prazo de dez minutos do cenário terminou antes do download.');
  }
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: remaining }),
    page.locator('#download').click({ timeout: remaining }),
  ]);
  if (await download.failure()) throw new Error('Download falhou.');
  return download;
}
