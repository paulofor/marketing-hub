import { describe, expect, it } from "vitest";
import { normalizeFramework } from "./types";

describe("normalizeFramework", () => {
  it("extrai campos legíveis quando o framework vem com JSON serializado", () => {
    const painPayload = JSON.stringify({
      surface: "Agenda cheia que some quando a cliente falta.",
      root: "Falta uma regra simples de confirmação.",
      emotional: "A profissional fica ansiosa.",
      social: "Parece desorganizada para a cliente.",
      cost: "Perde horário e deslocamento.",
      summary: "Dor prioritária da agenda instável.",
      evidenceSignals: ["Taxa de agendamento reduz faltas."],
    });
    const resultPayload = JSON.stringify({
      desiredOutcome: "Agenda mais previsível.",
      measurableChange: "Mais horários confirmados antes do deslocamento.",
      beforeAfterContrast: "Antes improvisa; depois confirma com clareza.",
      businessValue: "Protege tempo vendido.",
      summary: "Resultado desejável da agenda firme.",
    });

    const framework = normalizeFramework({
      version: "v1",
      pain: {
        surface: painPayload,
        root: painPayload,
        summary: painPayload,
      },
      result: {
        desiredResult: resultPayload,
        summary: resultPayload,
      },
      mechanism: {},
      proof: {},
      offer: {},
      checklist: {},
    });

    expect(framework.pain.surface).toBe(
      "Agenda cheia que some quando a cliente falta.",
    );
    expect(framework.pain.root).toBe("Falta uma regra simples de confirmação.");
    expect(framework.pain.evidenceSignals).toEqual([
      "Taxa de agendamento reduz faltas.",
    ]);
    expect(framework.result.desiredResult).toBe("Agenda mais previsível.");
    expect(framework.result.successSignal).toBe(
      "Mais horários confirmados antes do deslocamento.",
    );
  });
});
