import { Link, useParams } from "react-router-dom";
import {
  useOprmCnaeScore,
  useOprmCnaeVolume,
} from "../../api/oprm/useOprmCnaeDetail";
import {
  type OprmRoutineResearchCycleSummary,
  useOprmCnaePipelineCycles,
  useStartOprmCnaePipeline,
} from "../../api/oprm/useOprmCnaePipeline";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useBreadcrumbs } from "../../app/breadcrumbs";

const pipelineStages = [
  {
    code: "cycle",
    title: "1. Ciclo",
    description: "Ciclo pai criado para pesquisar a rotina real do CNAE.",
  },
  {
    code: "seed",
    title: "2. Seed",
    description: "Transforma CNAE em nicho operacional e queries de pesquisa.",
  },
  {
    code: "search",
    title: "3. Busca",
    description: "Procura fontes públicas sobre rotina, tarefas e dificuldades.",
  },
  {
    code: "fetch",
    title: "4. Coleta",
    description: "Coleta metadados e trechos úteis das fontes selecionadas.",
  },
  {
    code: "signals",
    title: "5. Sinais",
    description: "Extrai sinais estruturados sobre dores, rotina e linguagem.",
  },
  {
    code: "synthesis",
    title: "6. Síntese",
    description: "Monta o cartão de rotina do nicho com evidências.",
  },
  {
    code: "mei",
    title: "7. MEI",
    description: "Define o público MEI/autônomo dono-operador do nicho.",
  },
  {
    code: "quality",
    title: "8. Qualidade",
    description: "Valida se a pesquisa é específica, recente e sem solução pronta.",
  },
  {
    code: "materialization",
    title: "9. Materialização",
    description: "Grava o nicho enriquecido para alimentar ofertas e experimentos.",
  },
];

function formatNumber(value?: number | null) {
  if (value === null || value === undefined) {
    return "Sem dado";
  }
  return Number(value).toLocaleString("pt-BR");
}

function formatScore(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "Sem score";
  }
  return Number(value).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Ainda não finalizado";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function statusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    RUNNING: "Em execução",
    COMPLETED: "Concluído",
    READY_FOR_HYPOTHESIS: "Pronto",
    FAILED: "Falhou",
    NEEDS_MORE_RESEARCH: "Precisa aprofundar",
    NEEDS_MORE_MEI_RESEARCH: "Precisa de mais MEI",
    OUTDATED_SOURCES: "Fontes antigas",
    TOO_CORPORATE: "Corporativo demais",
    SOLUTION_CONTAMINATED: "Contaminado por solução",
    GENERIC: "Genérico",
    ENRICHED_NICHE_FAILED: "Falha na materialização",
  };
  return status ? (labels[status] ?? status) : "Aguardando";
}

function inferStageState(
  stageIndex: number,
  cycle?: OprmRoutineResearchCycleSummary,
) {
  if (!cycle) {
    return {
      label: "Aguardando início",
      className: "border-secondary text-secondary",
    };
  }
  if (cycle.status === "FAILED" || cycle.status?.includes("FAILED")) {
    return stageIndex === 0
      ? { label: "Falhou", className: "border-danger text-danger" }
      : { label: "Bloqueado", className: "border-secondary text-secondary" };
  }
  if (cycle.finishedAt) {
    return { label: "Concluído", className: "border-success text-success" };
  }
  if (stageIndex === 0) {
    return { label: "Em execução", className: "border-primary text-primary" };
  }
  if (stageIndex === 1 && cycle.totalQueries > 0) {
    return { label: "Concluído", className: "border-success text-success" };
  }
  if (stageIndex <= 4 && cycle.totalExtractedSignals > 0) {
    return { label: "Com sinais", className: "border-success text-success" };
  }
  return { label: "Na fila", className: "border-secondary text-secondary" };
}

export default function OprmCnaeDetailPlaceholderPage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : "CNAE";
  const volumeQuery = useOprmCnaeVolume(decodedCnaeCode);
  const scoreQuery = useOprmCnaeScore(decodedCnaeCode);
  const cyclesQuery = useOprmCnaePipelineCycles(decodedCnaeCode);
  const startPipelineMutation = useStartOprmCnaePipeline(decodedCnaeCode);
  const latestCycle = cyclesQuery.data?.[0];
  const cnaeDescription =
    volumeQuery.data?.cnaeDescription ??
    scoreQuery.data?.cnaeDescription ??
    "Descrição ainda não encontrada";

  useBreadcrumbs([
    { label: "OPRM", to: "/oprm" },
    { label: "Detalhe do nicho" },
  ]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Detalhe do nicho CNAE</PageTitle>
        <p className="text-secondary mb-0">
          Use esta visão para decidir se o CNAE merece pesquisa de rotina,
          acompanhar a execução do NichoCNAE e avançar apenas com oportunidades
          sustentadas por sinais reais.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-3">
                CNAE {decodedCnaeCode}
              </span>
              <h2 className="h4 mb-2">{cnaeDescription}</h2>
              <p className="text-secondary mb-0">
                Score e volume ajudam a priorizar mercados com maior chance de
                virar produto digital vendável sem perder a visão de rotina real.
              </p>
            </div>
            <div className="d-flex align-items-start gap-2">
              <button
                type="button"
                className="btn btn-primary"
                disabled={startPipelineMutation.isPending}
                onClick={() => startPipelineMutation.mutate()}
              >
                {startPipelineMutation.isPending
                  ? "Disparando..."
                  : "Disparar pipeline NichoCNAE"}
              </button>
              <Link className="btn btn-outline-secondary" to="/oprm">
                Voltar para CNAEs
              </Link>
            </div>
          </div>
          {startPipelineMutation.isSuccess ? (
            <div className="alert alert-success mb-0" role="status">
              Pipeline iniciado para o CNAE {decodedCnaeCode}. Ciclo #
              {startPipelineMutation.data.researchCycleId} criado para
              acompanhamento.
            </div>
          ) : null}
          {startPipelineMutation.isError ? (
            <div className="alert alert-warning mb-0" role="alert">
              {startPipelineMutation.error.message}
            </div>
          ) : null}
        </div>
      </section>

      <section className="row g-3">
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Score OPRM</span>
              <div className="display-6 fw-semibold">
                {formatScore(
                  scoreQuery.data?.opportunityScore ??
                    volumeQuery.data?.opportunityScore,
                )}
              </div>
              <span className="small text-secondary">
                {scoreQuery.data?.scoreStatus ??
                  volumeQuery.data?.scoreStatus ??
                  "Status não informado"}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Estabelecimentos</span>
              <div className="display-6 fw-semibold">
                {formatNumber(volumeQuery.data?.totalEstabelecimentos)}
              </div>
              <span className="small text-secondary">
                Ativos: {formatNumber(volumeQuery.data?.totalEstabelecimentosAtivos)}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Empresas MEI</span>
              <div className="display-6 fw-semibold">
                {formatNumber(volumeQuery.data?.totalEmpresasMei)}
              </div>
              <span className="small text-secondary">
                Empresas: {formatNumber(volumeQuery.data?.totalEmpresas)}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Componentes do score</span>
              <div className="small d-flex flex-column gap-1 mt-2">
                <span>Volume: {formatScore(scoreQuery.data?.marketVolumeScore)}</span>
                <span>MEI: {formatScore(scoreQuery.data?.meiDensityScore)}</span>
                <span>Digital: {formatScore(scoreQuery.data?.digitalFitScore)}</span>
                <span>Dor: {formatScore(scoreQuery.data?.painClarityScore)}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-2">
            <div>
              <h2 className="h5 mb-1">Execução do pipeline NichoCNAE</h2>
              <p className="text-secondary mb-0">
                Cards por fase para acompanhar onde a pesquisa está antes de
                transformar o CNAE em nicho enriquecido.
              </p>
            </div>
            <span className="badge text-bg-light align-self-start">
              Último ciclo: {latestCycle ? `#${latestCycle.researchCycleId}` : "nenhum"}
            </span>
          </div>

          {latestCycle ? (
            <div className="alert alert-info mb-0">
              Status atual: <strong>{statusLabel(latestCycle.status)}</strong> ·
              iniciado em {formatDateTime(latestCycle.startedAt)} · sinais extraídos: {" "}
              {formatNumber(latestCycle.totalExtractedSignals)}
            </div>
          ) : (
            <div className="alert alert-secondary mb-0">
              Nenhum ciclo encontrado para este CNAE. Use o botão de disparo
              quando já existir candidato pendente de NichoCNAE para esse CNAE.
            </div>
          )}

          <div className="row g-3">
            {pipelineStages.map((stage, index) => {
              const state = inferStageState(index, latestCycle);
              return (
                <div className="col-md-4" key={stage.title}>
                  <div className={`card h-100 ${state.className}`}>
                    <div className="card-body">
                      <div className="d-flex justify-content-between gap-2 mb-2">
                        <h3 className="h6 mb-0">{stage.title}</h3>
                        <span className="badge text-bg-light">{state.label}</span>
                      </div>
                      <p className="small text-secondary mb-3">
                        {stage.description}
                      </p>
                      {latestCycle ? (
                        <Link
                          className="btn btn-outline-primary btn-sm"
                          to={`/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}/pipeline/${latestCycle.researchCycleId}/stages/${stage.code}`}
                        >
                          Ver detalhes
                        </Link>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          disabled
                        >
                          Ver detalhes
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
