import assert from "node:assert/strict";
import { execFile, spawn } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

// Homologa a recuperação real sem persistência, publicação ou modelo de produção.
const execute = promisify(execFile);
const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
);
const output = path.join(root, "artifacts/publication-recovery");
const frontend = path.join(root, "frontend");
const backend = path.join(root, "backend/ads-service");
const worker = path.join(root, "customer-agent-worker");
const requireFrontend = createRequire(path.join(frontend, "package.json"));
const { chromium, devices } = requireFrontend("@playwright/test");
const { createServer } = await import(
  path.join(
    path.dirname(requireFrontend.resolve("vite/package.json")),
    "dist/node/index.js",
  )
);
const children = [];
const handles = [];
const sha256 = (value) => createHash("sha256").update(value).digest("hex");
let vite;
let browser;
const localEntry = ".publication-recovery-local";

async function classpath(directory) {
  const dependencies = (
    await fs.readFile(path.join(directory, "target/recovery.classpath"), "utf8")
  ).trim();
  const packaged = await fs.readdir(path.join(directory, "target"));
  const jar =
    packaged.find((name) => name.endsWith(".jar.original")) ??
    packaged.find((name) => name === "app.jar");
  assert.ok(jar, `Compile e empacote ${directory} antes de homologar`);
  return `${directory}/target/test-classes:${directory}/target/${jar}:${dependencies}`;
}

async function startJava(directory, mainClass, args, logName) {
  const log = await fs.open(path.join(output, logName), "w");
  handles.push(log);
  const child = spawn(
    "java",
    ["-cp", await classpath(directory), mainClass, ...args],
    {
      cwd: directory,
      stdio: ["ignore", log.fd, log.fd],
    },
  );
  children.push(child);
  return child;
}

async function waitReady(url, child) {
  for (let attempt = 0; attempt < 90; attempt++) {
    assert.equal(child.exitCode, null, `Servidor terminou: ${child.exitCode}`);
    try {
      if ((await fetch(url)).ok) return;
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`Servidor local não respondeu: ${url}`);
}

async function capture(source, label, expectedHash, expectedDecision) {
  const directory = path.join(output, label);
  await fs.mkdir(directory, { recursive: true });
  const input = path.join(directory, "input.json");
  const captured = path.join(directory, "capture.json");
  const artifacts = path.join(directory, "evidence");
  await fs.writeFile(
    input,
    JSON.stringify({ sourceUrl: source.publicUrl, captureSessionId: label }),
  );
  await execute(
    process.execPath,
    [
      path.join(worker, "src/main/resources/browser/bpm-visual-evidence.mjs"),
      input,
      captured,
      artifacts,
    ],
    {
      cwd: worker,
      env: { ...process.env, CUSTOMER_AGENT_VISUAL_TEST_MODE: "true" },
      maxBuffer: 1024 * 1024,
    },
  );
  const validation = await execute(
    "java",
    [
      "-cp",
      await classpath(worker),
      "com.marketinghub.customeragentworker.PublicationCaptureLocalCheck",
      captured,
      artifacts,
      expectedHash,
      expectedDecision,
    ],
    { cwd: worker },
  );
  assert.match(validation.stdout, /PASS:/);
  const result = JSON.parse(await fs.readFile(captured, "utf8"));
  const hashes = [];
  for (const artifact of result.artifacts) {
    const bytes = await fs.readFile(artifact.localPath);
    assert.deepEqual(
      [...bytes.subarray(0, 8)],
      [137, 80, 78, 71, 13, 10, 26, 10],
    );
    hashes.push({ key: artifact.evidenceKey, sha256: sha256(bytes) });
  }
  return {
    decision: expectedDecision,
    identity: result.pages[0].runtimeIdentity,
    artifacts: hashes,
  };
}

try {
  await fs.mkdir(output, { recursive: true });
  const portal = await startJava(
    path.join(root, "lead-portal/backend"),
    "com.marketinghub.leadportal.LeadPortalApplication",
    [
      "--spring.config.location=classpath:application-test.properties",
      "--spring.profiles.active=test",
      "--server.port=18081",
      "--server.address=127.0.0.1",
      "--lead-portal.clarity.project-id=",
      "--lead-portal.image-pipeline.enabled=false",
      "--lead-portal.marketing-hub.funnel-tracking-base-url=http://127.0.0.1:18096",
    ],
    "integration-portal.log",
  );
  await waitReady("http://127.0.0.1:18081/actuator/health", portal);
  const api = await startJava(
    backend,
    "com.marketinghub.gerasalespage.v1.service.PublicationRecoveryLocalApplication",
    [],
    "integration-api.log",
  );
  const endpoint =
    "http://127.0.0.1:18096/api/experiments/701/gerasalespage/v1/publications/901";
  await waitReady(`${endpoint}/recovery`, api);
  const source = JSON.parse(
    await fs.readFile(path.join(output, "local-source.json"), "utf8"),
  );
  const sourceHash = sha256(source.sourceHtml);
  const before = await capture(
    source,
    "before-recovery",
    sourceHash,
    "BLOCKED",
  );
  assert.notEqual(before.identity.publicationSourceSha256, sourceHash);

  await fs.writeFile(
    path.join(frontend, `${localEntry}.html`),
    `<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="root"></div><script type="module" src="/${localEntry}.tsx"></script></body></html>`,
  );
  await fs.writeFile(
    path.join(frontend, `${localEntry}.tsx`),
    `import React from 'react'; import {createRoot} from 'react-dom/client'; import {QueryClient,QueryClientProvider} from '@tanstack/react-query'; import 'bootstrap/dist/css/bootstrap.min.css'; import Recovery from './src/pages/experiment/SalesPagePublicationRecovery'; createRoot(document.getElementById('root')).render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><main className="container py-3"><h1>Publicação aprovada</h1><Recovery experimentId={701} publicationId={901}/></main></QueryClientProvider>);`,
  );
  vite = await createServer({
    root: frontend,
    configFile: false,
    esbuild: { jsx: "automatic" },
    server: {
      host: "127.0.0.1",
      port: 18097,
      strictPort: true,
      proxy: { "/api": "http://127.0.0.1:18096" },
    },
  });
  await vite.listen();
  browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const devicesChecked = [];
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    await context.route("**/*", (route) =>
      new URL(route.request().url()).hostname === "127.0.0.1"
        ? route.continue()
        : route.abort(),
    );
    const page = await context.newPage();
    await page.goto(`http://127.0.0.1:18097/${localEntry}.html`);
    await page
      .getByRole("button", { name: "Reenviar página aprovada" })
      .click();
    await page.getByRole("button", { name: "Confirmar reenvio" }).click();
    await page
      .getByRole("status")
      .filter({ hasText: "Página reenviada sem nova geração" })
      .waitFor();
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth,
      ),
      false,
    );
    await page.screenshot({
      path: path.join(output, `${name}.png`),
      fullPage: true,
    });
    devicesChecked.push(name);
    await context.close();
  }
  const repeated = await Promise.all(
    Array.from({ length: 4 }, async () => {
      const response = await fetch(`${endpoint}/republish`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ expectedSourceSha256: sourceHash }),
      });
      assert.equal(response.status, 200);
      const body = await response.json();
      assert.equal(body.sourceSha256, sourceHash);
      return body.publicationId;
    }),
  );
  assert.deepEqual(repeated, [901, 901, 901, 901]);
  const after = await capture(source, "after-recovery", sourceHash, "PASS");
  assert.equal(after.identity.publicationSourceSha256, sourceHash);
  const foreign = await capture(
    source,
    "foreign-source",
    "0".repeat(64),
    "BLOCKED",
  );
  const report = {
    status: "PASS",
    sourceSha256: sourceHash,
    devicesChecked,
    before,
    after,
    foreign,
    concurrentRequests: 4,
    paidModelCalls: 0,
    persistence: "repositórios backend simulados; Lead Portal H2 local",
    productionWrites: 0,
  };
  await fs.writeFile(
    path.join(output, "integration-result.json"),
    JSON.stringify(report, null, 2),
  );
  process.stdout.write(`${JSON.stringify(report)}\n`);
} finally {
  if (browser) await browser.close();
  if (vite) await vite.close();
  for (const child of children.reverse()) child.kill("SIGTERM");
  for (const log of handles) await log.close();
  await fs.rm(path.join(frontend, `${localEntry}.html`), { force: true });
  await fs.rm(path.join(frontend, `${localEntry}.tsx`), { force: true });
}
