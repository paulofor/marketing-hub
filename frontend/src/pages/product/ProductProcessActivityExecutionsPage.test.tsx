import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProductProcessActivityExecutionsPage from "./ProductProcessActivityExecutionsPage";

vi.mock("axios");

const dedaloTask = {
  taskId: 243,
  processDefinitionId: 18,
  processVersionNumber: 4,
  title: "Experimento #89 · construir e homologar a landing",
  status: "COMPLETED",
  sourceReference: "commercial-plan:4@v3:journey",
  assignedAgentKey: "landing-generator",
  assignedAgentNickname: "Dédalo",
  comments: JSON.stringify({ summary: "Landing construída" }),
  evidenceJson: JSON.stringify({ approvalRecommendation: "APPROVE" }),
  inputTokens: 947056,
  cachedInputTokens: 796288,
  outputTokens: 25323,
  estimatedCostUsd: 1.4280472,
  costEstimationStatus: "ESTIMATED",
  createdAt: "2026-08-27T03:26:19Z",
  startedAt: "2026-08-27T03:26:45Z",
  finishedAt: "2026-08-27T03:35:14Z",
  modelCode: "gpt-5.6-sol",
  reasoningEffort: "high",
  productInternalName: "Rigel",
  promptSent: "Construa a landing com ativos aprovados.",
};

const psiqueTask = {
  ...dedaloTask,
  taskId: 244,
  title: "Avaliar percepção da cliente",
  status: "BLOCKED",
  assignedAgentKey: "customer-agent",
  assignedAgentNickname: "Psique",
  comments: "Checkout ausente na evidência.",
  estimatedCostUsd: 0.1895768,
  reasoningEffort: undefined,
  executionMode: "MODEL",
  blockerGuidance: {
    category: "FUNCTIONAL_ADJUSTMENT",
    recommendedAction:
      "Vincule a versão comprada ao contrato de acesso e reinicie a tarefa.",
    helpLinks: [
      {
        label: "Abrir experiência revisada",
        url: "https://rigel.example/jornada",
      },
    ],
  },
  accessedUrls: [
    {
      label: "Landing do Rigel",
      url: "https://rigel.example/jornada",
      accessMethod: "BROWSER",
      accessedAt: "2026-08-28T16:15:48Z",
    },
  ],
  visualEvidence: [
    {
      id: 901,
      captureSessionId: "capture-rigel-258",
      evidenceKey: "page-1-full",
      evidenceType: "FULL_PAGE",
      label: "Página 1 · visão completa",
      deviceProfile: "IPHONE_15_PRO",
      pageNumber: 1,
      viewportWidth: 393,
      viewportHeight: 852,
      pageHeightPx: 1704,
      scrollY: 0,
      sourceUrl: "https://rigel.example/jornada",
      finalUrl: "https://rigel.example/jornada",
      contentUrl: "/api/agent-tasks/244/visual-evidence/901/content",
      sizeBytes: 240000,
      sha256: "a".repeat(64),
      capturedAt: "2026-08-28T16:14:48Z",
    },
    {
      id: 902,
      captureSessionId: "capture-rigel-258",
      evidenceKey: "page-1-fold-1",
      evidenceType: "FOLD",
      label: "Página 1 · dobra 1",
      deviceProfile: "IPHONE_15_PRO",
      pageNumber: 1,
      foldNumber: 1,
      viewportWidth: 393,
      viewportHeight: 852,
      pageHeightPx: 1704,
      scrollY: 0,
      sourceUrl: "https://rigel.example/jornada",
      finalUrl: "https://rigel.example/jornada",
      contentUrl: "/api/agent-tasks/244/visual-evidence/902/content",
      sizeBytes: 120000,
      sha256: "b".repeat(64),
      capturedAt: "2026-08-28T16:14:49Z",
    },
    {
      id: 903,
      captureSessionId: "capture-rigel-258",
      evidenceKey: "page-1-fold-2",
      evidenceType: "FOLD",
      label: "Página 1 · dobra 2",
      deviceProfile: "IPHONE_15_PRO",
      pageNumber: 1,
      foldNumber: 2,
      viewportWidth: 393,
      viewportHeight: 852,
      pageHeightPx: 1704,
      scrollY: 852,
      sourceUrl: "https://rigel.example/jornada",
      finalUrl: "https://rigel.example/jornada",
      contentUrl: "/api/agent-tasks/244/visual-evidence/903/content",
      sizeBytes: 118000,
      sha256: "c".repeat(64),
      capturedAt: "2026-08-28T16:14:50Z",
    },
  ],
  visualAudit: {
    captureSessionId: "capture-rigel-258",
    mobileFirst: true,
    fullPageEvidenceIds: [901],
    fullPageContinuity:
      "A promessa conduz naturalmente para a prova e para a próxima ação.",
    overallAestheticAssessment:
      "A composição é leve, coerente e passa profissionalismo sem frieza.",
    foldAnalyses: [
      {
        artifactId: 902,
        deviceProfile: "IPHONE_15_PRO",
        pageNumber: 1,
        foldNumber: 1,
        aestheticAssessment: "Abertura limpa e acolhedora.",
        visualHierarchy: "Título e benefício dominam corretamente.",
        legibility: "Texto e contraste são confortáveis no celular.",
        emotionEvoked: "Curiosidade com alívio inicial.",
        ctaVisibility: "CTA principal visível sem pressão.",
      },
      {
        artifactId: 903,
        deviceProfile: "IPHONE_15_PRO",
        pageNumber: 1,
        foldNumber: 2,
        aestheticAssessment: "Provas bem espaçadas e consistentes.",
        visualHierarchy: "Demonstração precede os detalhes.",
        legibility: "Blocos curtos favorecem a leitura.",
        emotionEvoked: "Confiança crescente.",
        ctaVisibility: "Próxima ação continua fácil de encontrar.",
      },
    ],
  },
  purchaseEmotion: {
    acquisitionExpectation:
      "Espero organizar o atendimento e responder sem parecer robótica.",
    acquisitionAnxiety:
      "Receio receber modelos genéricos que deem mais trabalho do que ajuda.",
    expectedPostDeliveryFeeling:
      "Imagino sentir alívio, controle e segurança para atender melhor.",
    emotionalTension:
      "Quero ganhar tempo, mas temo pagar por algo difícil de adaptar.",
    evidenceBoundary:
      "Reação simulada pela persona e pelos pixels, não satisfação real.",
  },
};

const themisTask = {
  ...dedaloTask,
  taskId: 245,
  title: "Executar revisão comercial independente",
  status: "PENDING",
  assignedAgentKey: "meta-ad-approver",
  assignedAgentNickname: "Têmis",
  comments: undefined,
  evidenceJson: undefined,
  estimatedCostUsd: undefined,
  reasoningEffort: undefined,
  startedAt: undefined,
  finishedAt: undefined,
};

const history = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
  commercialPlanId: 4,
  commercialPlanName: "Plano comercial do Rigel",
  selectedProcessDefinitionId: 18,
  processCode: "landing-page-generation",
  processName: "Geração de landing page",
  selectedProcessVersionNumber: 4,
  selectedProcessStatus: "PUBLISHED",
  currentExecutionReference: "commercial-plan:4@v3:journey",
  operationalState: "BLOCKED",
  objectiveAchieved: false,
  selectedActivityCount: 8,
  completedActivityCount: 4,
  remainingActivityCount: 4,
  blockedActivityCount: 1,
  currentActivityId: "customer",
  currentActivityName: "Avaliar percepção da cliente",
  currentActivityState: "BLOCKED",
  currentActivityStateReason: "Checkout ausente na evidência.",
  activityCount: 8,
  activitiesWithTasksCount: 6,
  uniqueTaskCount: 3,
  knownEstimatedCostUsd: 1.617624,
  costCoverage: "PARTIAL",
  activities: [
    {
      activityDefinitionId: 119,
      activityId: "select",
      activityName: "Selecionar provas reais da entrega",
      activityObjective: "Selecionar ativos aprovados e rastreáveis.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "COMPLETED",
      stateReason:
        "Atividade comprovadamente coberta pela tarefa composta #243.",
      objectiveAchieved: true,
      stateEvidence: "COMPOSITE_TASK_COVERAGE",
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 120,
      activityId: "strategy",
      activityName: "Definir estratégia de conversão",
      activityObjective: "Definir a estratégia da página.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 2,
      selectedVersionActivity: true,
      operationalState: "COMPLETED",
      stateReason:
        "Atividade comprovadamente coberta pela tarefa composta #243.",
      objectiveAchieved: true,
      stateEvidence: "COMPOSITE_TASK_COVERAGE",
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 121,
      activityId: "compose",
      activityName: "Solicitar composição ou edição visual quando necessária",
      activityObjective: "Compor os ativos visuais aprovados.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 3,
      selectedVersionActivity: true,
      operationalState: "COMPLETED",
      stateReason:
        "Atividade comprovadamente coberta pela tarefa composta #243.",
      objectiveAchieved: true,
      stateEvidence: "COMPOSITE_TASK_COVERAGE",
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 122,
      activityId: "html",
      activityName: "Construir HTML completo com ativos aprovados",
      activityObjective: "Entregar documento responsivo e instrumentado.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 4,
      selectedVersionActivity: true,
      operationalState: "COMPLETED",
      stateReason: "Objetivo da atividade atingido na instância BPM.",
      objectiveAchieved: true,
      stateEvidence: "DIRECT",
      activityInstanceId: 128,
      occurrenceNumber: 1,
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 123,
      activityId: "technical",
      activityName: "Validar técnica e fidelidade visual",
      activityObjective: "Registrar a validação técnica independente.",
      activityOwnerName: "Quality Review",
      sequenceNumber: 5,
      selectedVersionActivity: true,
      operationalState: "NOT_STARTED",
      stateReason:
        "Nenhuma tarefa ou instância foi registrada para esta atividade.",
      objectiveAchieved: false,
      stateEvidence: "NOT_RECORDED",
      taskCount: 0,
      tasks: [],
    },
    {
      activityDefinitionId: 124,
      activityId: "customer",
      activityName: "Avaliar percepção da cliente",
      activityObjective: "Validar clareza, desejo e confiança.",
      activityOwnerName: "Psique",
      sequenceNumber: 6,
      selectedVersionActivity: true,
      operationalState: "BLOCKED",
      stateReason: "Checkout ausente na evidência.",
      objectiveAchieved: false,
      stateEvidence: "DIRECT",
      activityInstanceId: 129,
      occurrenceNumber: 1,
      taskCount: 1,
      tasks: [psiqueTask],
    },
    {
      activityDefinitionId: 125,
      activityId: "commercial",
      activityName: "Executar revisão comercial independente",
      activityObjective: "Obter a decisão independente de Têmis.",
      activityOwnerName: "Têmis",
      sequenceNumber: 7,
      selectedVersionActivity: true,
      operationalState: "PENDING",
      stateReason: "Atividade aguardando execução ou liberação pelo backend.",
      objectiveAchieved: false,
      stateEvidence: "DIRECT",
      activityInstanceId: 130,
      occurrenceNumber: 1,
      taskCount: 1,
      tasks: [themisTask],
    },
    {
      activityDefinitionId: 126,
      activityId: "human",
      activityName: "Aprovação humana para publicar",
      activityObjective: "Autorizar publicação explícita.",
      activityOwnerName: "Operador humano",
      sequenceNumber: 8,
      selectedVersionActivity: true,
      operationalState: "NOT_STARTED",
      stateReason:
        "Nenhuma tarefa ou instância foi registrada para esta atividade.",
      objectiveAchieved: false,
      stateEvidence: "NOT_RECORDED",
      taskCount: 0,
      tasks: [],
    },
  ],
};

function renderPage(
  initialEntry = "/products/9/value-chain-history/processes/18/activities",
) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route
            path="/products/:productId/value-chain-history/processes/:processDefinitionId/activities"
            element={<ProductProcessActivityExecutionsPage />}
          />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe("ProductProcessActivityExecutionsPage", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("shows product activities and tasks without duplicating the summary", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: history });

    renderPage();

    expect(
      await screen.findByRole("heading", {
        name: "Rigel · Geração de landing page",
      }),
    ).toBeInTheDocument();
    const processNames = document.querySelectorAll(
      ".business-process-entity-name--process",
    );
    expect(processNames).toHaveLength(1);
    expect(
      processNames[0].querySelector(".lucide-workflow"),
    ).toBeInTheDocument();
    const activityNames = document.querySelectorAll(
      ".business-process-entity-name--activity",
    );
    expect(activityNames).toHaveLength(17);
    activityNames.forEach((activityName) => {
      expect(
        activityName.querySelector(".lucide-clipboard-list"),
      ).toBeInTheDocument();
      expect(
        activityName.querySelector(".lucide-workflow"),
      ).not.toBeInTheDocument();
    });
    const situation = screen.getByRole("region", {
      name: "Situação do processo",
    });
    expect(within(situation).getByText("Bloqueado")).toBeInTheDocument();
    expect(
      within(situation).getByText("4 de 8 atividades concluídas"),
    ).toBeInTheDocument();
    expect(
      within(situation).getAllByText("Checkout ausente na evidência."),
    ).toHaveLength(2);
    expect(
      within(situation).getByRole("heading", { name: "Já concluído" }),
    ).toBeInTheDocument();
    expect(
      within(situation).getByRole("heading", { name: "Falta concluir" }),
    ).toBeInTheDocument();
    expect(
      within(situation).getByText("Validar técnica e fidelidade visual"),
    ).toBeInTheDocument();
    expect(
      within(situation).getByText("Executar revisão comercial independente"),
    ).toBeInTheDocument();
    expect(screen.getByText(/6 com tarefas reais/)).toBeInTheDocument();
    expect(
      screen.getByText("Execuções sem duplicar tarefas compostas"),
    ).toBeInTheDocument();
    expect(screen.getByText("Cobertura parcial")).toBeInTheDocument();
    expect(screen.getByText("Avanço bloqueado")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Vincule a versão comprada ao contrato de acesso e reinicie a tarefa.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("URLs acessadas por Psique")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Landing do Rigel" }),
    ).toHaveAttribute("target", "_blank");
    expect(
      screen.getByText("https://rigel.example/jornada"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Antes e depois imaginados pela cliente",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Espero organizar o atendimento e responder sem parecer robótica.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Snapshots mobile e análise por dobra",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByAltText("Snapshot de Psique — Página 1 · dobra 1"),
    ).toHaveAttribute(
      "src",
      "/api/agent-tasks/244/visual-evidence/902/content",
    );
    expect(
      screen.getByRole("link", {
        name: "Snapshot de Psique — Página 1 · dobra 2",
      }),
    ).toHaveAttribute("target", "_blank");
    expect(
      screen.getByText("Texto e contraste são confortáveis no celular."),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "A promessa conduz naturalmente para a prova e para a próxima ação.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Selecionar provas reais da entrega",
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/Tarefa #243/)).toHaveLength(4);
    expect(
      screen.getAllByText("Nenhuma tarefa registrada para este produto."),
    ).toHaveLength(2);
    expect(
      screen.getByRole("link", { name: "Histórico de atividades" }),
    ).toHaveAttribute("href", "/products/9/value-chain-history");
    expect(
      screen.getByRole("link", { name: "Plano comercial" }),
    ).toHaveAttribute("href", "/planning/4");
    expect(screen.getByRole("link", { name: "Abrir BPM" })).toHaveAttribute(
      "href",
      "/business-processes?processId=18",
    );
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/18/products/9/activity-executions",
    );
  });

  it("makes a process with every objective achieved explicitly complete", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...history,
        operationalState: "COMPLETED",
        objectiveAchieved: true,
        completedActivityCount: 8,
        remainingActivityCount: 0,
        blockedActivityCount: 0,
        currentActivityId: undefined,
        currentActivityName: undefined,
        currentActivityState: undefined,
        currentActivityStateReason: undefined,
        activities: history.activities.map((activity) => ({
          ...activity,
          operationalState: "COMPLETED",
          objectiveAchieved: true,
          stateReason: "Objetivo da atividade atingido na instância BPM.",
        })),
      },
    });

    renderPage();

    const situation = await screen.findByRole("region", {
      name: "Situação do processo",
    });
    expect(within(situation).getByText("Concluído")).toBeInTheDocument();
    expect(
      within(situation).getByText("8 de 8 atividades concluídas"),
    ).toBeInTheDocument();
    expect(
      within(situation).getByText("Nenhuma atividade pendente."),
    ).toBeInTheDocument();
  });

  it("keeps a process without current execution visibly not started", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...history,
        currentExecutionReference: undefined,
        operationalState: "NOT_STARTED",
        objectiveAchieved: false,
        completedActivityCount: 0,
        remainingActivityCount: 8,
        blockedActivityCount: 0,
        currentActivityId: "select",
        currentActivityName: "Selecionar provas reais da entrega",
        currentActivityState: "NOT_STARTED",
        currentActivityStateReason:
          "Nenhuma tarefa ou instância foi registrada para esta atividade.",
        activities: history.activities.map((activity) => ({
          ...activity,
          operationalState: "NOT_STARTED",
          objectiveAchieved: false,
          stateEvidence: "NOT_RECORDED",
          stateReason:
            "Nenhuma tarefa ou instância foi registrada para esta atividade.",
          taskCount: 0,
          tasks: [],
        })),
      },
    });

    renderPage();

    const situation = await screen.findByRole("region", {
      name: "Situação do processo",
    });
    expect(within(situation).getByText("Não iniciado")).toBeInTheDocument();
    expect(
      within(situation).getByText("0 de 8 atividades concluídas"),
    ).toBeInTheDocument();
    expect(
      within(situation).getByText(
        "Nenhuma atividade possui conclusão comprovada.",
      ),
    ).toBeInTheDocument();
  });

  it("starts every responsible task from the current product activity", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...history,
        productId: 4,
        productName: "Método MUSA 7 Dias",
        productInternalName: "Vega",
        selectedProcessDefinitionId: 45,
        processCode: "pde-commercial-homologation-activation",
        processName: "Homologação e ativação comercial do PDE",
        selectedProcessVersionNumber: 5,
        selectedActivityCount: 1,
        completedActivityCount: 0,
        remainingActivityCount: 1,
        blockedActivityCount: 0,
        operationalState: "NOT_STARTED",
        currentActivityId: "pdeGate",
        currentActivityName: "Validar fatos, controle e valor do PDE",
        currentActivityState: "NOT_STARTED",
        activities: [
          {
            ...history.activities[0],
            activityId: "pdeGate",
            activityName: "Validar fatos, controle e valor do PDE",
            activityOwnerName: "Psique e Têmis",
            operationalState: "NOT_STARTED",
            objectiveAchieved: false,
            stateEvidence: "NOT_RECORDED",
            stateReason:
              "Nenhuma tarefa ou instância foi registrada para esta atividade.",
            taskCount: 0,
            tasks: [],
            executionRequestAvailable: true,
            executionRequestReason:
              "A atividade está pronta para abrir todas as tarefas responsáveis.",
          },
        ],
      },
    });
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        processDefinitionId: 45,
        productId: 4,
        activityId: "pdeGate",
        sourceReference: "experiment:90",
        tasks: [{ id: 301 }, { id: 302 }],
      },
    });

    renderPage("/products/4/value-chain-history/processes/45/activities");

    const button = await screen.findByRole("button", {
      name: "Executar atividade",
    });
    fireEvent.click(button);

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/business-processes/45/products/4/activities/pdeGate/execution-requests",
      ),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Todas as tarefas responsáveis foram abertas",
    );
  });

  it("restarts a blocked task while preserving its audited attempt", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...history,
        selectedProcessDefinitionId: 63,
        processCode: "pde-communication-sales-journey",
        processName: "Comunicação e jornada de venda do PDE",
        selectedProcessVersionNumber: 7,
        selectedActivityCount: 1,
        completedActivityCount: 0,
        remainingActivityCount: 1,
        blockedActivityCount: 1,
        currentActivityId: "communicationContract",
        currentActivityName: "Materializar contrato de comunicação",
        currentActivityState: "BLOCKED",
        activities: [
          {
            ...history.activities[0],
            activityDefinitionId: 201,
            activityId: "communicationContract",
            activityName: "Materializar contrato de comunicação",
            activityOwnerName: "Íris",
            operationalState: "BLOCKED",
            objectiveAchieved: false,
            stateEvidence: "DIRECT",
            stateReason: "A tentativa anterior foi bloqueada.",
            taskCount: 1,
            tasks: [
              {
                ...psiqueTask,
                taskId: 252,
                assignedAgentKey: "communication-director",
                assignedAgentNickname: "Íris",
              },
            ],
            executionRequestAvailable: true,
            executionRequestReason:
              "Estratégia, economia, PDE e provas estão prontos para Íris.",
          },
        ],
      },
    });
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        processDefinitionId: 63,
        productId: 9,
        activityId: "communicationContract",
        sourceReference: "experiment:89",
        tasks: [{ id: 253 }],
      },
    });

    renderPage("/products/9/value-chain-history/processes/63/activities");

    const button = await screen.findByRole("button", {
      name: "Reiniciar tarefa",
    });
    expect(button.querySelector(".lucide-rotate-ccw")).toBeInTheDocument();
    fireEvent.click(button);

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/business-processes/63/products/9/activities/communicationContract/execution-requests",
      ),
    );
  });

  it("validates a backend-owned integration and shows the persisted result", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...history,
        selectedProcessDefinitionId: 55,
        processCode: "pde-communication-sales-journey",
        processName: "Comunicação e jornada de venda do PDE",
        selectedProcessVersionNumber: 6,
        selectedActivityCount: 1,
        completedActivityCount: 0,
        remainingActivityCount: 1,
        blockedActivityCount: 0,
        operationalState: "NOT_STARTED",
        currentActivityId: "integration",
        currentActivityName: "Integrar canal, checkout, acesso e eventos",
        currentActivityState: "NOT_STARTED",
        activities: [
          {
            ...history.activities[0],
            activityDefinitionId: 175,
            activityId: "integration",
            activityName: "Integrar canal, checkout, acesso e eventos",
            activityOwnerName: "Marketing Hub",
            operationalState: "NOT_STARTED",
            objectiveAchieved: false,
            stateEvidence: "NOT_RECORDED",
            stateReason:
              "Nenhuma tarefa ou instância foi registrada para esta atividade.",
            taskCount: 0,
            tasks: [],
            executionRequestAvailable: true,
            executionRequestReason:
              "Comunicação, criativos e destino estão aprovados.",
          },
        ],
      },
    });
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        processDefinitionId: 55,
        productId: 9,
        activityId: "integration",
        sourceReference: "commercial-plan:4@v3:journey",
        tasks: [],
        operationalState: "COMPLETED",
        objectiveAchieved: true,
        message:
          "Canal, checkout, acesso e eventos foram preparados. O Rigel avançou para Homologação e ativação comercial.",
      },
    });

    renderPage("/products/9/value-chain-history/processes/55/activities");

    fireEvent.click(
      await screen.findByRole("button", { name: "Validar integração" }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/business-processes/55/products/9/activities/integration/execution-requests",
      ),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      "O Rigel avançou para Homologação e ativação comercial",
    );
  });

  it("makes a backend failure explicit", async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error("backend unavailable"));

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível consultar as atividades e tarefas deste produto no processo.",
    );
    expect(
      screen.queryByRole("region", { name: "Atividades e tarefas do produto" }),
    ).not.toBeInTheDocument();
  });

  it("rejects invalid product or process identifiers before querying", () => {
    renderPage(
      "/products/not-a-product/value-chain-history/processes/18/activities",
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Produto ou processo inválido.",
    );
    expect(axios.get).not.toHaveBeenCalled();
  });
});
