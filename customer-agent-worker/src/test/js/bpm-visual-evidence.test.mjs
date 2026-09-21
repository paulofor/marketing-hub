import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import fs from "node:fs/promises";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import { once } from "node:events";
import test from "node:test";
import { chromium } from "playwright-core";

const script =
  process.env.CUSTOMER_AGENT_BPM_VISUAL_SCRIPT ??
  path.resolve("src/main/resources/browser/bpm-visual-evidence.mjs");

/** Lê as dimensões declaradas no IHDR sem depender de biblioteca de imagem. */
function pngDimensions(pixels) {
  return {
    width: pixels.readUInt32BE(16),
    height: pixels.readUInt32BE(20),
  };
}

/** Executa o capturador real e preserva stdout e stderr para diagnóstico do teste. */
async function runCapture(input, output, evidence, environment = {}) {
  const child = spawn(
    globalThis.process.execPath,
    [script, input, output, evidence],
    {
      env: {
        ...globalThis.process.env,
        ...environment,
      },
    },
  );
  let log = "";
  child.stdout.on("data", (chunk) => {
    log += chunk.toString();
  });
  child.stderr.on("data", (chunk) => {
    log += chunk.toString();
  });
  const [code] = await once(child, "close");
  return { code, log };
}

/** Confere pixels do PNG real sem usar o metadado declarado como prova da posição. */
async function pixelAt(png, x, y) {
  const browser = await chromium.launch({
    ...(process.env.CHROMIUM_BIN
      ? { executablePath: process.env.CHROMIUM_BIN }
      : {}),
    args: ["--no-sandbox", "--disable-dev-shm-usage"],
  });
  try {
    const page = await browser.newPage();
    return await page.evaluate(
      async ({ data, x, y }) => {
        const image = new Image();
        image.src = `data:image/png;base64,${data}`;
        await image.decode();
        const canvas = document.createElement("canvas");
        canvas.width = image.width;
        canvas.height = image.height;
        const context = canvas.getContext("2d");
        context.drawImage(image, 0, 0);
        return [...context.getImageData(x, y, 1, 1).data];
      },
      { data: png.toString("base64"), x, y },
    );
  } finally {
    await browser.close();
  }
}

for (const locked of [false, true]) {
  test(
    locked
      ? "bloqueia captura quando a página impede a posição declarada"
      : "captura o cabeçalho no topo com rolagem suave sem deslocar pixels entre tentativas",
    { timeout: 60_000 },
    async () => {
      const directory = await fs.mkdtemp(
        path.join(os.tmpdir(), "psique-scroll-"),
      );
      const server = http.createServer((_request, response) => {
        response.writeHead(200, { "content-type": "text/html" });
        response.end(`<!doctype html><html><head>
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <style>html{scroll-behavior:smooth}body{margin:0;background:white}
          header{position:sticky;top:0;height:80px;background:rgb(255,0,255)}
          main{height:12000px}</style></head><body>
          <header></header><main><button>Comprar pacote</button></main>
          ${locked ? '<script>scrollTo({top:120,behavior:"instant"});window.scrollTo=()=>{};</script>' : ""}
          </body></html>`);
      });
      server.listen(0, "127.0.0.1");
      await once(server, "listening");
      try {
        const input = path.join(directory, "input.json");
        await fs.writeFile(
          input,
          JSON.stringify({
            sourceUrl: `http://127.0.0.1:${server.address().port}/kit`,
            captureSessionId: "capture-scroll-test",
          }),
        );
        const images = [];
        for (let attempt = 0; attempt < (locked ? 1 : 2); attempt++) {
          const output = path.join(directory, `capture-${attempt}.json`);
          const result = await runCapture(
            input,
            output,
            path.join(directory, `evidence-${attempt}`),
            { CUSTOMER_AGENT_VISUAL_TEST_MODE: "true" },
          );
          if (locked) {
            assert.notEqual(
              result.code,
              0,
              "A captura não pode declarar uma posição não atingida.",
            );
            assert.match(
              result.log,
              /posição.*esperad[ao].*0.*observad[ao].*120/i,
            );
            assert.equal(await fs.stat(output).catch(() => null), null);
            return;
          }
          assert.equal(result.code, 0, result.log);
          const capture = JSON.parse(await fs.readFile(output, "utf8"));
          const full = capture.artifacts.find(
            (a) => a.evidenceType === "FULL_PAGE",
          );
          assert.equal(full.scrollY, 0);
          const png = await fs.readFile(full.localPath);
          assert.deepEqual(await pixelAt(png, 10, 10), [255, 0, 255, 255]);
          images.push(png);
        }
        assert.deepEqual(images[0], images[1]);
      } finally {
        server.close();
        await once(server, "close");
        await fs.rm(directory, { recursive: true, force: true });
      }
    },
  );
}

test("captura página completa, dobras mobile e identidade pública com pixels reais", async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "psique-visual-"));
  const server = http.createServer((request, response) => {
    if (request.url === "/version-diagnostics.json") {
      response.writeHead(200, {
        "content-type": "application/json; charset=utf-8",
      });
      response.end(
        JSON.stringify({
          version: "v8",
          experienceVersion: "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
          frontendSourceSha256: "a".repeat(64),
          imageTag: "pde-platform-frontend-v8:test",
          commitSha: "test-commit",
        }),
      );
      return;
    }
    response.writeHead(200, { "content-type": "text/html; charset=utf-8" });
    const sections = Array.from(
      { length: 12 },
      (_, index) =>
        `<section><h2>Dobra ${index + 1}</h2><p>Prova visual contínua da jornada.</p>${
          index === 0
            ? "<button>Começar meu ajuste gratuito</button><p>Acesso por 90 dias, sem assinatura ou renovação.</p>"
            : ""
        }</section>`,
    ).join("");
    response.end(`<!doctype html>
      <html lang="pt-BR"><head><title>Jornada de homologação</title>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <meta name="mh-publication-source-sha256" content="${"c".repeat(64)}">
      <meta name="mh-served-html-sha256" content="${"e".repeat(64)}">
      <style>*{box-sizing:border-box}html,body{margin:0}section{height:852px;padding:48px;font:24px sans-serif}section:nth-child(odd){background:#f6f1ff}a{display:inline-block;padding:18px;background:#5f246e;color:white}</style>
      </head><body>${sections}</body></html>`);
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const address = server.address();
  assert.equal(typeof address, "object");
  const sourceUrl = `http://127.0.0.1:${address.port}/jornada`;
  const input = path.join(directory, "input.json");
  const output = path.join(directory, "output.json");
  const evidence = path.join(directory, "evidence");
  await fs.writeFile(
    input,
    JSON.stringify({ sourceUrl, captureSessionId: "capture-session-test" }),
  );

  try {
    const result = await runCapture(input, output, evidence, {
      CUSTOMER_AGENT_VISUAL_TEST_MODE: "true",
    });
    assert.equal(result.code, 0, result.log);
    const capture = JSON.parse(await fs.readFile(output, "utf8"));
    assert.equal(capture.captureSessionId, "capture-session-test");
    assert.equal(capture.deviceProfile, "IPHONE_15_PRO");
    assert.equal(capture.pages[0].finalUrl, sourceUrl);
    assert.deepEqual(capture.pages[0].runtimeIdentity, {
      version: "v8",
      experienceVersion: "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
      frontendSourceSha256: "a".repeat(64),
      imageTag: "pde-platform-frontend-v8:test",
      commitSha: "test-commit",
      publicationSourceSha256: "c".repeat(64),
      servedHtmlSha256: "e".repeat(64),
    });
    assert.deepEqual(capture.pages[0].viewport, {
      width: 393,
      height: 852,
      pageHeight: 10224,
      scrollWidth: 393,
    });
    assert.deepEqual(capture.pages[0].firstFoldCtas, [
      "Começar meu ajuste gratuito",
    ]);
    assert.match(
      capture.pages[0].visibleText,
      /Acesso por 90 dias, sem assinatura ou renovação\./,
    );
    const fullPages = capture.artifacts.filter(
      (artifact) => artifact.evidenceType === "FULL_PAGE",
    );
    const folds = capture.artifacts.filter(
      (artifact) => artifact.evidenceType === "FOLD",
    );
    assert.equal(fullPages.length, 1);
    assert.deepEqual(
      folds.map((artifact) => [artifact.foldNumber, artifact.scrollY]),
      Array.from({ length: 12 }, (_, index) => [index + 1, index * 852]),
    );
    for (const artifact of capture.artifacts) {
      const pixels = await fs.readFile(artifact.localPath);
      assert.deepEqual(
        [...pixels.subarray(0, 8)],
        [137, 80, 78, 71, 13, 10, 26, 10],
      );
    }
    const fullPagePixels = await fs.readFile(fullPages[0].localPath);
    const firstFoldPixels = await fs.readFile(folds[0].localPath);
    assert.deepEqual(pngDimensions(fullPagePixels), {
      width: 393,
      height: 10224,
    });
    assert.deepEqual(pngDimensions(firstFoldPixels), {
      width: 1179,
      height: 2556,
    });
  } finally {
    server.close();
    await once(server, "close");
    await fs.rm(directory, { recursive: true, force: true });
  }
});

test("preserva identidade do HTML quando o host não publica diagnóstico PDE em JSON", async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "psique-quartzo-"));
  const sourceHash = "d".repeat(64);
  const server = http.createServer((request, response) => {
    response.writeHead(200, { "content-type": "text/html; charset=utf-8" });
    if (request.url === "/version-diagnostics.json") {
      response.end("<!doctype html><html><body>Lead Portal</body></html>");
      return;
    }
    response.end(`<!doctype html><html><head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <meta name="mh-publication-source-sha256" content="${sourceHash}">
      <meta name="mh-served-html-sha256" content="${"e".repeat(64)}">
      <style>body{margin:0;font:20px sans-serif}main{min-height:852px;padding:32px}</style>
      </head><body><main><h1>Kit auditado</h1><button>Comprar</button></main></body></html>`);
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const address = server.address();
  assert.equal(typeof address, "object");
  const input = path.join(directory, "input.json");
  const output = path.join(directory, "output.json");
  const evidence = path.join(directory, "evidence");
  await fs.writeFile(
    input,
    JSON.stringify({
      sourceUrl: `http://127.0.0.1:${address.port}/kit`,
      captureSessionId: "capture-session-quartzo",
    }),
  );
  try {
    const result = await runCapture(input, output, evidence, {
      CUSTOMER_AGENT_VISUAL_TEST_MODE: "true",
    });
    assert.equal(result.code, 0, result.log);
    const capture = JSON.parse(await fs.readFile(output, "utf8"));
    assert.deepEqual(capture.pages[0].runtimeIdentity, {
      version: null,
      experienceVersion: null,
      frontendSourceSha256: null,
      imageTag: null,
      commitSha: null,
      publicationSourceSha256: sourceHash,
      servedHtmlSha256: "e".repeat(64),
    });
  } finally {
    server.close();
    await once(server, "close");
    await fs.rm(directory, { recursive: true, force: true });
  }
});

test("recusa rede privada por padrão antes de abrir o navegador", async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "psique-private-"));
  const input = path.join(directory, "input.json");
  const output = path.join(directory, "output.json");
  const evidence = path.join(directory, "evidence");
  await fs.writeFile(
    input,
    JSON.stringify({
      sourceUrl: "http://127.0.0.1:4567/jornada",
      captureSessionId: "capture-session-private",
    }),
  );
  try {
    const result = await runCapture(input, output, evidence);
    assert.notEqual(result.code, 0);
    assert.match(result.log, /URL pública inválida/);
  } finally {
    await fs.rm(directory, { recursive: true, force: true });
  }
});
