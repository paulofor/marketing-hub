import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';

// Usa o navegador com codecs da imagem e preserva o executável explícito da homologação local.
export function videoBrowserOptions(env = process.env) {
  const executablePath = env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || env.CHROMIUM_BIN || env.CHROME_BIN || env.PDE_VIDEO_BROWSER_EXECUTABLE;
  return {headless: true, ...(executablePath ? {executablePath} : {}), args: ['--no-sandbox']};
}

// Confere bytes dos dois ativos selecionados sem gerar mídia nem invocar modelos.
export async function verifyVideoIdentity(binding, fetcher = fetch) {
  assert.equal(binding.evidenceType, 'LEARNING_CYCLE_VIDEO_INTEGRATION_V1');
  assert.match(binding.integrationFingerprint, /^[a-f0-9]{64}$/);
  const verified = [];
  for (const [key, role] of [['campaignVideo', 'AD'], ['heroVideo', 'LANDING_HERO']]) {
    const media = binding[key];
    assert.equal(media.role, role);
    assert.equal(media.reviewStatus, 'APPROVED');
    assert.equal(media.captionsBurnedIn, true);
    assert.ok(media.captions.trim());
    const url = new URL(media.assetUrl);
    assert.equal(url.protocol, 'https:');
    assert.ok(!url.username && !url.password);
    const response = await fetcher(url, { signal: AbortSignal.timeout(60000), redirect: 'error' });
    assert.equal(response.status, 200, `Mídia ${role} indisponível`);
    assert.ok(Number(response.headers.get('content-length') || 0) <= 64 * 1024 * 1024);
    const hash = createHash('sha256');
    let bytes = 0;
    const reader = response.body.getReader();
    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        bytes += value.length;
        assert.ok(bytes <= 64 * 1024 * 1024, 'Mídia excede limite de inspeção');
        hash.update(value);
      }
    } finally { await reader.cancel(); }
    assert.ok(bytes > 0);
    const sha256 = hash.digest('hex');
    assert.equal(sha256, media.sha256, `Bytes divergentes no vídeo ${role}`);
    verified.push({ assetId: media.assetId, role, sha256, bytes });
  }
  assert.notEqual(binding.campaignVideo.assetId, binding.heroVideo.assetId);
  assert.notEqual(binding.campaignVideo.sha256, binding.heroVideo.sha256);
  return verified;
}

// Mede reprodução e áudio decodificado com o navegador real, sem inferir pelo status do cadastro.
export async function verifyPlayback(video, expectedSeconds) {
  await video.evaluate(element => element.play());
  await video.page().waitForFunction(element => element.currentTime >= .25 && !element.paused,
    await video.elementHandle(), { timeout: 30000 });
  const result = await video.evaluate(element => ({
    duration: element.duration, width: element.videoWidth, height: element.videoHeight,
    audioBytes: element.webkitAudioDecodedByteCount, muted: element.muted,
  }));
  assert.ok(Number.isFinite(result.duration) && Math.abs(result.duration - expectedSeconds) <= 1);
  assert.ok(result.width > 0 && result.height > 0);
  assert.ok(result.audioBytes > 0, 'Áudio precisa ser decodificado pelo navegador');
  assert.equal(result.muted, false);
  await video.evaluate(element => element.pause());
  return result;
}

// Verifica integração, CTA independente e recuperação da falha em cada dispositivo do harness.
export async function verifyIntegratedPage(page, binding, session) {
  assert.equal(session.videoIntegration?.integrationFingerprint, binding.integrationFingerprint);
  const start = page.getByRole('button', { name: 'Começar', exact: true });
  await start.waitFor();
  assert.ok(await start.isEnabled());
  const details = page.locator('details.video-demo');
  assert.equal(await details.getAttribute('open'), null);
  await details.locator('summary').first().click();
  const video = page.getByLabel('Como funciona seu primeiro ajuste');
  assert.equal(await video.getAttribute('src'), binding.heroVideo.assetUrl);
  assert.equal(await video.getAttribute('autoplay'), null);
  const hero = await verifyPlayback(video, binding.heroVideo.durationSeconds);
  const adPage = await page.context().newPage();
  let ad;
  try {
    await adPage.setContent('<video controls playsinline></video>');
    const adVideo = adPage.locator('video');
    await adVideo.evaluate((element, url) => { element.src = url; }, binding.campaignVideo.assetUrl);
    ad = await verifyPlayback(adVideo, binding.campaignVideo.durationSeconds);
  } finally { await adPage.close(); }
  const source = binding.heroVideo.assetUrl;
  await page.route(source, route => route.abort('failed'));
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.locator('details.video-demo > summary').click();
  await page.getByLabel('Como funciona seu primeiro ajuste').evaluate(element => element.load());
  await page.getByText('O vídeo não abriu. Você pode continuar e criar seu ajuste normalmente.').waitFor();
  assert.ok(await start.isEnabled());
  await page.unroute(source);
  await page.reload({ waitUntil: 'domcontentloaded' });
  await start.waitFor();
  return { hero, campaign: ad, optional: true, failureRecovery: true };
}
