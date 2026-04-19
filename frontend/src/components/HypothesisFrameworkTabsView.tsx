import { Fragment, useMemo, useState } from "react";
import * as Tabs from "@radix-ui/react-tabs";
import { Loader2 } from "lucide-react";
import axios from "axios";
import { toast } from "react-toastify";
import type { HypothesisFramework } from "../api/hypothesis/types";
import { normalizeFramework } from "../api/hypothesis/types";
import type { HypothesisFrameworkSection } from "../api/hypothesis/types";
import { useFrameworkGenerationJobs } from "../api/hypothesis/useFrameworkGenerationJobs";
import { useGenerateFrameworkSection } from "../api/hypothesis/useGenerateFrameworkSection";

const SECTIONS: { id: HypothesisFrameworkSection; label: string }[] = [
  { id: "pain", label: "Dor" },
  { id: "result", label: "Resultado" },
  { id: "mechanism", label: "Mecanismo" },
  { id: "proof", label: "Prova" },
  { id: "offer", label: "Oferta" },
];

interface Props {
  hypothesisId: string;
  nicheId?: string;
  nicheName?: string;
  framework?: HypothesisFramework | null;
  onRefresh?: () => void;
}

type RequestUiStatus =
  | "IDLE"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "INVALIDATED";

interface SectionRequestState {
  status: RequestUiStatus;
  requestedAt?: string;
  startedAt?: string;
  completedAt?: string;
  customInstructions?: string;
  errorMessage?: string;
  stageLabel?: string;
}

type RequestKind = "FULL" | "SUMMARY";

interface AiGenerationRecord {
  id: number;
  domain: string;
  model?: string;
  prompt?: string;
  rawResponse?: string;
  createdAt?: string;
}

interface PageResponse<T> {
  content: T[];
}

interface FrameworkGenerationReportRecord extends AiGenerationRecord {
  metadata: {
    sectionId: HypothesisFrameworkSection;
    sectionOrder: number;
    generationTypeLabel: string;
    generationOrder: number;
  };
}

const REPORT_SECTION_ORDER: HypothesisFrameworkSection[] = [
  "pain",
  "result",
  "mechanism",
  "proof",
  "offer",
];

const STATUS_LABELS: Record<RequestUiStatus, string> = {
  IDLE: "Sem solicitação",
  PROCESSING: "Em processamento",
  COMPLETED: "Concluída",
  FAILED: "Com erro",
  INVALIDATED: "Dependência alterada",
};

const STATUS_BADGES: Record<RequestUiStatus, string> = {
  IDLE: "secondary",
  PROCESSING: "warning",
  COMPLETED: "success",
  FAILED: "danger",
  INVALIDATED: "dark",
};

const STAGE_LABELS: Record<string, string> = {
  WAITING_AI_WORKER: "Aguardando AI Worker",
  SENT_TO_OPENAI: "Enviada para OpenAI",
  WAITING_OPENAI: "Aguardando resposta da OpenAI",
  COMPLETED: "Finalizada",
  FAILED: "Falhou",
};

const SECTION_REQUEST_INITIAL_STATE: Record<
  HypothesisFrameworkSection,
  SectionRequestState
> = {
  pain: { status: "IDLE" },
  result: { status: "IDLE" },
  mechanism: { status: "IDLE" },
  proof: { status: "IDLE" },
  offer: { status: "IDLE" },
};

function normalizeFrameworkSection(value?: string): HypothesisFrameworkSection | undefined {
  if (!value) return undefined;
  const normalized = value.toLowerCase() as HypothesisFrameworkSection;
  return SECTIONS.find((section) => section.id === normalized)?.id;
}


function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function parseTimestamp(value?: string) {
  if (!value) return undefined;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function getReferenceTimestamp(request: SectionRequestState) {
  return (
    parseTimestamp(request.completedAt) ?? parseTimestamp(request.requestedAt)
  );
}

function getWorkerStatus(request: SectionRequestState) {
  if (request.status === "FAILED") {
    return {
      label: "Worker IA retornou erro",
      badge: "danger",
    };
  }

  if (request.status === "COMPLETED") {
    return {
      label: "Worker IA concluiu com sucesso",
      badge: "success",
    };
  }

  if (
    request.status === "PROCESSING" ||
    request.startedAt ||
    request.stageLabel
  ) {
    return {
      label: "Worker IA em processamento",
      badge: "warning",
    };
  }

  return {
    label: "Worker IA ainda não acionado",
    badge: "secondary",
  };
}

function isSummaryJob(requestBodyJson?: string) {
  return requestBodyJson?.includes("_summary") ?? false;
}

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const responseMessage =
      typeof error.response?.data?.message === "string"
        ? error.response.data.message
        : undefined;
    return (
      responseMessage ?? error.message ?? "Falha ao processar solicitação."
    );
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Falha ao processar solicitação.";
}

function getGenerationReportMetadata(domain?: string) {
  if (!domain?.startsWith("hypothesis.framework.")) {
    return undefined;
  }

  const suffix = domain.replace("hypothesis.framework.", "");
  const isSummary = suffix.endsWith(".summary");
  const sectionId = (isSummary ? suffix.replace(".summary", "") : suffix) as
    | HypothesisFrameworkSection
    | string;

  if (!REPORT_SECTION_ORDER.includes(sectionId as HypothesisFrameworkSection)) {
    return undefined;
  }

  const sectionOrder = REPORT_SECTION_ORDER.indexOf(
    sectionId as HypothesisFrameworkSection,
  );

  return {
    sectionId: sectionId as HypothesisFrameworkSection,
    sectionOrder,
    generationTypeLabel: isSummary
      ? "Geração de resumo"
      : "Geração de subitens",
    generationOrder: isSummary ? 1 : 0,
  };
}

export function HypothesisFrameworkTabsView({
  hypothesisId,
  nicheId,
  nicheName,
  framework,
  onRefresh,
}: Props) {
  const data = normalizeFramework(framework);
  const [tab, setTab] = useState<HypothesisFrameworkSection>("pain");
  const [instructions, setInstructions] = useState<
    Record<HypothesisFrameworkSection, string>
  >({
    pain: "",
    result: "",
    mechanism: "",
    proof: "",
    offer: "",
  });
  const [pendingSection, setPendingSection] = useState<
    HypothesisFrameworkSection | undefined
  >();
  const [pendingSummarySection, setPendingSummarySection] = useState<
    HypothesisFrameworkSection | undefined
  >();
  const [isDownloadingReport, setIsDownloadingReport] = useState(false);
  const [requestsBySection, setRequestsBySection] = useState<
    Record<HypothesisFrameworkSection, SectionRequestState>
  >(SECTION_REQUEST_INITIAL_STATE);
  const jobsQuery = useFrameworkGenerationJobs(hypothesisId);
  const generate = useGenerateFrameworkSection(hypothesisId, nicheId);

  const requestsFromBackendByKind = useMemo(() => {
    const grouped: Record<
      RequestKind,
      Record<HypothesisFrameworkSection, SectionRequestState>
    > = {
      FULL: { ...SECTION_REQUEST_INITIAL_STATE },
      SUMMARY: { ...SECTION_REQUEST_INITIAL_STATE },
    };

    (jobsQuery.data ?? []).forEach((job) => {
      const sectionId = normalizeFrameworkSection(job.section as string);
      if (!sectionId) {
        return;
      }
      const kind: RequestKind = isSummaryJob(job.requestBodyJson)
        ? "SUMMARY"
        : "FULL";
      if (grouped[kind][sectionId]?.requestedAt) {
        return;
      }
      const uiStatus: RequestUiStatus =
        job.status === "COMPLETED"
          ? "COMPLETED"
          : job.status === "FAILED"
            ? "FAILED"
            : "PROCESSING";

      grouped[kind][sectionId] = {
        status: uiStatus,
        requestedAt: job.createdAt,
        startedAt: job.startedAt,
        completedAt: job.finishedAt,
        customInstructions: job.customInstructions,
        errorMessage: job.errorMessage,
        stageLabel:
          STAGE_LABELS[job.stage] ??
          (job.stage ? job.stage.split("_").join(" ") : undefined),
      };
    });

    return grouped;
  }, [jobsQuery.data]);

  const mergedFullRequestsBySection = SECTIONS.reduce<
    Record<HypothesisFrameworkSection, SectionRequestState>
  >(
    (acc, section) => {
      const localState = requestsBySection[section.id];
      const backendState = requestsFromBackendByKind.FULL[section.id];
      acc[section.id] =
        localState.status !== "IDLE" &&
        localState.status !== "COMPLETED" &&
        backendState.status === "IDLE"
          ? localState
          : backendState;
      return acc;
    },
    { ...SECTION_REQUEST_INITIAL_STATE },
  );

  const summaryRequestsBySection = requestsFromBackendByKind.SUMMARY;

  const invalidationBySection = useMemo(() => {
    const bySection: Partial<
      Record<
        HypothesisFrameworkSection,
        { sourceSection: string; sourceAt?: string; sourceTimestamp: number }
      >
    > = {};

    let latestDependency: {
      label: string;
      at?: string;
      timestamp: number;
    } | null = null;

    SECTIONS.forEach((section, index) => {
      const currentRequest = mergedFullRequestsBySection[section.id];
      const currentTimestamp = getReferenceTimestamp(currentRequest);

      if (
        index > 0 &&
        latestDependency &&
        currentRequest.status !== "PROCESSING" &&
        (currentTimestamp === undefined ||
          currentTimestamp < latestDependency.timestamp)
      ) {
        bySection[section.id] = {
          sourceSection: latestDependency.label,
          sourceAt: latestDependency.at,
          sourceTimestamp: latestDependency.timestamp,
        };
      }

      if (currentTimestamp !== undefined) {
        if (
          !latestDependency ||
          currentTimestamp > latestDependency.timestamp
        ) {
          latestDependency = {
            label: section.label,
            at: currentRequest.completedAt ?? currentRequest.requestedAt,
            timestamp: currentTimestamp,
          };
        }
      }
    });

    return bySection;
  }, [mergedFullRequestsBySection]);

  const handleGenerate = async (section: HypothesisFrameworkSection) => {
    const requestedAt = new Date().toISOString();
    const customInstructions = instructions[section]?.trim() ?? "";
    setRequestsBySection((prev) => ({
      ...prev,
      [section]: {
        status: "PROCESSING",
        requestedAt,
        startedAt: undefined,
        completedAt: undefined,
        customInstructions,
        errorMessage: undefined,
      },
    }));

    try {
      setPendingSection(section);
      await generate.mutateAsync({
        section,
        customInstructions,
        mode: "FULL",
      });
      setRequestsBySection((prev) => ({
        ...prev,
        [section]: {
          ...prev[section],
          status: "COMPLETED",
          completedAt: new Date().toISOString(),
          errorMessage: undefined,
        },
      }));
      onRefresh?.();
    } catch (error) {
      setRequestsBySection((prev) => ({
        ...prev,
        [section]: {
          ...prev[section],
          status: "FAILED",
          completedAt: undefined,
          errorMessage: getErrorMessage(error),
        },
      }));
    } finally {
      setPendingSection(undefined);
    }
  };

  const handleGenerateSummary = async (section: HypothesisFrameworkSection) => {
    try {
      setPendingSummarySection(section);
      await generate.mutateAsync({
        section,
        mode: "SUMMARY",
      });
      onRefresh?.();
    } finally {
      setPendingSummarySection(undefined);
    }
  };

  const getSummary = (section: HypothesisFrameworkSection) => {
    switch (section) {
      case "pain":
        return data.pain.summary;
      case "result":
        return data.result.summary;
      case "mechanism":
        return data.mechanism.summary;
      case "proof":
        return data.proof.summary;
      case "offer":
      default:
        return data.offer.summary;
    }
  };

  const getSummaryLabel = (section: HypothesisFrameworkSection) => {
    switch (section) {
      case "pain":
        return "Resumo da dor";
      case "result":
        return "Resumo do resultado";
      case "mechanism":
        return "Resumo do mecanismo";
      case "proof":
        return "Resumo da prova";
      case "offer":
      default:
        return "Resumo da oferta";
    }
  };

  const getSectionLabel = (section: HypothesisFrameworkSection) =>
    SECTIONS.find((item) => item.id === section)?.label ?? section;

  const renderRows = (
    rows: Array<{ label: string; value?: string | null }>,
  ) => (
    <dl className="row">
      {rows.map((row, index) => (
        <Fragment key={`${row.label}-${index}`}>
          <dt className="col-sm-4 text-muted">{row.label}</dt>
          <dd className="col-sm-8">{row.value?.trim() || "-"}</dd>
        </Fragment>
      ))}
    </dl>
  );

  const renderSection = (section: HypothesisFrameworkSection) => {
    switch (section) {
      case "pain":
        return renderRows([
          { label: "Superfície", value: data.pain.surface },
          { label: "Raiz", value: data.pain.root },
          { label: "Dor emocional", value: data.pain.emotional },
          { label: "Dor social", value: data.pain.social },
          { label: "Custo", value: data.pain.cost },
        ]);
      case "result":
        return renderRows([
          { label: "Resultado desejado", value: data.result.desiredResult },
          { label: "Identidade", value: data.result.desiredIdentity },
          { label: "Impacto de negócio", value: data.result.businessOutcome },
          { label: "Sinal de sucesso", value: data.result.successSignal },
        ]);
      case "mechanism":
        return renderRows([
          { label: "Mecanismo central", value: data.mechanism.core },
          { label: "Mecanismo único", value: data.mechanism.unique },
          { label: "O que é visível", value: data.mechanism.visible },
          { label: "Por que acreditar", value: data.mechanism.believability },
        ]);
      case "proof":
        return renderRows([
          { label: "Tipo", value: data.proof.type },
          { label: "Ativo", value: data.proof.asset },
          { label: "Mensagem", value: data.proof.message },
          { label: "Estágio", value: data.proof.deliveryStage },
        ]);
      case "offer":
      default:
        return renderRows([
          { label: "Nome", value: data.offer.name },
          { label: "Promessa", value: data.offer.corePromise },
          { label: "Entregáveis", value: data.offer.deliverables },
          { label: "Risco", value: data.offer.riskReversal },
          { label: "Narrativa de preço", value: data.offer.priceLogic },
          {
            label: "Preço",
            value:
              typeof data.offer.priceAmount === "number"
                ? `R$ ${data.offer.priceAmount.toFixed(2)}`
                : undefined,
          },
          { label: "CTA", value: data.offer.cta },
        ]);
    }
  };

  const getSectionFromDomain = (domain?: string) => {
    const suffix = domain?.split(".").pop();
    return (
      SECTIONS.find((section) => section.id === suffix)?.label ??
      domain ??
      "Seção"
    );
  };

  const handleDownloadReport = async () => {
    try {
      setIsDownloadingReport(true);
      const { data: response } = await axios.get<
        PageResponse<AiGenerationRecord>
      >("/api/ai/generations", {
        params: {
          referenceId: hypothesisId,
          size: 100,
        },
      });

      const frameworkGenerations: FrameworkGenerationReportRecord[] = (
        response.content ?? []
      )
        .map((item) => ({
          ...item,
          metadata: getGenerationReportMetadata(item.domain),
        }))
        .filter(
          (
            item,
          ): item is FrameworkGenerationReportRecord => item.metadata != null,
        )
        .sort((a, b) => {
          if (a.metadata.sectionOrder !== b.metadata.sectionOrder) {
            return a.metadata.sectionOrder - b.metadata.sectionOrder;
          }
          if (a.metadata.generationOrder !== b.metadata.generationOrder) {
            return a.metadata.generationOrder - b.metadata.generationOrder;
          }
          return (
            (parseTimestamp(a.createdAt) ?? Number.MAX_SAFE_INTEGER) -
            (parseTimestamp(b.createdAt) ?? Number.MAX_SAFE_INTEGER)
          );
        });

      const reportLines = [
        "# Relatório consolidado do framework Dor-Resultado-Oferta",
        "",
        `Nome do Nicho: ${nicheName ?? "Não informado"}`,
        "",
        "## Prompts e Respostas de cada geração de IA",
        ...(frameworkGenerations.length === 0
          ? [
              "Nenhuma geração de IA do framework foi encontrada para esta hipótese.",
            ]
          : frameworkGenerations.flatMap((generation, index) => [
              `### ${index + 1}. ${getSectionFromDomain(generation.domain)} — ${generation.metadata.generationTypeLabel}`,
              `- Data: ${formatDateTime(generation.createdAt)}`,
              `- Modelo: ${generation.model ?? "Não informado"}`,
              "",
              "**Prompt**",
              "```text",
              generation.prompt?.trim() || "Sem prompt registrado.",
              "```",
              "",
              "**Resposta**",
              "```text",
              generation.rawResponse?.trim() || "Sem resposta registrada.",
              "```",
              "",
            ])),
        "",
        "## Descrição dos itens",
        "",
        "### Dor",
        `- Superfície: ${data.pain.surface?.trim() || "-"}`,
        `- Raiz: ${data.pain.root?.trim() || "-"}`,
        `- Dor emocional: ${data.pain.emotional?.trim() || "-"}`,
        `- Dor social: ${data.pain.social?.trim() || "-"}`,
        `- Custo: ${data.pain.cost?.trim() || "-"}`,
        "",
        "### Resultado",
        `- Resultado desejado: ${data.result.desiredResult?.trim() || "-"}`,
        `- Identidade: ${data.result.desiredIdentity?.trim() || "-"}`,
        `- Impacto de negócio: ${data.result.businessOutcome?.trim() || "-"}`,
        `- Sinal de sucesso: ${data.result.successSignal?.trim() || "-"}`,
        "",
        "### Mecanismo",
        `- Mecanismo central: ${data.mechanism.core?.trim() || "-"}`,
        `- Mecanismo único: ${data.mechanism.unique?.trim() || "-"}`,
        `- O que é visível: ${data.mechanism.visible?.trim() || "-"}`,
        `- Por que acreditar: ${data.mechanism.believability?.trim() || "-"}`,
        "",
        "### Prova",
        `- Tipo: ${data.proof.type?.trim() || "-"}`,
        `- Ativo: ${data.proof.asset?.trim() || "-"}`,
        `- Mensagem: ${data.proof.message?.trim() || "-"}`,
        `- Estágio: ${data.proof.deliveryStage?.trim() || "-"}`,
        "",
        "### Oferta",
        `- Nome: ${data.offer.name?.trim() || "-"}`,
        `- Promessa: ${data.offer.corePromise?.trim() || "-"}`,
        `- Entregáveis: ${data.offer.deliverables?.trim() || "-"}`,
        `- Risco: ${data.offer.riskReversal?.trim() || "-"}`,
        `- Narrativa de preço: ${data.offer.priceLogic?.trim() || "-"}`,
        `- CTA: ${data.offer.cta?.trim() || "-"}`,
      ];

      const markdown = reportLines.join("\n");
      const blob = new Blob([markdown], { type: "text/markdown" });
      const url = URL.createObjectURL(blob);
      const downloadLink = document.createElement("a");
      downloadLink.href = url;
      downloadLink.download = `relatorio-framework-${hypothesisId}.md`;
      downloadLink.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setIsDownloadingReport(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header d-flex flex-column flex-lg-row gap-2 align-items-lg-center justify-content-lg-between">
        <div>
          <h3 className="h6 mb-0">Framework Dor → Resultado → Oferta</h3>
          <small className="text-muted">
            Revise ou gere novamente cada seção antes de aprovar a hipótese.
          </small>
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm align-self-start align-self-lg-center"
          onClick={handleDownloadReport}
          disabled={isDownloadingReport}
        >
          {isDownloadingReport ? (
            <span className="d-inline-flex align-items-center gap-1">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Gerando relatório...
            </span>
          ) : (
            "Baixar relatório consolidado"
          )}
        </button>
      </div>
      <div className="card-body">
        <Tabs.Root
          value={tab}
          onValueChange={(value) => setTab(value as HypothesisFrameworkSection)}
        >
          <Tabs.List className="nav nav-tabs mb-3">
            {SECTIONS.map((section) => (
              <Tabs.Trigger
                key={section.id}
                value={section.id}
                className={`nav-link ${tab === section.id ? "active" : ""}`}
              >
                {section.label}
              </Tabs.Trigger>
            ))}
          </Tabs.List>
          {SECTIONS.map((section) => (
            <Tabs.Content key={section.id} value={section.id}>
              {renderSection(section.id)}
              <div className="border rounded-3 bg-light-subtle p-3 mt-3">
                <div className="small text-muted text-uppercase fw-semibold mb-1">
                  {getSummaryLabel(section.id)}
                </div>
                <div>{getSummary(section.id)?.trim() || "-"}</div>
              </div>
              <div className="d-flex flex-column flex-md-row gap-2 mt-3">
                <textarea
                  className="form-control"
                  rows={2}
                  placeholder="Instruções extras para a IA (opcional)"
                  title="Escreva contexto adicional para guiar a IA nesta seção."
                  value={instructions[section.id]}
                  onChange={(event) =>
                    setInstructions((prev) => ({
                      ...prev,
                      [section.id]: event.target.value,
                    }))
                  }
                />
                <button
                  type="button"
                  className="btn btn-outline-primary align-self-start"
                  onClick={() => handleGenerate(section.id)}
                  disabled={generate.isPending}
                  title={`Gera os campos completos de ${getSectionLabel(section.id)} usando o Worker IA.`}
                >
                  {pendingSection === section.id && generate.isPending ? (
                    <span className="d-inline-flex align-items-center gap-1">
                      <Loader2 className="icon icon-sm spin" /> Gerando...
                    </span>
                  ) : (
                    `Gerar ${getSectionLabel(section.id)} com IA`
                  )}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary align-self-start"
                  onClick={() => handleGenerateSummary(section.id)}
                  disabled={generate.isPending}
                  title={`Gera somente o resumo de ${getSectionLabel(section.id)} com o mesmo modelo do framework.`}
                >
                  {pendingSummarySection === section.id && generate.isPending ? (
                    <span className="d-inline-flex align-items-center gap-1">
                      <Loader2 className="icon icon-sm spin" /> Gerando resumo...
                    </span>
                  ) : (
                    `Gerar resumo de ${getSectionLabel(section.id)}`
                  )}
                </button>
              </div>
            </Tabs.Content>
          ))}
        </Tabs.Root>

        <div className="border rounded-3 p-3 mt-4">
          <h4 className="h6 mb-2">Acompanhamento das solicitações IA</h4>
          <p className="small text-muted mb-3">
            Veja o que já foi solicitado, o que está em processamento e quais
            etapas precisam ser geradas novamente quando uma dependência
            anterior muda.
          </p>
          {jobsQuery.isLoading ? (
            <p className="small text-muted mb-3">
              Carregando histórico salvo das solicitações...
            </p>
          ) : null}
          {jobsQuery.isError ? (
            <p className="small text-danger mb-3">
              Não foi possível carregar o histórico das solicitações salvas.
            </p>
          ) : null}
          <div className="d-flex flex-column gap-2">
            {SECTIONS.map((section) => {
              const request = mergedFullRequestsBySection[section.id];
              const invalidation = invalidationBySection[section.id];
              const displayStatus: RequestUiStatus =
                request.status === "PROCESSING"
                  ? "PROCESSING"
                  : invalidation
                    ? "INVALIDATED"
                    : request.status;
              const workerStatus = getWorkerStatus(request);

              return (
                <div
                  key={`request-status-${section.id}`}
                  className="border rounded-2 p-2 d-flex flex-column gap-1"
                >
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <strong>{section.label}</strong>
                    <span
                      className={`badge text-bg-${STATUS_BADGES[displayStatus]}`}
                    >
                      {STATUS_LABELS[displayStatus]}
                    </span>
                  </div>
                  <small className="text-muted">
                    Solicitado em: {formatDateTime(request.requestedAt)} ·
                    Concluído em: {formatDateTime(request.completedAt)}
                  </small>
                  <div className="small d-flex flex-column gap-1">
                    <div>
                      <strong>1. Solicitação do usuário:</strong>{" "}
                      {request.requestedAt
                        ? `enviada em ${formatDateTime(request.requestedAt)}.`
                        : "ainda não enviada."}
                    </div>
                    <div className="d-flex flex-wrap align-items-center gap-2">
                      <strong>2. Atendimento do Worker IA:</strong>
                      <span className={`badge text-bg-${workerStatus.badge}`}>
                        {workerStatus.label}
                      </span>
                      <span>
                        {request.startedAt
                          ? `iniciado em ${formatDateTime(request.startedAt)}`
                          : "sem início registrado"}
                      </span>
                    </div>
                    <div>
                      <strong>3. Conclusão:</strong>{" "}
                      {request.completedAt
                        ? `finalizada em ${formatDateTime(request.completedAt)}.`
                        : request.status === "FAILED"
                          ? "fluxo encerrado com erro."
                          : "aguardando finalização."}
                    </div>
                  </div>
                  {request.stageLabel ? (
                    <small className="text-body-secondary">
                      Etapa atual: {request.stageLabel}
                    </small>
                  ) : null}
                  {request.customInstructions ? (
                    <small className="text-body-secondary">
                      Instruções enviadas: {request.customInstructions}
                    </small>
                  ) : null}
                  {invalidation ? (
                    <small className="text-warning-emphasis">
                      Dependência alterada em {invalidation.sourceSection} (
                      {formatDateTime(invalidation.sourceAt)}). Gere esta etapa
                      novamente para sincronizar o framework.
                    </small>
                  ) : null}
                  {request.errorMessage ? (
                    <small className="text-danger">
                      Erro: {request.errorMessage}
                    </small>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>

        <div className="border rounded-3 p-3 mt-3">
          <h4 className="h6 mb-2">Acompanhamento das solicitações de resumo IA</h4>
          <p className="small text-muted mb-3">
            Histórico separado das solicitações que geram somente o resumo de
            cada item do framework.
          </p>
          {jobsQuery.isLoading ? (
            <p className="small text-muted mb-3">
              Carregando histórico salvo dos resumos...
            </p>
          ) : null}
          {jobsQuery.isError ? (
            <p className="small text-danger mb-3">
              Não foi possível carregar o histórico dos resumos salvos.
            </p>
          ) : null}
          <div className="d-flex flex-column gap-2">
            {SECTIONS.map((section) => {
              const request = summaryRequestsBySection[section.id];
              const workerStatus = getWorkerStatus(request);

              return (
                <div
                  key={`summary-request-status-${section.id}`}
                  className="border rounded-2 p-2 d-flex flex-column gap-1"
                >
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <strong>{section.label}</strong>
                    <span className={`badge text-bg-${STATUS_BADGES[request.status]}`}>
                      {STATUS_LABELS[request.status]}
                    </span>
                  </div>
                  <small className="text-muted">
                    Solicitado em: {formatDateTime(request.requestedAt)} ·
                    Concluído em: {formatDateTime(request.completedAt)}
                  </small>
                  <div className="small d-flex flex-column gap-1">
                    <div>
                      <strong>1. Solicitação do usuário:</strong>{" "}
                      {request.requestedAt
                        ? `enviada em ${formatDateTime(request.requestedAt)}.`
                        : "ainda não enviada."}
                    </div>
                    <div className="d-flex flex-wrap align-items-center gap-2">
                      <strong>2. Atendimento do Worker IA:</strong>
                      <span className={`badge text-bg-${workerStatus.badge}`}>
                        {workerStatus.label}
                      </span>
                      <span>
                        {request.startedAt
                          ? `iniciado em ${formatDateTime(request.startedAt)}`
                          : "sem início registrado"}
                      </span>
                    </div>
                    <div>
                      <strong>3. Conclusão:</strong>{" "}
                      {request.completedAt
                        ? `finalizada em ${formatDateTime(request.completedAt)}.`
                        : request.status === "FAILED"
                          ? "fluxo encerrado com erro."
                          : "aguardando finalização."}
                    </div>
                  </div>
                  {request.stageLabel ? (
                    <small className="text-body-secondary">
                      Etapa atual: {request.stageLabel}
                    </small>
                  ) : null}
                  {request.customInstructions ? (
                    <small className="text-body-secondary">
                      Instruções enviadas: {request.customInstructions}
                    </small>
                  ) : null}
                  {request.errorMessage ? (
                    <small className="text-danger">
                      Erro: {request.errorMessage}
                    </small>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>

        <hr className="my-4" />
        <h4 className="h6">Checklist de aprovação</h4>
        <dl className="row mb-0">
          <dt className="col-sm-4">Dor validada</dt>
          <dd className="col-sm-8">
            {data.checklist.painReady ? "Sim" : "Não"}
          </dd>
          <dt className="col-sm-4">Resultado claro</dt>
          <dd className="col-sm-8">
            {data.checklist.resultReady ? "Sim" : "Não"}
          </dd>
          <dt className="col-sm-4">Mecanismo pronto</dt>
          <dd className="col-sm-8">
            {data.checklist.mechanismReady ? "Sim" : "Não"}
          </dd>
          <dt className="col-sm-4">Prova definida</dt>
          <dd className="col-sm-8">
            {data.checklist.proofReady ? "Sim" : "Não"}
          </dd>
          <dt className="col-sm-4">Oferta empacotada</dt>
          <dd className="col-sm-8">
            {data.checklist.offerReady ? "Sim" : "Não"}
          </dd>
          <dt className="col-sm-4">Liberado para experimento</dt>
          <dd className="col-sm-8">
            {data.checklist.approvedForExperiment ? "Sim" : "Não"}
          </dd>
          {data.checklist.notes && (
            <>
              <dt className="col-sm-4">Notas</dt>
              <dd className="col-sm-8">{data.checklist.notes}</dd>
            </>
          )}
        </dl>
      </div>
    </div>
  );
}
