import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixture = new URL("./fixtures/mira-manufacturer-20261105.json", import.meta.url);
const maximumAgeMs = 30 * 86400000;

// Valida snapshots sem renovar datas; o relógio é explícito somente nos testes desta função pura.
export function validateManufacturerSources(sources, now = Date.now()) {
  if (!Array.isArray(sources) || sources.length === 0 || !Number.isFinite(now)) {
    throw new Error("Fixture documental vazia ou relógio inválido.");
  }
  for (const source of sources) {
    const observed = Date.parse(source.observedAt);
    if (!Number.isFinite(observed) || observed > now || now - observed > maximumAgeMs) {
      const expiry = Number.isFinite(observed) ? new Date(observed + maximumAgeMs).toISOString() : "desconhecida";
      throw new Error(`Fixture documental sem validade: ${source.url}; limite ${expiry}. Reconsulte a fonte oficial e versione a evidência; repetir o teste não renova a consulta.`);
    }
    if (typeof source.labelDirections !== "string" ||
        createHash("sha256").update(source.labelDirections).digest("hex") !== source.sourceExcerptSha256) {
      throw new Error(`Fixture documental alterada: ${source.url}. Confira o trecho na fonte antes de homologar.`);
    }
  }
  return sources;
}

// Usa o instante real na execução; nenhuma variável de ambiente pode desativar o gate de frescor.
export async function loadManufacturerSources() {
  return validateManufacturerSources(JSON.parse(await readFile(fixture, "utf8")));
}

// Permite verificar o gate antes de compilar aplicações ou iniciar containers de homologação.
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const sources = await loadManufacturerSources();
  console.log(`Fixture documental válida: ${sources.length} fontes, ${fileURLToPath(fixture)}.`);
}
