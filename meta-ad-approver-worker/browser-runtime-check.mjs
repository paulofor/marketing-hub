import { chromium } from 'playwright-core';
import { createServer } from 'node:http';
import { captureCommercialLanding } from './landing-evidence.mjs';
import { validateVideoDecoder } from './video-frame-extractor.mjs';

const server = createServer((request, response) => {
  if (request.url?.startsWith('/product')) {
    response.writeHead(200, { 'content-type': 'application/json' });
    response.end(JSON.stringify({
      commercialCheckout: {
        provider: 'SANDBOX',
        checkoutUrl: `http://127.0.0.1:${server.address().port}/checkout`,
        offerReference: 'qa-offer',
        priceBrl: 67,
        currency: 'BRL',
        billingModel: 'ONE_TIME',
      },
    }));
    return;
  }
  if (request.url?.startsWith('/checkout')) {
    response.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
    response.end('<main><h1>Checkout de homologação</h1><p>Pagamento único de R$ 67,00.</p><p>Nenhuma compra será executada neste teste somente leitura.</p></main>');
    return;
  }
  response.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
  response.end(`<main><h1>Landing comercial de homologação</h1><p>${'Conteúdo verificável. '.repeat(15)}</p></main><script>fetch('/product').then(value => value.json())</script>`);
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));

const browser = await chromium.launch({
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
});

try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await page.setContent('<main><h1>Gate visual operacional</h1></main>');
  const screenshot = await page.screenshot({ type: 'png' });
  if (screenshot.length === 0) {
    throw new Error('O navegador não produziu a evidência visual de runtime.');
  }
  const video = await validateVideoDecoder();
  if (!/^[a-f0-9]{64}$/.test(video.sha256) || video.byteLength <= 0) {
    throw new Error('O decoder não devolveu SHA-256 e tamanho do arquivo completo.');
  }
  const landing = await captureCommercialLanding(
    browser,
    `http://127.0.0.1:${server.address().port}/landing`,
  );
  if (landing.screenshots.length !== 2 || !landing.checkout?.screenshot) {
    throw new Error('Landing e checkout não produziram as três evidências visuais.');
  }
  if (landing.checkout.priceBrl !== 67 || landing.checkout.interactionPerformed !== false) {
    throw new Error('Checkout não preservou preço e inspeção somente leitura.');
  }
  process.stdout.write(
    `${JSON.stringify({ browser: await browser.version(), screenshotBytes: screenshot.length, video: { ...video, frames: undefined }, checkout: { ...landing.checkout, screenshot: undefined } })}\n`,
  );
} finally {
  await browser.close();
  await new Promise(resolve => server.close(resolve));
}
