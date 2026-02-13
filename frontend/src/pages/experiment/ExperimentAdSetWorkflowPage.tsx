import { useMemo, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useQueries } from "@tanstack/react-query";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
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
    title: "Targeting Search (interest)",
    description: "Meta /targetingsearch para transformar seed em interesse",
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

type StepStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED" | "SKIPPED";

const STEP_STATUS_META: Record<StepStatus, { label: string; variant: string }> =
  {
    PENDING: { label: "Aguardando", variant: "secondary" },
    RUNNING: { label: "Em execução", variant: "info" },
    DONE: { label: "Concluído", variant: "success" },
    FAILED: { label: "Com erro", variant: "danger" },
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
        const { data } = await axios.get<{
          apiLogs: Array<{ requestPayload?: string | null; responsePayload?: string | null }>;
        }>(
          `/api/experiments/${workflow.experimentId}/adset-playbook/jobs/${job.id}`,
        );
        return data;
      },
      enabled: Boolean(workflow.experimentId && job.id),
      staleTime: 30_000,
    })),
  });

  const resolvedTerms = useMemo(
    () => parseResolvedTermsFromJobDetails(jobDetailQueries.map((query) => query.data)),
    [jobDetailQueries],
  );

  const steps = useMemo(
    () => buildPipelineSteps(workflow, resolvedTerms),
    [workflow, resolvedTerms],
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
        <ol className="list-unstyled mb-0">
          {steps.map((step, index) => (
            <li
              key={step.id}
              className={
                index < steps.length - 1 ? "pb-3 mb-3 border-bottom" : ""
              }
            >
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="d-flex align-items-center gap-2">
                    <span className="fw-semibold">
                      {index + 1}. {step.title}
                    </span>
                    {step.optional ? (
                      <span className="badge text-bg-light">Opcional</span>
                    ) : null}
                  </div>
                  <p className="text-muted small mb-2">{step.description}</p>
                  {step.detail}
                  {step.helper ? (
                    <div className="small mt-2">{step.helper}</div>
                  ) : null}
                </div>
                <StatusBadge status={step.status} />
              </div>
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}

function SpecsCard({ specs }: { specs: ExperimentAdSetSpec[] }) {
  if (!specs?.length) {
    return (
      <div className="card h-100">
        <div className="card-header">Targeting specs</div>
        <div className="card-body text-muted">
          Aguardando saída da etapa "IA monta públicos".
        </div>
      </div>
    );
  }
  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-column flex-lg-row justify-content-between gap-2">
        <div>
          <div className="fw-semibold">Targeting specs (3 hipóteses)</div>
          <small className="text-muted">
            Produto final das Etapas 6-10 ({PIPELINE_DOC_PATH}) · flexible_spec
            pronto para exportar
          </small>
        </div>
        <small className="text-muted">
          Faixa ideal do Reach no BR: {formatNumber(REACH_MIN)} –{" "}
          {formatNumber(REACH_MAX)} pessoas
        </small>
      </div>
      <div className="card-body">
        <div className="row g-3">
          {specs.map((spec) => (
            <div key={spec.id} className="col-12 col-lg-4">
              <SpecCardItem spec={spec} />
            </div>
          ))}
        </div>
      </div>
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
    ),
    buildAnchorSeedStep(workflow, getJobs("FACEBOOK_SEED_LOOKUP")),
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
  };
}

function buildTargetingSearchStep(
  workflow: ExperimentAdSetWorkflowDto,
  interestJobs: ExperimentAdSetJob[],
  positionJobs: ExperimentAdSetJob[],
  positionQueries: string[],
  resolvedTerms: TargetingSearchResolvedTerms,
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
    title: "Etapa 3 · Targeting Search (IDs oficiais)",
    description:
      "Ads Worker chama /targetingsearch para cada seed (interests, work_positions e behaviors).",
    status,
    detail: (
      <div className="small">
        <div>
          Keyword consultada: <strong>{workflow.seedKeyword ?? "—"}</strong>
        </div>
        <div>Locale: {workflow.seedLocale ?? "—"}</div>
        <SectionLabel className="mt-2">Interesses (adinterest)</SectionLabel>
        <ResolvedTermList
          items={resolvedTerms.interests}
          placeholder="Nenhum ID de interesse encontrado até agora."
        />
        {positionQueries.length ? (
          <div className="mt-2">
            <SectionLabel>Queries de cargos (adworkposition)</SectionLabel>
            <BadgeList items={positionQueries} />
            <ResolvedTermList
              items={resolvedTerms.workPositions}
              placeholder="Nenhum ID de cargo encontrado até agora."
            />
            <div className="text-muted mt-1">
              Status dos cargos: {STEP_STATUS_META[positionsStatus].label}
            </div>
          </div>
        ) : (
          <div className="text-muted mt-2">
            Sem cargos adicionais nesta rodada.
          </div>
        )}
        <SectionLabel className="mt-2">Comportamentos (adbehavior)</SectionLabel>
        <ResolvedTermList
          items={resolvedTerms.behaviors}
          placeholder="Nenhum ID de comportamento encontrado até agora."
        />
      </div>
    ),
    helper: docReference("Etapa 3"),
  };
}

function buildAnchorSeedStep(
  workflow: ExperimentAdSetWorkflowDto,
  interestJobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const status: StepStatus = workflow.seedInterestId
    ? "DONE"
    : inferStatusFromJobs(interestJobs);
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
        <div>
          Audience estimada: {formatNumber(workflow.seedAudienceLower)} –{" "}
          {formatNumber(workflow.seedAudienceUpper)} pessoas
        </div>
      </div>
    ),
    helper: docReference("Etapa 4"),
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
  };
}

function buildValidationStep(
  specs: ExperimentAdSetSpec[],
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const title = "Etapa 9 · Targeting Validation (opcional)";
  const description =
    "Meta verifica se todos os IDs do flexible_spec existem antes de rodar reachestimate.";
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
  };
}

function buildRecalibrationStep(
  specs: ExperimentAdSetSpec[],
  jobs: ExperimentAdSetJob[],
): PipelineStepSummary {
  const title = "Etapa 11 · Recalibração automática (IA)";
  const description =
    "IA ajusta o flexible_spec quando o reach sai da faixa ideal.";
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
}: {
  items: TargetingResolutionByTerm[];
  placeholder: string;
}) {
  if (!items.length) {
    return <div className="text-muted">{placeholder}</div>;
  }
  return (
    <ul className="list-unstyled mb-0 mt-1">
      {items.map((item) => (
        <li key={`${item.term}-${item.ids.join("-")}`} className="mb-1">
          <span className="fw-semibold">{item.term}</span>: <code>{item.ids.join(", ")}</code>
        </li>
      ))}
    </ul>
  );
}

function parseResolvedTermsFromJobDetails(
  details: Array<{ apiLogs?: Array<{ requestPayload?: string | null; responsePayload?: string | null }> } | undefined>,
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
