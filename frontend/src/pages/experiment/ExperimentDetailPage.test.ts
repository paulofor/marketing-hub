import { describe, expect, it } from "vitest";
import {
  buildExperimentTestUrl,
  buildLearnedLessonsPayload,
  buildStrategicPositioningPayload,
} from "./ExperimentDetailPage";

describe("buildExperimentTestUrl", () => {
  it("adiciona o parametro de teste em URL sem query string", () => {
    expect(buildExperimentTestUrl("https://vendas.exemplo.com/oferta")).toBe(
      "https://vendas.exemplo.com/oferta?mh_test=1",
    );
  });

  it("preserva parametros existentes e define o modo de teste", () => {
    expect(
      buildExperimentTestUrl(
        "https://vendas.exemplo.com/oferta?utm_source=meta",
      ),
    ).toBe("https://vendas.exemplo.com/oferta?utm_source=meta&mh_test=1");
  });

  it("retorna nulo quando nao existe URL publicada", () => {
    expect(buildExperimentTestUrl(" ")).toBeNull();
  });
});

describe("buildLearnedLessonsPayload", () => {
  it("normaliza espacos antes de salvar licoes aprendidas", () => {
    expect(
      buildLearnedLessonsPayload(
        "  CTR alto, mas sem avanço para diagnóstico.\nPróximo teste precisa reduzir fricção.  ",
      ),
    ).toEqual({
      learnedLessons:
        "CTR alto, mas sem avanço para diagnóstico.\nPróximo teste precisa reduzir fricção.",
    });
  });

  it("envia nulo quando o usuario limpa as licoes aprendidas", () => {
    expect(buildLearnedLessonsPayload("  ")).toEqual({ learnedLessons: null });
  });
});

describe("buildStrategicPositioningPayload", () => {
  it("normaliza objetivo comercial e funcao operacional antes de salvar", () => {
    expect(
      buildStrategicPositioningPayload(
        "  Validar se vídeo humano aumenta início do diagnóstico.  ",
        "  Validação operacional do A/B de página.  ",
      ),
    ).toEqual({
      commercialObjective:
        "Validar se vídeo humano aumenta início do diagnóstico.",
      currentOperationalFunction: "Validação operacional do A/B de página.",
    });
  });

  it("envia nulo quando os campos de posicionamento sao limpos", () => {
    expect(buildStrategicPositioningPayload(" ", "\n")).toEqual({
      commercialObjective: null,
      currentOperationalFunction: null,
    });
  });
});
