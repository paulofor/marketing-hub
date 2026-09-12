import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PsiqueTaskAudit from "./PsiqueTaskAudit";

describe("prova criativa produzida", () => {
  it("mostra a imagem de Íris sem atribuir a ela uma captura ou parecer de Psique", () => {
    render(
      <PsiqueTaskAudit
        assignedAgentKey="communication-director"
        visualEvidence={[
          {
            id: 910130,
            captureSessionId: "sandbox",
            evidenceKey: "creative-1",
            evidenceType: "CREATIVE_RENDER",
            label: "Criativo estático 1",
            deviceProfile: "CREATIVE_1080X1350",
            pageNumber: 1,
            viewportWidth: 1080,
            viewportHeight: 1350,
            pageHeightPx: 1350,
            scrollY: 0,
            sourceUrl: "https://sandbox.example/source",
            finalUrl: "https://sandbox.example/source",
            contentUrl:
              "/api/agent-tasks/910403/visual-evidence/910130/content",
            sizeBytes: 30000,
            sha256: "a".repeat(64),
            capturedAt: "2026-09-12T09:00:00Z",
          },
        ]}
      />,
    );
    expect(
      screen.getByRole("heading", { name: "Imagens finais do criativo" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("img", {
        name: "Criativo produzido — Criativo estático 1",
      }),
    ).toHaveAttribute(
      "src",
      "/api/agent-tasks/910403/visual-evidence/910130/content",
    );
    expect(screen.queryByText(/execução legada/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Há snapshots/)).not.toBeInTheDocument();
  });
});
