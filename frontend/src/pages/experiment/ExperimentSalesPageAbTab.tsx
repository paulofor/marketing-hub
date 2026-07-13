import { BarChart3, CheckCircle2, ExternalLink, FlaskConical } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  useExperimentSalesPageAbResults,
  type ExperimentSalesPageAbTestResult,
} from "../../api/experiment/useExperimentSalesPageAbResults";
import {
  useExperimentSalesPageTypeSelections,
  useSalesPageTypes,
  useUpdateExperimentSalesPageTypeSelections,
  type SalesPageType,
} from "../../api/experiment/useSalesPageTypes";

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

function totalPageViews(result: ExperimentSalesPageAbTestResult) {
  return result.variants.reduce((total, item) => total + item.pageViews, 0);
}

function totalCheckoutClicks(result: ExperimentSalesPageAbTestResult) {
  return result.variants.reduce((total, item) => total + item.checkoutClicks, 0);
}

function splitTrafficWeight(total: number) {
  if (total <= 0) return 50;
  return Number((100 / total).toFixed(2));
}

export default function ExperimentSalesPageAbTab({
  experimentId,
}: ExperimentSalesPageAbTabProps) {
  const { data, isLoading, isError } =
    useExperimentSalesPageAbResults(experimentId);
  const { data: types, isLoading: isLoadingTypes } = useSalesPageTypes();
  const { data: selections, isLoading: isLoadingSelections } =
    useExperimentSalesPageTypeSelections(experimentId);
  const updateSelections =
    useUpdateExperimentSalesPageTypeSelections(experimentId);
  const [selectedTypeCodes, setSelectedTypeCodes] = useState<string[]>([]);
  const [selectionFeedback, setSelectionFeedback] = useState<
    "success" | "error" | null
  >(null);

  useEffect(() => {
    if (selections && selections.length > 0) {
      setSelectedTypeCodes(selections.map((selection) => selection.typeCode));
      return;
    }
    if (types && types.length > 0 && selectedTypeCodes.length === 0) {
      setSelectedTypeCodes(
        types
          .filter((type) => type.defaultForAbTest)
          .map((type) => type.code),
      );
    }
  }, [selections, selectedTypeCodes.length, types]);

  const selectedTypes = useMemo(() => {
    const allTypes = types ?? [];
    return selectedTypeCodes
      .map((code) => allTypes.find((type) => type.code === code))
      .filter((type): type is SalesPageType => Boolean(type));
  }, [selectedTypeCodes, types]);

  const toggleType = (typeCode: string) => {
    setSelectionFeedback(null);
    setSelectedTypeCodes((current) => {
      if (current.includes(typeCode)) {
        return current.filter((code) => code !== typeCode);
      }
      return [...current, typeCode];
    });
  };

  const saveTypeSelection = async () => {
    setSelectionFeedback(null);
    try {
      await updateSelections.mutateAsync(
        selectedTypeCodes.map((typeCode, index) => ({
          typeCode,
          variantKey: String.fromCharCode(65 + index),
          trafficWeight: splitTrafficWeight(selectedTypeCodes.length),
          active: true,
        })),
      );
      setSelectionFeedback("success");
    } catch {
      setSelectionFeedback("error");
    }
  };

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

  return (
    <div className="d-flex flex-column gap-3 mt-3">
      <div className="creative-toolbar align-items-start">
        <div>
          <h5 className="mb-1 d-flex align-items-center gap-2">
            <FlaskConical size={18} /> Tipos de página de venda
          </h5>
          <p className="text-muted small mb-0">
            Defina quais experiências comerciais a campanha pode testar. O tipo
            chat IA entrega valor no próprio atendimento antes de apresentar a
            oferta completa.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={saveTypeSelection}
          disabled={
            updateSelections.isPending ||
            selectedTypeCodes.length === 0 ||
            isLoadingTypes ||
            isLoadingSelections
          }
        >
          {updateSelections.isPending ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" />
              Salvando
            </>
          ) : (
            "Salvar tipos"
          )}
        </button>
      </div>

      {selectionFeedback === "success" ? (
        <div className="alert alert-success mb-0" role="status">
          Tipos de página de venda salvos para este experimento.
        </div>
      ) : null}
      {selectionFeedback === "error" ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível salvar os tipos de página de venda.
        </div>
      ) : null}

      {isLoadingTypes || isLoadingSelections ? (
        <div className="text-muted small">Carregando tipos disponíveis...</div>
      ) : (
        <div className="row g-3">
          {(types ?? []).map((type) => {
            const checked = selectedTypeCodes.includes(type.code);
            return (
              <div className="col-12 col-xl-6" key={type.code}>
                <label className="border rounded-3 p-3 h-100 d-flex gap-3">
                  <input
                    className="form-check-input mt-1"
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggleType(type.code)}
                  />
                  <span>
                    <span className="d-flex align-items-center gap-2 mb-1">
                      <span className="fw-semibold">{type.name}</span>
                      {type.code === "AI_CHAT_DIGITAL_BAIT" ? (
                        <span className="badge text-bg-primary">Novo</span>
                      ) : null}
                    </span>
                    <span className="d-block text-muted small mb-2">
                      {type.description}
                    </span>
                    <span className="d-block small">
                      <strong>Mecanismo:</strong> {type.commercialMechanism}
                    </span>
                    <span className="d-block small">
                      <strong>Captura:</strong> {type.leadCaptureStrategy}
                    </span>
                    <span className="d-block small">
                      <strong>Isca:</strong> {type.digitalBaitDelivery}
                    </span>
                  </span>
                </label>
              </div>
            );
          })}
        </div>
      )}

      {selectedTypes.length > 0 ? (
        <div className="alert alert-info mb-0" role="status">
          <strong>Planejamento A/B:</strong>{" "}
          {selectedTypes
            .map((type, index) => `Variante ${String.fromCharCode(65 + index)}: ${type.name}`)
            .join(" · ")}
        </div>
      ) : null}

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

      {results.length === 0 ? (
        <div className="alert alert-light border mb-0">
          Nenhum teste A/B de página de venda foi configurado para este experimento.
        </div>
      ) : null}

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
                    const testUrl = item.variant.metricsSafeUrl;
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
                novos page views do teste. A URL segura vem do backend.
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
