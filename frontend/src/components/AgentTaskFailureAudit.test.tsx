import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AgentTaskFailureAudit from "./AgentTaskFailureAudit";

describe("AgentTaskFailureAudit", () => {
  it("expõe uma falha reconstruível com intenção, contexto, acesso e saída", () => {
    render(
      <AgentTaskFailureAudit
        audit={{
          readiness: "COMPLETE",
          intendedWork: "Validar a instrumentação do experimento.",
          sourceReference: "experiment:88",
          processCode: "operacao-otimizacao-experimento",
          activityName: "Confirmar instrumentação",
          authorityPolicy:
            "Somente leitura; publicação exige aprovação humana.",
          accessedEvidenceJson:
            '{"accessMode":"READ_ONLY","toolUsage":["consultar_funil"]}',
          producedOutputJson: '{"decision":"BLOCKED"}',
          error: "Evento de checkout ausente.",
          blockerGuidance: {
            category: "MISSING_EVIDENCE",
            recommendedAction:
              "Instale o evento de checkout e reinicie a tarefa.",
            helpLinks: [
              { label: "Abrir instrumentação", url: "/experiments/88" },
            ],
          },
          accessedUrls: [
            {
              label: "Checkout observado",
              url: "https://checkout.example/rigel",
              accessMethod: "BROWSER",
            },
          ],
          missingEvidence: [],
        }}
      />,
    );

    expect(screen.getByText("Reconstruível")).toBeInTheDocument();
    expect(screen.getByText(/Validar a instrumentação/)).toBeInTheDocument();
    expect(screen.getByText(/experiment:88/)).toBeInTheDocument();
    expect(screen.getByText(/Evento de checkout ausente/)).toBeInTheDocument();
    expect(
      screen.getByText(/Instale o evento de checkout/),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Abrir instrumentação" }),
    ).toHaveAttribute("target", "_blank");
    expect(
      screen.getByRole("link", { name: "Checkout observado" }),
    ).toHaveAttribute("href", "https://checkout.example/rigel");
    expect(
      screen.getByText("https://checkout.example/rigel"),
    ).toBeInTheDocument();
  });

  it("explicita o que faltou no histórico legado", () => {
    render(
      <AgentTaskFailureAudit
        audit={{
          readiness: "PARTIAL",
          intendedWork: "Avaliar orçamento.",
          missingEvidence: [
            "causa da falha ou bloqueio",
            "evidências acessadas",
          ],
        }}
      />,
    );

    expect(screen.getByText("Parcial")).toBeInTheDocument();
    expect(
      screen.getByText(/causa da falha ou bloqueio, evidências acessadas/),
    ).toBeInTheDocument();
  });
});
