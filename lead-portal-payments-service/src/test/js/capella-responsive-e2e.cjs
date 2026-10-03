// Homologa o JAR empacotado com pagamento/e-mail locais e acervo sintético de QA.
const { chromium, devices } = require("playwright");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const http = require("node:http");
const { spawn } = require("node:child_process");
const { createHash } = require("node:crypto");

const root = path.resolve(__dirname, "../../../..");
const output = path.join(root, "artifacts/capella-expansion/local-evidence");
const serviceRoot = path.join(root, "lead-portal-payments-service");
const emails = [];
const results = [];
let java, browser, provider;
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");

async function main() {
  const approved = path.join(output, "approved");
  for (const name of fs.readdirSync(path.join(approved, "barber-v1"))) {
    fs.copyFileSync(
      path.join(approved, "barber-v1", name),
      path.join(approved, name),
    );
  }
  fs.writeFileSync(
    path.join(approved, "approved-manifest.tsv"),
    fs
      .readFileSync(path.join(approved, "approved-manifest.tsv"), "utf8")
      .replace("# profile=barber-v1", "# profile=nails-v1"),
  );
  provider = http.createServer(async (req, res) => {
    let data = "";
    for await (const chunk of req) data += chunk;
    res.setHeader("Content-Type", "application/json");
    if (req.method === "GET" && req.url.startsWith("/v1/payments/qa-")) {
      const id = req.url.split("/").pop();
      res.end(
        JSON.stringify({
          id,
          status: id.includes("pending") ? "pending" : "approved",
          transaction_amount: 67,
          currency_id: "BRL",
          description: "Kit QA",
          payer: { email: `teste+${id}@sandbox.local` },
          external_reference: id.includes("barber")
            ? "capella-barbearia-v1"
            : "agenda-cheia-nail-design",
          metadata: {},
        }),
      );
    } else if (
      req.method === "POST" &&
      req.url === "/api/v1/product-deliveries/send"
    ) {
      const email = JSON.parse(data);
      assert.match(email.to, /@sandbox\.local$/);
      emails.push(email);
      res.end(JSON.stringify({ requestId: `qa-email-${emails.length}` }));
    } else {
      res.statusCode = 404;
      res.end("{}");
    }
  });
  await new Promise((resolve) => provider.listen(0, "127.0.0.1", resolve));
  const providerUrl = `http://127.0.0.1:${provider.address().port}`;
  const log = fs.openSync(path.join(output, "packaged-application.log"), "w");
  java = spawn(
    "java",
    [
      "-jar",
      path.join(
        serviceRoot,
        "target/lead-portal-payments-service-0.0.1-SNAPSHOT.jar",
      ),
      "--server.port=0",
      "--server.address=127.0.0.1",
      "--spring.profiles.active=test",
      "--mercado-pago.access-token=local-qa-token",
      `--mercado-pago.base-url=${providerUrl}`,
      "--agenda-cheia.production.openai-api-key=",
      "--agenda-cheia.production.openai-base-url=http://127.0.0.1:9",
      `--agenda-cheia.production.approved-photo-root=${approved}`,
      `--agenda-cheia.production.storage-root=${path.join(output, "runtime-kits")}`,
      "--delivery.enabled=false",
      "--product-ai.delivery.enabled=false",
      "--pde.entitlement.enabled=false",
      `--digital-product.delivery.email.email-service-base-url=${providerUrl}`,
      "--payments.public-base-url=http://127.0.0.1",
    ],
    {
      cwd: serviceRoot,
      env: {
        ...process.env,
        OPENAI_API_KEY: "",
        MERCADO_PAGO_ACCESS_TOKEN: "local-qa-token",
      },
      stdio: ["ignore", "pipe", "pipe"],
    },
  );
  const port = await new Promise((resolve, reject) => {
    const deadline = setTimeout(
      () => reject(new Error("Aplicação local não iniciou em 60s")),
      60000,
    );
    java.once("exit", (code) => {
      clearTimeout(deadline);
      reject(new Error(`Aplicação encerrou: ${code}`));
    });
    java.stdout.on("data", (bytes) => {
      fs.writeSync(log, bytes);
      const match = bytes.toString().match(/Tomcat started on port (\d+)/);
      if (match) {
        clearTimeout(deadline);
        resolve(Number(match[1]));
      }
    });
    java.stderr.on("data", (bytes) => fs.writeSync(log, bytes));
  });
  const base = `http://127.0.0.1:${port}`;
  browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  for (const [name, settings] of [
    ["desktop", { viewport: { width: 1440, height: 960 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(settings);
    const page = await context.newPage();
    const id = `qa-${name}`;
    await page.goto(`${base}/agenda-cheia/obrigado.html?payment_id=${id}`, {
      waitUntil: "networkidle",
    });
    await page.locator("#profession-context").waitFor();
    assert.match(
      await page.locator("#profession-context").innerText(),
      /Manicure/,
    );
    await page.locator("[name=buyerEmail]").fill(`teste+${id}@sandbox.local`);
    await page.locator("[name=professionalName]").fill(`Studio QA ${name}`);
    await page.locator("[name=cityRegion]").fill("Cidade QA");
    await page.locator("[name=whatsapp]").fill("11999999999");
    await page.locator("[name=services]").fill("Alongamento e manutenção");
    await page
      .locator("[name=visualStyle]")
      .selectOption({ label: "Clean e elegante" });
    await page
      .locator("[name=weeklyGoal]")
      .selectOption({ label: "Divulgar um serviço específico" });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      true,
    );
    await page.locator("button[type=submit]").scrollIntoViewIfNeeded();
    await page.screenshot({
      path: path.join(output, `${name}-briefing.png`),
      fullPage: true,
    });
    await page.locator("button[type=submit]").click();
    await page.getByText("Seu kit está pronto.").waitFor({ timeout: 60000 });
    await page.reload({ waitUntil: "networkidle" });
    assert.equal(await page.locator("#briefing").isVisible(), false);
    const delivery = emails.find(
      (email) => email.paymentId === id && email.downloadUrl,
    );
    assert.ok(delivery);
    const downloadPath = new URL(delivery.downloadUrl).pathname;
    const response = await page.request.get(base + downloadPath);
    assert.equal(response.status(), 200);
    const zip = await response.body();
    assert.equal(zip.subarray(0, 2).toString(), "PK");
    const repeat = await page.request.post(
      `${base}/api/v1/agenda-cheia/post-purchase/briefing`,
      {
        data: {
          paymentId: id,
          buyerEmail: `teste+${id}@sandbox.local`,
          professionalName: "Não substituir",
          cityRegion: "QA",
          whatsapp: "11999999999",
          services: "Outro serviço",
          visualStyle: "Clean",
          weeklyGoal: "QA",
          professionCode: "nails-v1",
        },
      },
    );
    assert.equal(repeat.status(), 200);
    assert.equal(emails.filter((email) => email.paymentId === id).length, 2);
    assert.equal(
      hash(await (await page.request.get(base + downloadPath)).body()),
      hash(zip),
    );
    await page.screenshot({
      path: path.join(output, `${name}-delivered.png`),
      fullPage: true,
    });
    results.push({
      device: name,
      paymentId: id,
      zipSha256: hash(zip),
      segregatedEmails: 2,
      overflow: false,
    });
    await context.close();
  }
  const page = await browser.newPage();
  for (const id of ["qa-pending", "qa-barber-unapproved"]) {
    await page.goto(`${base}/agenda-cheia/obrigado.html?payment_id=${id}`, {
      waitUntil: "networkidle",
    });
    assert.equal(await page.locator("#briefing").isVisible(), false);
    assert.equal(
      emails.some((email) => email.paymentId === id),
      false,
    );
    results.push({ id, blockedBeforeBriefing: true, emails: 0 });
  }
  // A candidata tem o mesmo formulário, com contrato de QA explícito e sem pagamento real.
  await page.close();
  for (const [device, settings] of [
    ["desktop", { viewport: { width: 1440, height: 960 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(settings);
    const preview = await context.newPage();
    await preview.route("**/api/v1/agenda-cheia/post-purchase?*", (route) =>
      route.fulfill({
        json: {
          paymentId: "qa-barber-preview",
          status: "AGUARDANDO_BRIEFING",
          profile: {
            code: "barber-v1",
            profession: "Barbeiro / barbearia",
            productName: "Capella — divulgação para barbearia",
            serviceExamples:
              "Corte, barba, acabamento: informe apenas serviços que oferece.",
          },
        },
      }),
    );
    await preview.goto(
      `${base}/agenda-cheia/obrigado.html?payment_id=qa-barber-preview`,
      { waitUntil: "networkidle" },
    );
    assert.match(
      await preview.locator("#profession-context").innerText(),
      /Barbeiro/,
    );
    assert.equal(
      await preview.locator("[name=professionCode]").inputValue(),
      "barber-v1",
    );
    assert.equal(
      await preview.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      true,
    );
    await preview.locator("button[type=submit]").scrollIntoViewIfNeeded();
    assert.equal(
      await preview.locator("button[type=submit]").isVisible(),
      true,
    );
    await preview.evaluate(() => {
      const banner = document.createElement("p");
      banner.textContent = "QA sintético — sem compra, cobrança ou envio";
      document.querySelector("main").prepend(banner);
    });
    await preview.screenshot({
      path: path.join(output, `barber-qa-${device}-briefing.png`),
      fullPage: true,
    });
    results.push({
      candidate: "barber-v1",
      device,
      uiContract: "QA",
      sale: false,
      commercialPhotographs: "PENDING",
    });
    await context.close();
  }
  fs.writeFileSync(
    path.join(output, "browser-results.json"),
    JSON.stringify({ results, paidCalls: 0, commerceEvidence: false }, null, 2),
  );
  console.log(
    JSON.stringify({
      tests: results.length,
      success: true,
      paidCalls: 0,
      commerceEvidence: false,
    }),
  );
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (browser) await browser.close();
    if (java) java.kill("SIGTERM");
    if (provider) provider.close();
  });
