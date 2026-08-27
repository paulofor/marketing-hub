import { render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import AgentDetailPage from "./AgentDetailPage";

vi.mock("../../api/agent/useAgentDetail", () => ({
  useAgentDetail: () => ({
    isLoading: false,
    isError: false,
    data: {
      id: 5,
      name: "Radar de mercado",
      nickname: "Argos",
      agentKey: "market-radar",
      status: "ACTIVE",
      currentVersion: 4,
      themeId: 3,
      themeName: "Pesquisa de mercado",
      ownerName: "Marketing Hub",
      description: "Descobre sinais comerciais reais.",
      businessObjective: "Priorizar oportunidades com demanda.",
      successMetrics: "Oportunidades comprovadas.",
      modelName: "gpt-5.6-sol",
      executionMode: "BATCH",
      automaticExecutionEnabled: true,
      automaticExecutionChangedAt: "2026-08-27T10:00:00Z",
      automaticExecutionChangedBy: "operador",
      triggerPolicy: "Executar no ciclo PDE.",
      responsibilityContract: "Comprovar dor e demanda.",
      orchestratorPolicy: "Bloquear sem evidência.",
      analysisPolicy: "Comparar comportamento pago.",
      offeringPolicy: "Entregar relatório rastreável.",
      authorityPolicy: "Pesquisa somente leitura.",
      promptContractPath: "prompts/argos/v1/research.md",
      schemaContractPath: "prompts/argos/v1/research-schema.json",
      inputs: [
        {
          id: 1,
          name: "Briefing comercial",
          type: "JSON",
          description: "Contexto da oportunidade.",
          orderIndex: 0,
        },
      ],
      outputs: [
        {
          id: 2,
          name: "Relatório de demanda",
          type: "JSON",
          description: "Evidências auditáveis.",
          orderIndex: 0,
        },
      ],
      internalFunctions: [
        {
          id: 3,
          name: "Pesquisa web",
          type: "TOOL",
          description: "Consulta fontes públicas.",
          orderIndex: 0,
        },
      ],
      executionResources: [
        {
          id: 8,
          resourceCode: "argos-market-radar",
          name: "Radar comercial de Argos",
          description: "Coleta sinais de mercado.",
          resourceType: "CONTAINER",
          executorReference: "market-radar-worker",
          usageInstructions: "Consumir o endpoint pending do backend.",
        },
      ],
      createdAt: "2026-08-01T10:00:00Z",
      updatedAt: "2026-08-27T10:00:00Z",
      lastContractChangeAt: "2026-08-27T11:00:00Z",
    },
  }),
}));

describe("AgentDetailPage", () => {
  it("mostra todos os grupos específicos do agente e os caminhos canônicos", () => {
    render(
      <MemoryRouter initialEntries={["/agents/5/details"]}>
        <Routes>
          <Route path="/agents/:id/details" element={<AgentDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: "Detalhe do agente" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Argos" })).toBeInTheDocument();
    expect(screen.getByText("gpt-5.6-sol")).toBeInTheDocument();
    expect(
      screen.getByText("prompts/argos/v1/research.md"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("prompts/argos/v1/research-schema.json"),
    ).toBeInTheDocument();

    const resources = screen
      .getByRole("heading", { name: "Recursos executáveis" })
      .closest("section");
    expect(resources).not.toBeNull();
    expect(
      within(resources!).getByText("Radar comercial de Argos"),
    ).toBeInTheDocument();
    expect(
      within(resources!).getByText("market-radar-worker"),
    ).toBeInTheDocument();
    expect(screen.getByText("Briefing comercial")).toBeInTheDocument();
    expect(screen.getByText("Relatório de demanda")).toBeInTheDocument();
    expect(screen.getByText("Pesquisa web")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Abrir mesa" })).toHaveAttribute(
      "href",
      "/agents/5",
    );
  });
});
