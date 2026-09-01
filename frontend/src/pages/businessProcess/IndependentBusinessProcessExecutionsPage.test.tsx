import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  IndependentBusinessProcessExecution,
  IndependentBusinessProcessFlowReport,
  IndependentBusinessProcessExecutionSummary,
  StartIndependentBusinessProcessExecution,
} from "../../api/businessProcess/types";
import IndependentBusinessProcessExecutionsPage from "./IndependentBusinessProcessExecutionsPage";
import IndependentBusinessProcessExecutionDetailPage from "./IndependentBusinessProcessExecutionDetailPage";

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
      marketExpansion: {
        strategyCode: "BOUNDED_ADJACENT_MARKET_EXPANSION_V1",
        attemptsCompleted: 2,
        maxAttempts: 3,
        stopReason: "DOSSIER_READY_FOUND",
        stopSummary:
          "Argos encontrou uma candidata factual pronta para o gate de Atena.",
        finalResearchLens: "Momento de reencontro antes de um evento",
        attempts: [
          {
            attemptNumber: 1,
            researchLens: "Rotina de beleza antes de sair",
            expansionAxis: "INITIAL_SCOPE",
            rationale: "Investigar o escopo inicial recebido.",
            newPublicEvidenceCount: 20,
            newComparableOfferCount: 1,
            newMetaAdCount: 0,
            candidateCount: 3,
            dossierReadyCount: 0,
            outcome: "ADJUST_AND_CONTINUE",
          },
          {
            attemptNumber: 2,
            researchLens: "Momento de reencontro antes de um evento",
            expansionAxis: "ADJACENT_LIFE_MOMENT",
            rationale: "Buscar uma situação concreta de decisão próxima.",
            newPublicEvidenceCount: 8,
            newComparableOfferCount: 9,
            newMetaAdCount: 2,
            candidateCount: 2,
            dossierReadyCount: 1,
            outcome: "DOSSIER_READY_FOUND",
          },
        ],
      },
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
            processDefinitionId: 52,
            processVersionNumber: 6,
            sourceReference: "product-discovery-cycle:77",
            status: execution.status,
            assignedAgentKey: "market-radar",
            assignedAgentNickname: "Argos",
            title: "Reunir evidências factuais",
            result: { decision: "RESEARCH_MORE" },
            evidence: undefined as unknown,
            inputTokens: 1800,
            cachedInputTokens: 400,
            outputTokens: 260,
            estimatedCostUsd: 0.01234567,
            costEstimationStatus: "ESTIMATED",
            modelCode: "gpt-5.6-sol",
            executionMode: "MODEL",
            reasoningEffort: "high",
            promptSent: "Prompt integral auditado para Argos.",
            agentPromptPart: "Você é o Argos auditável.",
            activityPromptPart: "Pesquise mercados adjacentes.",
            createdAt: "2026-08-30T14:00:00Z",
            startedAt: "2026-08-30T14:00:05Z",
            finishedAt: "2026-08-30T14:01:05Z",
          },
        ],
      },
    ],
  };
}

function renderPage(initialEntry = "/business-process-executions") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route
            path="/business-process-executions"
            element={<IndependentBusinessProcessExecutionsPage />}
          />
          <Route
            path="/business-process-executions/:executionId"
            element={<IndependentBusinessProcessExecutionDetailPage />}
          />
        </Routes>
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
    expect(
      screen.getByRole("heading", { name: "Ampliação controlada de mercado" }),
    ).toBeInTheDocument();
    expect(
      screen.getAllByText("Momento de reencontro antes de um evento"),
    ).toHaveLength(2);
    expect(screen.getByText("Dossiê pronto encontrado")).toBeInTheDocument();
    expect(screen.getByText("+9 ofertas")).toBeInTheDocument();
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

    renderPage("/business-process-executions/91");

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

  it("mostra na execução independente o mesmo bloco auditável da tarefa", async () => {
    const completed: IndependentBusinessProcessExecutionSummary = {
      ...summary,
      status: "COMPLETED",
      completedActivityCount: 1,
      startedAt: "2026-08-30T14:00:05Z",
      finishedAt: "2026-08-30T14:01:05Z",
    };
    const completedDetail = detail(completed);
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/91") {
        return { data: completedDetail };
      }
      throw new Error(`URL inesperada: ${url}`);
    });
    renderPage("/business-process-executions/91");
    const user = userEvent.setup();
    const taskSummary = await screen.findByText(
      /Tarefa #271 · Argos · Concluída/,
    );

    await user.click(taskSummary);

    const taskCard = within(taskSummary.closest("details") as HTMLElement);
    expect(taskCard.getByText("Produto interno")).toBeInTheDocument();
    expect(taskCard.getByText("Não vinculado")).toBeInTheDocument();
    expect(
      taskCard.getByText("product-discovery-cycle:77"),
    ).toBeInTheDocument();
    expect(taskCard.getByText("v6")).toBeInTheDocument();
    expect(taskCard.getByText("MODEL")).toBeInTheDocument();
    expect(taskCard.getByText("gpt-5.6-sol")).toBeInTheDocument();
    expect(taskCard.getByText("high")).toBeInTheDocument();
    expect(
      taskCard.getByText("entrada 1800 · cache 400 · saída 260"),
    ).toBeInTheDocument();
    expect(taskCard.getByText("US$ 0.01234567")).toBeInTheDocument();
    expect(
      taskCard.getByRole("heading", { name: "Parte do agente" }),
    ).toBeInTheDocument();
    expect(
      taskCard.getByRole("heading", { name: "Parte da atividade" }),
    ).toBeInTheDocument();
    expect(
      taskCard.getByRole("heading", {
        name: "Prompt completo enviado ao modelo por Argos",
      }),
    ).toBeInTheDocument();
    expect(taskCard.getByText("Você é o Argos auditável.")).toBeInTheDocument();
    expect(
      taskCard.getByText("Pesquise mercados adjacentes."),
    ).toBeInTheDocument();
    expect(
      taskCard.getByText("Prompt integral auditado para Argos."),
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
    const retriedBlocked = {
      ...blocked,
      id: 92,
      requestKey: "8c819fc2-32a6-4a0c-86bf-0a88406ceba0",
      sourceReference: "product-discovery-cycle:78",
    };
    vi.mocked(axios.post).mockResolvedValue({
      data: detail(retriedBlocked),
    });

    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Por que não executou")).toBeInTheDocument();
    expect(
      screen.getAllByText("Fonte pública recusou a consulta.").length,
    ).toBeGreaterThan(0);
    const detailLink = screen.getByRole("link", {
      name: /#91.*Ver detalhes/i,
    });
    expect(detailLink).toHaveAttribute(
      "href",
      "/business-process-executions/91",
    );
    await user.click(detailLink);

    expect(await screen.findByText("Causa registrada:")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Esta tentativa não executou" }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Bloqueada").length).toBeGreaterThan(0);
    expect(screen.queryByText("Venda")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Tentar novamente" }));
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
    const firstRetry = vi.mocked(axios.post).mock
      .calls[0][1] as StartIndependentBusinessProcessExecution;
    expect(firstRetry).toMatchObject({
      processDefinitionId: 52,
      requestedByName: "Marketing Hub",
      input: blocked.input,
    });
    expect(
      await screen.findByRole("heading", {
        name: "Execução #92 · agenda vazia para manicures",
      }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Tentar novamente" }));
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
    const secondRetry = vi.mocked(axios.post).mock
      .calls[1][1] as StartIndependentBusinessProcessExecution;
    expect(secondRetry.requestKey).not.toBe(firstRetry.requestKey);
  });

  it("distingue execução concluída retida pelo gate sem oferecer nova tentativa", async () => {
    const completedWithGaps: IndependentBusinessProcessExecutionSummary = {
      ...summary,
      status: "BLOCKED",
      completedActivityCount: 1,
      startedAt: "2026-08-30T14:00:05Z",
      finishedAt: "2026-08-30T14:04:00Z",
    };
    const completedWithGapsDetail = detail(completedWithGaps);
    completedWithGapsDetail.activities = completedWithGapsDetail.activities.map(
      (activity) => ({
        ...activity,
        status: "COMPLETED",
        tasks: activity.tasks.map((task) => ({
          ...task,
          status: "COMPLETED",
        })),
      }),
    );
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/independent-business-process-executions/catalog") {
        return { data: catalog };
      }
      if (url === "/api/independent-business-process-executions") {
        return { data: [completedWithGaps] };
      }
      if (url === "/api/independent-business-process-executions/91") {
        return { data: completedWithGapsDetail };
      }
      throw new Error(`URL inesperada: ${url}`);
    });

    const user = userEvent.setup();
    renderPage();

    expect(
      await screen.findByText("Concluída com lacunas"),
    ).toBeInTheDocument();
    await user.click(screen.getByRole("link", { name: /#91.*Ver detalhes/i }));

    expect(
      await screen.findByRole("heading", {
        name: "A execução terminou; o avanço comercial aguarda evidências",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Execução concluída com lacunas"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Esta tentativa não executou")).toBeNull();
    expect(
      screen.queryByRole("button", { name: "Tentar novamente" }),
    ).toBeNull();
    expect(
      screen.getByText(/Tarefa #271 · Argos · Concluída/),
    ).toBeInTheDocument();
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

    renderPage("/business-process-executions/91");

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
