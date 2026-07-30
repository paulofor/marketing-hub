import { expect, test } from '@playwright/test';

const productSlug = 'metodo-musa-7-dias';
const v5ExperienceVersion = 'musa-pde-entry-v5-video-explicativo';
const v6ExperienceVersion = 'musa-pde-entry-v6-video-motivacional';
const v7ExperienceVersion = 'musa-pde-entry-v7-espelho-antes-de-sair';

test.beforeEach(async ({ request }) => {
  const response = await request.post(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/reset-campaign-start`);
  expect(response.ok()).toBeTruthy();
});

test('v5, v6 e v7 usam backend PDE local real sem misturar contratos versionados', async ({ page, request }) => {
  await page.goto('http://v5.clubemusa.com.br:57180/?utm_source=local&utm_campaign=v5_local_validation');
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toHaveCount(0);
  await expect(page.locator('video.public-hero-video')).toHaveCount(0);
  await expect.poll(async () => {
    const response = await request.get(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/summary`);
    const summary = await response.json();
    return summary.experienceVersionBreakdown?.some(
      (metric: { experienceVersion: string; totalEvents: number }) =>
        metric.experienceVersion === v5ExperienceVersion && metric.totalEvents > 0,
    );
  }).toBeTruthy();

  await request.post(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/reset-campaign-start`);

  await page.goto('http://v6.clubemusa.com.br:57180/?utm_source=local&utm_campaign=v6_local_validation');
  await expect(page.getByRole('heading', { name: /falta presença/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveCount(1);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('muted', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('autoplay', false);
  await expect(page.locator('video.public-hero-video')).toHaveJSProperty('controls', true);
  await expect(page.locator('video.public-hero-video')).not.toHaveAttribute('poster');

  await expect.poll(async () => {
    const response = await request.get(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/summary`);
    const summary = await response.json();
    const versionMetric = summary.experienceVersionBreakdown?.find(
      (metric: { experienceVersion: string }) => metric.experienceVersion === v6ExperienceVersion,
    );
    const events = new Set((summary.eventBreakdown ?? []).map((metric: { eventType: string }) => metric.eventType));
    return Boolean(versionMetric?.totalEvents > 0 && events.has('VIDEO_VIEWED'));
  }).toBeTruthy();

  await request.post(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/reset-campaign-start`);

  await page.goto('http://v7.clubemusa.com.br:57180/?utm_source=local&utm_campaign=v7_local_validation&musa_video_variant=control');
  await expect(page.getByText('Quando você se olha pronta, o que mais faz o look parecer simples demais?')).toBeVisible();
  await expect(page.getByText('Quando você se olha pronta, o que mais te incomoda?')).toHaveCount(0);

  await expect.poll(async () => {
    const response = await request.get(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/summary`);
    const summary = await response.json();
    return summary.experienceVersionBreakdown?.some(
      (metric: { experienceVersion: string; totalEvents: number }) =>
        metric.experienceVersion === v7ExperienceVersion && metric.totalEvents > 0,
    );
  }).toBeTruthy();
});
