import { expect, test } from '@playwright/test';

const approvedHeroVideoUrl = '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8';

const v6ProductContract = {
  slug: 'metodo-musa-7-dias',
  experienceVersion: 'musa-pde-entry-v6-video-motivacional',
  layoutKey: 'video-motivacional',
  funnelVersion: 'musa-membership-funnel-v1',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Presença elegante acessível em 7 dias.',
  audience: 'Mulheres urbanas',
  priceLabel: 'R$67',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Mapa de Presença MUSA',
    intro: 'Entrada publicada no Hub.',
    questions: ['O que minha imagem comunica hoje?'],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Ler o sinal que sua imagem comunica',
      principle: 'A presença cresce quando você identifica o sinal visual.',
      action: 'Escolha uma microação para comunicar mais intenção.',
      evidence: 'Frase preenchida.',
      visualCue: 'Compare a sensação antes/depois.',
    },
  ],
  supportMaterials: [],
  heroVideos: [
    {
      experienceVersion: 'musa-pde-entry-v6-video-motivacional',
      placement: 'public_diagnostic_initial_explainer',
      playbackUrl: approvedHeroVideoUrl,
      hlsPlaybackUrl: approvedHeroVideoUrl,
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: 'MARKETING_HUB_MANAGED_HLS',
      status: 'READY',
      reviewStatus: 'APPROVED',
    },
  ],
  publicFirstFold: {
    headline: 'Você se arruma, mas ainda sente que sua presença não acompanha a mulher que você quer ser?',
    supportingText: 'Em poucos minutos, o MUSA lê 4 escolhas sobre seu espelho e mostra o primeiro ruído visual.',
    videoKicker: 'Prévia MUSA',
    videoHeadline: 'Antes de comprar outra peça, veja o sinal que está deixando seu look comum.',
    videoSupportingText: 'A prévia mostra como roupa, acabamento, cor e postura podem comunicar mais intenção.',
    videoExtraText: 'Depois do vídeo, responda 4 escolhas rápidas.',
    videoCtaLabel: 'Ver meu Mapa de Presença',
  },
  completionOffer: 'Continuidade',
};

test('v6 publica bloco de video nao-slide e segue direto para o diagnostico', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(v6ProductContract),
    });
  });
  const productRequest = page.waitForRequest('**/api/pde/products/metodo-musa-7-dias**');

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa', { waitUntil: 'domcontentloaded' });
  const productRequestUrl = (await productRequest).url();

  expect(productRequestUrl).toContain('slotCode=v6');
  expect(productRequestUrl).toContain('experienceVersion=musa-pde-entry-v6-video-motivacional');
  await expect(page.getByRole('heading', { name: /presença não acompanha/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCount(1);
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('loop', false);
  await expect(page.locator('video.public-hero-video')).not.toHaveAttribute('poster');
  await expect(page.locator('video.public-hero-video')).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCSS('pointer-events', 'auto');
  const videoReceivesPointer = await page.locator('video.public-hero-video').evaluate((video) => {
    const rect = video.getBoundingClientRect();
    const elementAtVideoCenter = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
    return elementAtVideoCenter === video;
  });
  expect(videoReceivesPointer).toBe(true);
  await expect(page.locator('.public-video-play-badge')).toHaveCount(0);
  await expect(page.locator('.public-video-watch-status')).toHaveCount(0);
  await expect(page.getByRole('region', { name: 'Diagnóstico de Presença' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Falta acabamento' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu Mapa de Presença/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /Descobrir meu primeiro ajuste/i })).toBeDisabled();
  await expect(page.getByText(/Domínios conhecidos apontados/i)).toHaveCount(0);
  await expect(page.getByText(/Slots versionados do Clube MUSA/i)).toHaveCount(0);
});

test('v6 bloqueia override global para HLS antigo de slides', async ({ page }) => {
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE: 'musa-pde-entry-v5-video-explicativo',
      VITE_MUSA_HERO_STREAM_URL: '/assets/hls/musa-v5-video-explicativo/index.m3u8',
    };
  });
  await page.route('**/api/pde/products/metodo-musa-7-dias**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(v6ProductContract),
    });
  });

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa', { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /presença não acompanha/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveAttribute(
    'src',
    approvedHeroVideoUrl,
  );
});

test('v7 usa contrato proprio sem alterar as perguntas publicas da v6', async ({ page }) => {
  await page.route('**/api/pde/products/metodo-musa-7-dias**', async (route) => {
    await route.fulfill({ status: 404, body: 'not found' });
  });

  await page.goto('http://v7.clubemusa.com.br:57180/?mh_preview=qa&musa_video_variant=control', { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /falta presença/i })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Falta acabamento' })).toBeVisible();
  await expect(page.getByText('Quando você se olha pronta, o que mais faz o look parecer simples demais?')).toBeVisible();
  await expect(page.getByText('Quando você se olha pronta, o que mais te incomoda?')).toHaveCount(0);

  await page.goto('http://v6.clubemusa.com.br:57180/?mh_preview=qa&musa_video_variant=control', { waitUntil: 'domcontentloaded' });

  await expect(page.getByText('Quando você se olha pronta, o que mais te incomoda?')).toBeVisible();
  await expect(page.getByText('Quando você se olha pronta, o que mais faz o look parecer simples demais?')).toHaveCount(0);
});
