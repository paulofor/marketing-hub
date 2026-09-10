import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import fs from "node:fs/promises";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import test from "node:test";

const harness = process.env.PDE_HARNESS_TEST_SCRIPT ||
  path.resolve("src/main/resources/browser/pde-agent-validation-harness.mjs");
const apiPath = "/api/pde/mira/private/v1";

// A tela simulada só altera o estado visual depois da resposta do backend.
const pageHtml = `<!doctype html><html lang="pt-BR"><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{margin:16px}input{display:block;max-width:90%}</style></head><body>
<main></main><script>
const token=sessionStorage.getItem('mira-private-session');let state;
async function api(p,body,method=body?'POST':'GET'){
 const r=await fetch('${apiPath}'+p,{method,headers:{'Content-Type':'application/json','X-Mira-Session':token},body:body?JSON.stringify(body):undefined});
 if(!r.ok)throw Error('Ação não confirmada');return r.json();
}
function render(){
 const root=document.querySelector('main');
 if(state.readingFinished){root.innerHTML='<h1>Homologação interna concluída</h1><label>Rotina preservada</label><button>Consultar rotina</button>';return}
 if(state.status==='READY'){
  root.innerHTML='<h1>Uma ordem simples para consultar</h1><label>Rotina</label><button id="used">Marcar uma parte como consultada</button>'+(state.events.includes('READY_RESULT_USED')?'<button id="finish">Concluir cenário interno</button>':'');
  document.querySelector('#used').onclick=()=>event('READY_RESULT_USED');
  if(document.querySelector('#finish'))document.querySelector('#finish').onclick=()=>event('AGENT_SCENARIO_COMPLETED');
  return;
 }
 root.innerHTML='<h1>Conte o mínimo necessário</h1><p data-testid="agent-validation-mode">Homologação interna automatizada</p><label>Objetivo de autocuidado<input id="goal"></label><label>Nome<input id="name1"></label><label>Como o rótulo orienta usar<input id="directions1"></label><label>Nome<input id="name2"></label><label>Como o rótulo orienta usar<input id="directions2"></label><button id="generate" disabled>Gerar rotina segura</button>'+(state.status==='BLOCKED'?'<div role="alert">O objetivo pede conclusão clínica.</div><button id="safe">Concluir cenário de segurança</button>':'');
 const fields=['goal','name1','directions1','name2','directions2'];
 function enable(){document.querySelector('#generate').disabled=fields.some(id=>!document.getElementById(id).value)}
 for(const id of fields){document.getElementById(id).value=state.input?.[id]||'';document.getElementById(id).oninput=enable}enable();
 document.querySelector('#generate').onclick=async()=>{
  try{state=await api('/input',Object.fromEntries(fields.map(id=>[id,document.getElementById(id).value])),'PUT');state=await api('/generate',{});render()}
  catch{root.insertAdjacentHTML('beforeend','<div role="alert">Falha recuperável</div>')}
 };
 if(document.querySelector('#safe'))document.querySelector('#safe').onclick=()=>event('AGENT_SCENARIO_COMPLETED');
}
async function event(eventType){try{state=await api('/events',{eventType});render()}catch{document.querySelector('main').insertAdjacentHTML('beforeend','<div role="alert">Falha recuperável</div>')}}
api('/session').then(s=>{state=s;render()});
</script></body></html>`;

/** Executa o script produtivo contra HTTP e navegador reais, com persistência simulada. */
async function executeHarness(mode, fault) {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "mira-harness-contract-"));
  const sessions = new Map();
  const calls = [];
  const token = "segredo-sintetico-nao-deve-aparecer";
  const server = http.createServer(async (request, response) => {
    if (!request.url.startsWith(apiPath)) {
      response.writeHead(200, { "content-type": "text/html; charset=utf-8" });
      response.end(pageHtml);
      return;
    }
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    const body = chunks.length ? JSON.parse(Buffer.concat(chunks)) : {};
    const route = request.url.slice(apiPath.length);
    const send = (value, status = 200) => {
      response.writeHead(status, { "content-type": "application/json" });
      response.end(JSON.stringify(value));
    };
    let session = sessions.get(request.headers["x-mira-session"]);
    if (route === "/internal/agent-validations/sessions") {
      assert.equal(request.headers["x-pde-internal-token"], token);
      const id = randomUUID();
      session = { sessionToken: id, evidenceId: id, scenarioCode: body.scenarioCode,
        agentValidation: true, trafficClass: "AGENT_VALIDATION", mhInternalTest: true,
        prototypeVersion: "mira-private-v2", status: "INPUT", events: ["EXPERIENCE_STARTED"],
        humanEvidenceClaimed: false, commercialEvidenceClaimed: false,
        sideEffects: { paymentEnabled: false, published: false, campaignCreated: false, mediaSpendBrl: 0 } };
      sessions.set(id, session);
    } else if (route.startsWith("/internal/agent-validations/evidence/")) {
      session = sessions.get(route.split("/").at(-1));
    } else if (route === "/input") {
      session.input = body;
    } else if (route === "/generate") {
      if (session.input.goal.includes("Diagnosticar")) session.status = "BLOCKED";
      else { session.status = "READY"; session.events.push("VALUE_MOMENT"); }
    } else if (route === "/events") {
      calls.push({ event: body.eventType, phase: "requested", id: session.evidenceId });
      if (body.eventType === "READY_RESULT_USED") {
        // Torna determinística a janela observada na #363 antes de registrar o uso.
        await new Promise(resolve => setTimeout(resolve, 450));
        if (fault === "http") return send({ error: token }, 503);
        if (fault === "missing") return send(session);
        if (fault === "json") { response.end("invalid-json " + token); return; }
      }
      if (body.eventType === "RECOVERY_COMPLETED" && !session.events.includes("READY_RESULT_USED")) {
        return send({ error: "A recuperação exige retomada e uso do resultado no cenário correto." }, 500);
      }
      if (body.eventType === "AGENT_SCENARIO_COMPLETED") {
        const required = session.scenarioCode === "SAFETY" ? "SAFETY_LIMIT_BLOCKED" :
          session.scenarioCode === "RECOVERY" ? "RECOVERY_COMPLETED" : "READY_RESULT_USED";
        if (!session.events.includes(required)) return send({ error: "Evidência incompleta" }, 409);
        session.readingFinished = true;
      }
      session.events.push(body.eventType);
      calls.push({ event: body.eventType, phase: "persisted", id: session.evidenceId });
    }
    send(session);
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  const inputPath = path.join(directory, "input.json");
  const outputPath = path.join(directory, "output.json");
  await fs.writeFile(inputPath, JSON.stringify({ mode, scenarioCode: "RECOVERY",
    captureSessionId: randomUUID(), sourceReference: "product:10@agent-validation-v1", productId: 10,
    productSlug: "pde-planejado-36", sourceUrl: `http://127.0.0.1:${server.address().port}/mira-private` }));
  try {
    const child = spawn(process.execPath, [harness, inputPath, outputPath, path.join(directory, "evidence")],
      { env: { ...process.env, PDE_INTERNAL_API_TOKEN: token } });
    let log = "";
    child.stdout.on("data", chunk => { log += chunk; });
    child.stderr.on("data", chunk => { log += chunk; });
    const timer = setTimeout(() => child.kill("SIGKILL"), 60000);
    const [code] = await once(child, "close");
    clearTimeout(timer);
    let output;
    try { output = JSON.parse(await fs.readFile(outputPath, "utf8")); } catch { /* Falha não produz aprovação. */ }
    for (const artifact of output?.artifacts || []) {
      const bytes = await fs.readFile(artifact.localPath);
      assert.deepEqual([...bytes.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
    }
    assert.equal(log.includes(token), false, "O log não pode expor credenciais da resposta.");
    return { code, log, output, calls };
  } finally {
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await fs.rm(directory, { recursive: true, force: true });
  }
}

test("aguarda uso persistido antes de confirmar recuperação nos cinco cenários", async () => {
  const result = await executeHarness("TECHNICAL");
  assert.equal(result.code, 0, result.log);
  assert.equal(result.output.decision, "APPROVED");
  assert.equal(result.output.scenarios.length, 5);
  assert.equal(result.output.devices.length, 3);
  assert.equal(result.output.artifacts.length, 5);
  assert.equal(Object.values(result.output.checks).every(Boolean), true);
  const recovery = result.output.scenarios.find(item => item.scenarioCode === "RECOVERY");
  const calls = result.calls.filter(call => call.id === recovery.evidenceId);
  assert.deepEqual(calls.map(call => `${call.event}:${call.phase}`), [
    "READY_RESULT_USED:requested", "READY_RESULT_USED:persisted",
    "RECOVERY_COMPLETED:requested", "RECOVERY_COMPLETED:persisted",
    "AGENT_SCENARIO_COMPLETED:requested", "AGENT_SCENARIO_COMPLETED:persisted",
  ]);
  assert.equal(result.output.humanEvidenceClaimed, false);
  assert.equal(result.output.commercialEvidenceClaimed, false);
});

for (const [fault, diagnostic] of [["http", /HTTP 503/], ["missing", /não confirmou.*READY_RESULT_USED/], ["json", /JSON inválido/]]) {
  test(`bloqueia ${fault} no registro de uso sem disparar a recuperação`, async () => {
    const result = await executeHarness("SCENARIO", fault);
    assert.notEqual(result.code, 0);
    assert.equal(result.output, undefined);
    assert.match(result.log, diagnostic);
    assert.equal(result.calls.some(call => call.event === "RECOVERY_COMPLETED"), false);
  });
}
