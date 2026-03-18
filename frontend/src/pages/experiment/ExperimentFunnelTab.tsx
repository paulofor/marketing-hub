import { useState, type FormEvent } from "react";
import { useExperimentFunnel, type ExperimentFunnelStageSummary } from "../../api/experiment/useExperimentFunnel";
import { useRegisterExperimentFunnelEvent } from "../../api/experiment/useRegisterExperimentFunnelEvent";

interface ExperimentFunnelTabProps {
  experimentId: string;
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
}: ExperimentFunnelTabProps) {
  const { data, isLoading, isError } = useExperimentFunnel(experimentId);
  const stages = (data ?? []).slice().sort((a, b) => a.order - b.order);
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
  const [form, setForm] = useState({
    stage: "VISUALIZACAO_ANUNCIO",
    leadId: "",
    source: "",
    payload: "",
  });

  const onSubmit = (evt: FormEvent) => {
    evt.preventDefault();
    registerEvent.mutate({
      stage: form.stage as any,
      leadId: form.leadId ? form.leadId.trim() : undefined,
      source: form.source ? form.source.trim() : undefined,
      payload: form.payload ? form.payload.trim() : undefined,
    });
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
                  <th>Automático</th>
                  <th>Manuais</th>
                  <th>Total</th>
                  <th>Únicos</th>
                  <th>Último evento</th>
                  <th>Fonte de dados</th>
                </tr>
              </thead>
              <tbody>
                {selectableStages.map((stage) => (
                  <tr key={stage.stage}>
                    <td>
                      <div className="fw-semibold">
                        {stage.order}. {stage.label}
                      </div>
                    </td>
                    <td>{stage.autoCount}</td>
                    <td>{stage.manualCount}</td>
                    <td>
                      <strong>{stage.totalCount}</strong>
                    </td>
                    <td>{stage.uniqueCount ?? "—"}</td>
                    <td>{formatDate(stage.lastEventAt)}</td>
                    <td className="text-muted small">
                      {stage.source ?? "Eventos manuais registrados na aplicação"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

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
