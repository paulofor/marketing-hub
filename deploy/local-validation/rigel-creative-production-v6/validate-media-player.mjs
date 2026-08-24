import { createRequire } from "node:module";
import { mkdir, readFile } from "node:fs/promises";
import { basename, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("@playwright/test");

const [playerFile, outputDirectory, contractFile] = process.argv.slice(2);
if (!playerFile || !outputDirectory || !contractFile) {
  throw new Error(
    "Uso: node validate-media-player.mjs <player-html> <diretorio-saida> <contrato>",
  );
}
const contract = JSON.parse(await readFile(resolve(contractFile), "utf8"));
const expectedDuration = contract.formats.find(
  (format) => format.id === "vertical-demo",
)?.durationSeconds;
if (!Number.isFinite(expectedDuration)) {
  throw new Error("Contrato sem duracao valida para vertical-demo");
}

const output = resolve(outputDirectory);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  headless: true,
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ??
    process.env.CHROMIUM_BIN ??
    process.env.CHROME_BIN,
  args: ["--allow-file-access-from-files"],
});

const results = [];
for (const deviceName of ["iPhone 15 Pro", "Pixel 7"]) {
  const context = await browser.newContext({ ...devices[deviceName] });
  const page = await context.newPage();
  await page.goto(pathToFileURL(resolve(playerFile)).href);
  await page.locator("#media").waitFor({ state: "visible" });
  await page.waitForFunction(() => {
    const media = document.querySelector("#media");
    return media instanceof HTMLVideoElement && media.readyState >= 1;
  });
  const metadata = await page.locator("#media").evaluate((element) => ({
    duration: element.duration,
    width: element.videoWidth,
    height: element.videoHeight,
  }));
  if (
    Math.abs(metadata.duration - expectedDuration) >= 0.15 ||
    metadata.width !== 1080 ||
    metadata.height !== 1920
  ) {
    throw new Error(`Metadados invalidos no ${deviceName}`);
  }
  await page.locator("#media").evaluate((element) => element.play());
  await page.waitForFunction(() => {
    const media = document.querySelector("#media");
    return media instanceof HTMLVideoElement && media.currentTime > 0.25;
  });
  const playbackTime = await page
    .locator("#media")
    .evaluate((element) => element.currentTime);
  const screenshot = `${deviceName.toLowerCase().replaceAll(/[^a-z0-9]+/g, "-")}.png`;
  await page.screenshot({ path: resolve(output, screenshot), fullPage: true });
  results.push({
    device: deviceName,
    durationSeconds: metadata.duration,
    resolution: `${metadata.width}x${metadata.height}`,
    playbackStartedAtSeconds: playbackTime,
    screenshot: basename(screenshot),
  });
  await context.close();
}
await browser.close();

process.stdout.write(
  `${JSON.stringify({ status: "APPROVED", devices: results }, null, 2)}\n`,
);
