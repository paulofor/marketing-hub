import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  sha256,
  validateNarrationTimeline,
  validateSources,
} from "./video-candidate-contract.mjs";

const scene = () => ({
  id: "primeiro-uso",
  start: 0,
  end: 5,
  speech: "Veja seu ajuste. Salve para retomar.",
  voiceDuration: 4,
  cues: [
    { start: 0, end: 2, text: "Veja seu ajuste." },
    { start: 2, end: 5, text: "Salve para retomar." },
  ],
});

test("aceita palavras idênticas com quebras visuais e pausas medidas", () => {
  const value = scene();
  value.cues[1].text = "Salve para\nretomar.";
  validateNarrationTimeline([value], 5);
});
test("recusa legenda resumida em vez da fala aprovada", () => {
  const value = scene();
  value.cues[1].text = "Salve o resultado.";
  assert.throws(() => validateNarrationTimeline([value], 5), /divergem/);
});
test("bloqueia narração longa sem cortar ou acelerar", () => {
  const value = scene();
  value.voiceDuration = 5.01;
  assert.throws(() => validateNarrationTimeline([value], 5), /não acelerar/);
});
test("bloqueia cue que invade a próxima cena", () => {
  const value = scene();
  value.cues[1].end = 5.1;
  assert.throws(() => validateNarrationTimeline([value], 5), /sobreposta/);
});
test("recusa tempos desconhecidos e timeline vazia", () => {
  const value = scene();
  value.cues[1].end = Number.NaN;
  assert.throws(() => validateNarrationTimeline([value], 5));
  assert.throws(() => validateNarrationTimeline([], 0));
});
test("não aceita lacunas nem duração final inventada", () => {
  const value = scene();
  value.start = 1;
  assert.throws(() => validateNarrationTimeline([value], 5));
  assert.throws(() => validateNarrationTimeline([scene()], 6), /Duração final/);
});
test("fonte adulterada, outra versão ou captura sem QA bloqueiam antes da locução", async () => {
  const dir = await mkdtemp(join(tmpdir(), "video-source-"));
  try {
    const content = Buffer.from("pixels homologados");
    await writeFile(join(dir, "step.png"), content);
    const manifest = {
      experienceVersion: "outra-v1",
      qaSegregated: true,
      captures: [{ file: "step.png", sha256: sha256(content) }],
      media: [],
    };
    await validateSources(dir, manifest, "outra-v1");
    await assert.rejects(validateSources(dir, manifest, "outra-v2"), /versão/);
    await assert.rejects(
      validateSources(dir, { ...manifest, qaSegregated: false }, "outra-v1"),
      /QA/,
    );
    await writeFile(join(dir, "step.png"), "outros pixels");
    await assert.rejects(
      validateSources(dir, manifest, "outra-v1"),
      /Fonte divergente/,
    );
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});
