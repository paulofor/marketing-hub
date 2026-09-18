#!/usr/bin/env node

import { promises as fs } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { sourceFingerprint } from "../pde-platform/frontend/scripts/source-fingerprint.mjs";

const TARGETS = new Set([
  "v5",
  "v6",
  "v7",
  "v8",
  "mira",
  "kit-whatsapp",
]);
const SHA256 = /^[a-f0-9]{64}$/;
const NON_MUSA_MANUAL_TARGETS = new Set(["mira", "kit-whatsapp"]);
const SHARED_COMPONENTS = new Set([
  "none",
  "backend",
  "ai-worker",
  "retention-worker",
]);

function manifestRevision(contract) {
  for (const field of ["contractVersion", "evidenceVersion"]) {
    const match = /(?:^|[.-])v([1-9][0-9]*)$/.exec(contract[field] ?? "");
    if (match) return Number(match[1]);
  }
  return 1;
}

function authorizedManifestPath(value) {
  const normalized = String(value).replaceAll("\\", "/").trim();
  return /^pde-platform\/contracts\/[^/]+\.json$/.test(normalized)
    ? normalized
    : null;
}

async function fileSha256(file) {
  return createHash("sha256").update(await fs.readFile(file)).digest("hex");
}

/** Seleciona a única superfície cujo manifesto pronto foi alterado na integração. */
export async function resolveDeploySelection(
  repositoryRoot,
  changedPaths,
  fingerprintResolver = sourceFingerprint,
) {
  const candidates = [];
  for (const changedPath of changedPaths) {
    const relativePath = authorizedManifestPath(changedPath);
    if (!relativePath) continue;
    let contract;
    try {
      contract = JSON.parse(
        await fs.readFile(path.join(repositoryRoot, relativePath), "utf8"),
      );
    } catch (error) {
      if (error.code === "ENOENT") continue;
      throw error;
    }
    const publication = contract.publicationContract;
    if (
      contract.status !== "READY_FOR_INDEPENDENT_REVIEW" ||
      publication?.automaticDeployOnMerge !== true
    ) {
      continue;
    }
    const target = publication.frontendVersion;
    const expectedHash = publication.requiredFrontendSourceSha256;
    const runtime = contract.liveVisualContract?.runtimeIdentity;
    if (!TARGETS.has(target)) {
      throw new Error(`Alvo de publicação PDE inválido em ${relativePath}`);
    }
    if (!/^https:\/\//.test(publication.publicUrl ?? "")) {
      throw new Error(`URL pública PDE inválida em ${relativePath}`);
    }
    if (
      !SHA256.test(expectedHash ?? "") ||
      runtime?.frontendSourceSha256 !== expectedHash
    ) {
      throw new Error(
        `Fingerprint público divergente no manifesto ${relativePath}`,
      );
    }
    const actualHash = await fingerprintResolver(
      path.join(repositoryRoot, "pde-platform/frontend"),
    );
    if (actualHash !== expectedHash) {
      throw new Error(
        `Fingerprint declarado não corresponde à fonte frontend em ${relativePath}`,
      );
    }
    candidates.push({
      target,
      relativePath,
      sourceSha256: expectedHash,
      contractSha256: await fileSha256(path.join(repositoryRoot, relativePath)),
    });
  }
  if (candidates.length > 1) {
    throw new Error(
      `Mais de uma superfície PDE solicitou publicação automática: ${candidates
        .map(({ relativePath }) => relativePath)
        .join(", ")}`,
    );
  }
  return candidates[0] ?? {
    target: "none",
    relativePath: "",
    sourceSha256: "",
    contractSha256: "",
  };
}

/** Mantém a API histórica para consumidores que precisam somente do target. */
export async function resolveDeployTarget(
  repositoryRoot,
  changedPaths,
  fingerprintResolver = sourceFingerprint,
) {
  return (await resolveDeploySelection(repositoryRoot, changedPaths, fingerprintResolver))
    .target;
}

function resolveSharedComponents(changedPaths, manualSharedComponent) {
  const manual = String(manualSharedComponent ?? "").trim();
  if (manual) {
    if (!SHARED_COMPONENTS.has(manual)) {
      throw new Error(`Componente compartilhado PDE inválido: ${manual}`);
    }
    return {
      backend: manual === "backend",
      aiWorker: manual === "ai-worker",
      retentionWorker: manual === "retention-worker",
    };
  }

  return {
    backend: changedPaths.some((file) => file.startsWith("pde-platform/backend/")),
    aiWorker: changedPaths.some((file) => file.startsWith("pde-platform/pde-ai-worker/")),
    retentionWorker: changedPaths.some((file) =>
      file.startsWith("pde-platform/pde-retention-worker/"),
    ),
  };
}

async function selectManualFrontend(repositoryRoot, target, fingerprintResolver) {
  if (!target || target === "none") {
    return {
      target: "none",
      relativePath: "",
      sourceSha256: "",
      contractSha256: "",
    };
  }
  if (!TARGETS.has(target)) {
    throw new Error(`Alvo de publicação PDE inválido: ${target}`);
  }

  const contractsDirectory = path.join(repositoryRoot, "pde-platform/contracts");
  const documents = {};
  for (const name of await fs.readdir(contractsDirectory)) {
    if (!name.endsWith(".json")) continue;
    const relativePath = `pde-platform/contracts/${name}`;
    documents[relativePath] = JSON.parse(
      await fs.readFile(path.join(repositoryRoot, relativePath), "utf8"),
    );
  }
  const matching = Object.entries(documents)
    .filter(
      ([, contract]) =>
        contract.status === "READY_FOR_INDEPENDENT_REVIEW" &&
        contract.publicationContract?.automaticDeployOnMerge === true &&
        contract.publicationContract?.frontendVersion === target,
    )
    .map(([relativePath, contract]) => ({
      relativePath,
      revision: manifestRevision(contract),
    }));
  if (matching.length === 0) {
    if (NON_MUSA_MANUAL_TARGETS.has(target)) {
      const relativePath =
        "pde-platform/contracts/product-runtime-isolation-v1.json";
      const inventory = documents[relativePath];
      const matches = (inventory?.products ?? []).flatMap((product) =>
        (product.surfaces ?? [])
          .filter((surface) => surface.deployTarget === target)
          .map((surface) => ({ product, surface })),
      );
      if (matches.length !== 1 || matches[0].surface.lifecycleStatus !== "SUPPORTED") {
        throw new Error(
          `A superfície ${target} não possui identidade suportada no inventário.`,
        );
      }
      return {
        target,
        relativePath,
        sourceSha256: await fingerprintResolver(
          path.join(repositoryRoot, "pde-platform/frontend"),
        ),
        contractSha256: await fileSha256(
          path.join(repositoryRoot, relativePath),
        ),
      };
    }
    throw new Error(
      `A superfície ${target} não possui manifesto imutável apto para publicação.`,
    );
  }

  const latestRevision = Math.max(...matching.map(({ revision }) => revision));
  const latest = matching.filter(({ revision }) => revision === latestRevision);
  if (latest.length !== 1) {
    throw new Error(
      `A superfície ${target} possui mais de um manifesto vigente na revisão v${latestRevision}.`,
    );
  }

  return resolveDeploySelection(
    repositoryRoot,
    [latest[0].relativePath],
    fingerprintResolver,
  );
}

/** Resolve frontend e componentes compartilhados como publicações independentes. */
export async function resolveDeploymentPlan(
  repositoryRoot,
  changedPaths,
  {
    manualFrontend = "",
    manualSharedComponent = "",
    fingerprintResolver = sourceFingerprint,
  } = {},
) {
  const frontend = manualFrontend
    ? await selectManualFrontend(repositoryRoot, manualFrontend, fingerprintResolver)
    : await resolveDeploySelection(repositoryRoot, changedPaths, fingerprintResolver);
  const shared = resolveSharedComponents(changedPaths, manualSharedComponent);
  return {
    frontend,
    ...shared,
    fullCompatibility: shared.backend,
    hasDeployment:
      frontend.target !== "none" ||
      shared.backend ||
      shared.aiWorker ||
      shared.retentionWorker,
  };
}

function argument(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const repositoryRoot = path.resolve(argument("--repository-root") ?? ".");
  const changedPathsFile = argument("--changed-paths-file");
  const output = argument("--output");
  if (!changedPathsFile || !output) {
    throw new Error("Informe --changed-paths-file e --output.");
  }
  const changedPaths = (await fs.readFile(changedPathsFile, "utf8"))
    .split(/\r?\n/)
    .filter(Boolean);
  const plan = await resolveDeploymentPlan(repositoryRoot, changedPaths, {
    manualFrontend: argument("--manual-frontend") ?? "",
    manualSharedComponent: argument("--manual-shared-component") ?? "",
  });
  const lines = [
    `frontend-version=${plan.frontend.target}`,
    `frontend-contract-path=${plan.frontend.relativePath}`,
    `frontend-contract-sha256=${plan.frontend.contractSha256}`,
    `frontend-source-sha256=${plan.frontend.sourceSha256}`,
    `deploy-backend=${plan.backend}`,
    `deploy-ai-worker=${plan.aiWorker}`,
    `deploy-retention-worker=${plan.retentionWorker}`,
    `full-compatibility=${plan.fullCompatibility}`,
    `has-deployment=${plan.hasDeployment}`,
  ];
  await fs.appendFile(output, `${lines.join("\n")}\n`);
}
