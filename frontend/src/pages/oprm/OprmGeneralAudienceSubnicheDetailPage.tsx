import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  OprmGeneralAudienceSubnicheStatus,
  subnicheStatusLabels,
  useApproveOprmGeneralAudienceSubniche,
  useOprmGeneralAudienceSubniche,
  useRejectOprmGeneralAudienceSubniche,
  useUpdateOprmGeneralAudienceSubniche,
} from "../../api/oprm/useOprmGeneralAudiences";
import OprmModuleNavigation from "./OprmModuleNavigation";

function parseRouteId(value: string | undefined) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function formatDateTime(value: string | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

function emptyToUndefined(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export default function OprmGeneralAudienceSubnicheDetailPage() {
  const { subnicheId: subnicheIdParam } = useParams();
  const subnicheId = parseRouteId(subnicheIdParam);
  const subnicheQuery = useOprmGeneralAudienceSubniche(subnicheId);
  const updateSubniche = useUpdateOprmGeneralAudienceSubniche(subnicheId ?? 0);
  const approveSubniche = useApproveOprmGeneralAudienceSubniche(
    subnicheId ?? 0,
  );
  const rejectSubniche = useRejectOprmGeneralAudienceSubniche(subnicheId ?? 0);
  const [form, setForm] = useState({
    personaSummary: "",
    painSummary: "",
    desiredOutcomeSummary: "",
    languagePatterns: "",
    channelsSummary: "",
    qualificationQuestion: "",
    opportunityScore: "",
    riskScore: "",
  });

  useEffect(() => {
    const subniche = subnicheQuery.data;
    if (subniche) {
      setForm({
        personaSummary: subniche.personaSummary ?? "",
        painSummary: subniche.painSummary ?? "",
        desiredOutcomeSummary: subniche.desiredOutcomeSummary ?? "",
        languagePatterns: subniche.languagePatterns ?? "",
        channelsSummary: subniche.channelsSummary ?? "",
        qualificationQuestion: subniche.qualificationQuestion ?? "",
        opportunityScore: subniche.opportunityScore?.toString() ?? "",
        riskScore: subniche.riskScore?.toString() ?? "",
      });
    }
  }, [subnicheQuery.data]);

  if (!subnicheId) {
    return <div className="alert alert-danger">Subnicho inválido.</div>;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await updateSubniche.mutateAsync({
      personaSummary: emptyToUndefined(form.personaSummary),
      painSummary: emptyToUndefined(form.painSummary),
      desiredOutcomeSummary: emptyToUndefined(form.desiredOutcomeSummary),
      languagePatterns: emptyToUndefined(form.languagePatterns),
      channelsSummary: emptyToUndefined(form.channelsSummary),
      qualificationQuestion: emptyToUndefined(form.qualificationQuestion),
      opportunityScore: form.opportunityScore
        ? Number(form.opportunityScore)
        : undefined,
      riskScore: form.riskScore ? Number(form.riskScore) : undefined,
    });
  }

  async function handleStatusAction(status: "approve" | "reject") {
    if (status === "approve") {
      await approveSubniche.mutateAsync();
      return;
    }
    await rejectSubniche.mutateAsync();
  }

  const subniche = subnicheQuery.data;
  const isActionPending =
    approveSubniche.isPending ||
    rejectSubniche.isPending ||
    updateSubniche.isPending;
  const isApproved = subniche?.status === "APPROVED_FOR_EXPERIMENT";
  const isRejected = subniche?.status === "REJECTED";
  const status = subniche?.status as
    | OprmGeneralAudienceSubnicheStatus
    | undefined;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>{subniche?.name ?? "Subnicho de Público Geral"}</PageTitle>
        <p className="text-secondary mb-0">
          Revise se o subnicho tem persona, dor, resultado desejado, linguagem
          real, canais e pergunta qualificadora antes de avançar para
          experimento.
        </p>
      </header>

      <OprmModuleNavigation />

      {subnicheQuery.isLoading ? (
        <div
          className="spinner-border text-primary"
          role="status"
          aria-label="Carregando subnicho"
        />
      ) : null}
      {subnicheQuery.isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar o subnicho de público geral.
        </div>
      ) : null}

      {subniche ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                <div>
                  <h2 className="h5 mb-2">Mapa de dores e linguagem</h2>
                  <p className="text-secondary mb-0">
                    Origem: Público Geral → Semente #{subniche.seedId} →{" "}
                    {subniche.name}. Este registro ainda não é oferta final.
                  </p>
                </div>
                <Link
                  className="btn btn-outline-secondary btn-sm"
                  to={`/oprm/general-audiences/seeds/${subniche.seedId}`}
                >
                  Voltar para semente
                </Link>
              </div>
              <dl className="row mt-4 mb-0">
                <dt className="col-md-3">Status</dt>
                <dd className="col-md-9">
                  {status ? subnicheStatusLabels[status] : "-"}
                </dd>
                <dt className="col-md-3">Pergunta qualificadora</dt>
                <dd className="col-md-9">
                  {subniche.qualificationQuestion || "Pendente"}
                </dd>
                <dt className="col-md-3">Última atualização</dt>
                <dd className="col-md-9">
                  {formatDateTime(subniche.updatedAt)}
                </dd>
              </dl>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">Revisão comercial</h2>
              <form className="row g-3" onSubmit={handleSubmit}>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="persona-summary">
                    Persona e contexto
                  </label>
                  <textarea
                    id="persona-summary"
                    className="form-control"
                    rows={3}
                    value={form.personaSummary}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        personaSummary: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="pain-summary">
                    Dores reais
                  </label>
                  <textarea
                    id="pain-summary"
                    className="form-control"
                    rows={3}
                    value={form.painSummary}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        painSummary: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="desired-outcome">
                    Resultado desejado
                  </label>
                  <textarea
                    id="desired-outcome"
                    className="form-control"
                    rows={3}
                    value={form.desiredOutcomeSummary}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        desiredOutcomeSummary: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="language-patterns">
                    Linguagem real
                  </label>
                  <textarea
                    id="language-patterns"
                    className="form-control"
                    rows={3}
                    value={form.languagePatterns}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        languagePatterns: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="channels-summary">
                    Canais e sinais de aquisição
                  </label>
                  <textarea
                    id="channels-summary"
                    className="form-control"
                    rows={3}
                    value={form.channelsSummary}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        channelsSummary: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label
                    className="form-label"
                    htmlFor="qualification-question"
                  >
                    Pergunta qualificadora
                  </label>
                  <textarea
                    id="qualification-question"
                    className="form-control"
                    rows={3}
                    value={form.qualificationQuestion}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        qualificationQuestion: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label" htmlFor="opportunity-score">
                    Score de oportunidade
                  </label>
                  <input
                    id="opportunity-score"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    className="form-control"
                    value={form.opportunityScore}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        opportunityScore: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label" htmlFor="risk-score">
                    Score de risco
                  </label>
                  <input
                    id="risk-score"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    className="form-control"
                    value={form.riskScore}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        riskScore: event.target.value,
                      }))
                    }
                  />
                </div>
                {updateSubniche.isError ? (
                  <div className="col-12">
                    <div className="alert alert-danger mb-0">
                      Não foi possível salvar a revisão do subnicho.
                    </div>
                  </div>
                ) : null}
                <div className="col-12">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={isActionPending}
                  >
                    {updateSubniche.isPending ? (
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        aria-hidden="true"
                      />
                    ) : null}
                    Salvar mapa
                  </button>
                </div>
              </form>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-2">Ações de decisão</h2>
              <p className="text-secondary">
                Aprovar libera o subnicho para etapa futura de ângulos e
                experimento. Rejeitar evita que público amplo ou inseguro vire
                campanha.
              </p>
              <div className="d-flex gap-2 flex-wrap">
                <button
                  type="button"
                  className="btn btn-success"
                  onClick={() => handleStatusAction("approve")}
                  disabled={isActionPending || isApproved}
                >
                  {approveSubniche.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
                  Aprovar para experimento
                </button>
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  onClick={() => handleStatusAction("reject")}
                  disabled={isActionPending || isRejected}
                >
                  {rejectSubniche.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
                  Rejeitar
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  disabled
                  title="Conversão para MarketNiche pertence à fase 5 do plano para manter controle arquitetural."
                >
                  Converter em nicho — fase 5
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  disabled
                  title="Criação de experimento de lead pertence à fase 5 do plano."
                >
                  Criar experimento de lead — fase 5
                </button>
              </div>
              {approveSubniche.isError || rejectSubniche.isError ? (
                <div className="alert alert-danger mt-3 mb-0">
                  Não foi possível registrar a decisão do subnicho.
                </div>
              ) : null}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
