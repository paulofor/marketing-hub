import assert from "node:assert/strict";
import test from "node:test";
import { chromium } from "playwright-core";
import { createStrategistServer } from "../../main/resources/mcp/experiment-strategist.mjs";
import {
  readPublicPages,
  handlePublicRoute,
  validateArguments,
  validatePublicUrl,
} from "../../main/resources/browser/public-research.mjs";

const publicDns = async () => [{ address: "93.184.216.34" }];
const env = { MCP_BACKEND_URL: "http://127.0.0.1:1", MCP_EXECUTION_ID: "701" };

test("bloqueia argumentos estranhos e endereços privados antes de abrir navegador", async () => {
  for (const args of [
    { urls: [] },
    { urls: ["https://a.example"], command: "id" },
    { urls: Array(4).fill("https://a.example") },
  ])
    assert.throws(() => validateArguments(args));
  for (const url of [
    "file:///etc/passwd",
    "http://localhost",
    "http://127.0.0.1",
    "http://2130706433",
    "http://169.254.169.254",
    "http://10.1.1.1",
    "http://172.16.0.1",
    "http://192.168.1.1",
    "http://100.64.1.1",
    "http://[::1]",
    "http://[::ffff:127.0.0.1]",
    "http://[fd00::1]",
    "http://user:secret@example.com",
    "http://example.com:8000",
  ]) {
    await assert.rejects(validatePublicUrl(url, publicDns), undefined, url);
  }
  await assert.rejects(
    validatePublicUrl("https://intranet.example", async () => [
      { address: "10.1.1.1" },
    ]),
  );
  await assert.rejects(
    validatePublicUrl("https://mixed.example", async () => [
      { address: "93.184.216.34" },
      { address: "127.0.0.1" },
    ]),
  );
  let launches = 0;
  await assert.rejects(
    readPublicPages(["http://127.0.0.1"], {
      launch: async () => {
        launches++;
      },
    }),
  );
  assert.equal(launches, 0);
});

test("MCP anuncia somente leitura e mantém a correlação sem permitir comandos arbitrários", async () => {
  const server = createStrategistServer(env, {
    readPages: async (urls) => ({
      pages: urls.map((requestedUrl) => ({ requestedUrl, outcome: "READ" })),
    }),
  });
  assert.equal(
    (await server.dispatch({ method: "initialize" })).serverInfo.version,
    "1.2.0",
  );
  const listed = await server.dispatch({ method: "tools/list" });
  const tool = listed.tools.find(
    (t) => t.name === "consultar_paginas_publicas",
  );
  assert.deepEqual(tool.annotations, {
    readOnlyHint: true,
    destructiveHint: false,
    openWorldHint: true,
  });
  const response = await server.dispatch({
    method: "tools/call",
    params: {
      name: tool.name,
      arguments: { urls: ["https://source.example"] },
    },
  });
  assert.equal(JSON.parse(response.content[0].text).executionId, "701");
  await assert.rejects(
    server.dispatch({
      method: "tools/call",
      params: { name: "shell", arguments: { command: "id" } },
    }),
  );
  await assert.rejects(
    server.dispatch({
      method: "tools/call",
      params: { name: tool.name, arguments: { urls: [], command: "id" } },
    }),
  );
});

test("MCP e Chromium reais preservam sucesso, falha e proteção contra escrita/redirecionamento", async () => {
  const requests = [];
  let closed = false;
  const launch = async (settings) => {
    const browser = await chromium.launch(settings);
    const close = browser.close.bind(browser);
    browser.close = async () => {
      closed = true;
      await close();
    };
    const newContext = browser.newContext.bind(browser);
    browser.newContext = async (settings) => {
      const context = await newContext(settings);
      const route = context.route.bind(context);
      context.route = async (pattern, handler) =>
        route(pattern, (original) =>
          handler(
            new Proxy(original, {
              get(target, property) {
                if (property === "fetch")
                  return async (options) => {
                    assert.equal(options.maxRedirects, 0);
                    const url = target.request().url();
                    requests.push({
                      action: "READ",
                      url,
                      method: target.request().method(),
                    });
                    if (url.endsWith("/error"))
                      throw new Error("Falha de rede simulada");
                    const redirected = url.endsWith("/redirect");
                    return {
                      status: () => (redirected ? 302 : 200),
                      headersArray: () =>
                        redirected
                          ? [
                              {
                                name: "location",
                                value: "http://127.0.0.1/private",
                              },
                            ]
                          : [
                              {
                                name: "content-type",
                                value: "text/html; charset=utf-8",
                              },
                            ],
                      body: async () =>
                        Buffer.from(
                          '<html><meta charset="utf-8"><title>Prova local</title><body><h1>Três combinações</h1><a href="/offer">Ver oferta</a><script>fetch("https://source.example/write",{method:"POST",body:"QA"})</script></body></html>',
                        ),
                      dispose: async () => {},
                    };
                  };
                if (property === "fulfill")
                  return async ({ response }) =>
                    target.fulfill({
                      status: response.status(),
                      headers: Object.fromEntries(
                        response
                          .headersArray()
                          .map(({ name, value }) => [name, value]),
                      ),
                      body: await response.body(),
                    });
                if (property === "abort")
                  return async (...args) => {
                    requests.push({
                      action: "BLOCKED",
                      url: target.request().url(),
                      method: target.request().method(),
                    });
                    return target.abort(...args);
                  };
                const value = Reflect.get(target, property);
                return typeof value === "function" ? value.bind(target) : value;
              },
            }),
          ),
        );
      return context;
    };
    return browser;
  };
  const server = createStrategistServer(env, {
    readPages: (urls) => readPublicPages(urls, { launch, lookup: publicDns }),
  });
  const response = await server.dispatch({
    method: "tools/call",
    params: {
      name: "consultar_paginas_publicas",
      arguments: {
        urls: [
          "https://source.example/ok",
          "https://source.example/redirect",
          "https://source.example/error",
        ],
      },
    },
  });
  const result = JSON.parse(response.content[0].text);
  assert.equal(result.readOnly, true);
  assert.equal(result.pages[0].title, "Prova local");
  assert.match(result.pages[0].textExcerpt, /Três combinações/);
  assert.match(result.pages[0].textSha256, /^[a-f0-9]{64}$/);
  assert.equal(result.pages[0].status, 200);
  assert.equal(result.pages[1].outcome, "FAILED");
  assert.equal(result.pages[2].outcome, "FAILED");
  assert.ok(
    requests.some((r) => r.method === "POST" && r.action === "BLOCKED"),
  );
  assert.ok(
    requests.some((r) => r.url.endsWith("/redirect") && r.action === "BLOCKED"),
  );
  assert.ok(requests.every((r) => r.action !== "READ" || r.method === "GET"));
  assert.ok(requests.every((r) => !r.url.includes("127.0.0.1")));
  assert.equal(closed, true);
});

test("encerramento concorrente não aborta rota consumida nem encerra o MCP", async () => {
  for (const scenario of ["fulfill", "redirect", "fetch", "dispose", "post"]) {
    let aborts = 0,
      disposals = 0,
      reads = 0,
      fulfills = 0;
    const response = {
      status: () => (scenario === "redirect" ? 302 : 200),
      dispose: async () => {
        disposals++;
        if (scenario === "dispose") throw new Error("Contexto encerrado");
      },
    };
    const route = {
      request: () => ({
        method: () => (scenario === "post" ? "POST" : "GET"),
        url: () => "https://source.example/page",
      }),
      fetch: async () => {
        reads++;
        if (scenario === "fetch") throw new Error("Página encerrada");
        return response;
      },
      fulfill: async () => {
        fulfills++;
        if (scenario === "fulfill")
          throw new Error("Route is already handled!");
      },
      abort: async () => {
        aborts++;
        throw new Error("Route is already handled!");
      },
    };
    await assert.doesNotReject(handlePublicRoute(route, publicDns), scenario);
    assert.equal(
      aborts,
      ["redirect", "fetch", "post"].includes(scenario) ? 1 : 0,
      scenario,
    );
    assert.equal(
      disposals,
      ["fetch", "post"].includes(scenario) ? 0 : 1,
      scenario,
    );
    assert.equal(reads, scenario === "post" ? 0 : 1, scenario);
    assert.equal(
      fulfills,
      ["fulfill", "dispose"].includes(scenario) ? 1 : 0,
      scenario,
    );
  }
});
