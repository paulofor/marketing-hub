import { useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  useCaptureMoisSalesLibrarySnapshots,
  useMoisCollectedReferenceUrlSummary,
  useMoisSalesLibraryPageSummary,
} from "../../api/mois/useMoisSalesLibrary";
import type { MoisSalesLibrarySnapshotCaptureItem } from "../../api/mois/types";

const WORKSPACE_ID = "workspace-001";
const DEFAULT_CAPTURE_LIMIT = 5;

const analysisChecklist = [
  "Jobs pendentes para análise por IA",
  "Páginas com HTML útil disponíveis como entrada",
  "Score comercial e blocos de oferta, promessa, mecanismo e prova",
  "Falhas de análise registradas no histórico operacional",
];

const htmlAcquisitionChecklist = [
  "URLs pendentes para captura",
  "Snapshots com HTML bruto salvo",
  "Falhas de acesso, bloqueio ou timeout",
  "Hash, tamanho e data da última captura",
];

const marketWarmupChecklist = [
  "Páginas com análise comercial concluída",
  "Sinais públicos de demanda, conversa e concorrência",
  "Score de aquecimento do mercado e risco de saturação",
  "Recomendação comercial para priorizar, observar ou diferenciar o ângulo",
];

function formatBytes(value: number) {
  if (!value) {
    return "0 B";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function getSnapshotStatusBadgeClass(status: string) {
  switch (status) {
    case "CAPTURED":
    case "DUPLICATE":
      return "bg-success-subtle text-success-emphasis";
    case "FAILED":
      return "bg-danger-subtle text-danger-emphasis";
    case "FETCHING":
      return "bg-primary-subtle text-primary-emphasis";
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}

function getResultMessage(item: MoisSalesLibrarySnapshotCaptureItem) {
  const redirectInfo = item.redirectDestinationUrl
    ? `Destino: ${item.redirectDestinationUrl}${
        item.redirectRootUrl ? ` · Raiz: ${item.redirectRootUrl}` : ""
      }`
    : "";
  if (item.errorMessage) {
    return [item.errorMessage, redirectInfo].filter(Boolean).join(" · ");
  }
  if (item.snapshotHash) {
    return [`hash ${item.snapshotHash.slice(0, 12)}...`, redirectInfo]
      .filter(Boolean)
      .join(" · ");
  }
  return redirectInfo || "Sem detalhe adicional";
}

export default function MoisSalesPagesPipelinePage() {
  const [forceCapture, setForceCapture] = useState(false);
  const summaryQuery = useMoisSalesLibraryPageSummary(WORKSPACE_ID);
  const collectedUrlSummaryQuery =
    useMoisCollectedReferenceUrlSummary(WORKSPACE_ID);
  const captureMutation = useCaptureMoisSalesLibrarySnapshots(WORKSPACE_ID);
  const summary = summaryQuery.data;
  const collectedUrlSummary = collectedUrlSummaryQuery.data;

  const lastRun = captureMutation.data;
  const isRunning = captureMutation.isPending;
  const capturedPages = summary?.captured ?? 0;
  const analyzedPages = summary?.analyzed ?? 0;
  const analysisBacklog = Math.max(capturedPages - analyzedPages, 0);
  const analysisPending = summary?.analysisPending ?? 0;
  const analysisRunning = summary?.analysisRunning ?? 0;
  const analysisFailed = summary?.analysisFailed ?? 0;
  const pendingPages = summary?.pending ?? 0;
  const marketWarmupEligiblePages = analyzedPages;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <PageTitle>Pipeline de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Execute e acompanhe o pipeline operacional: obter HTML bruto
            versionado, analisar comercialmente as páginas capturadas e preparar
            a pesquisa de aquecimento do mercado.
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
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
              <div>
                <h3 className="h6 mb-1">Executar captura agora</h3>
                <p className="text-secondary small mb-0">
                  O acionamento usa o backend existente e processa até{" "}
                  {DEFAULT_CAPTURE_LIMIT} URLs sem HTML útil por execução para
                  evitar bloqueios e timeouts longos.
                </p>
              </div>
              <div className="d-flex flex-wrap align-items-center gap-3">
                <div className="form-check form-switch mb-0">
                  <input
                    className="form-check-input"
                    id="forceCapture"
                    type="checkbox"
                    checked={forceCapture}
                    onChange={(event) => setForceCapture(event.target.checked)}
                    disabled={isRunning}
                  />
                  <label
                    className="form-check-label small"
                    htmlFor="forceCapture"
                  >
                    Forçar URLs sem HTML (ignorar cooldown)
                  </label>
                </div>
                <button
                  className="btn btn-primary"
                  type="button"
                  disabled={isRunning}
                  onClick={() =>
                    captureMutation.mutate({
                      limit: DEFAULT_CAPTURE_LIMIT,
                      force: forceCapture,
                    })
                  }
                >
                  {isRunning ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        aria-hidden="true"
                      />
                      Executando...
                    </>
                  ) : (
                    "Executar etapa 1"
                  )}
                </button>
              </div>
            </div>
            {captureMutation.isError ? (
              <div className="alert alert-danger mt-3 mb-0">
                Falha ao executar a captura. Verifique se o backend está
                acessível e tente novamente.
              </div>
            ) : null}
            {lastRun ? (
              <div className="alert alert-success mt-3 mb-0">
                Execução concluída em{" "}
                {new Date(lastRun.capturedAt).toLocaleString()}:{" "}
                {lastRun.processed} URL(s) processada(s), {lastRun.captured}{" "}
                captura(s) útil(is) e {lastRun.failed} falha(s).
              </div>
            ) : null}
          </div>

          <div>
            <h3 className="h6 mb-2">Informações que este card acompanha</h3>
            <ul className="mb-0 text-secondary">
              {htmlAcquisitionChecklist.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
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
                    Fila real: {analysisPending} pendentes · {analysisRunning}{" "}
                    em execução · {analysisFailed} falhas registradas
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
              <h2 className="h4 mb-2">Pesquisa de aquecimento e ecossistema</h2>
              <p className="text-secondary mb-0">
                Próximo bloco do pipeline: usar a análise comercial da etapa 2
                para medir se o mercado já está sendo aquecido por conversas,
                conteúdos, provas públicas e ofertas parecidas antes de criar
                novos produtos ou ângulos de venda.
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
                <h3 className="h6 mb-2">Score de aquecimento do mercado</h3>
                <p className="text-secondary small mb-0">
                  O resultado esperado classifica o mercado como quente,
                  promissor, morno, frio ou saturado com base em fontes públicas
                  rastreáveis.
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
                  A recomendação deve preservar o eixo dor, resultado,
                  mecanismo, prova e oferta, evitando opinião sem evidência.
                </p>
              </div>
            </div>
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
              <div>
                <h3 className="h6 mb-1">Resumo da etapa 3</h3>
                <p className="text-secondary small mb-0">
                  Indica o volume já pronto para pesquisa de aquecimento assim
                  que o worker dedicado for ativado.
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
                    Elegíveis para aquecimento
                  </p>
                  <h3 className="mb-0">
                    {summaryQuery.isLoading ? "..." : marketWarmupEligiblePages}
                  </h3>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Contrato oficial</p>
                  <h3 className="h6 mb-0">MARKET_WARMUP_RESEARCH</h3>
                  <p className="text-secondary small mb-0 mt-2">
                    Etapa canônica na posição 3 do pipeline MOIS.
                  </p>
                </div>
              </div>
              <div className="col-sm-6 col-lg-4">
                <div className="bg-light rounded-3 p-3 h-100">
                  <p className="text-secondary mb-1">Decisão apoiada</p>
                  <h3 className="h6 mb-0">Priorizar ou diferenciar</h3>
                  <p className="text-secondary small mb-0 mt-2">
                    Foco em escolher mercados com sinais reais de compra.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <h3 className="h6 mb-2">Informações que este card acompanha</h3>
              <ul className="mb-0 text-secondary">
                {marketWarmupChecklist.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
            <Link
              className="btn btn-outline-primary align-self-start"
              to="/mois/sales-pages-library"
            >
              Ver páginas elegíveis
            </Link>
          </div>
        </div>
      </section>

      {lastRun ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body table-responsive">
            <h2 className="h5 mb-3">Resultado da última execução</h2>
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Página</th>
                  <th>Status</th>
                  <th>HTTP</th>
                  <th>HTML</th>
                  <th>Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {lastRun.items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-secondary">
                      Nenhuma URL elegível encontrada para esta execução.
                    </td>
                  </tr>
                ) : (
                  lastRun.items.map((item) => (
                    <tr
                      key={`${item.pageId}-${item.snapshotId ?? "sem-snapshot"}`}
                    >
                      <td className="text-break">
                        <Link to={`/mois/sales-pages-library/${item.pageId}`}>
                          {item.urlCanonical}
                        </Link>
                      </td>
                      <td>
                        <span
                          className={`badge ${getSnapshotStatusBadgeClass(item.status)}`}
                        >
                          {item.status}
                        </span>
                      </td>
                      <td>{item.httpStatus ?? "—"}</td>
                      <td>{formatBytes(item.rawHtmlBytes)}</td>
                      <td className="text-secondary small text-break">
                        {getResultMessage(item)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}
