import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import fs from "node:fs/promises";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import { once } from "node:events";
import test from "node:test";

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

test("captura página completa e todas as dobras mobile com pixels reais", async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "psique-visual-"));
  const server = http.createServer((_request, response) => {
    response.writeHead(200, { "content-type": "text/html; charset=utf-8" });
    const sections = Array.from(
      { length: 12 },
      (_, index) => `<section><h2>Dobra ${index + 1}</h2><p>Prova visual contínua da jornada.</p></section>`,
    ).join("");
    response.end(`<!doctype html>
      <html lang="pt-BR"><head><title>Jornada de homologação</title>
      <meta name="viewport" content="width=device-width, initial-scale=1">
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
    assert.deepEqual(capture.pages[0].viewport, {
      width: 393,
      height: 852,
      pageHeight: 10224,
      scrollWidth: 393,
    });
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
