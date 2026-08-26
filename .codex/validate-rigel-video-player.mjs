import { writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { resolve } from "node:path";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("@playwright/test");
const playerPath = resolve(".codex/attachments/rigel-video-player.html");
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN });
const results = [];
for (const [name, profile] of [
  ["iphone15pro", devices["iPhone 15 Pro"]],
  ["pixel7", devices["Pixel 7"]],
]) {
  const context = await browser.newContext(profile);
  const page = await context.newPage();
  await page.goto(`file://${playerPath}`);
  const video = page.locator("video");
  await video.evaluate(async (element) => {
    element.muted = true;
    await element.play();
  });
  await page.waitForTimeout(2_000);
  const state = await video.evaluate((element) => ({
    paused: element.paused,
    currentTime: element.currentTime,
    duration: element.duration,
    videoWidth: element.videoWidth,
    videoHeight: element.videoHeight,
    readyState: element.readyState,
  }));
  if (
    state.paused ||
    state.currentTime <= 0 ||
    state.duration !== 30 ||
    state.videoWidth !== 1080 ||
    state.videoHeight !== 1920 ||
    state.readyState < 3
  ) {
    throw new Error(`${name} falhou: ${JSON.stringify(state)}`);
  }
  await page.screenshot({
    path: `.codex/attachments/rigel-video-${name}.jpg`,
    type: "jpeg",
    quality: 90,
  });
  results.push({ device: name, ...state });
  await context.close();
}
await browser.close();
writeFileSync(
  ".codex/attachments/rigel-video-player-audit.json",
  `${JSON.stringify(results, null, 2)}\n`,
);
console.log(JSON.stringify(results));
