import { expect, test } from '@playwright/test';

test('carrega a entrada visual do Clube MUSA', async ({ page }) => {
  const completedGuidance = {
    requestId: 'diagnostico-visual-1',
    productSlug: 'metodo-musa-7-dias',
    missionId: 'diagnostico-presenca-publico',
    guidanceType: 'MUSA_PUBLIC_PRESENCE_DIAGNOSTIC',
    status: 'COMPLETED',
    headline: 'Seu plano começa reduzindo ruído visual',
    summary: 'A Consultora MUSA identificou que sua presença precisa de acabamento simples e repetível.',
    signals: ['Acabamento', 'Intenção', 'Coerência'],
    microActions: [
      'Dia 1: escolha uma ocasião real e retire um excesso visual.',
      'Dia 2: defina uma peça-sinal para repetir com intenção.',
      'Dia 3: combine duas cores de forma mais limpa.',
      'Dia 4: ajuste cabelo, pele ou acessório antes de sair.',
      'Dia 5: monte uma fórmula simples com o que já existe.',
      'Dia 6: repita a fórmula em uma situação importante.',
      'Dia 7: registre sua assinatura MUSA pessoal.',
    ],
    caution: 'Use como orientação prática, sem promessa automática de resultado universal.',
  };

  await page.route('/api/pde/access/events', async (route) => {
    await route.fulfill({ json: { status: 'RECORDED' } });
  });
  await page.route('/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, json: { error: 'Produto carregado pelo fallback do teste visual.' } });
  });
  await page.route('/api/pde/public/presence-diagnostic', async (route) => {
    await route.fulfill({ json: completedGuidance });
  });
  await page.route('/api/pde/public/presence-diagnostic/diagnostico-visual-1', async (route) => {
    await route.fulfill({ json: completedGuidance });
  });
  await page.route('/api/pde/access/magic-link', async (route) => {
    await route.fulfill({
      json: {
        productSlug: 'metodo-musa-7-dias',
        email: 'teste+diagnostico@sandbox.local',
        deliveryStatus: 'SENT',
      },
    });
  });

  await page.goto('/?musa_video_variant=control');

  await expect(
    page.getByRole('heading', {
      name: /tirando elegância/i,
      level: 1,
    }),
  ).toBeVisible();
  await expect(page.getByRole('region', { name: 'Diagnóstico de Presença' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Ver meu ajuste MUSA/i })).toBeDisabled();
  await page.getByRole('button', { name: 'Falta acabamento' }).click();
  await page.getByRole('button', { name: 'Trabalho ou reunião' }).click();
  await page.getByRole('button', { name: 'Elegância discreta' }).click();
  await page.getByRole('button', { name: 'Cabelo e pele' }).click();
  await expect(page.getByRole('button', { name: /Ver meu ajuste MUSA/i })).toBeEnabled();
  await page.getByRole('button', { name: /Ver meu ajuste MUSA/i }).click();
  await expect(page.getByRole('heading', { name: /Seu plano começa reduzindo ruído visual/i })).toBeVisible();
  await expect(page.getByText(/Resultado MUSA gratuito/i)).toBeVisible();
  await expect(page.getByText(/Seu sinal principal hoje/i)).toBeVisible();
  await expect(page.getByRole('region', { name: /Preview bloqueado do plano MUSA de 7 dias/i })).toBeVisible();
  await expect(page.getByText(/Salve seu Plano MUSA/i)).toBeVisible();
  await page.getByPlaceholder('seuemail@exemplo.com').fill('teste+diagnostico@sandbox.local');
  await page.getByRole('button', { name: /Salvar meu Plano MUSA de 7 dias/i }).click();
  await expect(page.getByText(/Enviei para seu e-mail/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath('musa-entry.png'),
    fullPage: true,
  });
});

test('continua aguardando diagnostico publico quando IA demora mais que 20 segundos', async ({ page }) => {
  const pendingGuidance = {
    requestId: 'diagnostico-lento-1',
    productSlug: 'metodo-musa-7-dias',
    missionId: 'diagnostico-presenca-publico',
    guidanceType: 'MUSA_PUBLIC_PRESENCE_DIAGNOSTIC',
    status: 'PENDING',
    headline: '',
    summary: '',
    signals: [],
    microActions: [],
    caution: '',
  };
  const completedGuidance = {
    ...pendingGuidance,
    status: 'COMPLETED',
    headline: 'Seu plano chegou sem travar a tela',
    summary: 'A Consultora MUSA terminou depois da janela curta antiga e o resultado apareceu corretamente.',
    signals: ['Tempo de IA', 'Polling longo', 'Resultado entregue'],
    microActions: [
      'Dia 1: escolha uma base simples.',
      'Dia 2: retire um excesso visual.',
      'Dia 3: repita um detalhe de acabamento.',
      'Dia 4: alinhe cabelo ou pele.',
      'Dia 5: fotografe a combinacao.',
      'Dia 6: ajuste postura e presenca.',
      'Dia 7: salve sua formula final.',
    ],
    caution: 'Comece pelo que voce ja tem.',
  };
  let pollRequests = 0;

  await page.addInitScript(() => {
    const originalSetTimeout = window.setTimeout;
    window.setTimeout = ((handler: TimerHandler, timeout?: number, ...args: unknown[]) =>
      originalSetTimeout(handler, Math.min(Number(timeout ?? 0), 5), ...args)) as typeof window.setTimeout;
  });
  await page.route('/api/pde/public/presence-diagnostic', async (route) => {
    await route.fulfill({ json: pendingGuidance });
  });
  await page.route('/api/pde/public/presence-diagnostic/diagnostico-lento-1', async (route) => {
    pollRequests += 1;
    await route.fulfill({ json: pollRequests <= 14 ? pendingGuidance : completedGuidance });
  });

  await page.goto('/?musa_video_variant=control');

  await page.getByRole('button', { name: 'Falta acabamento' }).click();
  await page.getByRole('button', { name: 'Trabalho ou reunião' }).click();
  await page.getByRole('button', { name: 'Elegância discreta' }).click();
  await page.getByRole('button', { name: 'Roupa que já tenho' }).click();
  await page.getByRole('button', { name: /Ver meu ajuste MUSA/i }).click();

  await expect(page.getByRole('button', { name: /Montando seu plano/i })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Seu plano chegou sem travar a tela/i })).toBeVisible();
  expect(pollRequests).toBeGreaterThan(12);
});

test('modo Preview QA nao envia eventos comerciais', async ({ page }) => {
  let trackedEvents = 0;
  await page.route('/api/pde/access/events', async (route) => {
    trackedEvents += 1;
    await route.fulfill({ json: { status: 'RECORDED' } });
  });

  await page.goto('/?mh_preview=qa&pde_analytics=off&utm_source=internal&utm_medium=qa&utm_campaign=metodo-musa-7-dias_preview_qa&utm_content=product_card');

  await expect(
    page.getByRole('heading', {
      name: /tirando elegância/i,
      level: 1,
    }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Falta acabamento' }).click();
  await page.waitForTimeout(250);

  expect(trackedEvents).toBe(0);
});

test('bloqueia video de slides na versao publicada e permite controle sem player para QA', async ({ page }) => {
  await page.route('/api/pde/access/events', async (route) => {
    await route.fulfill({ json: { status: 'RECORDED' } });
  });
  await page.route('/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, json: { error: 'Produto carregado pelo fallback do teste visual.' } });
  });

  await page.goto('/');

  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();

  await page.goto('/?musa_video_variant=video');

  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();

  await page.goto('/?musa_video_variant=control');

  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();
});

test('exibe player na versao v6 motivacional com video real aprovado', async ({ page }) => {
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE: 'musa-pde-entry-v6-video-motivacional',
    };
  });
  await page.route('/api/pde/access/events', async (route) => {
    await route.fulfill({ json: { status: 'RECORDED' } });
  });
  await page.route('/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, json: { error: 'Produto carregado pelo fallback do teste visual.' } });
  });

  await page.goto('/');

  await expect(page.getByRole('heading', { name: /tirando elegância/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).not.toHaveAttribute('poster');
  await expect(page.getByRole('region', { name: 'Diagnóstico de Presença' })).toBeVisible();
});

test('mede reproducao real do video inicial MUSA', async ({ page }) => {
  const events: string[] = [];
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_HERO_VIDEO_URL: 'https://cdn.test/musa-video.mp4',
    };
  });
  await page.route('/api/pde/access/events', async (route) => {
    const body = route.request().postDataJSON() as { eventType?: string };
    if (body.eventType) {
      events.push(body.eventType);
    }
    await route.fulfill({ json: { status: 'RECORDED' } });
  });
  await page.route('/api/pde/products/metodo-musa-7-dias', async (route) => {
    await route.fulfill({ status: 404, json: { error: 'Produto carregado pelo fallback do teste visual.' } });
  });

  await page.goto('/?musa_video_variant=video');
  const video = page.locator('video.public-hero-video');
  await expect(video).toBeVisible();

  await video.evaluate((element) => {
    const htmlVideo = element as HTMLVideoElement;
    Object.defineProperty(htmlVideo, 'duration', { configurable: true, value: 40 });
    Object.defineProperty(htmlVideo, 'currentTime', { configurable: true, value: 0 });
    htmlVideo.dispatchEvent(new Event('play'));
    Object.defineProperty(htmlVideo, 'currentTime', { configurable: true, value: 20 });
    htmlVideo.dispatchEvent(new Event('timeupdate'));
    Object.defineProperty(htmlVideo, 'currentTime', { configurable: true, value: 39 });
    htmlVideo.dispatchEvent(new Event('timeupdate'));
  });

  await expect.poll(() => events).toEqual(expect.arrayContaining(['VIDEO_PLAY', 'VIDEO_PROGRESS_25', 'VIDEO_PROGRESS_50', 'VIDEO_COMPLETED']));
});
