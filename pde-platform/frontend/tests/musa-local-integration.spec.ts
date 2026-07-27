import { expect, test } from '@playwright/test';

const productSlug = 'metodo-musa-7-dias';
const v5ExperienceVersion = 'musa-pde-entry-v5-video-explicativo';
const v6ExperienceVersion = 'musa-pde-entry-v6-video-motivacional';

test.beforeEach(async ({ request }) => {
  const response = await request.post(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/reset-campaign-start`);
  expect(response.ok()).toBeTruthy();
});

test('v5 e v6 usam backend PDE local real, MP4 correto e analytics por versao', async ({ page, request }) => {
  await page.goto('http://v5.clubemusa.com.br:57180/?utm_source=local&utm_campaign=v5_local_validation');
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  await expect(page.locator('video.public-hero-video')).toHaveAttribute('src', '/assets/musa-v5-video-explicativo.mp4');
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
  await expect(page.getByRole('heading', { name: /Descubra em 30 segundos/i })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Vídeo curto Método MUSA' })).toBeVisible();
  const video = page.locator('video.public-hero-video');
  await expect(video).toBeVisible();
  await expect(video).toHaveAttribute('src', '/assets/musa-v6-video-motivacional.mp4');

  const mp4Response = await page.request.get('http://v6.clubemusa.com.br:57180/assets/musa-v6-video-motivacional.mp4');
  expect(mp4Response.ok()).toBeTruthy();
  expect(mp4Response.headers()['content-type']).toContain('video/mp4');

  await video.evaluate((element) => {
    const media = element as HTMLVideoElement;
    media.muted = true;
    return media.play();
  });

  await expect.poll(async () => {
    const response = await request.get(`http://127.0.0.1:8096/api/pde/access/analytics/${productSlug}/summary`);
    const summary = await response.json();
    const versionMetric = summary.experienceVersionBreakdown?.find(
      (metric: { experienceVersion: string }) => metric.experienceVersion === v6ExperienceVersion,
    );
    const events = new Set((summary.eventBreakdown ?? []).map((metric: { eventType: string }) => metric.eventType));
    return Boolean(versionMetric?.totalEvents > 0 && events.has('VIDEO_VIEWED') && events.has('VIDEO_PLAY'));
  }).toBeTruthy();
});
