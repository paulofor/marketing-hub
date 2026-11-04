// Aguarda a transferência no prazo do cenário e encerra recusas sem repetir o pedido.
import { remainingPrivateKitTime } from './private-kit-scenario.mjs';

export async function downloadPrivateKit(page, scenarioStartedAt) {
  const remaining = remainingPrivateKitTime(scenarioStartedAt);
  const address = new URL('./download', page.url()).href;
  let rejectFailure;
  const failure = new Promise((_, reject) => { rejectFailure = reject; });
  let received;
  const occurrence = new Promise(resolve => { received = resolve; });
  const timer = setTimeout(() => rejectFailure(new Error('Timeout no recebimento do download.')), remaining);
  const response = value => {
    if (value.url() === address && value.status() >= 400)
      rejectFailure(new Error('Download recusado pelo servidor (HTTP ' + value.status() + ').'));
  };
  const requestFailed = value => {
    if (value.url() === address)
      rejectFailure(new Error('Transporte do download falhou: ' + value.failure()?.errorText));
  };
  page.on('response', response); page.on('requestfailed', requestFailed); page.on('download', received);
  try {
    return await Promise.race([
      failure,
      (async () => {
        const [download] = await Promise.all([
          occurrence,
          page.locator('#download').click({ timeout: remaining }),
        ]);
        if (await download.failure()) throw new Error('Download falhou.');
        return download;
      })(),
    ]);
  } finally {
    clearTimeout(timer);
    page.off('response', response); page.off('requestfailed', requestFailed); page.off('download', received);
  }
}
