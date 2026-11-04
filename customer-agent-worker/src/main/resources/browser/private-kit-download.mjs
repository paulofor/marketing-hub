// Aguarda a transferência dentro do prazo do cenário, sem renovar prazo ou repetir o pedido.
import { remainingPrivateKitTime } from './private-kit-scenario.mjs';

export async function downloadPrivateKit(page, scenarioStartedAt) {
  const remaining = remainingPrivateKitTime(scenarioStartedAt);
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: remaining }),
    page.locator('#download').click({ timeout: remaining }),
  ]);
  if (await download.failure()) throw new Error('Download falhou.');
  return download;
}
