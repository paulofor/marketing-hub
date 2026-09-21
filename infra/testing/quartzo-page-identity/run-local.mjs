import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

const execute = promisify(execFile);
const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(currentDirectory, "../../..");
const customerAgentDirectory = path.join(
  repositoryRoot,
  "customer-agent-worker",
);
const captureScript = path.join(
  customerAgentDirectory,
  "src/main/resources/browser/bpm-visual-evidence.mjs",
);
const baseUrl = (process.argv[2] ?? "http://127.0.0.1:18081").replace(
  /\/$/,
  "",
);
const slug = "quartzo-page-identity-local";
const publicUrl = `${baseUrl}/api/flows/${slug}/page?mh_test=1`;
const sha256 = (value) =>
  createHash("sha256").update(value, "utf8").digest("hex");

const sourceHtml = `<!doctype html><html lang="pt-BR"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kit Quartzo local</title><style>body{margin:0;font:20px sans-serif}main{padding:32px}section{min-height:820px}button{padding:18px}</style>
</head><body><main><section><h1>Agenda Cheia</h1><p>Kit fixo para organizar a divulgação.</p><button>Comprar o kit por R$ 67</button></section><section><h2>Entrega</h2><p>Arquivos e suporte definidos.</p></section></main></body></html>`;
const publicationSourceSha256 = sha256(sourceHtml);
const publishedHtml = sourceHtml.replace(
  /<head>/i,
  `<head>\n<meta name="mh-publication-source-sha256" content="${publicationSourceSha256}">`,
);
const expectedServedHtmlSha256 = sha256(publishedHtml);
const temporaryDirectory = await fs.mkdtemp(
  path.join(os.tmpdir(), "quartzo-page-identity-"),
);
const inputPath = path.join(temporaryDirectory, "input.json");
const outputPath = path.join(temporaryDirectory, "output.json");
const evidenceDirectory = path.join(temporaryDirectory, "evidence");
let created = false;

try {
  const response = await fetch(`${baseUrl}/api/flows/${slug}`, {
    method: "PUT",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      name: "Identidade Quartzo local",
      description: "Fixture segregada da homologação local",
      customFormHtml: publishedHtml,
      questions: [],
    }),
  });
  assert.equal(response.status, 200, await response.text());
  created = true;

  const servedResponse = await fetch(publicUrl, { cache: "no-store" });
  assert.equal(servedResponse.status, 200);
  assert.match(
    servedResponse.headers.get("content-type") ?? "",
    /^text\/html/i,
  );
  const servedDocument = await servedResponse.text();
  assert.match(
    servedDocument,
    new RegExp(
      `name="mh-publication-source-sha256" content="${publicationSourceSha256}"`,
    ),
  );
  assert.match(
    servedDocument,
    new RegExp(
      `name="mh-served-html-sha256" content="${expectedServedHtmlSha256}"`,
    ),
  );

  await fs.writeFile(
    inputPath,
    JSON.stringify({
      sourceUrl: publicUrl,
      captureSessionId: "quartzo-page-identity-local",
    }),
  );
  await execute(
    process.execPath,
    [captureScript, inputPath, outputPath, evidenceDirectory],
    {
      cwd: customerAgentDirectory,
      env: {
        ...process.env,
        CUSTOMER_AGENT_VISUAL_TEST_MODE: "true",
      },
      maxBuffer: 1024 * 1024,
    },
  );

  const capture = JSON.parse(await fs.readFile(outputPath, "utf8"));
  const page = capture.pages?.[0];
  assert.equal(
    page?.runtimeIdentity?.publicationSourceSha256,
    publicationSourceSha256,
  );
  assert.equal(
    page?.runtimeIdentity?.servedHtmlSha256,
    expectedServedHtmlSha256,
  );
  assert.ok(page?.firstFoldCtas?.includes("Comprar o kit por R$ 67"));
  assert.ok(capture.artifacts?.length >= 3);
  const artifactHashes = [];
  for (const artifact of capture.artifacts) {
    const pixels = await fs.readFile(artifact.localPath);
    assert.deepEqual(
      [...pixels.subarray(0, 8)],
      [137, 80, 78, 71, 13, 10, 26, 10],
    );
    artifactHashes.push({
      evidenceKey: artifact.evidenceKey,
      sha256: createHash("sha256").update(pixels).digest("hex"),
    });
  }

  process.stdout.write(
    `${JSON.stringify({
      status: "PASS",
      publicationSourceSha256,
      servedHtmlSha256: expectedServedHtmlSha256,
      deviceProfile: capture.deviceProfile,
      firstFoldCta: "Comprar o kit por R$ 67",
      artifacts: artifactHashes,
      paidModelCalls: 0,
    })}\n`,
  );
} finally {
  if (created) {
    await fetch(`${baseUrl}/api/flows/${slug}`, { method: "DELETE" });
  }
  await fs.rm(temporaryDirectory, { recursive: true, force: true });
}
