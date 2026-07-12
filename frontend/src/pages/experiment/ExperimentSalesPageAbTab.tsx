import { BarChart3, CheckCircle2, ExternalLink, FlaskConical } from "lucide-react";
import {
  useExperimentSalesPageAbResults,
  type ExperimentSalesPageAbTestResult,
  type ExperimentSalesPageAbVariant,
} from "../../api/experiment/useExperimentSalesPageAbResults";

interface ExperimentSalesPageAbTabProps {
  experimentId: string;
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function formatInteger(value?: number | null) {
  return new Intl.NumberFormat("pt-BR").format(value ?? 0);
}

function formatRate(value?: number | string | null) {
  const numericValue =
    typeof value === "string" ? Number(value) : typeof value === "number" ? value : 0;
  if (!Number.isFinite(numericValue)) return "0,00%";
  return new Intl.NumberFormat("pt-BR", {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericValue);
}

function variantTypeLabel(value?: string | null) {
  switch (value) {
    case "TRADITIONAL":
      return "Página tradicional";
    case "HUMAN_VIDEO":
      return "Página com vídeo humano";
    default:
      return value || "-";
  }
}

function resultStatusLabel(value?: string | null) {
  switch (value) {
    case "SEM_DADOS":
      return "Sem dados A/B";
    case "AMOSTRA_INSUFICIENTE":
      return "Amostra insuficiente";
    case "SEM_CLIQUE_CHECKOUT":
      return "Sem clique no checkout";
    case "VENCEDOR_SUGERIDO":
      return "Vencedor sugerido";
    default:
      return "Inconclusivo";
  }
}

function resultStatusBadge(value?: string | null) {
  switch (value) {
    case "VENCEDOR_SUGERIDO":
      return "text-bg-success";
    case "SEM_DADOS":
    case "AMOSTRA_INSUFICIENTE":
    case "SEM_CLIQUE_CHECKOUT":
      return "text-bg-warning";
    default:
      return "text-bg-secondary";
  }
}

function safeTestUrl(variant: ExperimentSalesPageAbVariant) {
  const rawUrl = variant.salesPageUrl || variant.adDestinationUrl;
  if (!rawUrl) return null;
  try {
    const url = new URL(rawUrl);
    if (!url.searchParams.has("mh_test")) {
      url.searchParams.set("mh_test", "1");
    }
    return url.toString();
  } catch {
    return rawUrl.includes("mh_test=1")
      ? rawUrl
      : `${rawUrl}${rawUrl.includes("?") ? "&" : "?"}mh_test=1`;
  }
}

function totalPageViews(result: ExperimentSalesPageAbTestResult) {
  return result.variants.reduce((total, item) => total + item.pageViews, 0);
}

function totalCheckoutClicks(result: ExperimentSalesPageAbTestResult) {
  return result.variants.reduce((total, item) => total + item.checkoutClicks, 0);
}

export default function ExperimentSalesPageAbTab({
  experimentId,
}: ExperimentSalesPageAbTabProps) {
  const { data, isLoading, isError } =
    useExperimentSalesPageAbResults(experimentId);

  if (isLoading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Carregando teste A/B...</span>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="alert alert-danger mt-3" role="alert">
        Não foi possível carregar os resultados do teste A/B.
      </div>
    );
  }

  const results = data ?? [];

  if (results.length === 0) {
    return (
      <div className="alert alert-light border mt-3 mb-0">
        Nenhum teste A/B de página de venda foi configurado para este experimento.
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-3 mt-3">
      <div className="creative-toolbar align-items-start">
        <div>
          <h5 className="mb-1 d-flex align-items-center gap-2">
            <FlaskConical size={18} /> Resultados do teste A/B
          </h5>
          <p className="text-muted small mb-0">
            Comparação por variante usando eventos reais com parâmetro A/B na URL
            da página publicada.
          </p>
        </div>
      </div>

      {results.map((result) => (
        <div className="card" key={result.test.id}>
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <h5 className="card-title mb-1">{result.test.name}</h5>
                <p className="text-muted small mb-0">
                  Métrica principal: {result.test.primaryMetric || "checkout_click_rate"} ·
                  Amostra mínima: {result.test.minimumSampleSize ?? "-"} page views
                </p>
              </div>
              <span className={`badge ${resultStatusBadge(result.status)}`}>
                {resultStatusLabel(result.status)}
              </span>
            </div>

            <div className="row g-3">
              <div className="col-12 col-md-4">
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-muted small">Page views A/B</div>
                  <div className="fs-4 fw-semibold">
                    {formatInteger(totalPageViews(result))}
                  </div>
                </div>
              </div>
              <div className="col-12 col-md-4">
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-muted small">Cliques no checkout</div>
                  <div className="fs-4 fw-semibold">
                    {formatInteger(totalCheckoutClicks(result))}
                  </div>
                </div>
              </div>
              <div className="col-12 col-md-4">
                <div className="border rounded-3 p-3 h-100">
                  <div className="text-muted small">Vencedor atual</div>
                  <div className="fs-4 fw-semibold">
                    {result.winnerVariantKey
                      ? `Variante ${result.winnerVariantKey}`
                      : "-"}
                  </div>
                </div>
              </div>
            </div>

            <div className="alert alert-info mb-0" role="status">
              <strong>Leitura comercial:</strong> {result.recommendation}
            </div>

            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Variante</th>
                    <th>Tipo</th>
                    <th>Status</th>
                    <th>Tráfego</th>
                    <th>Page views</th>
                    <th>Sessões</th>
                    <th>Checkout</th>
                    <th>Taxa checkout</th>
                    <th>Compras</th>
                    <th>Último evento</th>
                    <th>Página</th>
                  </tr>
                </thead>
                <tbody>
                  {result.variants.map((item) => {
                    const testUrl = safeTestUrl(item.variant);
                    return (
                      <tr key={item.variant.id}>
                        <td>
                          <div className="fw-semibold">
                            Variante {item.variant.variantKey}
                          </div>
                          <div className="text-muted small">
                            {item.variant.name}
                          </div>
                        </td>
                        <td>{variantTypeLabel(item.variant.variantType)}</td>
                        <td>{item.variant.status}</td>
                        <td>{item.variant.trafficWeight ?? "-"}%</td>
                        <td>{formatInteger(item.pageViews)}</td>
                        <td>{formatInteger(item.sessions)}</td>
                        <td>{formatInteger(item.checkoutClicks)}</td>
                        <td>
                          <span className="d-inline-flex align-items-center gap-1">
                            <BarChart3 size={14} />
                            {formatRate(item.checkoutClickRate)}
                          </span>
                        </td>
                        <td>{formatInteger(item.purchases)}</td>
                        <td>{formatDate(item.lastEventAt)}</td>
                        <td>
                          {testUrl ? (
                            <a
                              className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-1"
                              href={testUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              <ExternalLink size={14} /> Ver sem métrica
                            </a>
                          ) : (
                            <span className="text-muted small">-</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <div className="d-flex align-items-start gap-2 text-muted small">
              <CheckCircle2 size={16} className="mt-1 text-success" />
              <span>
                Use os links com mh_test=1 para revisar as páginas sem gerar
                novos page views do teste.
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
