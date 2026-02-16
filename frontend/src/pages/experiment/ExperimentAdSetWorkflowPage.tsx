import { useMemo, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useQueries } from "@tanstack/react-query";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { ExperimentAdSetJobDetailDto } from "../../api/experiment/useExperimentAdSetJobDetail";
import {
  ExperimentAdSetWorkflowDto,
  ExperimentAdSetSpec,
  ExperimentAdSetJob,
  useExperimentAdSetWorkflow,
  useStartExperimentAdSetWorkflow,
} from "../../api/experiment/useExperimentAdSetWorkflow";

const STATUS_VARIANT: Record<string, string> = {
  NOT_STARTED: "secondary",
  RUNNING: "info",
  COMPLETED: "success",
  FAILED: "danger",
};

const SLOT_LABELS: Record<string, string> = {
  DESIGNERS: "Designers / Criadores",
  MARKETING: "Marketing / Social Media",
  SMB: "Decisores SMB",
  UNKNOWN: "Público",
};

const JOB_TYPE_INFO: Record<
  string,
  { title: string; description: string; docStep: string }
> = {
  AI_PREPARE_SEED: {
    title: "Planejar seed (IA)",
    description: "Lê o ICP do experimento e escolhe keyword/search terms",
    docStep: "Docs pipeline-3 · Etapa 2",
  },
  FACEBOOK_SEED_LOOKUP: {
    title: "Discovery (Meta)",
    description: "Meta roda /targetingsearch e /search (limit 200) para gerar catálogo de IDs",
    docStep: "Docs pipeline-3 · Etapa 3",
  },
  FACEBOOK_SOCIAL_POSITIONS: {
    title: "Targeting Search (cargos)",
    description: "Busca cargos sociais (adworkposition)",
    docStep: "Docs pipeline-3 · Etapa 3 (cargos opcionais)",
  },
  FACEBOOK_TARGETING_SUGGESTIONS: {
    title: "Targeting Suggestions",
    description: "Expande o interest anchor com o ecossistema do Meta",
    docStep: "Docs pipeline-3 · Etapa 5",
  },
  AI_BUILD_SPECS: {
    title: "Montar 3 públicos (IA)",
    description:
      "IA Worker cria flexible_spec para Designers · Marketing · SMB",
    docStep: "Docs pipeline-3 · Etapas 6-8",
  },
  FACEBOOK_VALIDATE_SPEC: {
    title: "Targeting Validation",
    description: "Meta /targetingvalidation para checar IDs oficiais",
    docStep: "Docs pipeline-3 · Etapa 9 (opcional)",
  },
  FACEBOOK_REACH_ESTIMATE: {
    title: "Reach Estimate (BR)",
    description: "Meta /reachestimate para calibrar o tamanho",
    docStep: "Docs pipeline-3 · Etapa 10",
  },
  AI_RECALIBRATE_SPEC: {
    title: "Recalibração automática",
    description: "IA ajusta idade e blocos quando reach foge de 200k-20M",
    docStep: "Docs pipeline-3 · Etapa 11",
  },
};

const PIPELINE_DOC_PATH = "docs/pipeline-3-publicos-meta-ads.md";
const REACH_MIN = 200_000;
const REACH_MAX = 20_000_000;
const ICP_REFERENCE_TEXT = `Produto: Marketing Hub — gera imagens e criativos por IA a partir de foto enviada pelo cliente.
Mercado: Brasil.
Quem compra: pequenos negócios, social medias, designers, empreendedores.
Uso: posts, anúncios, peças promocionais e diversão.
Dores: falta de tempo/habilidade para criar imagens consistentes; custo de designer; velocidade.
Canais: Instagram, Facebook, tráfego pago.`;

type StepStatus =
  | "PENDING"
  | "RUNNING"
  | "DONE"
  | "FAILED"
  | "WARNING"
  | "SKIPPED";

const STEP_STATUS_META: Record<StepStatus, { label: string; variant: string }> =
  {
    PENDING: { label: "Aguardando", variant: "secondary" },
    RUNNING: { label: "Em execução", variant: "info" },
    DONE: { label: "Concluído", variant: "success" },
    FAILED: { label: "Com erro", variant: "danger" },
    WARNING: { label: "Atenção", variant: "warning" },
    SKIPPED: { label: "Ignorado", variant: "light" },
  };

type PipelineStepSummary = {
  id: string;
  title: string;
  description: string;
  status: StepStatus;
  optional?: boolean;
  detail?: ReactNode;
  helper?: ReactNode;
  jobLinks?: PipelineJobLink[];
};

type PipelineJobLink = {
  jobId: number;
  label: string;
};

type DiscoveryTopItem = {
  id?: string;
  name?: string;
  type?: string;
  audienceLower?: number | null;
  audienceUpper?: number | null;
  sources?: string[];
  terms?: string[];
};

type DiscoverySummary = {
  seeds: string[];
  locales: string[];
  stats?: { rawCalls?: number; rawItems?: number; dedupItems?: number; byType?: Record<string, number> };
  topItems: DiscoveryTopItem[];
  jobId?: number;
};

type TargetingResolutionByTerm = {
  term: string;
  ids: string[];
};

type TargetingSearchResolvedTerms = {
  interests: TargetingResolutionByTerm[];
  workPositions: TargetingResolutionByTerm[];
  behaviors: TargetingResolutionByTerm[];
};

export default function ExperimentAdSetWorkflowPage() {
  const { id } = useParams();
  const experimentId = id ?? "";
  const { data, isLoading } = useExperimentAdSetWorkflow(experimentId);
  const startWorkflow = useStartExperimentAdSetWorkflow(experimentId);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Workflow não encontrado.</p>;

  const statusVariant = STATUS_VARIANT[data.status] ?? "secondary";
  const canStart = data.status === "NOT_STARTED";
  const canRestart = data.status === "FAILED" || data.status === "COMPLETED";
  const buttonLabel = canStart ? "Iniciar roteiro" : "Reiniciar roteiro";

  return (
    <div className="container-fluid">
      <div className="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
        <PageTitle
          title="Playbook de Ad Sets"
          subtitle={
            <span>
              Experimento{" "}
              <Link to={`/experiments/${experimentId}`}>#{experimentId}</Link>
            </span>
          }
        />
        <div className="d-flex gap-2">
          <span
            className={`badge text-bg-${statusVariant} align-self-center px-3 py-2`}
          >
            {data.status}
          </span>
          <button
            type="button"
            className="btn btn-primary"
            disabled={
              startWorkflow.isPending ||
              (!canStart && !canRestart) ||
              data.status === "RUNNING"
            }
            onClick={() => startWorkflow.mutate(canRestart)}
          >
            {startWorkflow.isPending ? "Processando..." : buttonLabel}
          </button>
        </div>
      </div>

      <PipelineDocExplainer />

      {data.lastError ? (
        <div className="alert alert-danger" role="alert">
          <strong>Último erro:</strong> {data.lastError}
        </div>
      ) : null}

      <div className="row g-4 mb-4">
        <div className="col-12 col-xl-4">
          <SeedCard workflow={data} />
        </div>
        <div className="col-12 col-xl-8">
          <PipelineTimeline workflow={data} />
        </div>
      </div>

      <div className="row g-4 mb-4">
        <div className="col-12">
          <SpecsCard specs={data.specs} />
        </div>
      </div>

      <JobsCard jobs={data.jobs} />
    </div>
  );
}

function PipelineDocExplainer() {
  return (
    <div className="alert alert-info mb-4" role="alert">
      <div className="fw-semibold">Fluxo oficial · 3 públicos Meta Ads</div>
      <p className="mb-2 small">
        Entrada: icp.md → IA Worker decide seeds/arquétipos → Facebook Ads
        Worker executa chamadas até reach READY.
      </p>
      <ul className="mb-2 small ps-3">
        <li>
          Saída: spec_1.json…spec_3.json + reach_1.json…reach_3.json e toda a
          trilha de auditoria.
        </li>
        <li>
          IA Worker nunca chuta IDs; todo ID vem da Graph API via Ads Worker.
        </li>
        <li>
          Jobs com worker = AI acionam ChatGPT em batch; jobs com worker =
          FACEBOOK gravam cada request da Graph API.
        </li>
        <li>
          Ordem: Targeting Search → Suggestions → flexible_spec → Validation →
          Reach → (opcional) criação de Ad Sets.
        </li>
      </ul>
      <div className="small text-muted mb-1">
        Guia completo: {PIPELINE_DOC_PATH}
      </div>
      <div className="small text-muted">
        Use o cartão "Histórico de jobs" e clique em <strong>Detalhe</strong>{" "}
        para ver as chamadas cruas (Graph API ou ChatGPT batch).
      </div>
    </div>
  );
}

function SeedCard({ workflow }: { workflow: ExperimentAdSetWorkflowDto }) {
  const aiPlan = safeJsonParse<{
    searchTerms?: string[];
    positionQueries?: string[];
    interests?: string[];
    work_positions?: string[];
    behaviors?: string[];
  }>(workflow.aiNotes);
  const searchTerms = collectStrings(aiPlan?.searchTerms ?? aiPlan?.interests);
  const positionQueries = collectStrings(
    aiPlan?.positionQueries ?? aiPlan?.work_positions,
  );
  const behaviorQueries = collectStrings(aiPlan?.behaviors);
  return (
    <div className="card h-100">
      <div className="card-header">
        <div className="fw-semibold">Seed atual</div>
        <small className="text-muted">
          Referência: {PIPELINE_DOC_PATH} · Etapas 1-4
        </small>
      </div>
      <div className="card-body">
        <dl className="row mb-3">
          <dt className="col-sm-5">Palavra-chave</dt>
          <dd className="col-sm-7">{workflow.seedKeyword ?? "—"}</dd>
          <dt className="col-sm-5">Locale</dt>
          <dd className="col-sm-7">{workflow.seedLocale ?? "—"}</dd>
          <dt className="col-sm-5">Interesse</dt>
          <dd className="col-sm-7">{workflow.seedInterestName ?? "—"}</dd>
          <dt className="col-sm-5">Audience (lower)</dt>
          <dd className="col-sm-7">
            {formatNumber(workflow.seedAudienceLower)}
          </dd>
          <dt className="col-sm-5">Audience (upper)</dt>
          <dd className="col-sm-7">
            {formatNumber(workflow.seedAudienceUpper)}
          </dd>
        </dl>
        <SectionLabel>Termos usados no Targeting Search</SectionLabel>
        <BadgeList
          items={searchTerms}
          placeholder="IA ainda não devolveu os termos"
        />
        <SectionLabel className="mt-3">Cargos consultados no Meta</SectionLabel>
        <BadgeList
          items={positionQueries}
          placeholder="Sem queries adicionais"
        />
        {workflow.aiNotes ? (
          <details className="mt-3">
            <summary>Ver JSON bruto dessa etapa</summary>
            <pre
              className="mt-2 small bg-light p-2 rounded overflow-auto"
              style={{ maxHeight: 220 }}
            >
              {formatJson(workflow.aiNotes)}
            </pre>
          </details>
        ) : null}
      </div>
    </div>
  );
}

function SpecsCard({ specs }: { specs: ExperimentAdSetSpec[] }) {
  return (
    <div className="card border-0 shadow-sm h-100">
      <div className="card-body">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <h2 className="h6 mb-0">Públicos gerados</h2>
          <span className="badge text-bg-light border text-muted">
            {specs.length} specs
          </span>
        </div>

        {!specs.length ? (
          <p className="text-muted mb-0 small">
            Nenhum público gerado até o momento.
          </p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Slot</th>
                  <th>Faixa etária</th>
                  <th>Validação</th>
                  <th>Reach</th>
                </tr>
              </thead>
              <tbody>
                {specs.map((spec) => (
                  <tr key={spec.id}>
                    <td>{slotLabel(spec.slot)}</td>
                    <td>
                      {spec.ageMin ?? "—"} - {spec.ageMax ?? "—"}
                    </td>
                    <td>
                      <span
                        className={`badge text-bg-${statusToVariant(
                          spec.validationStatus,
                        )}`}
                      >
                        {spec.validationStatus ?? "—"}
                      </span>
                    </td>
                    <td>
                      <div className="d-flex flex-column gap-1">
                        <span
                          className={`badge text-bg-${statusToVariant(
                            spec.reachStatus,
                          )} align-self-start`}
                        >
                          {spec.reachStatus ?? "—"}
                        </span>
                        <span className="small text-muted">
                          {formatNumber(spec.reachLowerBound)} – {formatNumber(spec.reachUpperBound)}
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function PipelineTimeline({
  workflow,
}: {
  workflow: ExperimentAdSetWorkflowDto;
}) {
  const jobsForResolution = useMemo(
    () =>
      (workflow.jobs ?? [])
        .filter(
          (job) =>
            job.status === "SUCCEEDED" &&
            (job.type === "FACEBOOK_SEED_LOOKUP" ||
              job.type === "FACEBOOK_SOCIAL_POSITIONS"),
        )
        .sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
        .slice(0, 2),
    [workflow.jobs],
  );

  const jobDetailQueries = useQueries({
    queries: jobsForResolution.map((job) => ({
      queryKey: [
        "experiment-adset-job-detail",
        workflow.experimentId,
        String(job.id),
      ],
      queryFn: async () => {
        const { data } = await axios.get<ExperimentAdSetJobDetailDto>(
          `/api/experiments/${workflow.experimentId}/adset-playbook/jobs/${job.id}`,
        );
        return data;
      },
      enabled: Boolean(workflow.experimentId && job.id),
      staleTime: 30_000,
    })),
  });

  const resolvedTerms = useMemo(
    () =>
      parseResolvedTermsFromJobDetails(
        jobDetailQueries.map((query) => query.data),
      ),
    [jobDetailQueries],
  );

  const discoveryDetail = jobDetailQueries.find(
    (query) => query.data?.job?.type === "FACEBOOK_SEED_LOOKUP",
  )?.data;
  const discoveryResultJson = useMemo(
    () => buildDiscoveryResultJson(jobDetailQueries.map((query) => query.data)),
    [jobDetailQueries],
  );
  const discoverySummary = useMemo(
    () => parseDiscoverySummaryFromJobDetail(discoveryDetail),
    [discoveryDetail],
  );

  const steps = useMemo(
    () =>
      buildPipelineSteps(
        workflow,
        resolvedTerms,
        discoverySummary,
        discoveryResultJson,
      ),
    [workflow, resolvedTerms, discoverySummary, discoveryResultJson],
  );
  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-column gap-1">
        <div className="fw-semibold">
          Pipeline dos 3 públicos (Meta Ads API)
        </div>
        <small className="text-muted">
          Sequência oficial · {PIPELINE_DOC_PATH} (Etapas 1–12)
        </small>
      </div>
      <div className="card-body">
        <div className="row g-3">
          {steps.map((step) => (
            <div key={step.id} className="col-12 col-lg-6">
              <PipelineStepCard step={step} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function PipelineStepCard({ step }: { step: PipelineStepSummary }) {
  return (
    <div className="border rounded p-3 h-100 d-flex flex-column">
      <div className="d-flex justify-content-between align-items-start gap-2">
        <div>
          <div className="fw-semibold">{step.title}</div>
          <div className="text-muted small">{step.description}</div>
        </div>
        <StatusBadge status={step.status} />
      </div>
      <div className="mt-3 flex-grow-1 small">
        {step.detail ?? <span className="text-muted">Sem dados desta etapa.</span>}
      </div>
      {step.jobLinks?.length ? (
        <div className="mt-3 d-flex flex-wrap gap-2">
          {step.jobLinks.map((link) => (
            <Link
              key={`${step.id}-${link.jobId}`}
              to={`jobs/${link.jobId}`}
              className="btn btn-sm btn-outline-secondary"
            >
              {link.label}
            </Link>
          ))}
        </div>
      ) : null}
      {step.helper ? (
        <div className="mt-3 text-muted small">{step.helper}</div>
      ) : null}
    </div>
  );
}

function SpecCardItem({ spec }: { spec: ExperimentAdSetSpec }) {
  const validationSummary = extractValidationSummary(spec.validationResponse);
  const reachSummary = extractReachSummary(spec.reachResponse);
  const reachOutOfRange = isSpecReachOutOfRange(spec);
  return (
    <div className="border rounded h-100 p-3 d-flex flex-column">
      <div className="d-flex justify-content-between align-items-start mb-2 gap-2">
        <div>
          <div className="fw-semibold">{slotLabel(spec.slot)}</div>
          <div className="small text-muted">{spec.label ?? "Sem rótulo"}</div>
        </div>
        <div className="text-end d-flex flex-column gap-2">
          <span
            className={`badge text-bg-${statusToVariant(spec.validationStatus)}`}
          >
            Validação: {spec.validationStatus ?? "PENDENTE"}
          </span>
          <span
            className={`badge text-bg-${statusToVariant(spec.reachStatus)}`}
          >
            Reach: {spec.reachStatus ?? "—"}
          </span>
        </div>
      </div>
      <dl className="row mb-2 small">
        <dt className="col-6">Idade mínima</dt>
        <dd className="col-6">{spec.ageMin ?? "—"}</dd>
        <dt className="col-6">Idade máxima</dt>
        <dd className="col-6">{spec.ageMax ?? "—"}</dd>
      </dl>
      <div
        className={`small ${reachOutOfRange ? "text-warning" : "text-muted"}`}
      >
        Alcance estimado:{" "}
        <strong>
          {formatNumber(spec.reachLowerBound)} –{" "}
          {formatNumber(spec.reachUpperBound)}
        </strong>{" "}
        pessoas.
        {reachOutOfRange ? " Fora da faixa recomendada (200k-20M)." : null}
      </div>
      {reachSummary ? (
        <div className="small text-muted mt-1">Meta: {reachSummary}</div>
      ) : null}
      {validationSummary ? (
        <div className="alert alert-warning small py-2 mt-2 mb-0">
          {validationSummary}
        </div>
      ) : null}
      <div className="mt-auto">
        {spec.targetingSpec ? (
          <details className="mt-3">
            <summary>Targeting spec (JSON)</summary>
            <pre
              className="small bg-light p-2 rounded mt-2 overflow-auto"
              style={{ maxHeight: 200 }}
            >
              {formatJson(spec.targetingSpec)}
            </pre>
          </details>
        ) : null}
        {spec.validationResponse ? (
          <details className="mt-2">
            <summary>Resposta do Targeting Validation</summary>
            <pre
              className="small bg-light p-2 rounded mt-2 overflow-auto"
              style={{ maxHeight: 200 }}
            >
              {formatJson(spec.validationResponse)}
            </pre>
          </details>
        ) : null}
        {spec.reachResponse ? (
          <details className="mt-2">
            <summary>Resposta do Reach Estimate</summary>
            <pre
              className="small bg-light p-2 rounded mt-2 overflow-auto"
              style={{ maxHeight: 200 }}
            >
              {formatJson(spec.reachResponse)}
            </pre>
          </details>
        ) : null}
        <div className="small text-muted mt-3">
          Atualizado em {formatDate(spec.updatedAt)}
        </div>
      </div>
    </div>
  );
}

function JobsCard({ jobs }: { jobs: ExperimentAdSetJob[] }) {
  if (!jobs?.length) {
    return (
      <div className="card">
        <div className="card-header">Histórico de jobs</div>
        <div className="card-body text-muted">Nenhum job criado ainda.</div>
      </div>
    );
  }
  return (
    <div className="card">
      <div className="card-header d-flex flex-column gap-1">
        <div className="fw-semibold">Histórico de jobs</div>
        <small className="text-muted">
          Cada job salva as chamadas externas (Graph API para FACEBOOK, ChatGPT
          batch para AI). Clique em "Detalhe" para ver os payloads
          enviados/recebidos.
        </small>
      </div>
      <div className="table-responsive">
        <table className="table table-sm mb-0 align-middle">
          <thead>
            <tr>
              <th>Etapa do pipeline</th>
              <th>Status</th>
              <th>Worker</th>
              <th>Início</th>
              <th>Término</th>
              <th>Erro</th>
              <th>Detalhe</th>
            </tr>
          </thead>
          <tbody>
            {jobs
              .slice()
              .sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
              .map((job) => {
                const info = JOB_TYPE_INFO[job.type ?? ""];
                return (
                  <tr key={job.id}>
                    <td>
                      <div className="fw-semibold">
                        {info?.title ?? job.type ?? "—"}
                      </div>
                      <div className="text-muted small">{job.type}</div>
                      {info?.description ? (
                        <div className="text-muted small">
                          {info.description}
                        </div>
                      ) : null}
                      {info?.docStep ? (
                        <div className="text-muted small">{info.docStep}</div>
                      ) : null}
                    </td>
                    <td>
                      <span
                        className={`badge text-bg-${statusToVariant(job.status)}`}
                      >
                        {job.status}
                      </span>
                    </td>
                    <td>{job.worker}</td>
                    <td>{formatDate(job.startedAt)}</td>
                    <td>{formatDate(job.finishedAt)}</td>
                    <td className="text-danger small">
                      {job.errorMessage ?? ""}
                    </td>
                    <td>
                      <Link
                        to={`jobs/${job.id}`}
                        className="btn btn-link btn-sm px-0"
                      >
                        Detalhe
                      </Link>
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>
      <div className="card-footer text-muted small">
        Referência: {PIPELINE_DOC_PATH}. O link "Detalhe" mostra cada request
        feita ao Facebook Ads.
      </div>
    </div>
  );
}

function buildPipelineSteps(
  workflow: ExperimentAdSetWorkflowDto,
  resolvedTerms: TargetingSearchResolvedTerms,
  discoverySummary?: DiscoverySummary,
  discoveryResultJson?: string,
): PipelineStepSummary[] {
  const aiPlan = safeJsonParse<{
    searchTerms?: string[];
    positionQueries?: string[];
    interests?: string[];
    work_positions?: string[];
    behaviors?: string[];
  }>(workflow.aiNotes);
  const searchTerms = collectStrings(aiPlan?.searchTerms ?? aiPlan?.interests);
  const positionQueries = collectStrings(
    aiPlan?.positionQueries ?? aiPlan?.work_positions,
  );
  const behaviorQueries = collectStrings(aiPlan?.behaviors);
  const jobsByType = groupJobsByType(workflow.jobs);
  const getJobs = (type: string) => jobsByType.get(type) ?? [];
  const specs = workflow.specs ?? [];
  const aiBuildJobs = getJobs("AI_BUILD_SPECS");

  return [
    buildIcpStep(workflow),
    buildSeedGenerationStep(
      workflow,
      getJobs("AI_PREPARE_SEED"),
      searchTerms,
      positionQueries,
      behaviorQueries,
    ),
    buildTargetingSearchStep(
      workflow,
      getJobs("FACEBOOK_SEED_LOOKUP"),
      getJobs("FACEBOOK_SOCIAL_POSITIONS"),
      positionQueries,
      resolvedTerms,
      discoverySummary,
      discoveryResultJson,
    ),
    buildAnchorSeedStep(
      workflow,
      getJobs("FACEBOOK_SEED_LOOKUP"),
      getJobs("AI_PREPARE_SEED"),
    ),
    buildSuggestionExpansionStep(
      workflow,
      getJobs("FACEBOOK_TARGETING_SUGGESTIONS"),
    ),
    buildSuggestionCurationStep(aiBuildJobs),
    buildAudiencePlanStep(specs, aiBuildJobs),
    buildSpecAssemblyStep(specs, aiBuildJobs),
    buildValidationStep(specs, getJobs("FACEBOOK_VALIDATE_SPEC")),
    buildReachStep(specs, getJobs("FACEBOOK_REACH_ESTIMATE")),
    buildRecalibrationStep(specs, getJobs("AI_RECALIBRATE_SPEC")),
    buildAdsetCreationStep(workflow),
  ];
}

function buildIcpStep(
  workflow: ExperimentAdSetWorkflowDto,
): PipelineStepSummary {
  const status: StepStatus =
    workflow.status === "NOT_STARTED" ? "PENDING" : "DONE";
  return {
    id: "DOC_STEP_1",
    title: "Etapa 1 · Definir ICP (entrada humana)",
    description:
      "Produto, público, dor e país viram o arquivo icp.md que guia todo o playbook.",
    status,
    detail: (
      <div className="small">
        <div>
          Experimento #{workflow.experimentId} precisa ter o ICP documentado
          antes de acionar a IA.
        </div>
        <div className="text-muted">
          Saída esperada: icp.md + ia_worker_config.json (opcional).
        </div>
        <SectionLabel className="mt-3">
          Texto de referência para o icp.md
        </SectionLabel>
        <pre
          className="mt-2 bg-light border rounded p-2 mb-0"
          style={{ whiteSpace: "pre-wrap" }}
        >
          {ICP_REFERENCE_TEXT}
        </pre>
      </div>
    ),
    helper: docReference("Etapa 1"),
  };
}

function buildSeedGenerationStep(
  workflow: ExperimentAdSetWorkflowDto,
  jobs: ExperimentAdSetJob[],
  searchTerms: string[],
  positionQueries: string[],
  behaviorQueries: string[],
): PipelineStepSummary {
  return {
    id: "DOC_STEP_2",
    title: "Etapa 2 · IA gera seeds",
    description:
      "IA Worker produz seed_candidates.json separando interests, cargos e behaviors com chance real de virar ID.",
    status: inferStatusFromJobs(jobs),
    detail: (
      <div className="small">
        <div>
          Seed keyword sugerida: <strong>{workflow.seedKeyword ?? "—"}</strong>
        </div>
        <SectionLabel className="mt-2">Lista de interests</SectionLabel>
        <BadgeList
          items={searchTerms}
          placeholder="IA ainda não retornou interests."
        />
        <SectionLabel className="mt-2">Lista de work_positions</SectionLabel>
        <BadgeList
          items={positionQueries}
          placeholder="Sem work_positions sugeridos nesta rodada."
        />
        <SectionLabel className="mt-2">Lista de behaviors</SectionLabel>
        <BadgeList
          items={behaviorQueries}
          placeholder="Sem behaviors sugeridos nesta rodada."
        />
      </div>
    ),
    helper: docReference("Etapa 2"),
    jobLinks: latestJobLink(jobs, "IA · seed_candidates.json"),
  };
}

function buildTargetingSearchStep(
  workflow: ExperimentAdSetWorkflowDto,
  interestJobs: ExperimentAdSetJob[],
  positionJobs: ExperimentAdSetJob[],
  positionQueries: string[],
  resolvedTerms: TargetingSearchResolvedTerms,
  discoverySummary?: DiscoverySummary,
  discoveryResultJson?: string,
): PipelineStepSummary {
  const interestStatus = inferStatusFromJobs(interestJobs);
  const positionsStatus: StepStatus =
    positionQueries.length === 0 && positionJobs.length === 0
      ? "SKIPPED"
      : inferStatusFromJobs(positionJobs);
  let status: StepStatus = interestStatus;
  if (interestStatus === "FAILED" || positionsStatus === "FAILED") {
    status = "FAILED";
  } else if (interestStatus === "DONE") {
    status = positionQueries.length > 0 ? positionsStatus : "DONE";
  }
  return {
    id: "DOC_STEP_3",
    title: "Etapa 3 · Discovery (IDs oficiais)",
    description:
      "Ads Worker roda /targetingsearch (mix) e /search por tipo (limit 200) para ampliar o catálogo de IDs.",
    status,
    detail: (
      <div className="small d-flex flex-column gap-3">
        <div>
          <SectionLabel>Resumo do discovery</SectionLabel>
          <DiscoverySummaryCard summary={discoverySummary} />
        </div>
        <div>
          <SectionLabel>Interesses confirmados (adinterest)</SectionLabel>
          <ResolvedTermList
            items={resolvedTerms.interests}
            placeholder="Nenhum ID de interesse encontrado até agora."
            showIds={false}
          />
        </div>
        {positionQueries.length ? (
          <div>
            <SectionLabel>Queries e IDs de cargos (adworkposition)</SectionLabel>
            <BadgeList items={positionQueries} />
            <ResolvedTermList
              items={resolvedTerms.workPositions}
              placeholder="Nenhum ID de cargo encontrado até agora."
              showIds={false}
            />
            <div className="text-muted mt-1">
              Status dos cargos: {STEP_STATUS_META[positionsStatus].label}
            </div>
          </div>
        ) : null}
        <div>
          <SectionLabel>Comportamentos (adbehavior)</SectionLabel>
          <ResolvedTermList
            items={resolvedTerms.behaviors}
            placeholder="Nenhum ID de comportamento encontrado até agora."
            showIds={false}
          />
        </div>
        <div>
          <SectionLabel>Resultado JSON do discovery</SectionLabel>
          {discoveryResultJson ? (
            <details>
              <summary>Abrir JSON consolidado</summary>
              <pre
                className="mt-2 bg-light border rounded p-2 mb-0"
                style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}
              >
                {formatJson(discoveryResultJson)}
              </pre>
            </details>
          ) : (
            <div className="text-muted">Resultado ainda não disponível.</div>
          )}
        </div>
      </div>
    ),
    helper: docReference("Etapa 3"),
    jobLinks: mergeJobLinks(
      latestJobLink(interestJobs, "Discovery (Graph API)"),
      latestJobLink(positionJobs, "Busca adicional de cargos"),
    ),
  };
}

function buildAnchorSeedStep(
  workflow: ExperimentAdSetWorkflowDto,
  interestJobs: ExperimentAdSetJob[],
  aiSeedJobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const anchorValidation = validateAnchorSeed(
    workflow.seedInterestId,
    workflow.seedInterestName,
  );
  const inferredStatus = inferStatusFromJobs(interestJobs);
  const status: StepStatus = anchorValidation.valid
    ? "DONE"
    : inferredStatus === "FAILED"
      ? "FAILED"
      : interestJobs.length
        ? "WARNING"
        : inferredStatus;

  const guidanceMessage =
    workflow.lastError && workflow.lastError.includes("Anchor seed inválido")
      ? workflow.lastError
      : `Anchor seed inválido para concluir a etapa 4. ${anchorValidation.reason} Revise seeds da etapa 1/3 e execute novamente.`;

  return {
    id: "DOC_STEP_4",
    title: "Etapa 4 · IA escolhe o anchor seed",
    description:
      "IA Worker salva seed_selected.json com o interest anchor aprovado e IDs auxiliares.",
    status,
    detail: (
      <div className="small">
        <div>
          Anchor: <strong>{workflow.seedInterestName ?? "—"}</strong>
        </div>
        <div>
          ID Meta: <code>{workflow.seedInterestId ?? "—"}</code>
        </div>
        {!anchorValidation.valid ? (
          <div className="text-warning-emphasis mt-1">{guidanceMessage}</div>
        ) : null}
        <div>
          Audience estimada: {formatNumber(workflow.seedAudienceLower)} –{" "}
          {formatNumber(workflow.seedAudienceUpper)} pessoas
        </div>
        <div className="text-muted mt-2">
          Abra a interação com o ChatGPT para visualizar lado a lado o JSON
          enviado e a resposta recebida.
        </div>
      </div>
    ),
    helper: docReference("Etapa 4"),
    jobLinks: latestJobLink(aiSeedJobs, "Interação com ChatGPT (JSON visual)"),
  };
}

function buildSuggestionExpansionStep(
  workflow: ExperimentAdSetWorkflowDto,
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  return {
    id: "DOC_STEP_5",
    title: "Etapa 5 · Targeting Suggestions",
    description:
      "Ads Worker expande o interest anchor com /targetingsuggestions (limit 150) e gera suggestions_raw.json.",
    status: inferStatusFromJobs(jobs),
    detail: (
      <div className="small">
        <div>
          Seed anchor: {workflow.seedInterestName ?? "—"} (
          {workflow.seedInterestId ?? "—"})
        </div>
        <div className="text-muted">Chamadas registradas: {jobs.length}</div>
      </div>
    ),
    helper: docReference("Etapa 5"),
    jobLinks: latestJobLink(jobs, "Meta /targetingsuggestions"),
  };
}

function buildSuggestionCurationStep(
  aiBuildJobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  return {
    id: "DOC_STEP_6",
    title: "Etapa 6 · Curadoria das sugestões (IA)",
    description:
      "IA Worker limpa ruídos, classifica por tipo e limita listas antes de montar os arquétipos.",
    status: inferStatusFromJobs(aiBuildJobs),
    detail: (
      <div className="small">
        Saída esperada: suggestions_curated.json com interesses, cargos e
        behaviors ordenados por relevância.
      </div>
    ),
    helper: docReference("Etapa 6"),
    jobLinks: latestJobLink(aiBuildJobs, "IA · curadoria"),
  };
}

function buildAudiencePlanStep(
  specs: ExperimentAdSetSpec[],
  aiBuildJobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const status: StepStatus = specs.length
    ? "DONE"
    : inferStatusFromJobs(aiBuildJobs);
  const labels = Array.from(
    new Set(
      specs
        .map((spec) => (spec.label ? spec.label.trim() : ""))
        .filter((label): label is string => Boolean(label)),
    ),
  );
  return {
    id: "DOC_STEP_7",
    title: "Etapa 7 · Definir 3 hipóteses (audience_plan.json)",
    description:
      "IA Worker transforma as sugestões em 3 arquétipos distintos para teste.",
    status,
    detail: (
      <div className="small">
        {labels.length ? (
          <>
            <SectionLabel>Arquétipos selecionados</SectionLabel>
            <BadgeList items={labels} />
          </>
        ) : (
          <span className="text-muted">
            IA está montando o arquivo audience_plan.json.
          </span>
        )}
      </div>
    ),
    helper: docReference("Etapa 7"),
    jobLinks: latestJobLink(aiBuildJobs, "IA · audience_plan"),
  };
}

function buildSpecAssemblyStep(
  specs: ExperimentAdSetSpec[],
  aiBuildJobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const status: StepStatus = specs.length
    ? "DONE"
    : inferStatusFromJobs(aiBuildJobs);
  return {
    id: "DOC_STEP_8",
    title: "Etapa 8 · Montar 3 targeting_spec (flexible_spec)",
    description:
      "IA Worker gera spec_1.json…spec_3.json com geo BR e blocos prontos para exportar.",
    status,
    detail: (
      <div className="small">
        Specs gerados: {specs.length || "—"} · cada público recebe um slot
        (Designers, Marketing, SMB).
      </div>
    ),
    helper: (
      <>
        {specs.length
          ? renderSpecStatusList(
              specs,
              (spec) => spec.label ?? slotLabel(spec.slot),
            )
          : null}
        <div className="mt-2">{docReference("Etapa 8")}</div>
      </>
    ),
    jobLinks: latestJobLink(aiBuildJobs, "IA · spec_1..3"),
  };
}

function buildValidationStep(
  specs: ExperimentAdSetSpec[],
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const title = "Etapa 9 · Targeting Validation (opcional)";
  const description =
    "Meta verifica se todos os IDs do flexible_spec existem antes de rodar reachestimate.";
  const validationLinks = latestJobLink(jobs, "Meta /targetingvalidation");
  if (!specs.length) {
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title,
      description,
      status: inferStatusFromJobs(jobs),
      detail: (
        <span className="text-muted">
          Depende da etapa "IA monta as 3 hipóteses".
        </span>
      ),
      helper: docReference("Etapa 9"),
    jobLinks: validationLinks,
    };
  }
  const invalidSpec = specs.find(
    (spec) => spec.validationStatus && spec.validationStatus !== "VALID",
  );
  const pendingSpec = specs.find((spec) => !spec.validationStatus);
  if (invalidSpec) {
    const failedValidationJob = jobs
      .filter((job) => job.status === "FAILED" && typeof job.id === "number")
      .sort((a, b) => (b.id ?? 0) - (a.id ?? 0))[0];
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title,
      description,
      status: "FAILED",
      detail: (
        <div className="small text-danger">
          {slotLabel(invalidSpec.slot)} recebeu status{" "}
          {invalidSpec.validationStatus}.{" "}
          {extractValidationSummary(invalidSpec.validationResponse) ??
            "Falha retornada pelo Meta."}
          {failedValidationJob?.id ? (
            <>
              {" "}
              <Link
                to={`jobs/${failedValidationJob.id}`}
                className="fw-semibold"
              >
                Ver detalhe do job #{failedValidationJob.id}
              </Link>
              .
            </>
          ) : null}
        </div>
      ),
      helper: docReference("Etapa 9"),
    jobLinks: validationLinks,
    };
  }
  if (pendingSpec) {
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title,
      description,
      status: jobs.some((job) => job.status === "RUNNING")
        ? "RUNNING"
        : "PENDING",
      detail: (
        <div className="small text-muted">
          Aguardando validação para {slotLabel(pendingSpec.slot)} (ver histórico
          de jobs).
        </div>
      ),
      helper: docReference("Etapa 9"),
    jobLinks: validationLinks,
    };
  }
  return {
    id: "FACEBOOK_VALIDATE_SPEC",
    title,
    description,
    status: "DONE",
    detail: (
      <div className="small">
        Todos os públicos receberam <strong>VALID</strong>.
      </div>
    ),
    helper: (
      <>
        {renderSpecStatusList(specs, (spec) => spec.validationStatus ?? "—")}
        <div className="mt-2">{docReference("Etapa 9")}</div>
      </>
    ),
  };
}

function buildReachStep(
  specs: ExperimentAdSetSpec[],
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const title = "Etapa 10 · Reach Estimate (BR)";
  const description =
    "Meta estima o alcance real no Brasil e dispara recalibração se sair da faixa.";
  const reachLinks = latestJobLink(jobs, "Meta /reachestimate");
  if (!specs.length) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title,
      description,
      status: inferStatusFromJobs(jobs),
      detail: (
        <span className="text-muted">Depende da validação dos públicos.</span>
      ),
      helper: docReference("Etapa 10"),
      jobLinks: reachLinks,
    };
  }
  const notValidated = specs.some((spec) => spec.validationStatus !== "VALID");
  if (notValidated) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title,
      description,
      status: "PENDING",
      detail: (
        <span className="text-muted">
          Executado somente após todos receberem VALID.
        </span>
      ),
      helper: docReference("Etapa 10"),
      jobLinks: reachLinks,
    };
  }
  const waitingSpec = specs.find(
    (spec) => spec.validationStatus === "VALID" && !spec.reachStatus,
  );
  if (waitingSpec) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title,
      description,
      status: jobs.some((job) => job.status === "RUNNING")
        ? "RUNNING"
        : "PENDING",
      detail: (
        <span className="text-muted">
          Aguardando resposta para {slotLabel(waitingSpec.slot)}.
        </span>
      ),
      helper: docReference("Etapa 10"),
      jobLinks: reachLinks,
    };
  }
  const outOfRange = specs.find((spec) => isSpecReachOutOfRange(spec));
  if (outOfRange) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title,
      description,
      status: "FAILED",
      detail: (
        <div className="small text-warning">
          {slotLabel(outOfRange.slot)} ficou fora da faixa de{" "}
          {formatNumber(REACH_MIN)} – {formatNumber(REACH_MAX)} pessoas. Uma
          nova rodada de recalibração foi solicitada.
        </div>
      ),
      helper: docReference("Etapa 10"),
      jobLinks: reachLinks,
    };
  }
  return {
    id: "FACEBOOK_REACH_ESTIMATE",
    title,
    description,
    status: "DONE",
    detail: <div className="small">Todos os públicos com status READY.</div>,
    helper: (
      <>
        {renderSpecStatusList(
          specs,
          (spec) =>
            `${formatNumber(spec.reachLowerBound)} – ${formatNumber(spec.reachUpperBound)} pessoas`,
        )}
        <div className="mt-2">{docReference("Etapa 10")}</div>
      </>
    ),
    jobLinks: reachLinks,
  };
}

function buildRecalibrationStep(
  specs: ExperimentAdSetSpec[],
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const title = "Etapa 11 · Recalibração automática (IA)";
  const description =
    "IA ajusta o flexible_spec quando o reach sai da faixa ideal.";
  const recalibrationLinks = jobs
    .filter((job): job is ExperimentAdSetJob & { id: number } => typeof job.id === "number")
    .map((job) => ({ jobId: job.id, label: `Recalibração #${job.id}` }))
    .slice(0, 3);
  if (!jobs.length) {
    const outOfRange = specs.some((spec) => isSpecReachOutOfRange(spec));
    return {
      id: "AI_RECALIBRATE_SPEC",
      title,
      description,
      optional: true,
      status: outOfRange ? "FAILED" : "SKIPPED",
      detail: (
        <span className="text-muted">
          {outOfRange
            ? "Algum público ficou fora da faixa e o workflow foi interrompido."
            : "Nenhum ajuste foi necessário (todos dentro da meta)."}
        </span>
      ),
      helper: docReference("Etapa 11"),
    };
  }
  return {
    id: "AI_RECALIBRATE_SPEC",
    title,
    description,
    optional: true,
    status: inferStatusFromJobs(jobs),
    detail: (
      <div className="small">
        Ajustes disparados para:
        <ul className="mb-0 ps-3">
          {jobs.map((job) => {
            const specSlot = specs.find(
              (spec) => spec.id === job.resourceId,
            )?.slot;
            const label = specSlot
              ? slotLabel(specSlot)
              : `Spec ${job.resourceId ?? "—"}`;
            return (
              <li key={job.id}>
                {label} · Job #{job.id} · Status {job.status}
                {job.attemptCount != null
                  ? ` · Tentativas ${job.attemptCount}`
                  : null}
              </li>
            );
          })}
        </ul>
      </div>
    ),
    helper: docReference("Etapa 11"),
    jobLinks: recalibrationLinks.length ? recalibrationLinks : undefined,
  };
}

function buildAdsetCreationStep(
  workflow: ExperimentAdSetWorkflowDto,
): PipelineStepSummary {
  const status: StepStatus =
    workflow.status === "FAILED" ? "FAILED" : "PENDING";
  return {
    id: "DOC_STEP_12",
    title: "Etapa 12 · Criar Ad Sets (opcional)",
    description:
      "Com os specs aprovados, podemos usar POST /adsets ou criar manualmente 3 conjuntos em PAUSED.",
    optional: true,
    status,
    detail: (
      <div className="small">
        Liberado assim que os 3 públicos estiverem READY; recomenda-se publicar
        em modo PAUSED e conectados a um campaign_id.
      </div>
    ),
    helper: docReference("Etapa 12"),
  };
}

function groupJobsByType(
  jobs?: ExperimentAdSetJob[],
): Map<string, ExperimentAdSetJob[]> {
  const map = new Map<string, ExperimentAdSetJob[]>();
  (jobs ?? []).forEach((job) => {
    if (!job.type) return;
    const list = map.get(job.type) ?? [];
    list.push(job);
    map.set(job.type, list);
  });
  map.forEach((list, key) => {
    list.sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
    map.set(key, list);
  });
  return map;
}

function inferStatusFromJobs(jobs: ExperimentAdSetJob[]): StepStatus {
  if (!jobs.length) return "PENDING";
  if (jobs.some((job) => job.status === "FAILED")) return "FAILED";
  if (jobs.some((job) => job.status === "RUNNING")) return "RUNNING";
  if (jobs.some((job) => job.status === "SUCCEEDED")) return "DONE";
  return "PENDING";
}

function validateAnchorSeed(seedId?: string | null, seedName?: string | null) {
  if (!seedId?.trim()) {
    return { valid: false, reason: "ID do seed não foi definido." };
  }
  if (!seedName?.trim()) {
    return { valid: false, reason: "Nome do seed não foi definido." };
  }
  const normalizedName = seedName.trim().toLowerCase();
  const invalidNames = new Set([
    "unspecified",
    "generic",
    "genérico",
    "generico",
    "n/a",
    "na",
  ]);
  if (
    invalidNames.has(normalizedName) ||
    normalizedName.includes("unspecified") ||
    normalizedName.includes("não especificado") ||
    normalizedName.includes("nao especificado")
  ) {
    return {
      valid: false,
      reason: `Nome do seed (${seedName.trim()}) é genérico e não qualifica um anchor válido.`,
    };
  }
  return { valid: true, reason: "" };
}

function renderSpecStatusList(
  specs: ExperimentAdSetSpec[],
  extractor: (spec: ExperimentAdSetSpec) => string,
): ReactNode {
  return (
    <ul className="mb-0 ps-3 small">
      {specs.map((spec) => (
        <li key={spec.id}>
          {slotLabel(spec.slot)}: {extractor(spec)}
        </li>
      ))}
    </ul>
  );
}

function docReference(stepLabel: string): ReactNode {
  return (
    <span>
      Referência: {PIPELINE_DOC_PATH} · {stepLabel}
    </span>
  );
}

function BadgeList({
  items,
  placeholder,
}: {
  items: string[];
  placeholder?: string;
}) {
  if (!items.length) {
    return <span className="text-muted">{placeholder ?? "—"}</span>;
  }
  return (
    <div className="d-flex flex-wrap gap-2">
      {items.map((item, index) => (
        <span
          key={`${item}-${index}`}
          className="badge text-bg-light border text-muted"
        >
          {item}
        </span>
      ))}
    </div>
  );
}

function ResolvedTermList({
  items,
  placeholder,
  showIds = true,
}: {
  items: TargetingResolutionByTerm[];
  placeholder: string;
  showIds?: boolean;
}) {
  if (!items.length) {
    return <div className="text-muted">{placeholder}</div>;
  }
  return (
    <ul className="list-unstyled mb-0 mt-1">
      {items.map((item) => (
        <li key={`${item.term}-${item.ids.join("-")}`} className="mb-1">
          <span className="fw-semibold">{item.term}</span>: {" "}
          {showIds ? (
            <code>{item.ids.join(", ")}</code>
          ) : (
            <span className="text-muted">{item.ids.length} IDs encontrados</span>
          )}
        </li>
      ))}
    </ul>
  );
}

function DiscoverySummaryCard({ summary }: { summary?: DiscoverySummary }) {
  if (!summary) {
    return (
      <div className="text-muted">
        Discovery ainda não foi executado nesta rodada.
      </div>
    );
  }
  return (
    <div className="d-flex flex-column gap-2">
      <div>
        <SectionLabel>Seeds consultados</SectionLabel>
        <BadgeList
          items={summary.seeds}
          placeholder="Nenhum seed disponível."
        />
      </div>
      <div>
        <SectionLabel>Locales utilizados</SectionLabel>
        <BadgeList
          items={summary.locales}
          placeholder="Locales não informados."
        />
      </div>
      {summary.stats ? (
        <div className="text-muted small">
          {summary.stats.rawCalls ?? 0} chamadas · {summary.stats.rawItems ?? 0} itens brutos · {summary.stats.dedupItems ?? 0} IDs únicos.
        </div>
      ) : null}
      {summary.topItems.length ? (
        <div>
          <SectionLabel>Principais candidatos</SectionLabel>
          <ul className="list-unstyled mb-0 small">
            {summary.topItems.map((item, index) => (
              <li key={`${item.id ?? item.name ?? index}`} className="mb-2">
                <div className="fw-semibold">{item.name ?? item.id ?? "ID desconhecido"}</div>
                <div className="text-muted">
                  {item.type ?? "—"} · {formatAudienceRange(item.audienceLower, item.audienceUpper)}
                </div>
                {item.sources?.length ? (
                  <div className="text-muted">Fontes: {item.sources.join(", ")}</div>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

function latestJobLink(jobs: ExperimentAdSetJob[], label: string): PipelineJobLink[] | undefined {
  const sorted = jobs
    .filter((job): job is ExperimentAdSetJob & { id: number } => typeof job.id === "number")
    .sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
  const job = sorted[0];
  return job?.id ? [{ jobId: job.id, label }] : undefined;
}

function mergeJobLinks(
  ...links: Array<PipelineJobLink[] | undefined>
): PipelineJobLink[] | undefined {
  const merged = links.flatMap((link) => link ?? []);
  return merged.length ? merged : undefined;
}

function buildDiscoveryResultJson(
  details: Array<ExperimentAdSetJobDetailDto | undefined>,
): string | undefined {
  const discoveryJobs = details.filter(
    (detail): detail is ExperimentAdSetJobDetailDto =>
      Boolean(
        detail &&
          (detail.job?.type === "FACEBOOK_SEED_LOOKUP" ||
            detail.job?.type === "FACEBOOK_SOCIAL_POSITIONS"),
      ),
  );
  if (!discoveryJobs.length) {
    return undefined;
  }

  const payload = discoveryJobs.map((detail) => {
    const responses = detail.apiLogs
      .map((log) => ({
        provider: log.provider,
        endpoint: log.endpoint,
        statusCode: log.statusCode,
        response: safeJsonParse<unknown>(log.responsePayload),
      }))
      .filter((log) => log.response != null);

    return {
      jobId: detail.job.id,
      jobType: detail.job.type,
      status: detail.job.status,
      resultPayload: safeJsonParse<unknown>(detail.resultPayload),
      responses: responses.length ? responses : undefined,
    };
  });

  if (
    payload.every(
      (item) =>
        item.resultPayload == null &&
        (!item.responses || item.responses.length === 0),
    )
  ) {
    return undefined;
  }

  return JSON.stringify({ jobs: payload });
}

function parseResolvedTermsFromJobDetails(
  details: Array<ExperimentAdSetJobDetailDto | undefined>,
): TargetingSearchResolvedTerms {
  const map: Record<"adinterest" | "adworkposition" | "adbehavior", Map<string, Set<string>>> = {
    adinterest: new Map(),
    adworkposition: new Map(),
    adbehavior: new Map(),
  };

  details.forEach((detail) => {
    detail?.apiLogs?.forEach((log) => {
      const request = safeJsonParse<Record<string, unknown>>(log.requestPayload);
      const response = safeJsonParse<unknown>(log.responsePayload);
      const rawType = String(request?.type ?? "").toLowerCase();
      const normalizedType: "adinterest" | "adworkposition" | "adbehavior" | null =
        rawType.includes("work")
          ? "adworkposition"
          : rawType.includes("behavior")
            ? "adbehavior"
            : rawType.includes("interest")
              ? "adinterest"
              : null;
      if (!normalizedType) return;
      const queryValue = String(request?.query ?? request?.q ?? "").trim();
      if (!queryValue) return;

      const candidates = extractCandidates(response);
      if (!candidates.length) return;
      const byTerm = map[normalizedType];
      const ids = byTerm.get(queryValue) ?? new Set<string>();
      candidates.forEach((item) => {
        const id = String(item?.id ?? "").trim();
        if (id) ids.add(id);
      });
      if (ids.size > 0) {
        byTerm.set(queryValue, ids);
      }
    });
  });

  const toList = (entries: Map<string, Set<string>>): TargetingResolutionByTerm[] =>
    Array.from(entries.entries()).map(([term, ids]) => ({
      term,
      ids: Array.from(ids),
    }));

  return {
    interests: toList(map.adinterest),
    workPositions: toList(map.adworkposition),
    behaviors: toList(map.adbehavior),
  };
}

function extractCandidates(response: unknown): Array<{ id?: unknown }> {
  if (Array.isArray(response)) {
    return response as Array<{ id?: unknown }>;
  }
  if (!response || typeof response !== "object") return [];
  const record = response as Record<string, unknown>;
  if (Array.isArray(record.data)) return record.data as Array<{ id?: unknown }>;
  if (Array.isArray(record.items)) return record.items as Array<{ id?: unknown }>;
  return [];
}

function parseDiscoverySummaryFromJobDetail(
  detail?: ExperimentAdSetJobDetailDto,
): DiscoverySummary | undefined {
  const node = safeJsonParse<any>(detail?.resultPayload);
  if (!node) return undefined;
  const seeds = Array.isArray(node.seedTerms)
    ? node.seedTerms.map((value: unknown) => String(value))
    : [];
  const locales = Array.isArray(node.locales)
    ? node.locales.map((value: unknown) => String(value))
    : [];
  const statsNode = node.stats ?? {};
  const stats = {
    rawCalls: toNumber(statsNode.rawCalls ?? statsNode.raw_calls),
    rawItems: toNumber(statsNode.rawItems ?? statsNode.raw_items),
    dedupItems: toNumber(statsNode.dedupItems ?? statsNode.dedup_items),
    byType: typeof statsNode.byType === "object" ? statsNode.byType : undefined,
  };
  const dedupArray: unknown[] = Array.isArray(node.dedup) ? node.dedup : [];
  const topItems = dedupArray
    .map((item): DiscoveryTopItem => {
      if (!item || typeof item !== "object") {
        return {};
      }
      const record = item as Record<string, unknown>;
      return {
        id: typeof record.id === "string" ? record.id : undefined,
        name: typeof record.name === "string" ? record.name : undefined,
        type: typeof record.type === "string" ? record.type : undefined,
        audienceLower: toNumber(
          record.audienceLowerBound ?? record.audience_lower_bound,
        ),
        audienceUpper: toNumber(
          record.audienceUpperBound ?? record.audience_upper_bound,
        ),
        sources: Array.isArray(record.sources)
          ? record.sources.map((value: unknown) => String(value))
          : [],
        terms: Array.isArray(record.terms)
          ? record.terms.map((value: unknown) => String(value))
          : [],
      };
    })
    .filter((item) => item.id || item.name)
    .sort((a: DiscoveryTopItem, b: DiscoveryTopItem) => {
      const aAudience = a.audienceUpper ?? a.audienceLower ?? 0;
      const bAudience = b.audienceUpper ?? b.audienceLower ?? 0;
      return bAudience - aAudience;
    })
    .slice(0, 5);
  return {
    seeds,
    locales,
    stats:
      stats.rawCalls != null || stats.rawItems != null || stats.dedupItems != null
        ? stats
        : undefined,
    topItems,
    jobId: detail?.job?.id,
  };
}

function SectionLabel({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={`text-uppercase text-muted small fw-semibold ${className ?? ""}`.trim()}
    >
      {children}
    </div>
  );
}

function StatusBadge({ status }: { status: StepStatus }) {
  const meta = STEP_STATUS_META[status];
  return <span className={`badge text-bg-${meta.variant}`}>{meta.label}</span>;
}

function slotLabel(slot?: string | null) {
  if (!slot) return SLOT_LABELS.UNKNOWN;
  return SLOT_LABELS[slot] ?? slot;
}

function collectStrings(value?: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => (typeof item === "string" ? item.trim() : ""))
    .filter(Boolean);
}

function safeJsonParse<T = unknown>(value?: string | null): T | undefined {
  if (!value) return undefined;
  try {
    return JSON.parse(value) as T;
  } catch (error) {
    return undefined;
  }
}

function extractValidationSummary(raw?: string | null): string | undefined {
  const node = safeJsonParse<any>(raw);
  if (!node) return undefined;
  if (typeof node.message === "string" && node.message.trim())
    return node.message;
  const details = node.details;
  if (details) {
    if (typeof details.message === "string" && details.message.trim()) {
      return details.message;
    }
    if (details.error?.message) {
      return details.error.message;
    }
    const data = Array.isArray(details.data) ? details.data : [];
    const first = data[0];
    if (first) {
      return (
        first.error_message ?? first.description ?? first.summary ?? undefined
      );
    }
  }
  return undefined;
}

function extractReachSummary(raw?: string | null): string | undefined {
  const node = safeJsonParse<any>(raw);
  const dataArray = node && Array.isArray(node.data) ? node.data : [];
  const first = dataArray[0];
  if (!first) return undefined;
  if (first.estimate_ready === false) return "Meta ainda calculando o reach.";
  if (
    first.estimate_ready === true &&
    first.users_lower_bound &&
    first.users_upper_bound
  ) {
    return `${formatNumber(first.users_lower_bound)} – ${formatNumber(first.users_upper_bound)} pessoas.`;
  }
  return undefined;
}

function isSpecReachOutOfRange(spec: ExperimentAdSetSpec) {
  if (!spec.reachLowerBound || !spec.reachUpperBound) {
    return false;
  }
  return spec.reachLowerBound < REACH_MIN || spec.reachUpperBound > REACH_MAX;
}

function formatNumber(value?: number | null) {
  if (value == null) return "—";
  return new Intl.NumberFormat("pt-BR").format(value);
}

function toNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function statusToVariant(status?: string | null) {
  switch (status) {
    case "SUCCEEDED":
    case "COMPLETED":
    case "VALID":
    case "READY":
      return "success";
    case "FAILED":
    case "INVALID":
      return "danger";
    case "RUNNING":
      return "info";
    default:
      return "secondary";
  }
}

function formatJson(value?: string | null) {
  if (!value) return "Não disponível";
  try {
    const parsed = JSON.parse(value);
    return JSON.stringify(parsed, null, 2);
  } catch (error) {
    return value;
  }
}

function formatAudienceRange(lower?: number | null, upper?: number | null) {
  if (lower != null && upper != null) {
    return `${formatNumber(lower)} – ${formatNumber(upper)} pessoas`;
  }
  const fallback = lower ?? upper;
  if (fallback == null) {
    return "Sem estimativa";
  }
  return `${formatNumber(fallback)} pessoas`;
}
