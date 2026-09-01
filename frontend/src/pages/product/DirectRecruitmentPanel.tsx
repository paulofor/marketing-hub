import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import {
  CheckCircle2,
  ExternalLink,
  Link2,
  PauseCircle,
  Radio,
  ShieldCheck,
} from "lucide-react";
import {
  useActivateDirectRecruitment,
  useCreateDirectRecruitmentDraft,
  useExperimentDirectRecruitment,
  usePauseDirectRecruitment,
} from "../../api/experiment/useExperimentDirectRecruitment";

type DirectRecruitmentPanelProps = {
  experimentId: number;
};

const acquisitionLabels = {
  NOT_CREATED: "Atividade não preparada",
  DRAFT_REQUIRES_APPROVAL: "Rascunho aguardando aprovação",
  ACTIVE_WITHOUT_DISTRIBUTION: "Ativo, sem canal de distribuição",
  READY_FOR_ORGANIC_DISTRIBUTION: "Pronto para distribuição orgânica",
  PAUSED: "Aquisição pausada",
  SAMPLE_COMPLETE: "Amostra concluída",
};

/** Extrai uma mensagem segura de falha para orientar o operador. */
function requestError(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

/** Orquestra a nova atividade de aquisição inbound dentro da amostra direta. */
export default function DirectRecruitmentPanel({
  experimentId,
}: DirectRecruitmentPanelProps) {
  const campaign = useExperimentDirectRecruitment(experimentId);
  const createDraft = useCreateDirectRecruitmentDraft(experimentId);
  const activate = useActivateDirectRecruitment(experimentId);
  const pause = usePauseDirectRecruitment(experimentId);
  const [operator, setOperator] = useState("");
  const [approvalConfirmed, setApprovalConfirmed] = useState(false);
  const [pauseReason, setPauseReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const publicUrl = useMemo(() => {
    if (!campaign.data?.publicPath || campaign.data.status === "DRAFT") {
      return null;
    }
    return new URL(campaign.data.publicPath, window.location.origin).toString();
  }, [campaign.data?.publicPath, campaign.data?.status]);

  /** Cria o rascunho, mas não autoriza nenhuma distribuição externa. */
  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await createDraft.mutateAsync(operator);
    } catch (cause) {
      setError(requestError(cause, "Não foi possível preparar o convite."));
    }
  }

  /** Ativa a URL somente depois da confirmação explícita do conteúdo. */
  async function handleActivate() {
    setError(null);
    try {
      await activate.mutateAsync(operator);
      setApprovalConfirmed(false);
    } catch (cause) {
      setError(requestError(cause, "Não foi possível ativar o convite."));
    }
  }

  /** Pausa novas adesões e registra a causa funcional. */
  async function handlePause(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await pause.mutateAsync({ pausedBy: operator, reason: pauseReason });
      setPauseReason("");
    } catch (cause) {
      setError(requestError(cause, "Não foi possível pausar o convite."));
    }
  }

  if (campaign.isLoading) {
    return (
      <div className="alert alert-light border" role="status">
        Carregando a aquisição da amostra...
      </div>
    );
  }

  if (campaign.isError || !campaign.data) {
    return (
      <div className="alert alert-danger" role="alert">
        Não foi possível consultar a atividade de aquisição consentida.
      </div>
    );
  }

  const data = campaign.data;
  const activating = data.status === "DRAFT" || data.status === "PAUSED";

  return (
    <section
      className="card border-primary-subtle mb-4"
      aria-label="Aquisição consentida da amostra"
    >
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h3 className="h5 d-flex align-items-center gap-2 mb-1">
              <Radio size={20} aria-hidden="true" />
              Aquisição consentida da amostra
            </h3>
            <p className="text-body-secondary mb-0">
              O Hub prepara o convite, qualifica a adesão e só contabiliza quem
              aceita conhecer a oferta. Ativação, distribuição e venda são
              eventos separados.
            </p>
          </div>
          <span
            className={`badge ${data.acquisitionStatus === "SAMPLE_COMPLETE" ? "text-bg-success" : "text-bg-primary"}`}
          >
            {acquisitionLabels[data.acquisitionStatus]}
          </span>
        </div>

        <div className="row g-3 mt-1">
          <div className="col-6 col-lg">
            <div className="border rounded p-3 h-100 bg-light">
              <div className="small text-body-secondary">Visitas únicas</div>
              <div className="fs-4 fw-semibold">{data.uniqueVisits}</div>
            </div>
          </div>
          <div className="col-6 col-lg">
            <div className="border rounded p-3 h-100 bg-light">
              <div className="small text-body-secondary">Adesões</div>
              <div className="fs-4 fw-semibold">{data.submissions}</div>
            </div>
          </div>
          <div className="col-6 col-lg">
            <div className="border rounded p-3 h-100 bg-light">
              <div className="small text-body-secondary">Qualificados</div>
              <div className="fs-4 fw-semibold">
                {data.qualifiedSubmissions}
              </div>
            </div>
          </div>
          <div className="col-6 col-lg">
            <div className="border rounded p-3 h-100 bg-light">
              <div className="small text-body-secondary">Contatos oficiais</div>
              <div className="fs-4 fw-semibold">
                {data.recordedContacts}/{data.targetContacts}
              </div>
            </div>
          </div>
        </div>

        <div
          className={`alert mt-3 mb-3 ${data.acquisitionStatus === "ACTIVE_WITHOUT_DISTRIBUTION" ? "alert-warning" : "alert-info"}`}
          role="status"
        >
          <strong>Próximo gate:</strong> {data.distributionGuidance}
          {data.acquisitionStatus === "ACTIVE_WITHOUT_DISTRIBUTION" ? (
            <div className="mt-2">
              <a
                href="/social-distribution"
                target="_blank"
                rel="noreferrer"
                className="alert-link"
              >
                Conectar canal orgânico no Marketing Hub
                <ExternalLink size={14} className="ms-1" aria-hidden="true" />
              </a>
            </div>
          ) : null}
        </div>

        <div className="border rounded p-3">
          <div className="small text-uppercase fw-semibold text-body-secondary mb-2">
            Comunicação que será apresentada
          </div>
          <h4 className="h5">{data.headline}</h4>
          <p>{data.bodyText}</p>
          <p className="small mb-2">
            <strong>Público:</strong> {data.audienceSummary}
          </p>
          <div className="small d-flex gap-2 align-items-start text-body-secondary">
            <ShieldCheck
              size={17}
              className="flex-shrink-0"
              aria-hidden="true"
            />
            <span>{data.consentText}</span>
          </div>
        </div>

        <div className="mt-3">
          <label className="form-label" htmlFor="direct-recruitment-operator">
            Responsável pela atividade *
          </label>
          <input
            className="form-control"
            id="direct-recruitment-operator"
            value={operator}
            onChange={(event) => setOperator(event.target.value)}
            required
            maxLength={100}
          />
        </div>

        {data.status === "NOT_CREATED" ? (
          <form className="mt-3" onSubmit={handleCreate}>
            <button
              className="btn btn-primary"
              type="submit"
              disabled={createDraft.isPending || !operator.trim()}
            >
              {createDraft.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              {createDraft.isPending
                ? "Preparando convite..."
                : "Preparar convite para aprovação"}
            </button>
          </form>
        ) : null}

        {activating ? (
          <div className="mt-3">
            <div className="form-check mb-3">
              <input
                className="form-check-input"
                id="direct-recruitment-approval"
                type="checkbox"
                checked={approvalConfirmed}
                onChange={(event) => setApprovalConfirmed(event.target.checked)}
                required
              />
              <label
                className="form-check-label"
                htmlFor="direct-recruitment-approval"
              >
                Aprovo esta comunicação e confirmo que ativar a página não
                autoriza post, mensagem, campanha ou gasto. *
              </label>
            </div>
            <button
              className="btn btn-success d-inline-flex align-items-center"
              type="button"
              onClick={handleActivate}
              disabled={
                activate.isPending || !approvalConfirmed || !operator.trim()
              }
            >
              {activate.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : (
                <CheckCircle2 size={17} className="me-2" aria-hidden="true" />
              )}
              {activate.isPending ? "Ativando..." : "Aprovar e ativar convite"}
            </button>
          </div>
        ) : null}

        {publicUrl ? (
          <div className="mt-3 p-3 rounded bg-light border">
            <div className="small text-body-secondary mb-1">
              Link público rastreável
            </div>
            <a href={publicUrl} target="_blank" rel="noreferrer">
              <Link2 size={16} className="me-1" aria-hidden="true" />
              {publicUrl}
            </a>
          </div>
        ) : null}

        {data.status === "ACTIVE" ? (
          <form className="row g-2 mt-2" onSubmit={handlePause}>
            <div className="col-12 col-lg">
              <label
                className="form-label"
                htmlFor="direct-recruitment-pause-reason"
              >
                Motivo da pausa *
              </label>
              <input
                className="form-control"
                id="direct-recruitment-pause-reason"
                value={pauseReason}
                onChange={(event) => setPauseReason(event.target.value)}
                maxLength={500}
                required
              />
            </div>
            <div className="col-12 col-lg-auto d-flex align-items-end">
              <button
                className="btn btn-outline-secondary d-inline-flex align-items-center"
                type="submit"
                disabled={
                  pause.isPending || !operator.trim() || !pauseReason.trim()
                }
              >
                {pause.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : (
                  <PauseCircle size={17} className="me-2" aria-hidden="true" />
                )}
                {pause.isPending ? "Pausando..." : "Pausar aquisição"}
              </button>
            </div>
          </form>
        ) : null}

        {error ? (
          <div className="alert alert-danger mt-3 mb-0" role="alert">
            {error}
          </div>
        ) : null}
      </div>
    </section>
  );
}
