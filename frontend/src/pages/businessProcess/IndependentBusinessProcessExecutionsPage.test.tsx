import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  IndependentBusinessProcessExecution,
  IndependentBusinessProcessFlowReport,
  IndependentBusinessProcessExecutionSummary,
} from "../../api/businessProcess/types";
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
        key: "theme",
        label: "Tema amplo",
        controlType: "TEXTAREA",
        required: true,
        maxLength: 191,
        helpText: "Os agentes derivam público, dor, oferta e experiência.",
      },
    ],
  },
];

const summary: IndependentBusinessProcessExecutionSummary = {
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

type ExecutionDetailFixture = IndependentBusinessProcessExecution & {
  processReport: IndependentBusinessProcessFlowReport;
};

function detail(execution = summary): ExecutionDetailFixture {
  return {
    execution,
    processReport: {
      reportType: "PDE_OPPORTUNITY_TO_PRODUCT_V1",
      status: execution.status,
      headline: "Uma candidata factual está pronta para priorização.",
      acquisitionChannel: "Instagram",
      candidateCount: 1,
      dossierReadyCount: 1,
      plannedProductCount: 0,
      sourceCoverage: [
        {
          sourceCode: "WEB",
          label: "Internet",
          status: "OBSERVED",
          itemCount: 12,
          summary: "Fontes públicas independentes coletadas.",
        },
        {
          sourceCode: "META",
          label: "Biblioteca Meta / Instagram",
          status: "OBSERVED",
          itemCount: 3,
          summary: "Anúncios públicos observados; isso não equivale a venda.",
        },
        {
          sourceCode: "PESQUISAS",
          label: "Acervo /pesquisas",
          status: "OBSERVED",
          itemCount: 7,
          summary: "Referências internas usadas para confrontar hipóteses.",
        },
      ],
      candidates: [
        {
          opportunityId: 501,
          name: "Guarda-roupa cápsula sensorial para mulheres 40+",
          primaryAudience: "Mulheres brasileiras de 40 a 55 anos",
          rootPain:
            "Escolher combinações confortáveis ainda exige tentativa manual.",
          score: 81,
          maturity: "DOSSIER_READY",
          decision: "APPROVE",
          purchaseSituation:
            "Mudança corporal no climatério antes de um evento.",
          observedLanguage: ["Quero me sentir eu de novo"],
          currentAlternatives: ["Consultoria de imagem"],
          residualEffort:
            "Montar combinações e adaptar peças ao próprio corpo.",
          instagramFitEvidence:
            "Transformação visual demonstrável em carrossel.",
          commercialRisk: "Validar disposição real de pagar.",
          dossierId: 301,
          dossierStatus: "UNDER_REVIEW",
          nextAction: "Aguardar Atena priorizar no máximo uma candidata.",
          sources: [
            {
              sourceType: "WEB",
              title: "Pesquisa factual",
              url: "https://example.test/pesquisa",
              evidence: "Dor recorrente observada.",
            },
            {
              sourceType: "PESQUISAS",
              title: "Artigo interno sobre climatério",
              url: "/pesquisas/climaterio.md",
              evidence: "Hipótese confrontada com o acervo versionado.",
            },
          ],
          stages: [
            {
              stageCode: "ARGOS",
              label: "Pesquisa factual",
              agent: "Argos",
              status: "COMPLETED",
              decision: "APPROVE",
              summary: "Candidata factual formada.",
            },
            {
              stageCode: "ATENA",
              label: "Priorização de mercado",
              agent: "Atena",
              status: "PENDING",
              summary: "Aguardando priorização.",
            },
            {
              stageCode: "PLUTUS",
              label: "Economia e limites",
              agent: "Plutus",
              status: "WAITING",
              summary: "Aguardando Atena.",
            },
            {
              stageCode: "DEDALO",
              label: "Harness e experiência PDE",
              agent: "Dédalo",
              status: "WAITING",
              summary: "Aguardando Plutus.",
            },
            {
              stageCode: "PRODUCT",
              label: "Produto planejado",
              agent: "Backend",
              status: "WAITING",
              summary: "Aguardando os gates.",
            },
          ],
        },
      ],
    },
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
            evidence: undefined as unknown,
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
    expect(
      await screen.findByRole("heading", {
        name: "Do tema amplo ao PDE planejado",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Produto planejado")).toBeInTheDocument();
    const theme = screen.getByLabelText("Tema amplo *");
    expect(theme).toHaveAttribute("maxlength", "191");
    expect(theme).toHaveAttribute(
      "title",
      "Os agentes derivam público, dor, oferta e experiência.",
    );
    await user.type(theme, "agenda vazia para manicures");
    await user.click(screen.getByRole("button", { name: "Iniciar processo" }));

    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
    const [, body] = vi.mocked(axios.post).mock.calls[0];
    expect(body).toMatchObject({
      processDefinitionId: 52,
      requestedByName: "Marketing Hub",
      input: {
        theme: "agenda vazia para manicures",
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
    expect(screen.getByText("Biblioteca Meta / Instagram")).toBeInTheDocument();
    expect(screen.getByText("Acervo /pesquisas")).toBeInTheDocument();
    expect(screen.getByText("/pesquisas/climaterio.md")).toBeInTheDocument();
    expect(
      screen.getByText("Guarda-roupa cápsula sensorial para mulheres 40+"),
    ).toBeInTheDocument();
    expect(screen.getByText("Harness e experiência PDE")).toBeInTheDocument();
    expect(
      screen.getByText("Aguardar Atena priorizar no máximo uma candidata."),
    ).toBeInTheDocument();
  });

  it("torna o produto planejado e sua linhagem visíveis no relatório", async () => {
    const completed: IndependentBusinessProcessExecutionSummary = {
      ...summary,
      status: "COMPLETED",
      completedActivityCount: 1,
      startedAt: "2026-08-30T14:00:05Z",
      finishedAt: "2026-08-30T14:04:00Z",
    };
    const completedDetail = detail(completed);
    completedDetail.processReport.plannedProductCount = 1;
    completedDetail.processReport.candidates[0] = {
      ...completedDetail.processReport.candidates[0],
      productId: 901,
      productName: "Cápsula sensorial PDE",
      productStatus: "PLANNED",
      commercialPlanId: 801,
      nextAction:
        "Abrir o produto planejado e iniciar a construção funcional governada do PDE.",
      stages: completedDetail.processReport.candidates[0].stages.map((stage) =>
        stage.stageCode === "PRODUCT"
          ? {
              ...stage,
              status: "COMPLETED",
              decision: "PLANNED",
              summary: "Produto #901 criado sem publicação ou gasto.",
            }
          : stage,
      ),
    };
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/catalog") {
        return { data: catalog };
      }
      if (url === "/api/independent-business-process-executions") {
        return { data: [completed] };
      }
      if (url === "/api/independent-business-process-executions/91") {
        return { data: completedDetail };
      }
      throw new Error(`URL inesperada: ${url}`);
    });

    renderPage();

    const productLink = await screen.findByRole("link", {
      name: "Abrir produto #901",
    });
    expect(productLink).toHaveAttribute("href", "/products/901/edit");
    expect(productLink).toHaveAttribute("target", "_blank");
    expect(
      screen.getByText("Plano #801", { exact: false }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Produto #901 criado sem publicação ou gasto."),
    ).toBeInTheDocument();
  });

  it("mostra bloqueio e causa persistida sem tratá-los como conclusão", async () => {
    const blocked: IndependentBusinessProcessExecutionSummary = {
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

  it("registra observação oficial e reabre Argos na mesma sessão Meta", async () => {
    const completed: IndependentBusinessProcessExecutionSummary = {
      ...summary,
      status: "COMPLETED",
      completedActivityCount: 1,
      startedAt: "2026-08-30T14:00:05Z",
      finishedAt: "2026-08-30T14:01:00Z",
    };
    const completedDetail = detail(completed);
    completedDetail.activities[0].tasks[0].evidence = {
      researchEvidenceReport: {
        metaCoverage: [
          {
            investigationId: 72,
            publisherPlatform: "INSTAGRAM",
            sourceStatus: "AWAITING_SUPERVISED_OBSERVATION",
          },
        ],
      },
    };
    const awaitingSession = {
      cycleId: 77,
      investigationId: 72,
      cycleStatus: "COMPLETED",
      query: "autocuidado feminino visual",
      country: "BR",
      publisherPlatform: "INSTAGRAM",
      sourceStatus: "AWAITING_SUPERVISED_OBSERVATION",
      collectionMode: "SUPERVISED",
      collectionReason: "Observação humana na fonte oficial.",
      searchUrl: "https://www.facebook.com/ads/library/?q=autocuidado",
      adsObserved: 0,
      activeAds: 0,
      advertisersObserved: 0,
      interpretation:
        "Cobertura aguardando observação; isso não significa ausência de mercado.",
      canRegisterObservation: true,
      canResume: false,
      resumeReason: "Registre um anúncio atual no Instagram.",
      items: [],
    };
    const observedSession = {
      ...awaitingSession,
      sourceStatus: "OBSERVED",
      adsObserved: 1,
      activeAds: 1,
      advertisersObserved: 1,
      canResume: true,
      resumeReason:
        "A evidência está pronta para uma nova tentativa auditável de Argos.",
      items: [
        {
          metaAdId: "ad-72",
          advertiserName: "Marca observada",
          adTexts: ["Seu ritual de cinco minutos começa agora."],
          publisherPlatforms: ["INSTAGRAM"],
          formatTypes: ["VIDEO"],
          active: true,
          commercialSignal: true,
          observations: 1,
          longevityDays: 0,
          sustainedInvestmentSignal: false,
          evidenceConfidence: "LOW",
          firstObservedAt: "2026-08-30T20:00:00Z",
          lastObservedAt: "2026-08-30T20:00:00Z",
        },
      ],
    };
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/catalog") {
        return { data: catalog };
      }
      if (url === "/api/independent-business-process-executions") {
        return { data: [completed] };
      }
      if (url === "/api/independent-business-process-executions/91") {
        return { data: completedDetail };
      }
      if (
        url === "/api/product-discovery/v1/cycles/77/supervised-meta-session"
      ) {
        return { data: awaitingSession };
      }
      throw new Error(`URL inesperada: ${url}`);
    });
    vi.mocked(axios.post).mockImplementation(async (url) => {
      if (
        url ===
        "/api/product-discovery/v1/cycles/77/supervised-meta-session/observations"
      ) {
        return { data: observedSession };
      }
      if (
        url ===
        "/api/product-discovery/v1/cycles/77/supervised-meta-session/resume"
      ) {
        return {
          data: {
            ...observedSession,
            cycleStatus: "READY_FOR_RESEARCH",
            canResume: false,
            resumeReason:
              "A reanálise de Argos já está na fila ou em execução.",
          },
        };
      }
      throw new Error(`URL inesperada: ${url}`);
    });
    const user = userEvent.setup();

    renderPage();

    expect(
      await screen.findByRole("heading", {
        name: "Confirmar anúncios e linguagem no Instagram",
      }),
    ).toBeInTheDocument();
    await user.type(screen.getByLabelText("ID do anúncio *"), "ad-72");
    await user.type(screen.getByLabelText("Anunciante *"), "Marca observada");
    await user.type(
      screen.getByLabelText("URL oficial do anúncio *"),
      "https://www.facebook.com/ads/library/?id=ad-72",
    );
    await user.type(
      screen.getByLabelText("Texto comercial visível *"),
      "Seu ritual de cinco minutos começa agora.",
    );
    await user.click(
      screen.getByRole("button", { name: "Registrar observação" }),
    );

    expect(
      await screen.findByText("Seu ritual de cinco minutos começa agora."),
    ).toBeInTheDocument();
    const resumeButton = screen.getByRole("button", {
      name: "Reanalisar com Argos",
    });
    expect(resumeButton).toBeEnabled();
    await user.click(resumeButton);

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/product-discovery/v1/cycles/77/supervised-meta-session/resume",
      ),
    );
    expect(
      await screen.findByText(
        "A reanálise de Argos já está na fila ou em execução.",
      ),
    ).toBeInTheDocument();
  });
});
