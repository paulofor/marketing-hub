#!/usr/bin/env node

import { promises as fs } from "node:fs";
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

function authorizedManifestPath(value) {
  const normalized = String(value).replaceAll("\\", "/").trim();
  return /^pde-platform\/contracts\/[^/]+\.json$/.test(normalized)
    ? normalized
    : null;
}

/** Seleciona a única superfície cujo manifesto pronto foi alterado na integração. */
export async function resolveDeployTarget(
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
    candidates.push({ target, relativePath });
  }
  if (candidates.length > 1) {
    throw new Error(
      `Mais de uma superfície PDE solicitou publicação automática: ${candidates
        .map(({ relativePath }) => relativePath)
        .join(", ")}`,
    );
  }
  return candidates[0]?.target ?? "none";
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
  const target = await resolveDeployTarget(repositoryRoot, changedPaths);
  await fs.appendFile(output, `frontend-version=${target}\n`);
}
