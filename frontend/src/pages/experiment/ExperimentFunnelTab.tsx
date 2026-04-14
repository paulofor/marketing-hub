import { useState, type FormEvent } from "react";
import axios from "axios";
import { toast } from "react-toastify";
import { useExperimentFunnel, type ExperimentFunnelStageSummary } from "../../api/experiment/useExperimentFunnel";
import { useRegisterExperimentFunnelEvent } from "../../api/experiment/useRegisterExperimentFunnelEvent";
import { useResetExperimentFunnel } from "../../api/experiment/useResetExperimentFunnel";
import { useExperimentFunnelDiagnostics, type FunnelDiagnosticStatus } from "../../api/experiment/useExperimentFunnelDiagnostics";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

interface ExperimentFunnelTabProps {
  experimentId: string;
  totalSpend?: number | null;
  spendLastSyncedAt?: string | null;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export default function ExperimentFunnelTab({
  experimentId,
  totalSpend,
  spendLastSyncedAt,
}: ExperimentFunnelTabProps) {
  const { data, isLoading, isError } = useExperimentFunnel(experimentId);
  const diagnosticsQuery = useExperimentFunnelDiagnostics(experimentId);
  const stages = (data ?? []).slice().sort((a, b) => a.order - b.order);
  const normalizedTotalSpend = normalizeSpend(totalSpend);
  const fallbackStages: ExperimentFunnelStageSummary[] = [
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
  const selectableStages = stages.length > 0 ? stages : fallbackStages;
  const registerEvent = useRegisterExperimentFunnelEvent(experimentId);
  const resetFunnel = useResetExperimentFunnel(experimentId);
  const [form, setForm] = useState({
    stage: "VISUALIZACAO_ANUNCIO",
    leadId: "",
    source: "",
    campaignCode: "",
    payload: "",
  });

  const onSubmit = (evt: FormEvent) => {
    evt.preventDefault();
    registerEvent.mutate({
      stage: form.stage as any,
      leadId: form.leadId ? form.leadId.trim() : undefined,
      source: form.source ? form.source.trim() : undefined,
      campaignCode: form.campaignCode ? form.campaignCode.trim() : undefined,
      payload: form.payload ? form.payload.trim() : undefined,
    });
  };

  const handleReset = async () => {
    if (resetFunnel.isPending) {
      return;
    }
    if (!window.confirm("Deseja zerar as contagens do funil a partir de agora?")) {
      return;
    }
    try {
      await resetFunnel.mutateAsync();
      toast.success("Funil reiniciado. Novos eventos passarão a ser contabilizados a partir deste momento.");
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message ?? error.response?.data?.detail ??
          "Não foi possível zerar o funil."
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
              Cada etapa consolida dados automáticos (Facebook Ads, Lead Portal e
              e-mails) e eventos manuais, para dar visibilidade ao avanço das
              leads no experimento.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-danger btn-sm"
            onClick={handleReset}
            disabled={resetFunnel.isPending}
          >
            {resetFunnel.isPending ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : (
              "Zerar contagens"
            )}
          </button>
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
                  <th>Último evento</th>
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
                    <td>{formatRateComparedToPreviousStage(stage.totalCount, selectableStages[index - 1]?.totalCount)}</td>
                    <td>
                      {formatCostPerConversion(normalizedTotalSpend, stage.totalCount)}
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
                        {item.minAcceptableRate != null ? ` · Mín. aceitável: ${formatPercent(item.minAcceptableRate)}` : ""}
                        {item.upper95RateIfZero != null ? ` · Limite 95% (0 sucessos): ${formatPercent(item.upper95RateIfZero)}` : ""}
                      </div>
                    </div>
                    <span className={`badge ${statusBadgeClass(item.status)}`} title="Diagnóstico calculado no backend">
                      {statusLabel(item.status)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <hr className="my-4" />
        <form className="row g-3 align-items-end" onSubmit={onSubmit}>
          <div className="col-12 col-lg-3">
            <label className="form-label" htmlFor="funnel_stage">
              Etapa
            </label>
            <select
              id="funnel_stage"
              className="form-select"
              value={form.stage}
              onChange={(e) => setForm({ ...form, stage: e.target.value })}
            >
              {stages.map((stage) => (
                <option key={stage.stage} value={stage.stage}>
                  {stage.order}. {stage.label}
                </option>
              ))}
            </select>
          </div>
          <div className="col-12 col-lg-3">
            <label className="form-label" htmlFor="funnel_lead">
              Lead (UUID) opcional
            </label>
            <input
              id="funnel_lead"
              type="text"
              className="form-control"
              placeholder="00000000-0000-0000-0000-000000000000"
              value={form.leadId}
              onChange={(e) => setForm({ ...form, leadId: e.target.value })}
            />
          </div>
          <div className="col-12 col-lg-3">
            <label className="form-label" htmlFor="funnel_source">
              Fonte (opcional)
            </label>
            <input
              id="funnel_source"
              type="text"
              className="form-control"
              placeholder="manual, integração, etc"
              value={form.source}
              onChange={(e) => setForm({ ...form, source: e.target.value })}
            />
          </div>
          <div className="col-12 col-lg-3">
            <label className="form-label" htmlFor="funnel_campaign_code">
              Código do anúncio (campaign)
            </label>
            <input
              id="funnel_campaign_code"
              type="text"
              className="form-control"
              placeholder="ex.: 123456789012345"
              value={form.campaignCode}
              onChange={(e) => setForm({ ...form, campaignCode: e.target.value })}
            />
            <div className="form-text">
              Use o mesmo valor configurado nos parâmetros da URL/UTM do anúncio.
            </div>
          </div>
          <div className="col-12 col-lg-3 d-flex align-items-end">
            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={registerEvent.isPending}
            >
              {registerEvent.isPending ? "Registrando..." : "Registrar evento"}
            </button>
          </div>
          <div className="col-12">
            <label className="form-label" htmlFor="funnel_payload">
              Observação ou payload (opcional)
            </label>
            <textarea
              id="funnel_payload"
              className="form-control"
              rows={2}
              placeholder="Detalhes adicionais para rastreabilidade"
              value={form.payload}
              onChange={(e) => setForm({ ...form, payload: e.target.value })}
            />
            {registerEvent.isSuccess ? (
              <div className="text-success small mt-1">
                Evento registrado com sucesso.
              </div>
            ) : null}
            {registerEvent.isError ? (
              <div className="text-danger small mt-1">
                Não foi possível salvar o evento. Verifique os dados e tente de
                novo.
              </div>
            ) : null}
          </div>
        </form>

        <div className="alert alert-info mb-0 mt-4" role="alert">
          <div className="fw-semibold mb-1">O que cada etapa representa</div>
          <ul className="mb-0 small ps-3">
            <li>1) Impressões do anúncio.</li>
            <li>2) Cliques que levaram ao formulário do experimento.</li>
            <li>3) Renderizações completas do formulário (evento lead-portal-render-complete).</li>
            <li>4) Envios de formulário (lead_portal_submission).</li>
            <li>5) Abertura do e-mail de amostra.</li>
            <li>6) Acessos ao checkout no Mercado Pago.</li>
            <li>7) Compras aprovadas.</li>
            <li>8) Abertura do e-mail de entrega da compra.</li>
            <li>9) Visualização/download do material pago.</li>
          </ul>
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

function calculateCostPerConversion(totalSpend: number | null, totalConversions?: number | null) {
  if (totalSpend === null || totalSpend === undefined || Number.isNaN(totalSpend)) {
    return null;
  }
  if (!totalConversions || totalConversions <= 0) {
    return null;
  }
  return totalSpend / totalConversions;
}

function formatCostPerConversion(totalSpend: number | null, totalConversions?: number | null) {
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
