#!/usr/bin/env node

import { createHash } from "node:crypto";
import { promises as fs } from "node:fs";
import path from "node:path";

const EVIDENCE_COLLECTIONS = [
  "homologationEvidence",
  "implementationEvidence",
  "executableEvidence",
];

const FIXED_REVIEW_PATHS = [
  "pde-platform/contracts/kit-whatsapp-pronto-v1.json",
  "pde-platform/contracts/kit-whatsapp-pronto-commercial-v2.json",
  "pde-platform/frontend/src/AssistedServiceApp.tsx",
  "pde-platform/frontend/src/assistedServiceTastingContracts.ts",
  "pde-platform/frontend/tests/assisted-service-local.spec.ts",
  "pde-platform/frontend/tests/assisted-service-public-analytics.spec.ts",
  "pde-platform/frontend/playwright.assisted-service-public-analytics.config.ts",
  "pde-platform/frontend/tests/public-health.spec.ts",
  "pde-platform/backend/src/main/java/com/marketinghub/pde/service/AccessService.java",
  "pde-platform/backend/src/test/java/com/marketinghub/pde/service/AccessServiceTest.java",
  "docs/homologacao/pde-kit-whatsapp-construcao-v1.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/01-comece-aqui.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/02-roteiro-de-briefing.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/03-biblioteca-de-respostas.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/04-qualificacao-e-followups.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/05-regras-de-escalonamento.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/06-modelo-microentrega-12h.md",
  "pde-platform/frontend/public/materials/kit-whatsapp-v1/07-guia-e-atualizacao.md",
];

function sha256(content) {
  return createHash("sha256").update(content).digest("hex");
}

function authorizedRelativePath(value) {
  if (typeof value !== "string" || value.trim() === "" || path.isAbsolute(value)) {
    throw new Error(`Caminho de evidência inválido: ${String(value)}`);
  }
  const normalized = path.posix.normalize(value.replaceAll("\\", "/"));
  if (normalized === ".." || normalized.startsWith("../")) {
    throw new Error(`Caminho de evidência fora do repositório: ${value}`);
  }
  return normalized;
}

async function regularFile(sourceRoot, relativePath) {
  const source = path.resolve(sourceRoot, relativePath);
  const relativeToRoot = path.relative(sourceRoot, source);
  if (relativeToRoot.startsWith("..") || path.isAbsolute(relativeToRoot)) {
    throw new Error(`Evidência fora da raiz autorizada: ${relativePath}`);
  }
  const stats = await fs.lstat(source);
  if (!stats.isFile() || stats.isSymbolicLink()) {
    throw new Error(`Evidência ausente ou não regular: ${relativePath}`);
  }
  const realSourceRoot = await fs.realpath(sourceRoot);
  const realSource = await fs.realpath(source);
  const realRelativeToRoot = path.relative(realSourceRoot, realSource);
  if (realRelativeToRoot.startsWith("..") || path.isAbsolute(realRelativeToRoot)) {
    throw new Error(`Evidência resolve para fora da raiz autorizada: ${relativePath}`);
  }
  return source;
}

async function buildBundle(sourceArgument, destinationArgument) {
  const sourceRoot = path.resolve(sourceArgument);
  const destinationRoot = path.resolve(destinationArgument);
  const destinationWithinSource = path.relative(sourceRoot, destinationRoot);
  if (
    destinationWithinSource === "" ||
    destinationWithinSource.startsWith("..") ||
    path.isAbsolute(destinationWithinSource)
  ) {
    throw new Error("A saída do pacote deve ser um subdiretório explícito do repositório.");
  }

  await fs.rm(destinationRoot, { recursive: true, force: true });
  const contractsDirectory = path.join(sourceRoot, "pde-platform/contracts");
  const contractNames = (await fs.readdir(contractsDirectory))
    .filter((name) => name.endsWith(".json"))
    .sort();

  const paths = new Set(FIXED_REVIEW_PATHS.map(authorizedRelativePath));
  const manifestPaths = [];
  for (const contractName of contractNames) {
    const relativePath = authorizedRelativePath(`pde-platform/contracts/${contractName}`);
    paths.add(relativePath);
    const contract = JSON.parse(await fs.readFile(path.join(sourceRoot, relativePath), "utf8"));
    let declaresEvidence = false;
    for (const collectionName of EVIDENCE_COLLECTIONS) {
      const collection = contract[collectionName];
      if (collection === undefined) continue;
      if (!Array.isArray(collection)) {
        throw new Error(`Coleção ${collectionName} inválida em ${relativePath}`);
      }
      declaresEvidence ||= collection.length > 0;
      for (const evidence of collection) {
        if (!/^[a-f0-9]{64}$/i.test(evidence?.sha256 ?? "")) {
          throw new Error(`SHA-256 de evidência inválido em ${relativePath}`);
        }
        paths.add(authorizedRelativePath(evidence?.path));
      }
    }
    if (declaresEvidence) {
      const productSlug = contract.product?.slug ?? contract.productSlug;
      const hasIdentity =
        Number.isInteger(contract.product?.id) || Number.isInteger(contract.experimentId);
      if (typeof productSlug !== "string" || productSlug.trim() === "" || !hasIdentity) {
        throw new Error(`Manifesto comercial sem identidade de produto: ${relativePath}`);
      }
      manifestPaths.push(relativePath);
    }
  }

  const files = [];
  for (const relativePath of [...paths].sort()) {
    const source = await regularFile(sourceRoot, relativePath);
    const content = await fs.readFile(source);
    const destination = path.join(destinationRoot, relativePath);
    await fs.mkdir(path.dirname(destination), { recursive: true });
    await fs.writeFile(destination, content);
    files.push({ path: relativePath, sha256: sha256(content), sizeBytes: content.length });
  }

  const index = {
    bundleVersion: "pde-commercial-review-evidence-v1",
    manifestPaths,
    files,
  };
  await fs.mkdir(destinationRoot, { recursive: true });
  await fs.writeFile(
    path.join(destinationRoot, "commercial-review-bundle-index-v1.json"),
    `${JSON.stringify(index, null, 2)}\n`,
  );
  return index;
}

const [sourceArgument, destinationArgument] = process.argv.slice(2);
if (!sourceArgument || !destinationArgument) {
  throw new Error(
    "Uso: node scripts/build-commercial-review-evidence.mjs <repositorio> <diretorio-saida>",
  );
}

const index = await buildBundle(sourceArgument, destinationArgument);
process.stdout.write(
  `Pacote comercial ${index.bundleVersion}: ${index.files.length} arquivos, ${index.manifestPaths.length} manifestos.\n`,
);
