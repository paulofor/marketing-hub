import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AgentForm from "./AgentForm";

describe("contrato operacional do agente", () => {
  it("envia regras de coordenação, análise e oferta no cadastro", () => {
    const submit = vi.fn();
    render(
      <AgentForm
        initialValue={{
          name: "Especialista comercial",
          nickname: "Closer",
          agentKey: "commercial-specialist",
          status: "DRAFT",
          executionMode: "DECISION_GATE",
          themeId: 1,
          inputs: [],
          outputs: [],
          internalFunctions: [],
        }}
        themes={[{ id: 1, name: "Comercial" }]}
        onSubmit={submit}
      />,
    );

    fireEvent.change(screen.getByLabelText(/responsabilidade do agente/i), {
      target: { value: "Avaliar viabilidade." },
    });
    fireEvent.change(screen.getByLabelText(/regras para o orquestrador/i), {
      target: { value: "Bloquear sem evidências." },
    });
    fireEvent.change(screen.getByLabelText(/o que deve analisar/i), {
      target: { value: "Conversão e risco." },
    });
    fireEvent.change(screen.getByLabelText(/o que deve oferecer/i), {
      target: { value: "Parecer e próximo teste." },
    });
    fireEvent.change(screen.getByLabelText(/^apelido/i), {
      target: { value: "Conselheiro" },
    });
    expect(screen.getByLabelText(/figura mitológica/i)).toHaveAttribute(
      "accept",
      "image/png,image/jpeg,image/webp",
    );
    fireEvent.click(screen.getByRole("button", { name: /^salvar$/i }));

    expect(submit).toHaveBeenCalledWith(
      expect.objectContaining({
        nickname: "Conselheiro",
        responsibilityContract: "Avaliar viabilidade.",
        orchestratorPolicy: "Bloquear sem evidências.",
        analysisPolicy: "Conversão e risco.",
        offeringPolicy: "Parecer e próximo teste.",
      }),
    );
  });
});
