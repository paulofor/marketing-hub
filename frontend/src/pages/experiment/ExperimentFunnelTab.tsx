import axios from "axios";
import { toast } from "react-toastify";
import {
  useExperimentFunnel,
  type ExperimentFunnelStageSummary,
} from "../../api/experiment/useExperimentFunnel";
import type { ExperimentType } from "../../api/experiment/useExperiments";
import { useResetExperimentFunnel } from "../../api/experiment/useResetExperimentFunnel";
import {
  useExperimentFunnelDiagnostics,
  type FunnelDiagnosticStatus,
} from "../../api/experiment/useExperimentFunnelDiagnostics";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

const BACKTEST_TARGET_TOTAL = 500;
const ZERO_PRIMARY_RESULT_MINIMUM_SPEND = 25;
const LOW_IMPRESSIONS_MINIMUM = 100;
const LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE_HOURS = 48;
const EMERGENCY_ZERO_LEAD_SPEND_THRESHOLD = 25;

function formatPercentage(value: number) {
  return `${value.toFixed(1)}%`;
}

interface ExperimentFunnelTabProps {
  experimentId: string;
  experimentType?: ExperimentType | null;
  totalSpend?: number | null;
  spendLastSyncedAt?: string | null;
  alterationLocked?: boolean;
}

const BRAZIL_OPERATIONAL_TIME_ZONE = "America/Sao_Paulo";

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: BRAZIL_OPERATIONAL_TIME_ZONE,
    timeZoneName: "short",
  });
}

export default function ExperimentFunnelTab({
  experimentId,
  experimentType,
  totalSpend,
  spendLastSyncedAt,
  alterationLocked = false,
}: ExperimentFunnelTabProps) {
  const isLowTicketProduct = experimentType === "LOW_TICKET_PRODUCT";
  const { data, isLoading, isError } = useExperimentFunnel(experimentId);
  const diagnosticsQuery = useExperimentFunnelDiagnostics(experimentId);
  const stages = (data ?? []).slice().sort((a, b) => a.order - b.order);
  const normalizedTotalSpend = normalizeSpend(totalSpend);
  const canResetFunnelMetrics = normalizedTotalSpend === 0;
  const leadFunnelFallbackStages: ExperimentFunnelStageSummary[] = [
    {
      stage: "VISUALIZACAO_ANUNCIO",
      label: "Visualização do anúncio",
      order: 1,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ACESSO_FORM_LEAD",
      label: "Acesso ao formulário de lead",
      order: 2,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "VISUALIZACAO_FORM",
      label: "Visualização do formulário",
      order: 3,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ENVIO_FORM",
      label: "Envio do formulário",
      order: 4,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ABERTURA_EMAIL_AMOSTRA",
      label: "Abertura do e-mail de amostra",
      order: 5,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ACESSO_CHECKOUT",
      label: "Acesso ao checkout (Mercado Pago)",
      order: 6,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "COMPRA",
      label: "Compra",
      order: 7,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ABERTURA_EMAIL_COMPRA",
      label: "Abertura do e-mail de compra",
      order: 8,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "DOWNLOAD_MATERIAL_PAGO",
      label: "Download do material pago",
      order: 9,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
  ];
  const lowTicketFallbackStages: ExperimentFunnelStageSummary[] = [
    {
      stage: "VISUALIZACAO_ANUNCIO",
      label: "Visualização do anúncio",
      order: 1,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ACESSO_FORM_LEAD",
      label: "Clique para a página de venda",
      order: 2,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "VISUALIZACAO_FORM",
      label: "Visualização da página de venda",
      order: 3,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "ACESSO_CHECKOUT",
      label: "Clique no checkout",
      order: 6,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "COMPRA",
      label: "Compra",
      order: 7,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
    {
      stage: "DOWNLOAD_MATERIAL_PAGO",
      label: "Download do material pago",
      order: 9,
      autoCount: 0,
      manualCount: 0,
      totalCount: 0,
      uniqueCount: null,
      lastEventAt: null,
      source: null,
    },
  ];
  const fallbackStages = isLowTicketProduct
    ? lowTicketFallbackStages
    : leadFunnelFallbackStages;
  const selectableStages = stages.length > 0 ? stages : fallbackStages;
  const outcomeQuantities = selectableStages.map((stage) => ({
    label: stage.label,
    quantity: stage.totalCount,
  }));
  const maxOutcomeQuantity = outcomeQuantities.reduce(
    (max, item) => Math.max(max, item.quantity),
    0,
  );
  const totalOutcomes = outcomeQuantities.reduce(
    (accumulator, item) => accumulator + item.quantity,
    0,
  );
  const outcomeTargetPercent =
    BACKTEST_TARGET_TOTAL > 0
      ? (totalOutcomes / BACKTEST_TARGET_TOTAL) * 100
      : 0;
  const resetFunnel = useResetExperimentFunnel(experimentId);
  const statisticallyFailedStages =
    diagnosticsQuery.data?.diagnostics?.filter(
      (item) => item.status === "STATISTICALLY_FAILED",
    ) ?? [];
  const backendStopRules = [
    {
      title: "Baixa entrega",
      detail: `Parar se a campanha tiver menos de ${LOW_IMPRESSIONS_MINIMUM} impressões após ${LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE_HOURS} horas em execução.`,
      status:
        "Decisão no backend; o pedido de pausa é enviado ao Facebook Ads Worker.",
    },
    {
      title: "Gasto sem resultado primário",
      detail: `Parar se o gasto chegar a ${formatCurrency(ZERO_PRIMARY_RESULT_MINIMUM_SPEND)} sem envio de formulário, abertura do e-mail de amostra ou compra.`,
      status:
        normalizedTotalSpend != null &&
        normalizedTotalSpend >= ZERO_PRIMARY_RESULT_MINIMUM_SPEND
          ? "Piso de gasto já atingido; depende dos resultados primários do funil."
          : `Ainda abaixo do piso de ${formatCurrency(ZERO_PRIMARY_RESULT_MINIMUM_SPEND)}.`,
    },
    {
      title: "Etapa prioritária reprovada",
      detail:
        "Parar quando qualquer transição prioritária do funil ficar estatisticamente reprovada.",
      status: statisticallyFailedStages.length
        ? `${statisticallyFailedStages.length} etapa(s) reprovada(s): ${statisticallyFailedStages
            .map((stage) => stage.stageLabel)
            .join(", ")}.`
        : "Nenhuma etapa prioritária reprovada no diagnóstico carregado.",
    },
  ];
  const workerStopRules = [
    {
      title: "Execução da pausa oficial",
      detail:
        "Consome /api/facebook-campaigns/stop-requests, aplica status=PAUSED na Meta e reporta o resultado ao backend.",
      status: "O worker executa a decisão; a regra de negócio fica no backend.",
    },
    {
      title: "Trava financeira emergencial",
      detail: `Pausa diretamente na Meta se o Insights indicar gasto de pelo menos ${formatCurrency(EMERGENCY_ZERO_LEAD_SPEND_THRESHOLD)} com zero leads.`,
      status:
        "Protege orçamento mesmo se o backend estiver indisponível durante a sincronização de métricas.",
    },
  ];

  const handleReset = async () => {
    if (resetFunnel.isPending) {
      return;
    }
    if (
      !window.confirm(
        `Deseja zerar as contagens e analytics somente deste experimento (${experimentId}) a partir de agora?`,
      )
    ) {
      return;
    }
    try {
      await resetFunnel.mutateAsync();
      toast.success(
        "Funil e analytics deste experimento foram reiniciados. Novos eventos passarão a ser contabilizados a partir deste momento.",
      );
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível zerar o funil.")
        : "Não foi possível zerar o funil.";
      toast.error(message);
    }
  };

  return (
    <div className="card">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h5 className="card-title mb-1">Funil de vendas do experimento</h5>
            <p className="text-muted small mb-0">
              {isLowTicketProduct
                ? "Venda direta low-ticket: anúncio, página de venda, clique no checkout, compra e entrega paga."
                : "Cada etapa consolida os eventos da jornada (anúncios, Lead Portal e e-mails) para dar visibilidade ao avanço das leads no experimento."}
            </p>
          </div>
          {canResetFunnelMetrics ? (
            <button
              type="button"
              className="btn btn-outline-danger btn-sm"
              onClick={handleReset}
              disabled={resetFunnel.isPending}
            >
              {resetFunnel.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                "Zerar contagens"
              )}
            </button>
          ) : null}
        </div>

        <div className="rounded-3 border p-3 bg-light mb-4">
          <div className="text-uppercase text-muted small fw-semibold">
            Total gasto na campanha
          </div>
          <div className="fs-3 fw-bold mt-1">
            {formatCurrency(normalizedTotalSpend)}
          </div>
          <p className="text-muted small mb-0">
            Consolidado a partir da Marketing API do Meta Ads.
          </p>
          {spendLastSyncedAt ? (
            <p className="text-muted small mb-0">
              Última sincronização: {formatDate(spendLastSyncedAt)}
            </p>
          ) : null}
        </div>
        <p className="text-muted small mb-4">
          Cada custo por conversão abaixo divide o total gasto pela quantidade
          de conversões registradas em cada etapa.
        </p>

        <div className="rounded-3 border p-3 mb-4">
          <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
            <div>
              <div className="text-uppercase text-muted small fw-semibold">
                Backtest · total atual de outcomes
              </div>
              <div className="fs-2 fw-bold">{totalOutcomes}</div>
            </div>
            <div className="text-end">
              <div className="text-muted small">
                Meta ideal: {BACKTEST_TARGET_TOTAL}
              </div>
              <div className="fs-5 fw-semibold">
                {formatPercentage(outcomeTargetPercent)} da meta
              </div>
            </div>
          </div>
          <div className="d-flex flex-column gap-2">
            {outcomeQuantities.map((item) => {
              const widthPercent =
                maxOutcomeQuantity > 0
                  ? (item.quantity / maxOutcomeQuantity) * 100
                  : 0;
              return (
                <div key={item.label}>
                  <div className="d-flex justify-content-between small">
                    <span>{item.label}</span>
                    <strong>{item.quantity}</strong>
                  </div>
                  <div
                    className="progress"
                    role="img"
                    aria-label={`Quantidade do outcome ${item.label}: ${item.quantity}`}
                  >
                    <div
                      className="progress-bar"
                      style={{
                        width: `${Math.max(widthPercent, item.quantity > 0 ? 4 : 0)}%`,
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {isLoading ? (
          <div className="text-muted">Carregando funil...</div>
        ) : isError ? (
          <div className="alert alert-danger" role="alert">
            Não foi possível carregar o funil. Tente novamente mais tarde.
          </div>
        ) : stages.length === 0 ? (
          <div className="alert alert-warning" role="alert">
            Nenhuma etapa encontrada. Gere tráfego ou registre eventos manuais
            para acompanhar o fluxo.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table align-middle">
              <thead>
                <tr>
                  <th style={{ minWidth: 220 }}>Etapa</th>
                  <th>Total</th>
                  <th>% vs. etapa anterior</th>
                  <th>Custo por conv.</th>
                  <th>Únicos</th>
                  <th>Último evento (horário de Brasília)</th>
                </tr>
              </thead>
              <tbody>
                {selectableStages.map((stage, index) => (
                  <tr key={stage.stage}>
                    <td>
                      <div className="fw-semibold">
                        {stage.order}. {stage.label}
                      </div>
                    </td>
                    <td>
                      <strong>{stage.totalCount}</strong>
                    </td>
                    <td>
                      {formatRateComparedToPreviousStage(
                        stage.totalCount,
                        selectableStages[index - 1]?.totalCount,
                      )}
                    </td>
                    <td>
                      {formatCostPerConversion(
                        normalizedTotalSpend,
                        stage.totalCount,
                      )}
                    </td>
                    <td>{stage.uniqueCount ?? "—"}</td>
                    <td>{formatDate(stage.lastEventAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {diagnosticsQuery.isError ? (
          <div className="alert alert-warning py-2 mt-4" role="alert">
            Não foi possível carregar o diagnóstico estatístico agora.
          </div>
        ) : null}

        {diagnosticsQuery.data?.diagnostics?.length ? (
          <div className="mt-4">
            <h6 className="mb-2">Diagnóstico estatístico do funil</h6>
            {diagnosticsQuery.data.contextualAlert ? (
              <div className="alert alert-warning py-2" role="alert">
                {diagnosticsQuery.data.contextualAlert}
              </div>
            ) : null}
            <div className="list-group mb-3">
              {diagnosticsQuery.data.diagnostics.map((item) => (
                <div key={item.stageKey} className="list-group-item">
                  <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start">
                    <div>
                      <div className="fw-semibold">{item.stageLabel}</div>
                      <div className="small text-muted">{item.message}</div>
                      <div className="small text-muted">
                        Tentativas: {item.attempts} · Sucessos: {item.successes}
                        {item.minAcceptableRate != null
                          ? ` · Mín. aceitável: ${formatPercent(item.minAcceptableRate)}`
                          : ""}
                        {item.upper95RateIfZero != null
                          ? ` · Limite superior 95%: ${formatPercent(item.upper95RateIfZero)}`
                          : ""}
                      </div>
                      {item.thresholdChecks?.map((thresholdCheck) => (
                        <div
                          key={`${item.stageKey}-${thresholdCheck.minAcceptableRate}`}
                          className={`small ${thresholdCheck.statisticallyFailed ? "text-danger" : "text-muted"}`}
                        >
                          Limite{" "}
                          {formatPercent(thresholdCheck.minAcceptableRate)} ·
                          Tentativas mín. (95%, 0 sucessos):{" "}
                          {thresholdCheck.attemptsFor95Confidence}
                          {thresholdCheck.upper95RateIfZero != null
                            ? ` · Limite 95% atual: ${formatPercent(thresholdCheck.upper95RateIfZero)}`
                            : ""}
                          {thresholdCheck.statisticallyFailed
                            ? " · Reprovada"
                            : thresholdCheck.attemptsTargetReached
                              ? " · Alvo de tentativas atingido"
                              : " · Ainda coletando"}
                        </div>
                      ))}
                    </div>
                    <span
                      className={`badge ${statusBadgeClass(item.status)}`}
                      title="Diagnóstico calculado no backend"
                    >
                      {statusLabel(item.status)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {alterationLocked ? (
          <div className="alert alert-secondary mb-0" role="status">
            Funil bloqueado para alteração manual porque o experimento já foi
            liberado ou está em execução.
          </div>
        ) : null}

        <hr className="my-4" />
        <div className="rounded-3 border p-3 bg-light">
          <div className="d-flex flex-wrap justify-content-between gap-2 mb-3">
            <div>
              <h6 className="mb-1">Regras de parada do experimento</h6>
              <p className="text-muted small mb-0">
                Critérios que protegem orçamento e impedem manter campanha ruim
                rodando sem sinal comercial.
              </p>
            </div>
            <span className="badge text-bg-secondary align-self-start">
              Experimento #{experimentId}
            </span>
          </div>
          <div className="row g-3">
            <div className="col-12 col-xl-6">
              <div className="h-100">
                <div className="text-uppercase text-muted small fw-semibold mb-2">
                  Backend
                </div>
                <div className="list-group">
                  {backendStopRules.map((rule) => (
                    <div key={rule.title} className="list-group-item">
                      <div className="fw-semibold">{rule.title}</div>
                      <div className="small">{rule.detail}</div>
                      <div className="small text-muted mt-1">{rule.status}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="col-12 col-xl-6">
              <div className="h-100">
                <div className="text-uppercase text-muted small fw-semibold mb-2">
                  Facebook Ads Worker
                </div>
                <div className="list-group">
                  {workerStopRules.map((rule) => (
                    <div key={rule.title} className="list-group-item">
                      <div className="fw-semibold">{rule.title}</div>
                      <div className="small">{rule.detail}</div>
                      <div className="small text-muted mt-1">{rule.status}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function normalizeSpend(value?: number | null) {
  if (value === null || value === undefined) {
    return null;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return null;
  }
  return parsed;
}

function formatCurrency(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }
  return currencyFormatter.format(value);
}

function calculateCostPerConversion(
  totalSpend: number | null,
  totalConversions?: number | null,
) {
  if (
    totalSpend === null ||
    totalSpend === undefined ||
    Number.isNaN(totalSpend)
  ) {
    return null;
  }
  if (!totalConversions || totalConversions <= 0) {
    return null;
  }
  return totalSpend / totalConversions;
}

function formatCostPerConversion(
  totalSpend: number | null,
  totalConversions?: number | null,
) {
  const cost = calculateCostPerConversion(totalSpend, totalConversions);
  return cost === null ? "—" : formatCurrency(cost);
}

function formatRateComparedToPreviousStage(
  currentCount?: number | null,
  previousCount?: number | null,
) {
  if (!currentCount || currentCount < 0) {
    return "0%";
  }
  if (!previousCount || previousCount <= 0) {
    return "—";
  }
  const conversionRate = (currentCount / previousCount) * 100;
  return `${conversionRate.toFixed(1).replace(".", ",")}%`;
}

function formatPercent(value?: number | null) {
  if (value == null || Number.isNaN(value)) {
    return "—";
  }
  return `${(value * 100).toFixed(1).replace(".", ",")}%`;
}

function statusLabel(status: FunnelDiagnosticStatus) {
  switch (status) {
    case "NO_DATA":
      return "Sem dados";
    case "INSUFFICIENT_DATA":
      return "Ainda cedo";
    case "TECHNICAL_ISSUE_SUSPECTED":
      return "Suspeita técnica";
    case "WEAK_SIGNAL":
      return "Sinal fraco";
    case "STATISTICALLY_FAILED":
      return "Reprovada";
    default:
      return "Saudável/Inconclusiva";
  }
}

function statusBadgeClass(status: FunnelDiagnosticStatus) {
  switch (status) {
    case "NO_DATA":
    case "INSUFFICIENT_DATA":
      return "text-bg-secondary";
    case "TECHNICAL_ISSUE_SUSPECTED":
      return "text-bg-warning";
    case "WEAK_SIGNAL":
      return "text-bg-info";
    case "STATISTICALLY_FAILED":
      return "text-bg-danger";
    default:
      return "text-bg-success";
  }
}
