import { readFile } from "node:fs/promises";
import { validateFunctionalResult } from "./contract.mjs";

const role = String(process.env.AGENT_ROLE || "").trim().toLowerCase();
if (!new Set(["argos", "dedalo", "psique"]).has(role)) {
  throw new Error("AGENT_ROLE deve ser argos, dedalo ou psique.");
}
if (!process.env.OPENAI_API_KEY) {
  throw new Error("OPENAI_API_KEY não configurada.");
}

const input = await readStandardInput();
const promptTemplate = await readFile(`/app/prompts/${role}.md`, "utf8");
const schema = JSON.parse(await readFile(`/app/schemas/${role}.json`, "utf8"));
const model = process.env.OPENAI_MODEL || (role === "argos" ? "gpt-5.4-mini" : "gpt-5.4");
const effort = process.env.REASONING_EFFORT || (role === "argos" ? "medium" : "high");
const request = {
  model,
  service_tier: "flex",
  reasoning: { effort },
  input: promptTemplate.replace("{{INPUT_JSON}}", JSON.stringify(input)),
  text: {
    format: {
      type: "json_schema",
      name: `pde_opportunity_${role}`,
      strict: true,
      schema,
    },
  },
};

const response = await executeFlex(request);
const outputText = response.output
  ?.flatMap((item) => item.content || [])
  .find((item) => item.type === "output_text")?.text;
if (!outputText) throw new Error(`Resposta de ${role} sem output_text.`);
const result = JSON.parse(outputText);
validateFunctionalResult(role, input, result);

process.stdout.write(
  JSON.stringify({
    agent: role,
    model: response.model,
    serviceTier: response.service_tier,
    usage: response.usage,
    result,
  }),
);

/** Lê o contexto integral sem misturar payload funcional com logs. */
async function readStandardInput() {
  let value = "";
  for await (const chunk of process.stdin) value += chunk;
  if (!value.trim()) throw new Error("Contexto da atividade ausente.");
  return JSON.parse(value);
}

/** Usa Flex por padrão e repete somente indisponibilidade transitória do provedor. */
async function executeFlex(body) {
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    const response = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
    const payload = await response.json();
    if (response.ok) return payload;
    if (response.status !== 429 || attempt === 4) {
      throw new Error(`OpenAI HTTP ${response.status}: ${payload.error?.type || "erro"}`);
    }
    await new Promise((resolve) => setTimeout(resolve, attempt * 3000));
  }
  throw new Error("Flex indisponível após quatro tentativas.");
}
