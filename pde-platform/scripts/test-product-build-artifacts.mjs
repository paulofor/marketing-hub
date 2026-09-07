import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const repositoryRoot = fileURLToPath(new URL("../../", import.meta.url));

/** Lista recursivamente somente os arquivos regulares do diretório compilado. */
async function filesInside(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map(async (entry) => {
      const entryPath = path.join(directory, entry.name);
      return entry.isDirectory() ? filesInside(entryPath) : [entryPath];
    }),
  );
  return nested.flat();
}

/** Concatena os artefatos textuais para detectar vazamento de identidade entre produtos. */
async function compiledSurface(relativeDirectory) {
  const directory = path.join(repositoryRoot, relativeDirectory);
  const files = await filesInside(directory);
  const textFiles = files.filter((file) => /\.(?:css|html|js|json|svg|txt)$/i.test(file));
  const contents = await Promise.all(textFiles.map((file) => readFile(file, "utf8")));
  return {
    names: files.map((file) => path.relative(directory, file)).join("\n"),
    text: contents.join("\n"),
  };
}

test("o bundle de Vega não contém a superfície de Mira", async () => {
  const vega = await compiledSurface("pde-platform/frontend/dist");
  assert.doesNotMatch(
    `${vega.names}\n${vega.text}`,
    /mira-private|pde-planejado-36|Sua rotina, organizada com calma/i,
    "[ARQUITETURA] A imagem compilada de Vega contém artefato de Mira.",
  );
});

test("o bundle de Mira não contém a superfície de Vega", async () => {
  const mira = await compiledSurface("pde-platform/frontend/dist-mira");
  assert.match(mira.names, /mira\.html/);
  assert.match(mira.text, /Sua rotina, organizada com calma/);
  assert.doesNotMatch(
    `${mira.names}\n${mira.text}`,
    /Clube MUSA|metodo-musa-7-dias|musa-pde-entry|logo-musa|musa-product/i,
    "[ARQUITETURA] A imagem compilada de Mira contém artefato de Vega.",
  );
});
