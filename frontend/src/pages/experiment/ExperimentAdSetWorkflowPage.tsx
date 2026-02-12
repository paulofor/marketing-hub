import { useMemo, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
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

const JOB_TYPE_INFO: Record<string, { title: string; description: string; docStep: string }> = {
  AI_PREPARE_SEED: {
    title: "Planejar seed (IA)",
    description: "Lê o ICP do experimento e escolhe keyword/search terms",
    docStep: "Doc · seção 1",
  },
  FACEBOOK_SEED_LOOKUP: {
    title: "Targeting Search (interest)",
    description: "Meta /targetingsearch para transformar seed em interesse",
    docStep: "Doc · seção 2",
  },
  FACEBOOK_SOCIAL_POSITIONS: {
    title: "Targeting Search (cargos)",
    description: "Busca cargos sociais (adworkposition)",
    docStep: "Doc · seção 2 (opcional)",
  },
  FACEBOOK_TARGETING_SUGGESTIONS: {
    title: "Targeting Suggestions",
    description: "Expande o interest anchor com o ecossistema do Meta",
    docStep: "Doc · seção 3",
  },
  AI_BUILD_SPECS: {
    title: "Montar 3 públicos (IA)",
    description: "IA Worker cria flexible_spec para Designers · Marketing · SMB",
    docStep: "Doc · seção 4-5",
  },
  FACEBOOK_VALIDATE_SPEC: {
    title: "Targeting Validation",
    description: "Meta /targetingvalidation para checar IDs oficiais",
    docStep: "Doc · seção 6",
  },
  FACEBOOK_REACH_ESTIMATE: {
    title: "Reach Estimate (BR)",
    description: "Meta /reachestimate para calibrar o tamanho",
    docStep: "Doc · seção 7",
  },
  AI_RECALIBRATE_SPEC: {
    title: "Recalibração automática",
    description: "IA ajusta idade e blocos quando reach foge de 200k-20M",
    docStep: "Doc · seção 7 (loop)",
  },
};

const PIPELINE_DOC_PATH = "docs/facebook-ads-worker/pipeline-3-publicos-meta-ads-api-ia-worker.md";
const REACH_MIN = 200_000;
const REACH_MAX = 20_000_000;

type StepStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED" | "SKIPPED";

const STEP_STATUS_META: Record<StepStatus, { label: string; variant: string }> = {
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
              Experimento <Link to={`/experiments/${experimentId}`}>#{experimentId}</Link>
            </span>
          }
        />
        <div className="d-flex gap-2">
          <span className={`badge text-bg-${statusVariant} align-self-center px-3 py-2`}>
            {data.status}
          </span>
          <button
            type="button"
            className="btn btn-primary"
            disabled={startWorkflow.isPending || (!canStart && !canRestart) || data.status === "RUNNING"}
            onClick={() => startWorkflow.mutate(canRestart)}
          >
            {startWorkflow.isPending ? "Processando..." : buttonLabel}
          </button>
        </div>
      </div>

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

function SeedCard({ workflow }: { workflow: ExperimentAdSetWorkflowDto }) {
  const aiPlan = safeJsonParse<{ searchTerms?: string[]; positionQueries?: string[] }>(workflow.aiNotes);
  const searchTerms = collectStrings(aiPlan?.searchTerms);
  const positionQueries = collectStrings(aiPlan?.positionQueries);
  return (
    <div className="card h-100">
      <div className="card-header">
        <div className="fw-semibold">Seed atual</div>
        <small className="text-muted">
          Etapas 1 e 2 do roteiro ({PIPELINE_DOC_PATH})
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
          <dd className="col-sm-7">{formatNumber(workflow.seedAudienceLower)}</dd>
          <dt className="col-sm-5">Audience (upper)</dt>
          <dd className="col-sm-7">{formatNumber(workflow.seedAudienceUpper)}</dd>
        </dl>
        <SectionLabel>Termos usados no Targeting Search</SectionLabel>
        <BadgeList items={searchTerms} placeholder="IA ainda não devolveu os termos" />
        <SectionLabel className="mt-3">Cargos consultados no Meta</SectionLabel>
        <BadgeList items={positionQueries} placeholder="Sem queries adicionais" />
        {workflow.aiNotes ? (
          <details className="mt-3">
            <summary>Ver JSON bruto dessa etapa</summary>
            <pre className="mt-2 small bg-light p-2 rounded overflow-auto" style={{ maxHeight: 220 }}>
              {formatJson(workflow.aiNotes)}
            </pre>
          </details>
        ) : null}
      </div>
    </div>
  );
}

function PipelineTimeline({ workflow }: { workflow: ExperimentAdSetWorkflowDto }) {
  const steps = useMemo(() => buildPipelineSteps(workflow), [workflow]);
  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-column gap-1">
        <div className="fw-semibold">Pipeline dos 3 públicos (Meta Ads API)</div>
        <small className="text-muted">
          Targeting Search → Suggestions → flexible_spec → Validation → Reach (referência: {PIPELINE_DOC_PATH})
        </small>
      </div>
      <div className="card-body">
        <ol className="list-unstyled mb-0">
          {steps.map((step, index) => (
            <li
              key={step.id}
              className={index < steps.length - 1 ? "pb-3 mb-3 border-bottom" : ""}
            >
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="d-flex align-items-center gap-2">
                    <span className="fw-semibold">
                      {index + 1}. {step.title}
                    </span>
                    {step.optional ? <span className="badge text-bg-light">Opcional</span> : null}
                  </div>
                  <p className="text-muted small mb-2">{step.description}</p>
                  {step.detail}
                  {step.helper ? <div className="small mt-2">{step.helper}</div> : null}
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
        <div className="card-body text-muted">Aguardando saída da etapa "IA monta públicos".</div>
      </div>
    );
  }
  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-column flex-lg-row justify-content-between gap-2">
        <div>
          <div className="fw-semibold">Targeting specs (3 hipóteses)</div>
          <small className="text-muted">
            Produto final das etapas 4-7 do pipeline · flexible_spec pronto para exportar
          </small>
        </div>
        <small className="text-muted">
          Faixa ideal do Reach no BR: {formatNumber(REACH_MIN)} – {formatNumber(REACH_MAX)} pessoas
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
          <span className={`badge text-bg-${statusToVariant(spec.validationStatus)}`}>
            Validação: {spec.validationStatus ?? "PENDENTE"}
          </span>
          <span className={`badge text-bg-${statusToVariant(spec.reachStatus)}`}>
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
      <div className={`small ${reachOutOfRange ? "text-warning" : "text-muted"}`}>
        Alcance estimado: <strong>{formatNumber(spec.reachLowerBound)} – {formatNumber(spec.reachUpperBound)}</strong> pessoas.
        {reachOutOfRange ? " Fora da faixa recomendada (200k-20M)." : null}
      </div>
      {reachSummary ? <div className="small text-muted mt-1">Meta: {reachSummary}</div> : null}
      {validationSummary ? (
        <div className="alert alert-warning small py-2 mt-2 mb-0">
          {validationSummary}
        </div>
      ) : null}
      <div className="mt-auto">
        {spec.targetingSpec ? (
          <details className="mt-3">
            <summary>Targeting spec (JSON)</summary>
            <pre className="small bg-light p-2 rounded mt-2 overflow-auto" style={{ maxHeight: 200 }}>
              {formatJson(spec.targetingSpec)}
            </pre>
          </details>
        ) : null}
        {spec.validationResponse ? (
          <details className="mt-2">
            <summary>Resposta do Targeting Validation</summary>
            <pre className="small bg-light p-2 rounded mt-2 overflow-auto" style={{ maxHeight: 200 }}>
              {formatJson(spec.validationResponse)}
            </pre>
          </details>
        ) : null}
        {spec.reachResponse ? (
          <details className="mt-2">
            <summary>Resposta do Reach Estimate</summary>
            <pre className="small bg-light p-2 rounded mt-2 overflow-auto" style={{ maxHeight: 200 }}>
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
          Cada job do Facebook guarda as chamadas da Graph API. Clique em "Detalhe" para ver os payloads enviados/recebidos.
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
                      <div className="fw-semibold">{info?.title ?? job.type ?? "—"}</div>
                      <div className="text-muted small">{job.type}</div>
                      {info?.description ? (
                        <div className="text-muted small">{info.description}</div>
                      ) : null}
                      {info?.docStep ? (
                        <div className="text-muted small">{info.docStep}</div>
                      ) : null}
                    </td>
                    <td>
                      <span className={`badge text-bg-${statusToVariant(job.status)}`}>
                        {job.status}
                      </span>
                    </td>
                    <td>{job.worker}</td>
                    <td>{formatDate(job.startedAt)}</td>
                    <td>{formatDate(job.finishedAt)}</td>
                    <td className="text-danger small">{job.errorMessage ?? ""}</td>
                    <td>
                      <Link to={`jobs/${job.id}`} className="btn btn-link btn-sm px-0">
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
        Referência: {PIPELINE_DOC_PATH}. O link "Detalhe" mostra cada request feita ao Facebook Ads.
      </div>
    </div>
  );
}

function buildPipelineSteps(workflow: ExperimentAdSetWorkflowDto): PipelineStepSummary[] {
  const aiPlan = safeJsonParse<{ searchTerms?: string[]; positionQueries?: string[] }>(workflow.aiNotes);
  const searchTerms = collectStrings(aiPlan?.searchTerms);
  const positionQueries = collectStrings(aiPlan?.positionQueries);
  const jobsByType = groupJobsByType(workflow.jobs);
  const specs = workflow.specs ?? [];
  const steps: PipelineStepSummary[] = [];

  const getJobs = (type: string) => jobsByType.get(type) ?? [];

  steps.push({
    id: "AI_PREPARE_SEED",
    title: "Planejamento do seed (IA Worker)",
    description: "IA lê ICP, nicho e hipótese para gerar seedKeyword, searchTerms e positionQueries.",
    status: inferStatusFromJobs(getJobs("AI_PREPARE_SEED")),
    detail: (
      <div className="small">
        <div>
          Seed: <strong>{workflow.seedKeyword ?? "—"}</strong> · Locale: {workflow.seedLocale ?? "—"}
        </div>
        {searchTerms.length ? (
          <div className="mt-2">
            <SectionLabel>Search terms enviados ao Targeting Search</SectionLabel>
            <BadgeList items={searchTerms} />
          </div>
        ) : null}
        {positionQueries.length ? (
          <div className="mt-2">
            <SectionLabel>Cargos a serem consultados</SectionLabel>
            <BadgeList items={positionQueries} />
          </div>
        ) : null}
      </div>
    ),
  });

  steps.push({
    id: "FACEBOOK_SEED_LOOKUP",
    title: "Targeting Search (interest)",
    description: "/targetingsearch identifica o ID oficial do interesse anchor.",
    status: inferStatusFromJobs(getJobs("FACEBOOK_SEED_LOOKUP")),
    detail: (
      <div className="small">
        <div>Interesse encontrado: <strong>{workflow.seedInterestName ?? "—"}</strong></div>
        {workflow.seedInterestId ? (
          <div>
            ID Meta: <code>{workflow.seedInterestId}</code>
          </div>
        ) : null}
        <div>
          Audience apontado pela Meta: {formatNumber(workflow.seedAudienceLower)} – {formatNumber(workflow.seedAudienceUpper)} pessoas
        </div>
      </div>
    ),
  });

  const positionsJobs = getJobs("FACEBOOK_SOCIAL_POSITIONS");
  steps.push({
    id: "FACEBOOK_SOCIAL_POSITIONS",
    title: "Targeting Search (cargos)",
    description: "Busca cargos/positions relacionados ao seed para combinar com interesses.",
    optional: true,
    status: positionQueries.length === 0 && positionsJobs.length === 0 ? "SKIPPED" : inferStatusFromJobs(positionsJobs),
    detail: (
      <div className="small">
        <SectionLabel>Queries enviados</SectionLabel>
        <BadgeList items={positionQueries} placeholder="Nenhum cargo definido" />
        <div className="text-muted mt-2">
          Jobs executados: {positionsJobs.length || 0} · consultar tabela abaixo para ver chamadas /targetingsearch
        </div>
      </div>
    ),
  });

  const suggestionsJobs = getJobs("FACEBOOK_TARGETING_SUGGESTIONS");
  steps.push({
    id: "FACEBOOK_TARGETING_SUGGESTIONS",
    title: "Targeting Suggestions",
    description: "Expande o interesse anchor em até 100 sugestões relevantes.",
    status: inferStatusFromJobs(suggestionsJobs),
    detail: (
      <div className="small">
        <div>Seed anchor: {workflow.seedInterestName ?? "—"}</div>
        <div className="text-muted">Chamadas registradas: {suggestionsJobs.length || 0} GET /targetingsuggestions.</div>
      </div>
    ),
  });

  const buildSpecsJobs = getJobs("AI_BUILD_SPECS");
  steps.push({
    id: "AI_BUILD_SPECS",
    title: "IA monta as 3 hipóteses",
    description: "Com as sugestões filtradas, a IA gera Designers · Marketing · SMB usando flexible_spec.",
    status: inferStatusFromJobs(buildSpecsJobs),
    detail: (
      <div className="small">
        {specs.length ? (
          <ul className="mb-0 ps-3">
            {specs.map((spec) => (
              <li key={spec.id}>
                <strong>{slotLabel(spec.slot)}</strong>: {spec.label ?? "Sem rótulo"}
              </li>
            ))}
          </ul>
        ) : (
          <span className="text-muted">Aguardando retorno da IA Worker.</span>
        )}
      </div>
    ),
  });

  steps.push(buildValidationStep(specs, getJobs("FACEBOOK_VALIDATE_SPEC")));
  steps.push(buildReachStep(specs, getJobs("FACEBOOK_REACH_ESTIMATE")));
  steps.push(buildRecalibrationStep(specs, getJobs("AI_RECALIBRATE_SPEC")));

  return steps;
}

function buildValidationStep(specs: ExperimentAdSetSpec[], jobs: ExperimentAdSetJob[]): PipelineStepSummary {
  if (!specs.length) {
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title: "Targeting Validation",
      description: "Meta verifica se todos os IDs do flexible_spec existem.",
      status: inferStatusFromJobs(jobs),
      detail: <span className="text-muted">Depende da etapa "IA monta as 3 hipóteses".</span>,
    };
  }
  const invalidSpec = specs.find((spec) => spec.validationStatus && spec.validationStatus !== "VALID");
  const pendingSpec = specs.find((spec) => !spec.validationStatus);
  if (invalidSpec) {
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title: "Targeting Validation",
      description: "Meta verifica se todos os IDs do flexible_spec existem.",
      status: "FAILED",
      detail: (
        <div className="small text-danger">
          {slotLabel(invalidSpec.slot)} recebeu status {invalidSpec.validationStatus}.
          {" "}
          {extractValidationSummary(invalidSpec.validationResponse) ?? "Ver detalhe do job para o erro completo."}
        </div>
      ),
    };
  }
  if (pendingSpec) {
    return {
      id: "FACEBOOK_VALIDATE_SPEC",
      title: "Targeting Validation",
      description: "Meta verifica se todos os IDs do flexible_spec existem.",
      status: jobs.some((job) => job.status === "RUNNING") ? "RUNNING" : "PENDING",
      detail: (
        <div className="small text-muted">
          Aguardando validação para {slotLabel(pendingSpec.slot)} (ver histórico de jobs).
        </div>
      ),
    };
  }
  return {
    id: "FACEBOOK_VALIDATE_SPEC",
    title: "Targeting Validation",
    description: "Meta verifica se todos os IDs do flexible_spec existem.",
    status: "DONE",
    detail: <div className="small">Todos os públicos receberam <strong>VALID</strong>.</div>,
    helper: renderSpecStatusList(specs, (spec) => spec.validationStatus ?? "—"),
  };
}

function buildReachStep(specs: ExperimentAdSetSpec[], jobs: ExperimentAdSetJob[]): PipelineStepSummary {
  if (!specs.length) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title: "Reach Estimate (BR)",
      description: "Meta estima o alcance e dispara recalibração se sair da faixa.",
      status: inferStatusFromJobs(jobs),
      detail: <span className="text-muted">Depende da validação dos públicos.</span>,
    };
  }
  const notValidated = specs.some((spec) => spec.validationStatus !== "VALID");
  if (notValidated) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title: "Reach Estimate (BR)",
      description: "Meta estima o alcance e dispara recalibração se sair da faixa.",
      status: "PENDING",
      detail: <span className="text-muted">Executado somente após todos receberem VALID.</span>,
    };
  }
  const waitingSpec = specs.find((spec) => spec.validationStatus === "VALID" && !spec.reachStatus);
  if (waitingSpec) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title: "Reach Estimate (BR)",
      description: "Meta estima o alcance e dispara recalibração se sair da faixa.",
      status: jobs.some((job) => job.status === "RUNNING") ? "RUNNING" : "PENDING",
      detail: <span className="text-muted">Aguardando resposta para {slotLabel(waitingSpec.slot)}.</span>,
    };
  }
  const outOfRange = specs.find((spec) => isSpecReachOutOfRange(spec));
  if (outOfRange) {
    return {
      id: "FACEBOOK_REACH_ESTIMATE",
      title: "Reach Estimate (BR)",
      description: "Meta estima o alcance e dispara recalibração se sair da faixa.",
      status: "FAILED",
      detail: (
        <div className="small text-warning">
          {slotLabel(outOfRange.slot)} ficou fora da faixa de {formatNumber(REACH_MIN)} – {formatNumber(REACH_MAX)} pessoas.
          {" "}Uma nova rodada de recalibração foi solicitada.
        </div>
      ),
    };
  }
  return {
    id: "FACEBOOK_REACH_ESTIMATE",
    title: "Reach Estimate (BR)",
    description: "Meta estima o alcance e dispara recalibração se sair da faixa.",
    status: "DONE",
    detail: <div className="small">Todos os públicos com status READY.</div>,
    helper: renderSpecStatusList(specs, (spec) =>
      `${formatNumber(spec.reachLowerBound)} – ${formatNumber(spec.reachUpperBound)} pessoas`
    ),
  };
}

function buildRecalibrationStep(specs: ExperimentAdSetSpec[], jobs: ExperimentAdSetJob[]): PipelineStepSummary {
  if (!jobs.length) {
    const outOfRange = specs.some((spec) => isSpecReachOutOfRange(spec));
    return {
      id: "AI_RECALIBRATE_SPEC",
      title: "Recalibração automática (IA)",
      description: "IA ajusta o flexible_spec quando o reach sai da faixa ideal.",
      optional: true,
      status: outOfRange ? "FAILED" : "SKIPPED",
      detail: (
        <span className="text-muted">
          {outOfRange
            ? "Algum público ficou fora da faixa e o workflow foi interrompido."
            : "Nenhum ajuste foi necessário (todos dentro da meta)."}
        </span>
      ),
    };
  }
  return {
    id: "AI_RECALIBRATE_SPEC",
    title: "Recalibração automática (IA)",
    description: "IA ajusta o flexible_spec quando o reach sai da faixa ideal.",
    optional: true,
    status: inferStatusFromJobs(jobs),
    detail: (
      <div className="small">
        Ajustes disparados para:
        <ul className="mb-0 ps-3">
          {jobs.map((job) => {
            const specSlot = specs.find((spec) => spec.id === job.resourceId)?.slot;
            const label = specSlot ? slotLabel(specSlot) : `Spec ${job.resourceId ?? "—"}`;
            return (
              <li key={job.id}>
                {label} · Job #{job.id} · Status {job.status}
                {job.attemptCount != null ? ` · Tentativas ${job.attemptCount}` : null}
              </li>
            );
          })}
        </ul>
      </div>
    ),
  };
}

function groupJobsByType(jobs?: ExperimentAdSetJob[]): Map<string, ExperimentAdSetJob[]> {
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

function BadgeList({ items, placeholder }: { items: string[]; placeholder?: string }) {
  if (!items.length) {
    return <span className="text-muted">{placeholder ?? "—"}</span>;
  }
  return (
    <div className="d-flex flex-wrap gap-2">
      {items.map((item, index) => (
        <span key={`${item}-${index}`} className="badge text-bg-light border text-muted">
          {item}
        </span>
      ))}
    </div>
  );
}

function SectionLabel({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={`text-uppercase text-muted small fw-semibold ${className ?? ""}`.trim()}>
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
  if (typeof node.message === "string" && node.message.trim()) return node.message;
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
      return first.error_message ?? first.description ?? first.summary ?? undefined;
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
  if (first.estimate_ready === true && first.users_lower_bound && first.users_upper_bound) {
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
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
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
