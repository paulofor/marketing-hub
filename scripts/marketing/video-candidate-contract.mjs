/** Confere fontes, tempos e identidade textual antes de compor uma candidata audiovisual. */
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";

export const sha256 = (bytes) =>
  createHash("sha256").update(bytes).digest("hex");

export const words = (text) =>
  String(text)
    .normalize("NFKC")
    .toLocaleLowerCase("pt-BR")
    .match(/[\p{L}\p{N}]+/gu) ?? [];

/** Recusa legenda divergente, perda de palavras, sobreposição ou voz cortada. */
export function validateNarrationTimeline(scenes, durationSeconds) {
  if (
    !scenes.length ||
    !Number.isFinite(durationSeconds) ||
    durationSeconds <= 0
  ) {
    throw new Error("Timeline vazia ou duração final inválida.");
  }
  let end = 0;
  for (const scene of scenes) {
    if (
      !scene.speech?.trim() ||
      !Number.isFinite(scene.voiceDuration) ||
      scene.voiceDuration <= 0 ||
      scene.start !== end ||
      scene.end <= scene.start ||
      scene.voiceDuration > scene.end - scene.start
    ) {
      throw new Error(
        `Narração ou tempo inválido em ${scene.id}; não acelerar nem cortar a voz.`,
      );
    }
    const cues = scene.cues ?? [];
    if (
      JSON.stringify(words(cues.map((cue) => cue.text).join(" "))) !==
      JSON.stringify(words(scene.speech))
    ) {
      throw new Error(`Legenda e locução divergem em ${scene.id}.`);
    }
    let previousEnd = scene.start;
    for (const cue of cues) {
      if (
        !Number.isFinite(cue.start) ||
        !Number.isFinite(cue.end) ||
        cue.start < previousEnd ||
        cue.end <= cue.start ||
        cue.end > scene.end ||
        cue.text.split("\n").length > 2
      ) {
        throw new Error(
          `Legenda sobreposta, cortada ou ilegível em ${scene.id}.`,
        );
      }
      previousEnd = cue.end;
    }
    end = scene.end;
  }
  if (Math.abs(end - durationSeconds) > 0.001)
    throw new Error("Duração final divergente.");
}

/** Comprova os bytes das fontes e da versão antes de qualquer síntese ou render. */
export async function validateSources(sourceDir, manifest, expectedVersion) {
  if (
    manifest.experienceVersion !== expectedVersion ||
    manifest.qaSegregated !== true ||
    !Array.isArray(manifest.media) ||
    !manifest.captures?.length
  ) {
    throw new Error("Capturas sem versão compatível ou isolamento de QA.");
  }
  for (const item of [...manifest.captures, ...manifest.media]) {
    if (
      !/^[a-zA-Z0-9._-]+$/.test(item.file) ||
      !/^[a-f0-9]{64}$/.test(item.sha256) ||
      sha256(await readFile(`${sourceDir}/${item.file}`)) !== item.sha256
    ) {
      throw new Error(`Fonte divergente: ${item.file}; não produzir.`);
    }
  }
}
