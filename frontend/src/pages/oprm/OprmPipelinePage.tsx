import { Link } from "react-router-dom";
import { useOprmNicheResearchSeedBuilderDetail } from "../../api/oprm/useOprmNicheResearchSeedBuilderDetail";
import { useOprmNicheResearchSeedBuilderPending } from "../../api/oprm/useOprmNicheResearchSeedBuilderPending";
import { useOprmEnrichedNicheMaterializerDetail } from "../../api/oprm/useOprmEnrichedNicheMaterializerDetail";
import { useOprmRoutineResearchOrchestratorRecent } from "../../api/oprm/useOprmRoutineResearchOrchestratorRecent";
import { useOprmRoutineSynthesizerDetail } from "../../api/oprm/useOprmRoutineSynthesizerDetail";
import { useOprmRoutineQualityGateDetail } from "../../api/oprm/useOprmRoutineQualityGateDetail";
import { useOprmSourceFetcherDetail } from "../../api/oprm/useOprmSourceFetcherDetail";
import { useOprmSignalExtractorDetail } from "../../api/oprm/useOprmSignalExtractorDetail";
import { useOprmSourceSearcherDetail } from "../../api/oprm/useOprmSourceSearcherDetail";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const pipelineStages = [
  {
    number: "1",
    title: "Ciclo de Pesquisa de Rotina",
    technicalName: "oprmRoutineResearchCycle",
    description:
      "Controla a execução completa da pesquisa de rotina do nicho, mantendo CNAE, score, status, contadores e rastreabilidade.",
    output: "Ciclo pai do pipeline pronto para receber as próximas etapas.",
  },
  {
    number: "2",
    title: "Seed de Pesquisa do Nicho",
    technicalName: "oprmNicheResearchSeedBuilder",
    description:
      "Usa IA para transformar o CNAE em nicho operacional, contexto de rotina e queries sem procurar solução.",
    output:
      "Seed do nicho e frases de pesquisa para rotina, tarefas, dificuldades, perguntas e linguagem pública.",
  },
  {
    number: "3",
    title: "Busca de Fontes",
    technicalName: "oprmSourceSearcher",
    description:
      "Executa as queries planejadas em provedor de busca e registra páginas, documentos e conteúdos públicos candidatos.",
    output: "Fontes candidatas vinculadas ao ciclo e às queries pesquisadas.",
  },
  {
    number: "4",
    title: "Coleta de Fontes",
    technicalName: "oprmSourceFetcher",
    description:
      "Seleciona fontes relevantes e coleta metadados, snippets e trechos curtos sem armazenar HTML completo no MVP.",
    output: "Snapshots leves das fontes para extração de sinais.",
  },
  {
    number: "5",
    title: "Extração de Sinais",
    technicalName: "oprmSignalExtractor",
    description:
      "Extrai sinais estruturados sobre rotina, tarefas recorrentes, dificuldades, perguntas, linguagem e risco de solução.",
    output: "Sinais classificados para sustentar a síntese da rotina.",
  },
  {
    number: "6",
    title: "Síntese da Rotina",
    technicalName: "oprmRoutineSynthesizer",
    description:
      "Monta o cartão de rotina do nicho a partir dos sinais extraídos, sem criar oferta, campanha ou landing page.",
    output:
      "oprm_niche_routine_card com rotina, dores, resultados e evidências.",
  },
  {
    number: "7",
    title: "Gate de Qualidade",
    technicalName: "oprmRoutineQualityGate",
    description:
      "Avalia se o card representa a rotina e as dificuldades com fontes suficientes, baixa duplicação e baixo risco de solução.",
    output:
      "Decisão operacional: rotina pesquisada, precisa de mais pesquisa ou ficou genérica/contaminada.",
  },
  {
    number: "8",
    title: "Nicho Enriquecido",
    technicalName: "oprmEnrichedNicheMaterializer",
    description:
      "Etapa final que alimenta a tabela de nicho e a tabela de nicho enriquecido a partir do card aprovado.",
    output:
      "market_niche + market_niche_enrichment_profile com nome neutro, rotina, dificuldades, linguagem e evidências auditáveis.",
  },
];

function getOperationalName(item: {
  neutralNicheName?: string | null;
  nicheName: string;
}) {
  return item.neutralNicheName?.trim() || item.nicheName;
}

function getOriginalName(item: {
  originalNicheName?: string | null;
  nicheName: string;
}) {
  return item.originalNicheName?.trim() || item.nicheName;
}

function countByValue<T>(
  items: T[],
  selector: (item: T) => string | null | undefined,
) {
  return items.reduce<Record<string, number>>((acc, item) => {
    const rawValue = selector(item)?.trim();
    const value = rawValue || "Não classificado";
    acc[value] = (acc[value] ?? 0) + 1;
    return acc;
  }, {});
}

function formatMix(mix: Record<string, number>) {
  const entries = Object.entries(mix);
  if (entries.length === 0) {
    return "Sem dados";
  }
  return entries
    .sort(([, leftCount], [, rightCount]) => rightCount - leftCount)
    .map(([label, count]) => `${label}: ${count}`)
    .join(" · ");
}

function formatRiskScore(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "Sem score";
  }
  return `${Number(value).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
}

function formatProcessedAt(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Horário indisponível";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

const statusLabels: Record<string, string> = {
  LIGHTLY_RESEARCHED: "Pesquisa inicial concluída",
  NEEDS_MORE_RESEARCH: "Precisa de mais pesquisa",
  GENERIC: "Genérico",
  FAILED: "Falhou",
  RUNNING: "Em execução",
  PENDING: "Pendente",
  COMPLETED: "Concluído",
  ENRICHED_NICHE_CREATED: "Nicho enriquecido criado",
  ENRICHED_NICHE_FAILED: "Falha no nicho enriquecido",
};

function formatStatusLabel(status: string) {
  return statusLabels[status] ?? status;
}

function formatQualityNotes(value: string) {
  return Object.entries(statusLabels).reduce(
    (formatted, [status, label]) => formatted.split(status).join(label),
    value,
  );
}

function buildStatusBadgeClass(status: string) {
  if (status === "LIGHTLY_RESEARCHED") {
    return "badge text-bg-success-subtle border border-success-subtle text-success";
  }
  if (status === "NEEDS_MORE_RESEARCH") {
    return "badge text-bg-warning-subtle border border-warning-subtle text-warning";
  }
  if (status === "GENERIC") {
    return "badge text-bg-secondary-subtle border border-secondary-subtle text-secondary";
  }
  if (status === "FAILED") {
    return "badge text-bg-danger-subtle border border-danger-subtle text-danger";
  }
  if (status === "RUNNING") {
    return "badge text-bg-primary-subtle border border-primary-subtle text-primary";
  }
  return "badge text-bg-light border text-secondary";
}

function buildFailureMessage(errorMessage?: string | null) {
  if (!errorMessage?.trim()) {
    return "Falha sem mensagem registrada. Verifique os logs do backend/coletor.";
  }
  return errorMessage.trim();
}

function getStageCardClass(stageNumber: string, hasContinuationError: boolean) {
  if (stageNumber === "1" && hasContinuationError) {
    return "card border border-warning-subtle shadow-sm h-100";
  }
  return "card border-0 shadow-sm h-100";
}

function formatStageCount(value?: number) {
  return Number.isFinite(value) ? value : 0;
}

function formatQueryGoal(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return "Erro operacional sem mensagem detalhada. Verifique logs do backend/coletor OPRM.";
}

export default function OprmPipelinePage() {
  const {
    data: recentProcessed = [],
    isError,
    isLoading,
  } = useOprmRoutineResearchOrchestratorRecent(10);
  const latestCycle = recentProcessed[0];
  const latestRunningCycle = recentProcessed.find(
    (item) => item.cycleStatus === "RUNNING",
  );
  const {
    data: seedBuilderPending = [],
    error: seedBuilderPendingError,
    isError: isSeedBuilderPendingError,
    isFetching: isSeedBuilderPendingFetching,
  } = useOprmNicheResearchSeedBuilderPending(Boolean(latestRunningCycle));
  const hasStageOneContinuationError =
    Boolean(latestRunningCycle) && isSeedBuilderPendingError;
  const stageOnePendingSeed = latestRunningCycle
    ? seedBuilderPending.find(
        (item) => item.researchCycleId === latestRunningCycle.researchCycleId,
      )
    : undefined;
  const {
    data: seedBuilderDetail,
    error: seedBuilderDetailError,
    isError: isSeedBuilderDetailError,
    isFetching: isSeedBuilderDetailFetching,
  } = useOprmNicheResearchSeedBuilderDetail(latestCycle?.researchCycleId);
  const generatedSeed = seedBuilderDetail?.seed;
  const {
    data: sourceSearcherDetail,
    error: sourceSearcherDetailError,
    isError: isSourceSearcherDetailError,
    isFetching: isSourceSearcherDetailFetching,
  } = useOprmSourceSearcherDetail(latestCycle?.researchCycleId);
  const latestSourceCandidate = sourceSearcherDetail?.candidates?.[0];
  const {
    data: sourceFetcherDetail,
    error: sourceFetcherDetailError,
    isError: isSourceFetcherDetailError,
    isFetching: isSourceFetcherDetailFetching,
  } = useOprmSourceFetcherDetail(latestCycle?.researchCycleId);
  const sourceSnapshots = sourceFetcherDetail?.snapshots ?? [];
  const latestSourceSnapshot = sourceSnapshots[sourceSnapshots.length - 1];
  const {
    data: signalExtractorDetail,
    error: signalExtractorDetailError,
    isError: isSignalExtractorDetailError,
    isFetching: isSignalExtractorDetailFetching,
  } = useOprmSignalExtractorDetail(latestCycle?.researchCycleId);
  const latestSignals =
    signalExtractorDetail?.signals?.slice(-3).reverse() ?? [];
  const {
    data: routineSynthesizerDetail,
    error: routineSynthesizerDetailError,
    isError: isRoutineSynthesizerDetailError,
    isFetching: isRoutineSynthesizerDetailFetching,
  } = useOprmRoutineSynthesizerDetail(latestCycle?.researchCycleId);
  const routineCard = routineSynthesizerDetail?.routineCard;
  const {
    data: routineQualityGateDetail,
    error: routineQualityGateDetailError,
    isError: isRoutineQualityGateDetailError,
    isFetching: isRoutineQualityGateDetailFetching,
  } = useOprmRoutineQualityGateDetail(latestCycle?.researchCycleId);
  const {
    data: enrichedNicheMaterializerDetail,
    error: enrichedNicheMaterializerDetailError,
    isError: isEnrichedNicheMaterializerDetailError,
    isFetching: isEnrichedNicheMaterializerDetailFetching,
  } = useOprmEnrichedNicheMaterializerDetail(latestCycle?.researchCycleId);
  const queryGoalMix = countByValue(
    generatedSeed?.queries ?? [],
    (query) => query.queryGoal,
  );
  const sourceIntentMix = countByValue(
    sourceSearcherDetail?.candidates ?? [],
    (candidate) => candidate.sourceIntent,
  );
  const snapshotIntentMix = countByValue(
    sourceSnapshots,
    (snapshot) => snapshot.sourceIntent,
  );
  const sourceSolutionRiskCount = (
    sourceSearcherDetail?.candidates ?? []
  ).filter((candidate) => candidate.solutionLanguageRisk).length;
  const snapshotSolutionRiskCount = sourceSnapshots.filter(
    (snapshot) => snapshot.solutionLanguageRisk,
  ).length;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Pipeline NichoCNAE</PageTitle>
        <p className="text-secondary mb-0">
          Esta tela mostra o pipeline OPRM que transforma um nicho CNAE já
          priorizado em um cartão de rotina pesquisado. Ingestão de mercado,
          cálculo de score e enriquecimento CNAE ficam na aba CNAEs; aqui o foco
          é conhecer como o nicho funciona no dia a dia antes de qualquer
          solução, produto ou oferta.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-2">Fluxo do pipeline de pesquisa da rotina</h2>
          <p className="text-secondary mb-0">
            Entrada: nicho CNAE com score alto. Saída esperada:
            <strong> oprm_niche_routine_card</strong>, usado para entender
            rotina, dificuldades, perguntas e evidências antes de qualquer fluxo
            posterior de hipótese ou oferta.
          </p>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-column flex-lg-row justify-content-between gap-2 mb-3">
            <div>
              <span className="badge text-bg-primary mb-2">
                oprmRoutineResearchOrchestrator
              </span>
              <h2 className="h5 mb-1">Últimos 10 nichos processados</h2>
              <p className="text-secondary mb-0">
                Mostra os nichos que o orquestrador já selecionou, com o horário
                em que o ciclo de pesquisa de rotina foi criado.
              </p>
            </div>
            <span className="text-secondary small align-self-lg-start">
              Saída esperada: RESEARCH_RUNNING + ciclo criado
            </span>
          </div>

          {isLoading ? (
            <p className="text-secondary mb-0">
              Carregando nichos processados...
            </p>
          ) : isError ? (
            <div className="alert alert-warning mb-0" role="alert">
              Não foi possível carregar os últimos nichos processados pelo
              orquestrador. Atualize a tela ou verifique o backend OPRM.
            </div>
          ) : recentProcessed.length === 0 ? (
            <div className="alert alert-info mb-0" role="status">
              Nenhum ciclo foi criado ainda. Os candidatos com score continuam
              na fila; esta lista só será preenchida depois que o disparo
              agendado ou manual da etapa zero criar o primeiro ciclo.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">Horário</th>
                    <th scope="col">Nicho</th>
                    <th scope="col">CNAE</th>
                    <th scope="col" className="text-end">
                      Score
                    </th>
                    <th scope="col">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentProcessed.map((item) => (
                    <tr key={item.researchCycleId}>
                      <td className="text-nowrap">
                        {formatProcessedAt(item.processedAt)}
                      </td>
                      <td>
                        <span className="fw-semibold d-block">
                          {getOperationalName(item)}
                        </span>
                        <span className="text-secondary small d-block">
                          Original: {getOriginalName(item)}
                        </span>
                        <span className="text-secondary small">
                          Ciclo #{item.researchCycleId} ·{" "}
                          {item.researchMode ?? "modo não informado"}
                        </span>
                      </td>
                      <td>
                        <span className="text-nowrap">{item.cnaeCode}</span>
                        <span className="text-secondary small d-block">
                          {item.cnaeDescription}
                        </span>
                      </td>
                      <td className="text-end">
                        {item.sourceScore.toLocaleString("pt-BR", {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}
                      </td>
                      <td>
                        <span
                          className={buildStatusBadgeClass(item.cycleStatus)}
                        >
                          {formatStatusLabel(item.cycleStatus)}
                        </span>
                        {item.cycleStatus === "FAILED" ? (
                          <div className="text-danger small mt-1">
                            <span className="fw-semibold">Detalhe:</span>{" "}
                            {buildFailureMessage(item.errorMessage)}
                          </div>
                        ) : null}
                        {item.finishedAt ? (
                          <div className="text-secondary small mt-1">
                            Finalizado em {formatProcessedAt(item.finishedAt)}
                          </div>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      <section className="row g-3" aria-label="Etapas do pipeline NichoCNAE">
        {pipelineStages.map((stage) => (
          <div className="col-12 col-lg-3" key={stage.number}>
            <article
              className={getStageCardClass(
                stage.number,
                hasStageOneContinuationError,
              )}
            >
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex align-items-start justify-content-between gap-3">
                  <div
                    className="rounded-circle bg-primary text-white d-inline-flex align-items-center justify-content-center fw-bold flex-shrink-0"
                    style={{ width: 44, height: 44 }}
                    aria-hidden="true"
                  >
                    {stage.number}
                  </div>
                  <span className="badge text-bg-light border text-secondary text-wrap">
                    {stage.technicalName}
                  </span>
                </div>
                <div>
                  <h2 className="h5 mb-2">{stage.title}</h2>
                  <p className="text-secondary mb-3">{stage.description}</p>
                  <div className="border-top pt-3">
                    <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                      Saída da etapa
                    </span>
                    <p className="mb-0 small">{stage.output}</p>
                  </div>
                  {stage.number === "1" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Execução mais recente
                      </span>
                      <dl className="row g-2 small mb-0">
                        <dt className="col-5 text-secondary fw-normal">
                          Ciclo
                        </dt>
                        <dd className="col-7 mb-0 fw-semibold">
                          #{latestCycle.researchCycleId}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Criado em
                        </dt>
                        <dd className="col-7 mb-0">
                          {formatProcessedAt(latestCycle.processedAt)}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">CNAE</dt>
                        <dd className="col-7 mb-0">
                          <span className="d-block">
                            {latestCycle.cnaeCode}
                          </span>
                          <span className="text-secondary">
                            {latestCycle.cnaeDescription}
                          </span>
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Nome neutro
                        </dt>
                        <dd className="col-7 mb-0">
                          {getOperationalName(latestCycle)}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Nome original
                        </dt>
                        <dd className="col-7 mb-0">
                          {getOriginalName(latestCycle)}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">Modo</dt>
                        <dd className="col-7 mb-0">
                          {latestCycle.researchMode ?? "Não informado"}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Risco solução
                        </dt>
                        <dd className="col-7 mb-0">
                          {formatRiskScore(
                            latestCycle.solutionLanguageRiskScore,
                          )}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Score
                        </dt>
                        <dd className="col-7 mb-0">
                          {latestCycle.sourceScore.toLocaleString("pt-BR", {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </dd>
                        <dt className="col-5 text-secondary fw-normal">
                          Status
                        </dt>
                        <dd className="col-7 mb-0">
                          <span
                            className={buildStatusBadgeClass(
                              latestCycle.cycleStatus,
                            )}
                          >
                            {formatStatusLabel(latestCycle.cycleStatus)}
                          </span>
                          {latestCycle.finishedAt ? (
                            <span className="text-secondary d-block mt-1">
                              Finalizado em{" "}
                              {formatProcessedAt(latestCycle.finishedAt)}
                            </span>
                          ) : null}
                        </dd>
                      </dl>
                      {latestCycle.errorMessage ? (
                        <div className="text-danger small mt-2">
                          <span className="fw-semibold">Erro:</span>{" "}
                          {buildFailureMessage(latestCycle.errorMessage)}
                        </div>
                      ) : null}
                    </div>
                  ) : null}

                  {stage.number === "2" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Dados gerados pela IA
                      </span>
                      {isSeedBuilderDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando seed e queries do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isSeedBuilderDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(seedBuilderDetailError)}
                        </div>
                      ) : generatedSeed ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-5 text-secondary fw-normal">
                              Seed
                            </dt>
                            <dd className="col-7 mb-0 fw-semibold">
                              #{generatedSeed.nicheResearchSeedId}
                            </dd>
                            <dt className="col-5 text-secondary fw-normal">
                              Gerado em
                            </dt>
                            <dd className="col-7 mb-0">
                              {formatProcessedAt(generatedSeed.createdAt)}
                            </dd>
                            <dt className="col-5 text-secondary fw-normal">
                              Nicho
                            </dt>
                            <dd className="col-7 mb-0">
                              {generatedSeed.nicheName}
                            </dd>
                            <dt className="col-5 text-secondary fw-normal">
                              Mix objetivos
                            </dt>
                            <dd className="col-7 mb-0">
                              {formatMix(queryGoalMix)}
                            </dd>
                            <dt className="col-5 text-secondary fw-normal">
                              Tipo
                            </dt>
                            <dd className="col-7 mb-0">
                              {generatedSeed.businessType}
                            </dd>
                            <dt className="col-5 text-secondary fw-normal">
                              Queries
                            </dt>
                            <dd className="col-7 mb-0 fw-semibold">
                              {generatedSeed.totalQueries}
                            </dd>
                          </dl>
                          <p className="text-secondary mb-2">
                            {generatedSeed.initialAssumptions}
                          </p>
                          <p className="text-secondary mb-2">
                            {generatedSeed.queries.length} queries geradas para
                            as próximas etapas. Abra o detalhe para ver a
                            requisição enviada à IA e o JSON completo gerado.
                          </p>
                          <Link
                            className="btn btn-outline-primary btn-sm"
                            to={`/oprm/pipeline/niche-research-seed-builder/${latestCycle.researchCycleId}`}
                          >
                            Ver detalhe da IA
                          </Link>
                        </div>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não tem
                          seed nem queries gravadas pela etapa 2.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "3" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isSourceSearcherDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando buscas executadas do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isSourceSearcherDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(sourceSearcherDetailError)}
                        </div>
                      ) : sourceSearcherDetail ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Queries concluídas
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                sourceSearcherDetail.completedQueries,
                              )}{" "}
                              /{" "}
                              {formatStageCount(
                                sourceSearcherDetail.cycleTotalQueries,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Pendentes
                            </dt>
                            <dd className="col-6 mb-0">
                              {formatStageCount(
                                sourceSearcherDetail.pendingQueries,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Falhas
                            </dt>
                            <dd className="col-6 mb-0">
                              {formatStageCount(
                                sourceSearcherDetail.failedQueries,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Fontes candidatas
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                sourceSearcherDetail.cycleTotalSourceCandidates,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Mix intenção
                            </dt>
                            <dd className="col-6 mb-0">
                              {formatMix(sourceIntentMix)}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Risco solução
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {sourceSolutionRiskCount}
                            </dd>
                            {sourceSearcherDetail.lastExecutedAt ? (
                              <>
                                <dt className="col-6 text-secondary fw-normal">
                                  Última busca
                                </dt>
                                <dd className="col-6 mb-0">
                                  {formatProcessedAt(
                                    sourceSearcherDetail.lastExecutedAt,
                                  )}
                                </dd>
                              </>
                            ) : null}
                            {sourceSearcherDetail.lastSearchProvider ? (
                              <>
                                <dt className="col-6 text-secondary fw-normal">
                                  Provedor
                                </dt>
                                <dd className="col-6 mb-0">
                                  {sourceSearcherDetail.lastSearchProvider}
                                </dd>
                              </>
                            ) : null}
                          </dl>
                          {latestSourceCandidate ? (
                            <p className="text-secondary mb-2">
                              Fonte mais recente:{" "}
                              {latestSourceCandidate.sourceDomain}
                            </p>
                          ) : (
                            <p className="text-secondary mb-2">
                              A busca automática ainda não registrou fontes para
                              este ciclo. O agendador consulta a fila da etapa 3
                              periodicamente.
                            </p>
                          )}
                          {sourceSearcherDetail.lastErrorMessage ? (
                            <div className="text-danger mb-0">
                              <span className="fw-semibold">Última falha:</span>{" "}
                              {sourceSearcherDetail.lastErrorMessage}
                            </div>
                          ) : null}
                        </div>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          resumo da etapa 3.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "4" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isSourceFetcherDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando coletas do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isSourceFetcherDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(sourceFetcherDetailError)}
                        </div>
                      ) : sourceFetcherDetail ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Fontes candidatas
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                sourceFetcherDetail.cycleTotalSourceCandidates,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Snapshots coletados
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                sourceFetcherDetail.cycleTotalSourceSnapshots,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Mix intenção
                            </dt>
                            <dd className="col-6 mb-0">
                              {formatMix(snapshotIntentMix)}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Risco solução
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {snapshotSolutionRiskCount}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Status do ciclo
                            </dt>
                            <dd className="col-6 mb-0">
                              <span
                                className={buildStatusBadgeClass(
                                  sourceFetcherDetail.cycleStatus,
                                )}
                              >
                                {formatStatusLabel(
                                  sourceFetcherDetail.cycleStatus,
                                )}
                              </span>
                            </dd>
                            {latestSourceSnapshot ? (
                              <>
                                <dt className="col-6 text-secondary fw-normal">
                                  Última coleta
                                </dt>
                                <dd className="col-6 mb-0">
                                  {formatProcessedAt(
                                    latestSourceSnapshot.fetchedAt,
                                  )}
                                </dd>
                                <dt className="col-6 text-secondary fw-normal">
                                  HTTP
                                </dt>
                                <dd className="col-6 mb-0">
                                  {latestSourceSnapshot.httpStatus ?? "-"}
                                </dd>
                              </>
                            ) : null}
                          </dl>
                          {latestSourceSnapshot ? (
                            <div className="text-secondary mb-0">
                              <span className="fw-semibold d-block">
                                Fonte mais recente:{" "}
                                {latestSourceSnapshot.sourceDomain}
                              </span>
                              <span className="d-block">
                                {latestSourceSnapshot.sourceTitle}
                              </span>
                              <span className="d-block text-truncate">
                                {latestSourceSnapshot.shortExcerpt}
                              </span>
                            </div>
                          ) : sourceFetcherDetail.cycleTotalSourceCandidates >
                            0 ? (
                            <p className="text-primary mb-0">
                              Há{" "}
                              {sourceFetcherDetail.cycleTotalSourceCandidates}{" "}
                              fontes candidatas aguardando a coleta curta da
                              etapa 4.
                            </p>
                          ) : (
                            <p className="text-secondary mb-0">
                              Nenhuma fonte candidata disponível para coleta
                              neste ciclo.
                            </p>
                          )}
                        </div>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          resumo da etapa 4.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "5" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isSignalExtractorDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando sinais do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isSignalExtractorDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(signalExtractorDetailError)}
                        </div>
                      ) : signalExtractorDetail ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Snapshots coletados
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                signalExtractorDetail.cycleTotalSourceSnapshots,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Sinais extraídos
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                signalExtractorDetail.cycleTotalExtractedSignals,
                              )}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Status do ciclo
                            </dt>
                            <dd className="col-6 mb-0">
                              <span
                                className={buildStatusBadgeClass(
                                  signalExtractorDetail.cycleStatus,
                                )}
                              >
                                {formatStatusLabel(
                                  signalExtractorDetail.cycleStatus,
                                )}
                              </span>
                            </dd>
                          </dl>
                          {latestSignals.length > 0 ? (
                            <div className="d-flex flex-column gap-2">
                              {latestSignals.map((signal) => (
                                <div
                                  className="border rounded-2 p-2 bg-light"
                                  key={signal.extractedSignalId}
                                >
                                  <span className="badge text-bg-light border text-secondary mb-1">
                                    {signal.signalType}
                                  </span>
                                  <p className="mb-1 fw-semibold">
                                    {signal.signalText}
                                  </p>
                                  <p className="mb-1 text-secondary text-truncate">
                                    {signal.evidenceExcerpt}
                                  </p>
                                  <span className="text-secondary">
                                    Fonte: {signal.sourceDomain} · Confiança:{" "}
                                    {signal.confidenceScore}%
                                  </span>
                                </div>
                              ))}
                            </div>
                          ) : signalExtractorDetail.cycleTotalSourceSnapshots >
                            0 ? (
                            <p className="text-primary mb-0">
                              Há{" "}
                              {signalExtractorDetail.cycleTotalSourceSnapshots}{" "}
                              snapshots aguardando a extração estruturada da
                              etapa 5.
                            </p>
                          ) : (
                            <p className="text-secondary mb-0">
                              Nenhum snapshot coletado disponível para extração
                              de sinais neste ciclo.
                            </p>
                          )}
                        </div>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          resumo da etapa 5.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "6" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isRoutineSynthesizerDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando síntese do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isRoutineSynthesizerDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(routineSynthesizerDetailError)}
                        </div>
                      ) : routineCard ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Card
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              #{routineCard.routineCardId}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Confiança
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {routineCard.confidenceScore}%
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Evidência rotina
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {routineCard.routineEvidenceScore}%
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Risco solução
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {routineCard.solutionLanguageRiskScore}%
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Status do ciclo
                            </dt>
                            <dd className="col-6 mb-0">
                              <span
                                className={buildStatusBadgeClass(
                                  routineSynthesizerDetail.cycleStatus,
                                )}
                              >
                                {formatStatusLabel(
                                  routineSynthesizerDetail.cycleStatus,
                                )}
                              </span>
                            </dd>
                          </dl>
                          <p className="mb-2 fw-semibold">
                            {routineCard.nicheName}
                          </p>
                          <p className="text-secondary mb-2 text-truncate">
                            {routineCard.routineSummary}
                          </p>
                          <p className="text-secondary mb-0">
                            Fontes: {routineCard.sourceDomains}
                          </p>
                        </div>
                      ) : (routineSynthesizerDetail?.cycleTotalExtractedSignals ??
                          0) > 0 ? (
                        <p className="text-primary mb-0 small">
                          Há{" "}
                          {routineSynthesizerDetail?.cycleTotalExtractedSignals ??
                            0}{" "}
                          sinais aguardando a síntese do cartão de rotina.
                        </p>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          sinais suficientes para a etapa 6.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "7" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isRoutineQualityGateDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando gate de qualidade do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isRoutineQualityGateDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(routineQualityGateDetailError)}
                        </div>
                      ) : routineQualityGateDetail?.qualityStatus ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Decisão
                            </dt>
                            <dd className="col-6 mb-0">
                              <span
                                className={buildStatusBadgeClass(
                                  routineQualityGateDetail.qualityStatus,
                                )}
                              >
                                {formatStatusLabel(
                                  routineQualityGateDetail.qualityStatus,
                                )}
                              </span>
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Pronto para hipótese
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {routineQualityGateDetail.readyForHypothesis
                                ? "Sim"
                                : "Não"}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Especificidade
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                routineQualityGateDetail.specificityScore ??
                                  undefined,
                              )}
                              %
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Confiança
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                routineQualityGateDetail.confidenceScore ??
                                  undefined,
                              )}
                              %
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Duplicação
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                routineQualityGateDetail.duplicationScore ??
                                  undefined,
                              )}
                              %
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Evidência rotina
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                routineQualityGateDetail.routineEvidenceScore ??
                                  undefined,
                              )}
                              %
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Risco solução
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              {formatStageCount(
                                routineQualityGateDetail.solutionLanguageRiskScore ??
                                  undefined,
                              )}
                              %
                            </dd>
                            {routineQualityGateDetail.checkedAt ? (
                              <>
                                <dt className="col-6 text-secondary fw-normal">
                                  Avaliado em
                                </dt>
                                <dd className="col-6 mb-0">
                                  {formatProcessedAt(
                                    routineQualityGateDetail.checkedAt,
                                  )}
                                </dd>
                              </>
                            ) : null}
                          </dl>
                          {routineQualityGateDetail.qualityNotes ? (
                            <p className="text-secondary mb-0">
                              {formatQualityNotes(
                                routineQualityGateDetail.qualityNotes,
                              )}
                            </p>
                          ) : null}
                        </div>
                      ) : routineCard ? (
                        <p className="text-primary mb-0 small">
                          O card #{routineCard.routineCardId} está sintetizado e
                          aguarda avaliação automática de qualidade.
                        </p>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          card de rotina para avaliar.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "8" && latestCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Resumo da última execução
                      </span>
                      {isEnrichedNicheMaterializerDetailFetching ? (
                        <p className="text-secondary small mb-0">
                          Consultando nicho enriquecido do ciclo #
                          {latestCycle.researchCycleId}...
                        </p>
                      ) : isEnrichedNicheMaterializerDetailError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          {formatErrorMessage(
                            enrichedNicheMaterializerDetailError,
                          )}
                        </div>
                      ) : enrichedNicheMaterializerDetail?.enrichedNicheProfileId ? (
                        <div className="small">
                          <dl className="row g-2 mb-2">
                            <dt className="col-6 text-secondary fw-normal">
                              Nicho
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              #{enrichedNicheMaterializerDetail.marketNicheId}
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Nicho enriquecido
                            </dt>
                            <dd className="col-6 mb-0 fw-semibold">
                              #
                              {
                                enrichedNicheMaterializerDetail.enrichedNicheProfileId
                              }
                            </dd>
                            <dt className="col-6 text-secondary fw-normal">
                              Status do ciclo
                            </dt>
                            <dd className="col-6 mb-0">
                              <span
                                className={buildStatusBadgeClass(
                                  enrichedNicheMaterializerDetail.cycleStatus,
                                )}
                              >
                                {formatStatusLabel(
                                  enrichedNicheMaterializerDetail.cycleStatus,
                                )}
                              </span>
                            </dd>
                            {enrichedNicheMaterializerDetail.materializedAt ? (
                              <>
                                <dt className="col-6 text-secondary fw-normal">
                                  Materializado em
                                </dt>
                                <dd className="col-6 mb-0">
                                  {formatProcessedAt(
                                    enrichedNicheMaterializerDetail.materializedAt,
                                  )}
                                </dd>
                              </>
                            ) : null}
                          </dl>
                          <p className="mb-2 fw-semibold">
                            {enrichedNicheMaterializerDetail.neutralNicheName ??
                              enrichedNicheMaterializerDetail.nicheName}
                          </p>
                          <p className="text-secondary mb-2">
                            Original:{" "}
                            {enrichedNicheMaterializerDetail.originalNicheName ??
                              "Não informado"}{" "}
                            · Modo:{" "}
                            {enrichedNicheMaterializerDetail.researchMode ??
                              "Não informado"}{" "}
                            · Risco solução:{" "}
                            {formatRiskScore(
                              enrichedNicheMaterializerDetail.solutionLanguageRiskScore,
                            )}
                          </p>
                          <p className="text-secondary mb-2 text-truncate">
                            {enrichedNicheMaterializerDetail.painsSummary}
                          </p>
                          <p className="text-secondary mb-0">
                            Evidências:{" "}
                            {enrichedNicheMaterializerDetail.evidenceSummary}
                          </p>
                        </div>
                      ) : routineQualityGateDetail?.readyForHypothesis ? (
                        <p className="text-primary mb-0 small">
                          O card aprovado está pronto para alimentar nicho e
                          nicho enriquecido. O agendador da etapa final executa
                          automaticamente.
                        </p>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestCycle.researchCycleId} ainda não possui
                          card aprovado para materialização final.
                        </p>
                      )}
                    </div>
                  ) : null}

                  {stage.number === "1" && latestRunningCycle ? (
                    <div className="border-top pt-3">
                      <span className="d-block small fw-semibold text-secondary text-uppercase mb-1">
                        Continuidade
                      </span>
                      {hasStageOneContinuationError ? (
                        <div
                          className="alert alert-warning py-2 px-3 mb-0 small"
                          role="alert"
                        >
                          <span className="fw-semibold d-block">
                            Etapa seguinte não inicializou para o ciclo #
                            {latestRunningCycle.researchCycleId}.
                          </span>
                          <span className="d-block">
                            A consulta da fila do seed retornou erro:{" "}
                            {formatErrorMessage(seedBuilderPendingError)}
                          </span>
                        </div>
                      ) : isSeedBuilderPendingFetching ? (
                        <p className="text-secondary small mb-0">
                          Validando fila da etapa 2 para o ciclo #
                          {latestRunningCycle.researchCycleId}...
                        </p>
                      ) : stageOnePendingSeed ? (
                        <p className="text-primary small mb-0">
                          Ciclo #{latestRunningCycle.researchCycleId} disponível
                          para a etapa 2 gerar o seed de pesquisa.
                        </p>
                      ) : (
                        <p className="text-secondary small mb-0">
                          Ciclo #{latestRunningCycle.researchCycleId} em
                          execução; sem erro detectado na consulta da etapa 2.
                        </p>
                      )}
                    </div>
                  ) : null}
                </div>
              </div>
            </article>
          </div>
        ))}
      </section>
    </div>
  );
}
