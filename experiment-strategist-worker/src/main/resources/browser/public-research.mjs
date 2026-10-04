import { createHash } from "node:crypto";
import { lookup } from "node:dns/promises";
import { isIP, BlockList } from "node:net";
import { pathToFileURL } from "node:url";
import { chromium } from "playwright-core";

const blocked = new BlockList();
for (const [address, prefix] of [
  ["0.0.0.0", 8],
  ["10.0.0.0", 8],
  ["100.64.0.0", 10],
  ["127.0.0.0", 8],
  ["169.254.0.0", 16],
  ["172.16.0.0", 12],
  ["192.168.0.0", 16],
  ["198.18.0.0", 15],
  ["224.0.0.0", 4],
  ["240.0.0.0", 4],
]) {
  blocked.addSubnet(address, prefix, "ipv4");
}

// Mantém a pesquisa restrita a HTTP público, inclusive nos subrecursos.
export async function validatePublicUrl(value, resolve = lookup) {
  const url = new URL(value);
  const host = url.hostname.replace(/^\[|\]$/g, "").toLowerCase();
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    (url.port && !["80", "443"].includes(url.port)) ||
    host === "localhost" ||
    host.endsWith(".localhost") ||
    host.endsWith(".local")
  ) {
    throw new Error("Destino público HTTP(S) sem credenciais obrigatório.");
  }
  const addresses = isIP(host)
    ? [{ address: host }]
    : await resolve(host, { all: true });
  if (
    !addresses.length ||
    addresses.some(({ address }) => {
      const family = isIP(address);
      return family === 4
        ? blocked.check(address, "ipv4")
        : family !== 6 || !/^[23][0-9a-f]{3}:/i.test(address);
    })
  )
    throw new Error("Destino de rede não pública bloqueado.");
  return url;
}

// Confere o lote antes de iniciar Chromium ou qualquer consulta externa.
export function validateArguments(args) {
  if (
    !args ||
    Object.keys(args).some((key) => key !== "urls") ||
    !Array.isArray(args.urls) ||
    args.urls.length < 1 ||
    args.urls.length > 3 ||
    args.urls.some((url) => typeof url !== "string" || url.length > 2048)
  ) {
    throw new Error("Informe somente urls, com uma a três URLs públicas.");
  }
  return args.urls;
}

// Reutiliza o navegador do executor; o modelo não recebe shell nem controles de interação.
export async function readPublicPages(urls, options = {}) {
  validateArguments({ urls });
  const resolve = options.lookup ?? lookup;
  const targets = await Promise.all(
    urls.map((url) => validatePublicUrl(url, resolve)),
  );
  const launch = options.launch ?? ((settings) => chromium.launch(settings));
  const browser = await launch({
    headless: true,
    timeout: 15000,
    ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH }
      : {}),
    args: ["--no-sandbox", "--disable-dev-shm-usage"],
  });
  const pages = [];
  try {
    const context = await browser.newContext({
      viewport: { width: 393, height: 852 },
      isMobile: true,
      hasTouch: true,
      serviceWorkers: "block",
      acceptDownloads: false,
    });
    await context.route("**/*", async (route) => {
      try {
        if (!["GET", "HEAD"].includes(route.request().method()))
          return await route.abort();
        await validatePublicUrl(route.request().url(), resolve);
        // Chromium não intercepta novamente todos os redirects de rede; não os siga implicitamente.
        const response = await route.fetch({ maxRedirects: 0, timeout: 15000 });
        if (response.status() >= 300 && response.status() < 400) {
          await response.dispose();
          return await route.abort();
        }
        await route.fulfill({ response });
        await response.dispose();
      } catch {
        await route.abort();
      }
    });
    await context.routeWebSocket("**/*", (socket) => socket.close());
    for (const target of targets) {
      const page = await context.newPage();
      const accessedAt = new Date().toISOString();
      try {
        const response = await page.goto(target.href, {
          waitUntil: "domcontentloaded",
          timeout: 20000,
        });
        await validatePublicUrl(page.url(), resolve);
        const text = (
          await page.locator("body").innerText({ timeout: 3000 })
        ).replace(/\s+/g, " ");
        pages.push({
          requestedUrl: target.href,
          finalUrl: page.url(),
          accessedAt,
          status: response?.status() ?? null,
          outcome: response?.ok() ? "READ" : "HTTP_ERROR",
          title: await page.title(),
          headings: (await page.locator("h1, h2").allTextContents()).slice(
            0,
            20,
          ),
          visibleCtas: await page.locator("a, button").evaluateAll((items) =>
            items
              .filter((item) => item.checkVisibility())
              .map((item) => item.textContent?.trim())
              .filter(Boolean)
              .slice(0, 30),
          ),
          textExcerpt: text.slice(0, 12000),
          textSha256: createHash("sha256").update(text).digest("hex"),
        });
      } catch (error) {
        pages.push({
          requestedUrl: target.href,
          accessedAt,
          outcome: "FAILED",
          error: error.message,
          action:
            "Confira a URL pública direta, sem redirecionamento, e a disponibilidade da fonte.",
        });
      } finally {
        await page.close();
      }
    }
  } finally {
    await browser.close();
  }
  return { device: "MOBILE_393X852", readOnly: true, pages };
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  process.stdout.write(
    JSON.stringify(
      await readPublicPages(
        validateArguments({ urls: JSON.parse(process.argv[2] ?? "[]") }),
      ),
    ),
  );
}
