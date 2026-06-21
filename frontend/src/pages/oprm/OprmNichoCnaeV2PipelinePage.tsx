import { Bot, Globe2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import {
  useOprmNichoCnaeV2Jobs,
  type OprmNichoCnaeV2JobSummary,
} from "../../api/oprm/useOprmNichoCnaeV2Jobs";
import { useStartOprmNichoCnaeV2Job } from "../../api/oprm/useStartOprmNichoCnaeV2Job";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const stageLabels: Record<string, string> = {
  "candidate-generator": "Gerador de Candidatos",
  "candidate generator": "Gerador de Candidatos",
  "source-safety-filter": "Filtro de Segurança das Fontes",
  "source safety filter": "Filtro de Segurança das Fontes",
  "adaptive-query-planner": "Planejador Adaptativo de Buscas",
  "adaptive query planner": "Planejador Adaptativo de Buscas",
  "candidate-tournament": "Torneio de Candidatos",
  "candidate tournament": "Torneio de Candidatos",
  "source-fetcher-reranker": "Coletor e Reordenador de Fontes",
  "source fetcher reranker": "Coletor e Reordenador de Fontes",
  "signal-extractor": "Extrator de Sinais",
  "signal extractor": "Extrator de Sinais",
  "semantic-judge-entailment": "Juiz Semântico e Validação de Evidência",
  "semantic judge entailment": "Juiz Semântico e Validação de Evidência",
  "knowledge-accumulator": "Acumulador de Conhecimento",
  "knowledge accumulator": "Acumulador de Conhecimento",
  "reprocess-controller": "Controlador de Reprocessamento",
  "reprocess controller": "Controlador de Reprocessamento",
  "routine-synthesizer": "Sintetizador de Rotina",
  "routine synthesizer": "Sintetizador de Rotina",
  "commercial-evidence-gate": "Gate de Nível de Evidência E0–E5",
  "commercial evidence gate": "Gate de Nível de Evidência E0–E5",
  "enriched-niche-materializer": "Materializador de Nicho Enriquecido",
  "enriched niche materializer": "Materializador de Nicho Enriquecido",
};

export function formatStage(stageCode: string | null | undefined) {
  const normalizedStageCode = stageCode?.trim().toLowerCase();
  if (!normalizedStageCode) return "Sem etapa aberta";
  const slugStageCode = normalizedStageCode.replace(/[_\s]+/g, "-");
  return (
    stageLabels[normalizedStageCode] ?? stageLabels[slugStageCode] ?? stageCode
  );
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatAiCost(value: number | string | null | undefined) {
  const numericValue = Number(value ?? 0);
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
    maximumFractionDigits: 6,
  }).format(Number.isFinite(numericValue) ? numericValue : 0);
}

function formatJobId(jobId: string) {
  const parts = jobId.split("-");
  const lastPart = parts[parts.length - 1];
  if (lastPart?.startsWith("job")) {
    return lastPart.toUpperCase();
  }
  return jobId;
}

function explainStatus(job: OprmNichoCnaeV2JobSummary, open: boolean) {
  if (open) return "Aguardando processamento";
  if (job.outcomeStatus === "FAILURE") return "Fracasso";
  if (job.outcomeStatus === "SUCCESS") return "Sucesso";
  if (job.lastStageStatus === "FAILED") return "Falhou e parou";
  if (job.finalDecision === "NO_VIABLE_SUBNICHE") return "Sem subnicho viável";
  return job.finalDecisionLabel ?? job.finalDecision ?? "Concluído";
}

function simplifyDecisionReason(reason: string | null | undefined) {
  if (!reason) return null;
  const reasonCode = reason.match(/reasonCode=([^;]+)/)?.[1];
  const exception =
    reason.match(/exception=([^;]+)/)?.[1] ??
    reason.match(/^([A-Za-z]+Exception)/)?.[1];
  if (reasonCode === "TECHNICAL_RETRY_LIMIT_EXCEEDED") {
    return "O sistema tentou novamente, mas atingiu o limite técnico desta etapa.";
  }
  if (exception === "NullPointerException") {
    return "Erro técnico interno ao preparar os dados da etapa.";
  }
  const firstSentence = reason.split(/[.;]/)[0]?.trim();
  return firstSentence || reason;
}

function getOperatorNextAction(job: OprmNichoCnaeV2JobSummary, open: boolean) {
  if (open) return "Aguardar o executor continuar este job.";
  if (job.outcomeMessage) return job.outcomeMessage;
  if (job.lastStageStatus === "FAILED") {
    return "Corrigir a falha técnica antes de iniciar novo job.";
  }
  if (job.finalDecision === "NO_VIABLE_SUBNICHE") {
    return "Usar o histórico como aprendizado e pesquisar outro recorte.";
  }
  return "Verificar se o nicho gerado já pode avançar para produto.";
}

function decisionBadgeClass(job: OprmNichoCnaeV2JobSummary, open: boolean) {
  if (open) return "badge text-bg-warning";
  if (job.outcomeStatus === "FAILURE") return "badge text-bg-danger";
  if (job.outcomeStatus === "SUCCESS") return "badge text-bg-success";
  if (job.finalDecision === "NO_VIABLE_SUBNICHE") {
    return "badge text-bg-secondary";
  }
  if (job.lastStageStatus === "FAILED") return "badge text-bg-danger";
  return "badge text-bg-success";
}

function JobsTable({
  jobs,
  emptyMessage,
  open,
}: {
  jobs: OprmNichoCnaeV2JobSummary[];
  emptyMessage: string;
  open: boolean;
}) {
  if (jobs.length === 0) {
    return <p className="text-secondary mb-0">{emptyMessage}</p>;
  }

  return (
    <div className="oprm-v2-job-list">
      {jobs.map((job) => {
        const stageName = formatStage(
          open ? job.currentStageCode : job.lastStageCode,
        );
        const simplifiedReason = simplifyDecisionReason(
          job.finalDecisionReason,
        );
        return (
          <article className="oprm-v2-job-card" key={job.jobId}>
            <div className="oprm-v2-job-card__header">
              <div>
                <div className="oprm-v2-job-card__label">Job</div>
                <div className="oprm-v2-job-card__title">
                  {formatJobId(job.jobId)}
                </div>
                <div className="oprm-v2-job-card__meta" title={job.jobId}>
                  Código completo: {job.jobId}
                </div>
              </div>
              <span className={decisionBadgeClass(job, open)}>
                {explainStatus(job, open)}
              </span>
            </div>

            <div className="oprm-v2-job-card__body">
              <div className="oprm-v2-job-card__main">
                <div className="oprm-v2-job-card__label">
                  {open ? "Onde está parado" : "Última etapa"}
                </div>
                <div className="oprm-v2-job-card__stage">{stageName}</div>
                {simplifiedReason ? (
                  <p className="oprm-v2-job-card__reason mb-0">
                    {simplifiedReason}
                  </p>
                ) : null}
              </div>
              <div className="oprm-v2-job-card__action">
                <div className="oprm-v2-job-card__label">
                  Resultado e próximo passo
                </div>
                <div>{getOperatorNextAction(job, open)}</div>
                {!open && job.actionLabel && job.actionUrl ? (
                  <Link
                    className="btn btn-sm btn-primary mt-3"
                    to={job.actionUrl}
                  >
                    {job.actionLabel}
                  </Link>
                ) : null}
              </div>
            </div>

            <div className="oprm-v2-job-card__footer">
              <span>IA: {formatAiCost(job.aiCostUsd)}</span>
              <span>
                Tentativa: {job.attemptNumber ?? "—"}
                {job.technicalRetryNumber
                  ? ` · retry ${job.technicalRetryNumber}`
                  : ""}
              </span>
              <span>Atualizado: {formatDateTime(job.updatedAt)}</span>
            </div>
          </article>
        );
      })}
    </div>
  );
}

const v2Stages: Array<{
  number: number;
  title: string;
  status: string;
  purpose: string;
  output: string;
  businessGate: string;
  usesAi: boolean;
  usesWeb: boolean;
}> = [
  {
    number: 1,
    title: "Gerador de Candidatos",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Gerar candidatos neutros de subnicho sem contaminar nome, dor, canal ou promessa antes da evidência.",
    output:
      "4 a 6 candidatos comparáveis, com identidade do executor, job operacional e hipóteses separadas.",
    businessGate:
      "Nenhum vencedor obrigatório quando não houver candidato viável.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 2,
    title: "Filtro de Segurança das Fontes",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Bloquear domínios inseguros, conteúdo inadequado e resultados fora do contexto antes de gastar IA.",
    output: "Lista segura e deduplicada de URLs candidatas para pesquisa.",
    businessGate: "Conteúdo inseguro ou contaminado não entra no pipeline.",
    usesAi: false,
    usesWeb: true,
  },
  {
    number: 3,
    title: "Planejador Adaptativo de Buscas",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Planejar buscas pelos gaps reais de conhecimento, reaproveitando queries, fontes e falhas anteriores.",
    output:
      "Plano de pesquisa curto, natural e orientado a lacunas de evidência.",
    businessGate:
      "A pesquisa aprofunda apenas o que pode mudar a decisão comercial.",
    usesAi: true,
    usesWeb: true,
  },
  {
    number: 4,
    title: "Torneio de Candidatos",
    status: "Design",
    purpose:
      "Comparar candidatos por densidade e qualidade de evidências antes de escolher finalistas.",
    output: "Até dois finalistas ou decisão NO_VIABLE_SUBNICHE.",
    businessGate:
      "O vencedor nasce de evidência observada, não de opinião prévia do modelo.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 5,
    title: "Coletor e Reordenador de Fontes",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Coletar páginas úteis e priorizar fontes diretas, independentes e alinhadas ao objetivo do gate.",
    output:
      "Snapshots rastreáveis com origem, trecho curto, metadados e custo.",
    businessGate:
      "Fonte adjacente não substitui prova direta do executor específico.",
    usesAi: true,
    usesWeb: true,
  },
  {
    number: 6,
    title: "Extrator de Sinais",
    status: "Design aprovado · implementação parcial",
    purpose:
      "Extrair claims somente quando houver trecho exato, ator correto, contexto compatível e relação sustentada.",
    output:
      "Claims auditáveis com trecho literal, fonte e diagnóstico semântico.",
    businessGate: "Nenhum claim sem trecho exato pode avançar para síntese.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 7,
    title: "Juiz Semântico e Validação de Evidência",
    status: "Design",
    purpose:
      "Validar se o trecho realmente sustenta a afirmação sobre o executor e o contexto pesquisado.",
    output:
      "Claims aprovados, rejeitados, contraditórios ou pendentes por nível de evidência.",
    businessGate: "Proximidade lexical não é prova de mercado.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 8,
    title: "Acumulador de Conhecimento",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Consolidar conhecimento versionado do ciclo, preservando fontes aceitas, rejeições, gaps e aprendizados.",
    output:
      "Snapshot de conhecimento com versão, linhagem e lacunas acionáveis.",
    businessGate:
      "Reprocessar sem repetir erro, fonte rejeitada ou custo desnecessário.",
    usesAi: false,
    usesWeb: false,
  },
  {
    number: 9,
    title: "Controlador de Reprocessamento",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Decidir o menor rewind necessário quando um gate falhar por qualidade, mantendo o mesmo job quando aplicável.",
    output:
      "Retry técnico ou reprocessamento cognitivo com motivo, estágio de retorno e versão de conhecimento.",
    businessGate:
      "Falha técnica não vira reprovação de mercado; falta de evidência não reinicia tudo.",
    usesAi: false,
    usesWeb: false,
  },
  {
    number: 10,
    title: "Sintetizador de Rotina",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Sintetizar rotina, dores e resultados apenas a partir de claims aprovados e evidências rastreáveis.",
    output:
      "Rotina funcional do executor com evidências, IDs de claims, domínios e limites explícitos.",
    businessGate: "Síntese não pode inventar dor, canal ou impacto econômico.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 11,
    title: "Gate de Nível de Evidência E0–E5",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Separar existência da atividade, dor prática, impacto econômico e intenção de compra por nível de evidência.",
    output:
      "Nível E0 a E5, confiança explicável, motivos de reprovação e próximos movimentos.",
    businessGate:
      "Materialização automática só avança com evidência comercial mínima.",
    usesAi: true,
    usesWeb: false,
  },
  {
    number: 12,
    title: "Materializador de Nicho Enriquecido",
    status:
      "Design aprovado · implementação inicial protegida por feature flag",
    purpose:
      "Materializar no executor externo o nicho enriquecido somente depois dos gates de evidência, qualidade e segurança.",
    output:
      "Nicho pronto para decisão de produto: executor, dor, resultado, mecanismo plausível e fontes.",
    businessGate: "A v2 calibra antes de publicar automaticamente para vendas.",
    usesAi: false,
    usesWeb: false,
  },
];

export default function OprmNichoCnaeV2PipelinePage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : undefined;
  const startJobMutation = useStartOprmNichoCnaeV2Job(decodedCnaeCode ?? "");
  const jobsQuery = useOprmNichoCnaeV2Jobs(decodedCnaeCode ?? "");
  const cnaeAiCostUsd = jobsQuery.data?.cnaeAiCostUsd ?? 0;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <PageTitle>Pipeline NichoCNAE v2</PageTitle>
            <p className="text-secondary mb-0">
              Design das etapas da v2 para transformar CNAEs em nichos vendáveis
              com evidência auditável, reprocessamento inteligente e gates de
              qualidade antes da materialização.
            </p>
          </div>
          <Link className="btn btn-outline-secondary" to="/oprm">
            Voltar para CNAEs
          </Link>
        </div>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">
                {decodedCnaeCode
                  ? `CNAE ${decodedCnaeCode}`
                  : "Visão geral da v2"}
              </h2>
              <p className="text-secondary mb-0">
                A tela é um mapa de produto: mostra a sequência planejada mesmo
                quando uma etapa ainda está em design ou protegida por feature
                flag.
              </p>
            </div>
            <div className="d-flex flex-column align-items-start align-items-sm-end gap-2">
              <span className="badge text-bg-primary align-self-start align-self-sm-end">
                v2 · qualidade antes de escala
              </span>
              {decodedCnaeCode ? (
                <button
                  className="btn btn-primary"
                  type="button"
                  onClick={() => startJobMutation.mutate()}
                  disabled={startJobMutation.isPending}
                >
                  {startJobMutation.isPending ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        aria-hidden="true"
                      />
                      Iniciando job...
                    </>
                  ) : (
                    "Iniciar novo job v2"
                  )}
                </button>
              ) : null}
            </div>
          </div>
          {startJobMutation.isSuccess ? (
            <div className="alert alert-success mt-3 mb-0" role="status">
              Job {startJobMutation.data.jobId} gravado como pendente. O módulo
              externo OPRM fará a execução pelo endpoint pending.
            </div>
          ) : null}
          {startJobMutation.isError ? (
            <div className="alert alert-danger mt-3 mb-0" role="alert">
              {startJobMutation.error.message}
            </div>
          ) : null}
          {decodedCnaeCode && jobsQuery.data ? (
            <div className="alert alert-info mt-3 mb-0" role="status">
              <strong>Custo de IA contabilizado no CNAE:</strong>{" "}
              {formatAiCost(cnaeAiCostUsd)}.{" "}
              {jobsQuery.data.cnaeUsedAi
                ? "Há sinal de uso de IA em pelo menos um job deste CNAE."
                : "Nenhum uso de IA foi registrado nos jobs listados deste CNAE."}
            </div>
          ) : null}
        </div>
      </section>

      {decodedCnaeCode ? (
        <section
          className="oprm-v2-jobs-section"
          aria-label="Jobs do CNAE no pipeline NichoCNAE v2"
        >
          <article className="oprm-v2-job-panel oprm-v2-job-panel--open">
            <div className="oprm-v2-job-panel__accent" aria-hidden="true" />
            <div className="oprm-v2-job-panel__content">
              <div className="d-flex align-items-start justify-content-between gap-3 mb-3">
                <div>
                  <span className="oprm-v2-job-panel__eyebrow">Agora</span>
                  <h2 className="h5 mb-1">Jobs abertos</h2>
                  <p className="text-secondary small mb-0">
                    Mostra onde cada execução ainda aberta está parada agora.
                  </p>
                </div>
                <span
                  className="oprm-v2-job-panel__status-dot"
                  aria-hidden="true"
                />
              </div>
              {jobsQuery.isLoading ? (
                <p className="text-secondary mb-0">Carregando jobs...</p>
              ) : jobsQuery.isError ? (
                <div className="alert alert-danger mb-0" role="alert">
                  {jobsQuery.error.message}
                </div>
              ) : (
                <JobsTable
                  jobs={jobsQuery.data?.openJobs ?? []}
                  emptyMessage="Nenhum job aberto para este CNAE."
                  open
                />
              )}
            </div>
          </article>
          <article className="oprm-v2-job-panel oprm-v2-job-panel--completed">
            <div className="oprm-v2-job-panel__accent" aria-hidden="true" />
            <div className="oprm-v2-job-panel__content">
              <div className="d-flex align-items-start justify-content-between gap-3 mb-3">
                <div>
                  <span className="oprm-v2-job-panel__eyebrow">Histórico</span>
                  <h2 className="h5 mb-1">Jobs concluídos</h2>
                  <p className="text-secondary small mb-0">
                    Histórico dos jobs encerrados com mensagem clara de sucesso
                    ou fracasso e o comando para visualizar ou materializar o
                    nicho.
                  </p>
                </div>
                <span
                  className="oprm-v2-job-panel__status-dot"
                  aria-hidden="true"
                />
              </div>
              {jobsQuery.isLoading ? (
                <p className="text-secondary mb-0">Carregando histórico...</p>
              ) : jobsQuery.isError ? (
                <div className="alert alert-danger mb-0" role="alert">
                  {jobsQuery.error.message}
                </div>
              ) : (
                <JobsTable
                  jobs={jobsQuery.data?.completedJobs ?? []}
                  emptyMessage="Nenhum job concluído para este CNAE."
                  open={false}
                />
              )}
            </div>
          </article>
        </section>
      ) : null}

      <section className="row g-3" aria-label="Etapas do pipeline NichoCNAE v2">
        {v2Stages.map((stage) => (
          <article className="col-12 col-xl-6" key={stage.number}>
            <div className="card h-100 border-0 shadow-sm">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex justify-content-between gap-3">
                  <div>
                    <span className="badge text-bg-light border mb-2">
                      Etapa {stage.number}
                    </span>
                    <h3 className="h5 mb-1">{stage.title}</h3>
                    <span className="small text-primary fw-semibold">
                      {stage.status}
                    </span>
                  </div>
                  <div
                    className="d-flex flex-wrap justify-content-end align-content-start gap-2"
                    aria-label="Recursos usados pela etapa"
                  >
                    {stage.usesAi ? (
                      <span className="badge rounded-pill text-bg-primary d-inline-flex align-items-center gap-1">
                        <Bot size={14} aria-hidden="true" />
                        IA
                      </span>
                    ) : null}
                    {stage.usesWeb ? (
                      <span className="badge rounded-pill text-bg-info d-inline-flex align-items-center gap-1">
                        <Globe2 size={14} aria-hidden="true" />
                        Web
                      </span>
                    ) : null}
                  </div>
                </div>
                <p className="mb-0">{stage.purpose}</p>
                <dl className="row small mb-0 g-2">
                  <dt className="col-sm-4 text-secondary fw-normal">Saída</dt>
                  <dd className="col-sm-8 mb-0">{stage.output}</dd>
                  <dt className="col-sm-4 text-secondary fw-normal">Gate</dt>
                  <dd className="col-sm-8 mb-0">{stage.businessGate}</dd>
                </dl>
              </div>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
