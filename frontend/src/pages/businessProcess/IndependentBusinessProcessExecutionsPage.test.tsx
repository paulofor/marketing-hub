import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import IndependentBusinessProcessExecutionsPage from "./IndependentBusinessProcessExecutionsPage";

vi.mock("axios");

const catalog = [
  {
    processDefinitionId: 52,
    processCode: "pde-opportunity-discovery",
    name: "Descoberta e priorização da oportunidade PDE",
    purpose: "Reunir evidências factuais antes de criar qualquer produto.",
    ownerName: "Argos",
    triggerDescription: "Uma pergunta real de mercado.",
    outcomeDescription: "Dossiê factual auditável.",
    versionNumber: 6,
    executionAvailable: true,
    executionAvailabilityReason: "Pronto para iniciar sem produto.",
    inputFields: [
      {
        key: "researchMode",
        label: "O que Argos deve fazer?",
        controlType: "SELECT",
        required: true,
        maxLength: 32,
        defaultValue: "DISCOVER_MARKETS",
        helpText: "Descobrir ou validar um mercado.",
        options: [
          { value: "DISCOVER_MARKETS", label: "Descobrir mercados candidatos" },
          { value: "VALIDATE_MARKET", label: "Validar um mercado informado" },
        ],
      },
      {
        key: "marketType",
        label: "Tipo de comprador",
        controlType: "SELECT",
        required: true,
        maxLength: 32,
        defaultValue: "B2C",
        options: [
          { value: "B2C", label: "Pessoa física (B2C)" },
          { value: "B2B", label: "Empresa (B2B)" },
        ],
      },
      {
        key: "theme",
        label: "Público, universo ou mercado de partida",
        controlType: "TEXT",
        required: true,
        maxLength: 191,
        helpText: "Descreva a dor pesquisada.",
      },
      {
        key: "acquisitionChannel",
        label: "Canal provável de aquisição",
        controlType: "TEXT",
        required: false,
        maxLength: 120,
        defaultValue: "Instagram",
      },
      {
        key: "referenceSources",
        label: "Fontes editoriais de referência",
        controlType: "TEXTAREA",
        required: false,
        maxLength: 5000,
      },
      {
        key: "country",
        label: "País",
        controlType: "TEXT",
        required: true,
        maxLength: 16,
        defaultValue: "BR",
      },
      {
        key: "language",
        label: "Idioma",
        controlType: "TEXT",
        required: true,
        maxLength: 16,
        defaultValue: "pt-BR",
      },
    ],
  },
];

const summary = {
  id: 91,
  requestKey: "b82df168-e383-4acd-8ca4-ab858b39fd3e",
  processDefinitionId: 52,
  processCode: "pde-opportunity-discovery",
  processName: "Descoberta e priorização da oportunidade PDE",
  processVersionNumber: 6,
  sourceReference: "product-discovery-cycle:77",
  displayName: "agenda vazia para manicures",
  requestedByName: "Marketing Hub",
  input: {
    theme: "agenda vazia para manicures",
    country: "BR",
    language: "pt-BR",
  },
  status: "PENDING",
  activityCount: 1,
  completedActivityCount: 0,
  costCoverage: "NOT_REPORTED",
  createdAt: "2026-08-30T14:00:00Z",
};

function detail(execution = summary) {
  return {
    execution,
    activities: [
      {
        activityId: "marketEvidence",
        activityName: "Reunir evidências factuais de mercado",
        status: execution.status,
        tasks: [
          {
            taskId: 271,
            status: execution.status,
            assignedAgentKey: "market-radar",
            assignedAgentNickname: "Argos",
            title: "Reunir evidências factuais",
            costEstimationStatus: "NOT_REPORTED",
            createdAt: "2026-08-30T14:00:00Z",
          },
        ],
      },
    ],
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <IndependentBusinessProcessExecutionsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("IndependentBusinessProcessExecutionsPage", () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/catalog") {
        return { data: catalog };
      }
      if (url === "/api/independent-business-process-executions") {
        return { data: [] };
      }
      if (url === "/api/independent-business-process-executions/91") {
        return { data: detail() };
      }
      throw new Error(`URL inesperada: ${url}`);
    });
    vi.mocked(axios.post).mockResolvedValue({ data: detail() });
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("inicia a atividade de Argos sem fabricar produto ou experimento", async () => {
    renderPage();
    const user = userEvent.setup();

    expect(
      await screen.findByRole("heading", {
        name: "Executar processos independentes",
      }),
    ).toBeInTheDocument();
    expect(await screen.findByDisplayValue("BR")).toBeInTheDocument();
    expect(screen.getByDisplayValue("pt-BR")).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "Descobrir mercados candidatos" }),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("Pessoa física (B2C)")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Instagram")).toBeInTheDocument();
    await user.type(
      screen.getByLabelText("Público, universo ou mercado de partida *"),
      "agenda vazia para manicures",
    );
    await user.type(
      screen.getByLabelText("Fontes editoriais de referência"),
      "https://revistamarieclaire.globo.com/",
    );
    await user.click(screen.getByRole("button", { name: "Iniciar processo" }));

    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
    const [, body] = vi.mocked(axios.post).mock.calls[0];
    expect(body).toMatchObject({
      processDefinitionId: 52,
      requestedByName: "Marketing Hub",
      input: {
        theme: "agenda vazia para manicures",
        researchMode: "DISCOVER_MARKETS",
        marketType: "B2C",
        acquisitionChannel: "Instagram",
        referenceSources: "https://revistamarieclaire.globo.com/",
        country: "BR",
        language: "pt-BR",
      },
    });
    expect(body).not.toHaveProperty("productId");
    expect(body).not.toHaveProperty("experimentId");
    expect(body).toHaveProperty("requestKey");
    expect(
      await screen.findByRole("heading", {
        name: "Execução #91 · agenda vazia para manicures",
      }),
    ).toBeInTheDocument();
  });

  it("mostra bloqueio e causa persistida sem tratá-los como conclusão", async () => {
    const blocked = {
      ...summary,
      status: "BLOCKED",
      latestError: "Fonte pública recusou a consulta.",
      startedAt: "2026-08-30T14:00:05Z",
      finishedAt: "2026-08-30T14:01:00Z",
    };
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/catalog") {
        return { data: catalog };
      }
      if (url === "/api/independent-business-process-executions") {
        return { data: [blocked] };
      }
      if (url === "/api/independent-business-process-executions/91") {
        return { data: detail(blocked) };
      }
      throw new Error(`URL inesperada: ${url}`);
    });

    renderPage();

    expect(await screen.findByText("Causa registrada:")).toBeInTheDocument();
    expect(
      screen.getAllByText("Fonte pública recusou a consulta.").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("Bloqueada").length).toBeGreaterThan(0);
    expect(screen.queryByText("Venda")).not.toBeInTheDocument();
  });
});
