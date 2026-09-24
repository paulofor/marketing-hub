import { readFile } from "node:fs/promises";

const UNSUPPORTED_STRICT_KEYWORDS = [
  "allOf",
  "oneOf",
  "not",
  "if",
  "then",
  "else",
  "dependentRequired",
  "dependentSchemas",
  "uniqueItems",
];

const ARGOS_OUTPUT_SCHEMA_RESOURCES = [
  {
    name: "planejamento inicial de Argos",
    url: new URL(
      "../prompts/productdiscovery.v1/plan/plan-schema.json",
      import.meta.url,
    ),
  },
  {
    name: "aprofundamento de lacunas de Argos",
    url: new URL(
      "../prompts/productdiscovery.v1/plan/gap-deepening-schema.json",
      import.meta.url,
    ),
  },
  {
    name: "síntese factual de Argos",
    url: new URL(
      "../prompts/productdiscovery.v1/research/response-schema.json",
      import.meta.url,
    ),
  },
];

/** Valida localmente o subconjunto estrito antes de entregar um schema ao Codex. */
export function validateStrictOutputSchema(schema, schemaName = "schema") {
  if (!schema || typeof schema !== "object" || Array.isArray(schema)) {
    throw new Error(`${schemaName}: raiz do schema deve ser um objeto`);
  }
  if (schema.type !== "object") {
    throw new Error(`${schemaName}: raiz do schema deve declarar type=object`);
  }
  visitStrictSchemaNode(schema, "$", schemaName);
  return schema;
}

/** Lê, interpreta e valida um contrato de saída estrita versionado. */
export async function readStrictOutputSchema(
  resource,
  schemaName,
  readFileFn = readFile,
) {
  const contract = await readFileFn(resource, "utf8");
  let schema;
  try {
    schema = JSON.parse(contract);
  } catch (error) {
    throw new Error(`${schemaName}: JSON inválido`, { cause: error });
  }
  validateStrictOutputSchema(schema, schemaName);
  return { contract, schema };
}

/** Confere todos os contratos do executor antes de iniciar polling ou reportar prontidão. */
export async function validateArgosOutputSchemaContracts(
  readFileFn = readFile,
) {
  for (const resource of ARGOS_OUTPUT_SCHEMA_RESOURCES) {
    await readStrictOutputSchema(resource.url, resource.name, readFileFn);
  }
}

/** Percorre objetos, arrays e alternativas aceitas pelo modo estrito. */
function visitStrictSchemaNode(node, path, schemaName) {
  if (!node || typeof node !== "object" || Array.isArray(node)) return;
  for (const keyword of UNSUPPORTED_STRICT_KEYWORDS) {
    if (Object.hasOwn(node, keyword)) {
      throw new Error(
        `${schemaName}: ${path} usa palavra-chave incompatível ${keyword}`,
      );
    }
  }

  const types = Array.isArray(node.type) ? node.type : [node.type];
  const objectNode = types.includes("object") || node.properties;
  if (objectNode) {
    if (node.additionalProperties !== false) {
      throw new Error(
        `${schemaName}: ${path} deve declarar additionalProperties=false`,
      );
    }
    const properties = node.properties || {};
    const propertyNames = Object.keys(properties);
    if (!Array.isArray(node.required)) {
      throw new Error(`${schemaName}: ${path} deve declarar required`);
    }
    const requiredNames = new Set(node.required);
    const missing = propertyNames.filter((name) => !requiredNames.has(name));
    const unknown = node.required.filter(
      (name) => !Object.hasOwn(properties, name),
    );
    if (
      missing.length > 0 ||
      unknown.length > 0 ||
      requiredNames.size !== node.required.length
    ) {
      throw new Error(
        `${schemaName}: ${path} deve exigir exatamente todas as propriedades; ausentes=${missing.join(",") || "nenhuma"}; desconhecidas=${unknown.join(",") || "nenhuma"}`,
      );
    }
    for (const [name, property] of Object.entries(properties)) {
      visitStrictSchemaNode(property, `${path}.${name}`, schemaName);
    }
  }

  if (node.items) {
    visitStrictSchemaNode(node.items, `${path}[]`, schemaName);
  }
  for (const [index, alternative] of (node.anyOf || []).entries()) {
    visitStrictSchemaNode(
      alternative,
      `${path}.anyOf[${index}]`,
      schemaName,
    );
  }
}
