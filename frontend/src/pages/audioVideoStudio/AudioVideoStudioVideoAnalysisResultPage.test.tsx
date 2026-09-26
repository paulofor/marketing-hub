import { describe, expect, it } from "vitest";
import {
  getAudioAnalysisLabel,
  getCapabilityVerdictLabel,
  getReferenceSummary,
} from "./AudioVideoStudioVideoAnalysisResultPage";
import type {
  VideoReference,
  VideoReferenceAnalysisExecution,
} from "../../api/salesVideo/types";

describe("resultado da analise audiovisual", () => {
  it("traduz capacidade e evidência auditiva sem esconder limites", () => {
    expect(getCapabilityVerdictLabel("READY_WITH_LIMITS")).toBe(
      "Pronto com limites explícitos",
    );
    expect(
      getAudioAnalysisLabel({
        hasAudio: true,
        audioTranscription: { status: "NO_SPEECH_DETECTED" },
      }),
    ).toBe("áudio sem fala identificada");
    expect(
      getAudioAnalysisLabel({
        hasAudio: false,
        audioTranscription: { status: "NOT_APPLICABLE" },
      }),
    ).toBe("sem faixa de áudio");
  });

  it("prioriza o diagnóstico automático concluído no resumo", () => {
    const reference = {
      id: 1,
      title: "Referência",
      sourceUrl: "https://example.com/reference.mp4",
      primaryLearningGoal: "Aprender o mecanismo comercial.",
      status: "ANALYZED",
    } as VideoReference;
    const automaticAnalysis = {
      status: "COMPLETED",
      output: {
        commercialDiagnosis: "Diagnóstico comercial estruturado e auditável.",
      },
    } as VideoReferenceAnalysisExecution;

    expect(getReferenceSummary(reference, automaticAnalysis)).toBe(
      "Diagnóstico comercial estruturado e auditável.",
    );
  });
});
