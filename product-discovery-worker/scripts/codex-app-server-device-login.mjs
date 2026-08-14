#!/usr/bin/env node

import { spawn } from "node:child_process";
import readline from "node:readline";

const child = spawn(
  process.env.CODEX_APP_SERVER_COMMAND || "codex",
  ["app-server", "--listen", "stdio://"],
  {
    env: process.env,
    stdio: ["pipe", "pipe", "inherit"],
  },
);
const lines = readline.createInterface({ input: child.stdout });
const pending = new Map();
const reconnectId = process.env.CODEX_AUTH_RECONNECT_ID;
const callbackBaseUrl = String(
  process.env.CODEX_AUTH_CALLBACK_BASE_URL || "",
).replace(/\/$/, "");
let nextId = 1;
let loginId;
let finished = false;

async function callback(path, body) {
  const url = `${callbackBaseUrl}/api/internal/agents/executor-health/codex-auth/reconnections/${reconnectId}/${path}`;
  let lastError;
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(30000),
      });
      if (response.ok) return;
      lastError = new Error(
        `Backend recusou callback ${path} (${response.status})`,
      );
    } catch (error) {
      lastError = error;
    }
    if (attempt < 3) await delay(attempt * 1000);
  }
  throw new Error(
    `Falha ao registrar ${path} no backend após 3 tentativas: ${lastError?.message || "erro desconhecido"}`,
  );
}

/** Aguarda entre retentativas transitórias sem reiniciar o fluxo OAuth. */
function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function request(method, params) {
  const id = nextId++;
  child.stdin.write(
    `${JSON.stringify({ method, id, ...(params ? { params } : {}) })}\n`,
  );
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

function notify(method, params = {}) {
  child.stdin.write(`${JSON.stringify({ method, params })}\n`);
}

function stop(code, message) {
  if (finished) return;
  finished = true;
  clearTimeout(timeout);
  if (message) (code === 0 ? console.log : console.error)(message);
  lines.close();
  child.kill("SIGTERM");
  process.exitCode = code;
}

lines.on("line", async (line) => {
  let message;
  try {
    message = JSON.parse(line);
  } catch {
    return;
  }
  if (typeof message.id === "number") {
    const waiter = pending.get(message.id);
    if (!waiter) return;
    pending.delete(message.id);
    if (message.error)
      waiter.reject(
        new Error(message.error.message || "Falha no Codex App Server"),
      );
    else waiter.resolve(message.result || {});
    return;
  }
  if (
    message.method !== "account/login/completed" ||
    message.params?.loginId !== loginId
  )
    return;
  if (!message.params?.success)
    return stop(
      1,
      `Falha na autenticação Codex: ${message.params?.error || "erro não informado"}`,
    );
  try {
    const account = await request("account/read", { refreshToken: false });
    const authMode = account.account?.type || account.authMode;
    if (!authMode)
      throw new Error("Codex App Server não confirmou a conta autenticada");
    await callback("completion", {
      authenticated: true,
      detail: `Sessão de Argos confirmada pelo App Server (${authMode}).`,
    });
    stop(0, `Sessão Codex de Argos confirmada (modo ${authMode}).`);
  } catch (error) {
    stop(1, error.message);
  }
});

child.once("error", (error) =>
  stop(1, `Não foi possível iniciar o Codex App Server: ${error.message}`),
);
child.once("exit", (code) => {
  if (!finished)
    stop(code || 1, "Codex App Server encerrou antes da autenticação.");
});
const timeout = setTimeout(
  () => {
    if (loginId) notify("account/login/cancel", { loginId });
    stop(1, "Tempo esgotado aguardando a confirmação do device code.");
  },
  Number(process.env.CODEX_DEVICE_LOGIN_TIMEOUT_MS || 900000),
);

try {
  await request("initialize", {
    clientInfo: {
      name: "marketing_hub_argos",
      title: "Marketing Hub Argos",
      version: "1.0.0",
    },
  });
  notify("initialized");
  const login = await request("account/login/start", {
    type: "chatgptDeviceCode",
  });
  loginId = login.loginId;
  if (!loginId || !login.verificationUrl || !login.userCode)
    throw new Error(
      "Codex App Server não devolveu URL e código de autenticação.",
    );
  await callback("device-code", {
    verificationUrl: login.verificationUrl,
    userCode: login.userCode,
  });
  console.log(
    `Abra ${login.verificationUrl} e informe o código ${login.userCode}.`,
  );
} catch (error) {
  stop(1, error.message);
}
