import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { existsSync } from 'node:fs';
import { downloadPrivateKit } from '../../main/resources/browser/private-kit-download.mjs';

const require = createRequire(import.meta.url);
let library;
try { library = require('playwright'); } catch { library = require('playwright-core'); }
const { chromium, devices } = library;

// Exercita a espera no navegador e a transferência dos bytes, sem banco ou serviço externo.
test('download respeita prazo do cenário, preserva bytes e não repete transferência', async () => {
  let requests = 0;
  let status = 200;
  const payload = Buffer.from('kit sintético íntegro, sem evidência comercial');
  const server = createServer((req, res) => {
    if (req.url === '/download') {
      requests++;
      setTimeout(() => {
        res.writeHead(status, { 'Content-Type': 'application/zip' });
        res.end(payload);
      }, 250);
      return;
    }
    res.setHeader('Content-Type', 'text/html');
    res.end(`<button id="download">Baixar pacote</button><script>
      document.querySelector('#download').onclick = async () => {
        const response = await fetch('/download');
        if (!response.ok) return;
        const blob = await response.blob();
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'kit.zip';
        link.click();
      };
    </script>`);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const executable = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || '/usr/bin/chromium';
  const browser = await chromium.launch({
    ...(existsSync(executable) ? { executablePath: executable } : {}), args: ['--no-sandbox'],
  });
  try {
    for (const [name, profile, timeout] of [
      ['caso móvel com espera implícita curta', devices['Pixel 7'], 100],
      ['outro contexto móvel', devices['iPhone 15 Pro'], 100],
      ['caminho desktop anteriormente válido', { viewport: { width: 1440, height: 900 } }, 30000],
    ]) {
      const context = await browser.newContext({ ...profile, defaultBrowserType: undefined, acceptDownloads: true });
      const page = await context.newPage();
      await page.goto(`http://127.0.0.1:${server.address().port}`);
      page.setDefaultTimeout(timeout);
      const before = requests;
      const download = await downloadPrivateKit(page, Date.now());
      assert.deepEqual(await readFile(await download.path()), payload, name);
      assert.equal(requests, before + 1, name);

      // Indisponibilidade e acesso recusado encerram a espera; não geram evento nem repetem GET.
      for (const code of [504, 401]) {
        status = code;
        const listeners = page.listenerCount('download');
        const count = requests, started = Date.now();
        await assert.rejects(downloadPrivateKit(page, started), new RegExp('HTTP ' + code));
        assert.ok(Date.now() - started < 2000, 'Um erro conhecido não deve esperar dez minutos.');
        assert.equal(requests, count + 1);
        assert.equal(page.listenerCount('download'), listeners);
      }
      status = 200;
      const afterFailures = requests;

      // Uma ação sem download deve falhar no prazo restante, sem esperar outros dez minutos.
      await page.evaluate(() => { document.querySelector('#download').onclick = () => {}; });
      const start = Date.now();
      await assert.rejects(downloadPrivateKit(page, start - 600000 + 150), /Timeout/);
      assert.ok(Date.now() - start < 5000, 'O prazo do cenário não pode ser renovado.');
      assert.equal(requests, afterFailures);
      await assert.rejects(downloadPrivateKit(page, Date.now() - 600001), /prazo de dez minutos/);
      await context.close();
    }
  } finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
  }
});
