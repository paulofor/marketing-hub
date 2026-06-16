import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  useMoisCollectedReferenceUrlSummary,
  useMoisSalesLibraryPageSummary,
  useReprocessStaleMoisSalesLibraryMarketWarmup,
} from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const analysisChecklist = [
  "Jobs pendentes para análise por IA",
  "Páginas com HTML útil disponíveis como entrada",
  "Score comercial e blocos de oferta, promessa, mecanismo e prova",
  "Falhas de análise registradas no histórico operacional",
];

const marketWarmupChecklist = [
  "Páginas com análise comercial concluída",
  "Sinais públicos de demanda, conversa e concorrência",
  "Score de engenharia de sucesso e riscos comerciais",
  "Recomendação comercial para priorizar, observar ou diferenciar o ângulo",
];

function formatDateTime(value?: string) {
  if (!value) {
    return "Sem captura registrada";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatAverage(value?: number) {
  if (!value) {
    return "0,0/h";
  }
  return `${value.toLocaleString("pt-BR", {
    maximumFractionDigits: 1,
    minimumFractionDigits: 1,
  })}/h`;
}

export default function MoisSalesPagesPipelinePage() {
  const summaryQuery = useMoisSalesLibraryPageSummary(WORKSPACE_ID);
  const collectedUrlSummaryQuery =
    useMoisCollectedReferenceUrlSummary(WORKSPACE_ID);
  const reprocessStaleWarmupMutation =
    useReprocessStaleMoisSalesLibraryMarketWarmup(WORKSPACE_ID);
  const summary = summaryQuery.data;
  const collectedUrlSummary = collectedUrlSummaryQuery.data;

  const capturedPages = summary?.captured ?? 0;
  const analyzedPages = summary?.analyzed ?? 0;
  const analysisBacklog = Math.max(capturedPages - analyzedPages, 0);
  const analysisPending = summary?.analysisPending ?? 0;
  const analysisRunning = summary?.analysisRunning ?? 0;
  const analysisFailed = summary?.analysisFailed ?? 0;
  const pendingPages = summary?.pending ?? 0;
  const automaticProcessingActive = summary?.automaticProcessingActive ?? false;
  const lastCapturedAt = summary?.lastCapturedAt;
  const capturedLastHour = summary?.capturedLastHour ?? 0;
  const remainingWithoutHtml = summary?.remainingWithoutHtml ?? 0;
  const averageCapturesPerHour = summary?.averageCapturesPerHour ?? 0;
  const marketWarmupEligiblePages = summary?.marketWarmupEligible ?? 0;
  const marketWarmupPending = summary?.marketWarmupPending ?? 0;
  const marketWarmupRunning = summary?.marketWarmupRunning ?? 0;
  const marketWarmupCompleted = summary?.marketWarmupCompleted ?? 0;
  const marketWarmupFailed = summary?.marketWarmupFailed ?? 0;
  const marketWarmupHot = summary?.marketWarmupHot ?? 0;
  const marketWarmupPromising = summary?.marketWarmupPromising ?? 0;
  const marketWarmupCold = summary?.marketWarmupCold ?? 0;
  const marketWarmupSaturated = summary?.marketWarmupSaturated ?? 0;
  const marketWarmupStuck = summary?.marketWarmupStuck ?? 0;
  const requeuedWarmupJobs = reprocessStaleWarmupMutation.data?.requeuedJobs;
  const isReprocessingWarmup = reprocessStaleWarmupMutation.isPending;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <PageTitle>Pipeline de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Execute e acompanhe o pipeline operacional: obter HTML bruto
            versionado, analisar comercialmente as páginas capturadas e preparar
            a investigação de sucesso do produto.
          </p>
        </div>
        <Link
          className="btn btn-outline-secondary"
          to="/mois/sales-pages-library"
        >
          Voltar à biblioteca
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-2">Etapa 1</span>
              <h2 className="h4 mb-2">Obtenção dos HTML</h2>
              <p className="text-secondary mb-0">
                Primeiro bloco operacional do pipeline: transformar URLs
                ingeridas em snapshots brutos rastreáveis para permitir análise
                de copy, estrutura, prova, oferta e padrões visuais com base em
                evidência real.
              </p>
            </div>
            <span className="badge text-bg-success align-self-start">
              Pronto para execução
            </span>
          </div>

          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Entrada
                </p>
                <h3 className="h6 mb-2">URLs normalizadas da biblioteca</h3>
                <p className="text-secondary small mb-0">
                  A etapa parte das páginas ingeridas, priorizando URLs sem
                  snapshot capturado ou, se solicitado, recapturando páginas já
                  processadas.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Saída esperada
                </p>
                <h3 className="h6 mb-2">HTML bruto versionado</h3>
                <p className="text-secondary small mb-0">
                  Cada captura gera snapshot com hash, tamanho, data/hora,
                  status HTTP e status operacional para auditoria.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Critério de qualidade
                </p>
                <h3 className="h6 mb-2">HTML útil para análise</h3>
                <p className="text-secondary small mb-0">
                  O conteúdo deve conter corpo relevante da página, sem marcador
                  técnico interno nem payload contaminado.
                </p>
              </div>
            </div>
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
              <div>
                <h3 className="h6 mb-1">Resumo operacional</h3>
                <p className="text-secondary small mb-0">
                  Apenas os totais necessários para entender a origem, a
                  consolidação na tabela sales e o volume com página obtida.
                </p>
              </div>
              {collectedUrlSummaryQuery.isLoading || summaryQuery.isLoading ? (
                <span className="badge text-bg-secondary">Carregando...</span>
              ) : null}
            </div>

            {collectedUrlSummaryQuery.isError ? (
              <div className="alert alert-warning mb-3">
                Não foi possível carregar o total bruto coletado.
              </div>
            ) : null}

            <div className="row g-3">
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Total bruto</p>
                  <h3 className="mb-0">
                    {collectedUrlSummaryQuery.isLoading
                      ? "..."
                      : (collectedUrlSummary?.uniqueEffectiveUrls ?? 0)}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">
                    Total na tabela de sales
                  </p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : (summary?.total ?? 0)}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">
                    Total com HTML útil (html_bytes &gt; 0)
                  </p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : (summary?.captured ?? 0)}
                  </h3>
                </div>
              </div>
            </div>

            <div className="border rounded-3 p-3 mt-3">
              <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
                <div>
                  <h4 className="h6 mb-1">Processamento automático</h4>
                  <p className="text-secondary small mb-0">
                    Indicadores atualizados automaticamente para confirmar se a
                    biblioteca continua tratando páginas sem ação manual.
                  </p>
                </div>
                <span
                  className={`badge ${
                    automaticProcessingActive
                      ? "text-bg-success"
                      : "text-bg-secondary"
                  }`}
                >
                  {automaticProcessingActive
                    ? "Processamento automático ativo"
                    : "Sem avanço recente"}
                </span>
              </div>
              <div className="row g-3">
                <div className="col-sm-6 col-xl-3">
                  <div className="bg-light rounded-3 p-3 h-100">
                    <p className="text-secondary mb-1">Última captura feita</p>
                    <h5 className="mb-0">
                      {summaryQuery.isLoading
                        ? "..."
                        : formatDateTime(lastCapturedAt)}
                    </h5>
                  </div>
                </div>
                <div className="col-sm-6 col-xl-3">
                  <div className="bg-light rounded-3 p-3 h-100">
                    <p className="text-secondary mb-1">
                      Capturadas na última hora
                    </p>
                    <h5 className="mb-0">
                      {summaryQuery.isLoading ? "..." : capturedLastHour}
                    </h5>
                  </div>
                </div>
                <div className="col-sm-6 col-xl-3">
                  <div className="bg-light rounded-3 p-3 h-100">
                    <p className="text-secondary mb-1">Ainda faltam</p>
                    <h5 className="mb-0">
                      {summaryQuery.isLoading ? "..." : remainingWithoutHtml}
                    </h5>
                  </div>
                </div>
                <div className="col-sm-6 col-xl-3">
                  <div className="bg-light rounded-3 p-3 h-100">
                    <p className="text-secondary mb-1">Velocidade média</p>
                    <h5 className="mb-0">
                      {summaryQuery.isLoading
                        ? "..."
                        : formatAverage(averageCapturesPerHour)}
                    </h5>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-2">Etapa 2</span>
              <h2 className="h4 mb-2">Análise comercial da página</h2>
              <p className="text-secondary mb-0">
                Etapa já implementada no backend: o worker reserva jobs de
                análise pendentes, usa o HTML bruto capturado na etapa 1 e grava
                score, promessa, mecanismo, prova, oferta e histórico auditável
                da execução.
              </p>
            </div>
            <span className="badge text-bg-success align-self-start">
              Implementada via worker
            </span>
          </div>

          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Entrada
                </p>
                <h3 className="h6 mb-2">HTML útil capturado</h3>
                <p className="text-secondary small mb-0">
                  Páginas com html_bytes &gt; 0 ficam aptas para análise
                  comercial por IA, sem depender de dados simulados.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Saída esperada
                </p>
                <h3 className="h6 mb-2">Diagnóstico comercial estruturado</h3>
                <p className="text-secondary small mb-0">
                  A execução atualiza score e resumos de oferta, promessa,
                  mecanismo e prova para apoiar decisões de produto e vendas.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Critério de qualidade
                </p>
                <h3 className="h6 mb-2">Análise rastreável</h3>
                <p className="text-secondary small mb-0">
                  Cada análise deve ficar vinculada à página e à execução, com
                  status, erro e data para auditoria operacional.
                </p>
              </div>
            </div>
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
              <div>
                <h3 className="h6 mb-1">Resumo da etapa 2</h3>
                <p className="text-secondary small mb-0">
                  Mostra se a análise já está consumindo o volume obtido na
                  etapa 1.
                </p>
              </div>
              {summaryQuery.isLoading ? (
                <span className="badge text-bg-secondary">Carregando...</span>
              ) : null}
            </div>

            <div className="row g-3">
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">
                    Disponíveis para análise
                  </p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : capturedPages}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Já analisadas</p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : analyzedPages}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Backlog estimado</p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : analysisBacklog}
                  </h3>
                  <p className="text-secondary small mb-0 mt-2">
                    Fila real da investigação: {analysisPending} pendentes ·{" "}
                    {analysisRunning} em execução · {analysisFailed} falhas
                    registradas
                  </p>
                </div>
              </div>
            </div>

            {pendingPages > 0 ? (
              <div className="alert alert-info mt-3 mb-0">
                Existem {pendingPages} página(s) pendente(s) no pipeline para o
                worker consumir.
              </div>
            ) : null}
          </div>

          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <h3 className="h6 mb-2">Informações que este card acompanha</h3>
              <ul className="mb-0 text-secondary">
                {analysisChecklist.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
            <Link
              className="btn btn-outline-primary align-self-start"
              to="/mois/sales-pages-library"
            >
              Ver páginas analisadas
            </Link>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-2">Etapa 3</span>
              <h2 className="h4 mb-2">Investigação de sucesso do produto</h2>
              <p className="text-secondary mb-0">
                Próximo bloco do pipeline: usar a análise comercial da etapa 2
                para descobrir como o produto aparentemente vencedor vende:
                autoridade por trás, canais de audiência, funil, prova social e
                distribuição.
              </p>
            </div>
            <span className="badge text-bg-warning align-self-start">
              Preparada para ativação
            </span>
          </div>

          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Entrada
                </p>
                <h3 className="h6 mb-2">Diagnóstico comercial concluído</h3>
                <p className="text-secondary small mb-0">
                  A etapa 3 só deve iniciar quando dor, promessa, mecanismo,
                  prova, público e categoria já estiverem identificados na
                  análise comercial.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Saída esperada
                </p>
                <h3 className="h6 mb-2">Score da engenharia de sucesso</h3>
                <p className="text-secondary small mb-0">
                  O resultado esperado classifica a força da máquina de venda
                  como quente, promissora, morna, fria ou saturada com base em
                  fontes públicas rastreáveis.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Critério de qualidade
                </p>
                <h3 className="h6 mb-2">Decisão comercial explicável</h3>
                <p className="text-secondary small mb-0">
                  A recomendação deve explicar autoridade, canal, captura,
                  oferta, prova e checkout, evitando opinião sem evidência.
                </p>
              </div>
            </div>
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
              <div>
                <h3 className="h6 mb-1">Resumo da etapa 3</h3>
                <p className="text-secondary small mb-0">
                  Mostra cobertura real da Etapa 3 para priorizar produtos com
                  dossiê concluído, fila visível e força comercial.
                </p>
              </div>
              {summaryQuery.isLoading ? (
                <span className="badge text-bg-secondary">Carregando...</span>
              ) : null}
            </div>

            <div className="row g-3">
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">
                    Elegíveis para dossiê de sucesso
                  </p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : marketWarmupEligiblePages}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Dossiês concluídos</p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : marketWarmupCompleted}
                  </h3>
                  <p className="text-secondary small mb-0 mt-2">
                    Dossiê pronto para explicar a máquina de venda e revisão
                    humana.
                  </p>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Quentes/promissores</p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading
                      ? "..."
                      : marketWarmupHot + marketWarmupPromising}
                  </h3>
                  <p className="text-secondary small mb-0 mt-2">
                    Produtos com maior prioridade para estudo de engenharia de
                    venda.
                  </p>
                </div>
              </div>
            </div>
            <p className="text-secondary small mb-0 mt-3">
              Fila real da investigação: {marketWarmupPending} pendentes ·{" "}
              {marketWarmupRunning} em pesquisa · {marketWarmupStuck} presos há
              mais de 120 min · {marketWarmupFailed} falhas · {marketWarmupCold}{" "}
              fracos · {marketWarmupSaturated} saturados.
            </p>
          </div>

          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <h3 className="h6 mb-2">Informações que este card acompanha</h3>
              <ul className="mb-0 text-secondary">
                {marketWarmupChecklist.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
              {requeuedWarmupJobs != null ? (
                <p className="text-success small mb-0 mt-2">
                  {requeuedWarmupJobs} job(s) preso(s) refileirado(s) para nova
                  execução.
                </p>
              ) : null}
              {reprocessStaleWarmupMutation.isError ? (
                <p className="text-danger small mb-0 mt-2">
                  Não foi possível refileirar os jobs presos agora.
                </p>
              ) : null}
            </div>
            <div className="d-flex flex-wrap gap-2 align-self-start">
              <button
                className="btn btn-outline-warning"
                type="button"
                disabled={isReprocessingWarmup || marketWarmupStuck === 0}
                onClick={() => reprocessStaleWarmupMutation.mutate(120)}
              >
                {isReprocessingWarmup ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : null}
                Reprocessar presos
              </button>
              <Link
                className="btn btn-outline-primary"
                to="/mois/sales-pages-library?warmupFilter=HOT_OR_PROMISING&sort=MARKET_WARMUP_SCORE"
              >
                Priorizar quentes/promissores
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
