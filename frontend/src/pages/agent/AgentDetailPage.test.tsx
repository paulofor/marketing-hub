import { fireEvent, render, screen, within } from "@testing-library/react";
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
      harness: {
        status: "COMPLETE",
        contractVersion: "agent-harness-v2",
        sourceReference:
          "docs/canonical/premium-ai-agent-architecture-canon.v1.md",
        sensitiveValuesPolicy:
          "Nenhum secret, token ou conteúdo privado de raciocínio é exibido.",
        sections: [
          {
            code: "runtime",
            title: "Runtime do modelo",
            description: "Configuração efetiva usada por Argos.",
            items: [
              {
                key: "reasoning",
                label: "Esforço de raciocínio",
                value: "Herda a sessão Codex; o módulo não sobrescreve",
                description: "A tela não inventa um valor.",
                sourceReference: "product-discovery-worker/src/argos-codex.js",
              },
            ],
          },
        ],
        artifacts: [
          {
            artifactType: "PROMPT",
            name: "Sistema do planejador",
            version: "productdiscovery.v1",
            path: "product-discovery-worker/prompts/productdiscovery.v1/plan/system.md",
            description: "Responsabilidade e limites do planejador.",
          },
          {
            artifactType: "OUTPUT_SCHEMA",
            name: "Schema do plano",
            version: "productdiscovery.v1",
            path: "product-discovery-worker/prompts/productdiscovery.v1/plan/plan-schema.json",
            description: "Contrato estruturado do plano.",
          },
        ],
        behaviorFiles: [
          {
            behaviorType: "PROMPT",
            name: "Sistema do planejador",
            version: "productdiscovery.v1",
            path: "product-discovery-worker/prompts/productdiscovery.v1/plan/system.md",
            description: "Responsabilidade e limites do planejador.",
            mediaType: "text/markdown",
            sha256:
              "8af0a4aa5da78de1a0ec1467e72bc90bf9854292390fcbc89330eb6a22f53b91",
            content: "Você é Argos. Comprove dor e demanda com evidências.",
          },
          {
            behaviorType: "OUTPUT_SCHEMA",
            name: "Schema do plano",
            version: "productdiscovery.v1",
            path: "product-discovery-worker/prompts/productdiscovery.v1/plan/plan-schema.json",
            description: "Contrato estruturado do plano.",
            mediaType: "application/json",
            sha256:
              "284bb8f7db3cf9423ef252ce6f60a5efb7d6f0312a7bb6edc68610d5513e2326",
            content: '{"type":"object","required":["queries"]}',
          },
        ],
      },
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
      screen.getByRole("heading", { name: "Detalhe do agente — Argos" }),
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
    expect(
      screen.getByRole("heading", { name: "Harness completo do agente" }),
    ).toBeInTheDocument();
    expect(screen.getByText("agent-harness-v2")).toBeInTheDocument();
    expect(screen.getByText("Runtime do modelo")).toBeInTheDocument();
    expect(
      screen.getByText("Herda a sessão Codex; o módulo não sobrescreve"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Sistema do planejador")).toHaveLength(2);
    expect(
      screen.getAllByText(
        "product-discovery-worker/prompts/productdiscovery.v1/plan/system.md",
      ),
    ).toHaveLength(1);
    expect(
      screen.getByRole("heading", {
        name: "Arquivos que definem o comportamento",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Prompt ou instrução")).toBeInTheDocument();
    expect(screen.getAllByText("Schema de saída")).toHaveLength(2);
    expect(screen.getAllByText("Abrir arquivo")).toHaveLength(2);
    const promptSummary = screen
      .getAllByText("Sistema do planejador")
      .map((element) => element.closest("summary"))
      .find(Boolean);
    expect(promptSummary).not.toBeNull();
    fireEvent.click(promptSummary!);
    expect(screen.getByText("Fechar arquivo")).toBeInTheDocument();
    expect(
      screen.getAllByText(
        "product-discovery-worker/prompts/productdiscovery.v1/plan/system.md",
      ),
    ).toHaveLength(2);
    expect(
      screen.getByText("Você é Argos. Comprove dor e demanda com evidências."),
    ).toBeInTheDocument();
    const schemaSummary = screen
      .getAllByText("Schema do plano")
      .map((element) => element.closest("summary"))
      .find(Boolean);
    expect(schemaSummary).not.toBeNull();
    fireEvent.click(schemaSummary!);
    expect(screen.getByText("2 campo(s)")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Abrir mesa" })).toHaveAttribute(
      "href",
      "/agents/5",
    );
  });
});
