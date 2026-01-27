import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useInstantForm } from "../../api/instantForms/useInstantForm";
import { useUpdateInstantFormApproval } from "../../api/instantForms/useUpdateInstantFormApproval";
import { useDeleteInstantForm } from "../../api/instantForms/useDeleteInstantForm";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import {
  useFacebookAdSetExperimentsReady,
  type ReadyTargetingElement,
} from "../../api/useFacebookAdSetExperimentsReady";

function formatCurrency(value?: number | string | null) {
  if (value == null) return "—";
  const numeric = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(numeric)) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(numeric);
}

function formatDateOnly(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString("pt-BR");
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function InstantFormDetailPage() {
  const navigate = useNavigate();
  const { id, instantFormId } = useParams();
  const experimentId = id as string;
  const experimentNumericId = Number(experimentId);
  const { data: experiment } = useExperiment(experimentId);
  const { data: instantForm, isLoading, isError } = useInstantForm(instantFormId ?? "");
  const { data: niche } = useNiche(experiment?.nicheId ?? 0);
  const { data: hypothesis } = useHypothesis(
    experiment ? String(experiment.nicheId) : undefined,
    experiment ? String(experiment.hypothesisId) : undefined,
  );
  const {
    data: experimentsReady,
    isLoading: isLoadingFacebookPayload,
    isError: isErrorFacebookPayload,
  } = useFacebookAdSetExperimentsReady();
  const facebookPayload =
    Array.isArray(experimentsReady) && !Number.isNaN(experimentNumericId)
      ? experimentsReady.find((item) => item.experiment?.id === experimentNumericId)
      : undefined;
  const [feedback, setFeedback] = useState<string | null>(null);
  const [pendingApprovalAction, setPendingApprovalAction] = useState<"approve" | "revoke" | null>(null);
  const updateApproval = useUpdateInstantFormApproval({
    id: Number(instantFormId ?? 0),
    hypothesisId: experiment?.hypothesisId ? String(experiment.hypothesisId) : undefined,
  });
  const deleteInstantForm = useDeleteInstantForm({
    id: Number(instantFormId ?? 0),
    hypothesisId: experiment?.hypothesisId ? String(experiment.hypothesisId) : undefined,
    experimentId,
  });

  useBreadcrumbs(
    experiment && instantForm
      ? [
          {
            label: niche?.name ?? "Nicho",
            to: `/niches/${experiment.nicheId}`,
            icon: nicheIcon,
          },
          {
            label: hypothesis?.title ?? "Hipótese",
            to: `/niches/${experiment.nicheId}/hypotheses/${experiment.hypothesisId}`,
            icon: hypothesisIcon,
          },
          {
            label: experiment.name,
            to: `/experiments/${experimentId}`,
            icon: experimentIcon,
          },
          {
            label: instantForm.name,
          },
        ]
      : [],
  );

  const handleApproval = async (approved: boolean) => {
    if (!instantFormId) return;
    setPendingApprovalAction(approved ? "approve" : "revoke");
    try {
      await updateApproval.mutateAsync(approved);
      setFeedback(approved ? "Instant form aprovado." : "Aprovação removida.");
    } catch (error) {
      setFeedback("Não foi possível atualizar a aprovação. Tente novamente.");
    } finally {
      setPendingApprovalAction(null);
    }
  };

  const handleDelete = async () => {
    if (!instantFormId) return;
    if (!confirm("Excluir este instant form? Esta ação não pode ser desfeita.")) return;
    try {
      await deleteInstantForm.mutateAsync();
      navigate(`/experiments/${experimentId}`);
    } catch (error) {
      setFeedback("Não foi possível excluir o instant form. Tente novamente.");
    }
  };

  if (isLoading) {
    return <p>Carregando detalhamento do instant form...</p>;
  }

  if (isError || !instantForm) {
    return <p>Não foi possível carregar este instant form.</p>;
  }

  const approvalBadgeClass = instantForm.approved ? "text-bg-success" : "text-bg-secondary";
  const approvalLabel = instantForm.approved ? "Aprovado" : "Pendente";

  const renderTargetingGroup = (
    label: string,
    items?: ReadyTargetingElement[] | null,
  ) => {
    if (!items || items.length === 0) {
      return (
        <div className="col" key={label}>
          <div className="card h-100 border border-dashed border-2 border-secondary-subtle">
            <div className="card-body text-center text-body-secondary">
              <strong className="d-block mb-1">{label}</strong>
              <span className="small">Nenhum elemento aprovado.</span>
            </div>
          </div>
        </div>
      );
    }
    return (
      <div className="col" key={label}>
        <div className="card h-100">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <strong>{label}</strong>
              <span className="badge text-bg-success">{items.length}</span>
            </div>
            <ul className="list-unstyled mb-0 d-flex flex-column gap-3">
              {items.map((item) => (
                <li key={`${label}-${item.id}`}>
                  <div className="fw-semibold">{item.term}</div>
                  {item.description ? (
                    <p className="text-muted small mb-1">{item.description}</p>
                  ) : null}
                  <dl className="row small mb-0">
                    <dt className="col-sm-4">Modelo</dt>
                    <dd className="col-sm-8">{item.model ?? "—"}</dd>
                    <dt className="col-sm-4">Meta ID</dt>
                    <dd className="col-sm-8">{item.metaId ?? "—"}</dd>
                    <dt className="col-sm-4">Prompt</dt>
                    <dd className="col-sm-8">
                      {item.prompt ? (
                        <pre
                          className="bg-body-tertiary rounded-3 p-2 mb-0"
                          style={{ whiteSpace: "pre-wrap" }}
                        >
                          {item.prompt}
                        </pre>
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </dd>
                  </dl>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="mt-3">
      <div className="d-flex justify-content-between align-items-center gap-3">
        <PageTitle>Instant form · {instantForm.name}</PageTitle>
        <Link to={`/experiments/${experimentId}`} className="btn btn-outline-secondary">
          Voltar para o experimento
        </Link>
      </div>

      {feedback ? (
        <div className="alert alert-info mt-3" role="alert">
          {feedback}
        </div>
      ) : null}

      <section className="card mt-3 mb-4">
        <div className="card-header d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
          <div>
            <h5 className="mb-1">Resumo do instant form</h5>
            <p className="text-muted small mb-0">
              Acompanhe os atributos sincronizados do formulário instantâneo selecionado para a jornada.
            </p>
          </div>
          <span className={`badge ${approvalBadgeClass}`}>{approvalLabel}</span>
        </div>
        <div className="card-body">
          <div className="row gy-3">
            <div className="col-md-6">
              <dl className="row mb-0">
                <dt className="col-sm-5">Página</dt>
                <dd className="col-sm-7">
                  <div className="fw-semibold">{instantForm.facebookPageName}</div>
                  <div className="text-muted small">{instantForm.facebookPageExternalId}</div>
                </dd>
                <dt className="col-sm-5">ID do formulário</dt>
                <dd className="col-sm-7">{instantForm.facebookFormId ?? "—"}</dd>
                <dt className="col-sm-5">Status Meta</dt>
                <dd className="col-sm-7">{instantForm.status ?? "—"}</dd>
                <dt className="col-sm-5">Idioma</dt>
                <dd className="col-sm-7">{instantForm.locale ?? "—"}</dd>
                <dt className="col-sm-5">Modelo</dt>
                <dd className="col-sm-7">{instantForm.model ?? "—"}</dd>
                <dt className="col-sm-5">Leads</dt>
                <dd className="col-sm-7">{instantForm.leadsCount ?? "—"}</dd>
              </dl>
            </div>
            <div className="col-md-6">
              <dl className="row mb-0">
                <dt className="col-sm-5">Criado em</dt>
                <dd className="col-sm-7">{formatDate(instantForm.createdAt)}</dd>
                <dt className="col-sm-5">Atualizado em</dt>
                <dd className="col-sm-7">{formatDate(instantForm.updatedAt)}</dd>
                <dt className="col-sm-5">Última sync Meta</dt>
                <dd className="col-sm-7">{formatDate(instantForm.updatedTime)}</dd>
                <dt className="col-sm-5">Aprovado em</dt>
                <dd className="col-sm-7">{formatDate(instantForm.approvedAt)}</dd>
                <dt className="col-sm-5">Follow-up</dt>
                <dd className="col-sm-7">
                  {instantForm.followUpActionUrl ? (
                    <div className="d-flex flex-column gap-1">
                      <a
                        href={instantForm.followUpActionUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="small"
                      >
                        Página de agradecimento
                      </a>
                      <span className="text-muted small text-break">
                        {instantForm.followUpActionUrl}
                      </span>
                    </div>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </dd>
                <dt className="col-sm-5">Privacidade</dt>
                <dd className="col-sm-7">
                  {instantForm.privacyPolicyUrl ? (
                    <div className="d-flex flex-column gap-1">
                      <a
                        href={instantForm.privacyPolicyUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="small"
                      >
                        Política de privacidade
                      </a>
                      <span className="text-muted small text-break">
                        {instantForm.privacyPolicyUrl}
                      </span>
                    </div>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </dd>
              </dl>
            </div>
          </div>
        </div>
        <div className="card-footer d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-success"
            disabled={updateApproval.isPending || instantForm.approved}
            onClick={() => handleApproval(true)}
          >
            {updateApproval.isPending && pendingApprovalAction === "approve" ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Atualizando...
              </>
            ) : (
              "Aprovar"
            )}
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={updateApproval.isPending || !instantForm.approved}
            onClick={() => handleApproval(false)}
          >
            {updateApproval.isPending && pendingApprovalAction === "revoke" ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Atualizando...
              </>
            ) : (
              "Remover aprovação"
            )}
          </button>
          <button
            type="button"
            className="btn btn-outline-danger ms-auto"
            disabled={deleteInstantForm.isPending}
            onClick={handleDelete}
          >
            {deleteInstantForm.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Excluindo...
              </>
            ) : (
              "Excluir instant form"
            )}
          </button>
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <h5 className="mb-1">Dados enviados para o Facebook Ads</h5>
          <p className="text-muted small mb-0">
            Confirmar quais informações alimentam a criação automática de campanhas e conjuntos de anúncios.
          </p>
        </div>
        <div className="card-body">
          {isLoadingFacebookPayload ? (
            <div className="d-flex align-items-center gap-2 text-muted small">
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
              Carregando dados de envio...
            </div>
          ) : isErrorFacebookPayload ? (
            <p className="text-danger mb-0">
              Não foi possível carregar os dados preparados para o Facebook Ads. Tente novamente mais tarde.
            </p>
          ) : !facebookPayload ? (
            <p className="text-muted mb-0">
              Nenhuma preparação encontrada para este experimento. Verifique se ele está elegível na fila de campanhas e se as
              os elementos de segmentação foram aprovados.
            </p>
          ) : (
            <div className="row gy-4">
              <div className="col-lg-6">
                <h6 className="text-uppercase text-muted small">Experimento</h6>
                <dl className="row small mb-0">
                  <dt className="col-sm-5">Nome</dt>
                  <dd className="col-sm-7">{facebookPayload.experiment?.name ?? "—"}</dd>
                  <dt className="col-sm-5">Hipótese</dt>
                  <dd className="col-sm-7">{facebookPayload.experiment?.hypothesis ?? "—"}</dd>
                  <dt className="col-sm-5">Status</dt>
                  <dd className="col-sm-7">{facebookPayload.experiment?.status ?? "—"}</dd>
                  <dt className="col-sm-5">Plataforma</dt>
                  <dd className="col-sm-7">{facebookPayload.experiment?.platform ?? "—"}</dd>
                  <dt className="col-sm-5">KPI alvo</dt>
                  <dd className="col-sm-7">{formatCurrency(facebookPayload.experiment?.kpiTargetCpl)}</dd>
                  <dt className="col-sm-5">Início planejado</dt>
                  <dd className="col-sm-7">{formatDateOnly(facebookPayload.experiment?.startDate)}</dd>
                  <dt className="col-sm-5">Término planejado</dt>
                  <dd className="col-sm-7">{formatDateOnly(facebookPayload.experiment?.endDate)}</dd>
                  <dt className="col-sm-5">Página Meta</dt>
                  <dd className="col-sm-7">
                    {facebookPayload.experiment?.facebookPage ? (
                      <>
                        <div className="fw-semibold">{facebookPayload.experiment.facebookPage.name}</div>
                        <div className="text-muted">{facebookPayload.experiment.facebookPage.pageId}</div>
                      </>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </dd>
                  <dt className="col-sm-5">Instant form vinculado</dt>
                  <dd className="col-sm-7">
                    {facebookPayload.experiment?.facebookInstantForm ? (
                      <>
                        <div className="fw-semibold">{facebookPayload.experiment.facebookInstantForm.name}</div>
                        <div className="text-muted">
                          {facebookPayload.experiment.facebookInstantForm.facebookFormId ?? "—"}
                        </div>
                      </>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </dd>
                </dl>
              </div>
              <div className="col-lg-6">
                <h6 className="text-uppercase text-muted small">Contexto estratégico</h6>
                <dl className="row small mb-4">
                  <dt className="col-sm-5">Nicho</dt>
                  <dd className="col-sm-7">{facebookPayload.niche?.name ?? "—"}</dd>
                  <dt className="col-sm-5">Segmentação base</dt>
                  <dd className="col-sm-7">
                    {facebookPayload.niche?.baseSegmentation ? (
                      <span>{facebookPayload.niche.baseSegmentation}</span>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </dd>
                  <dt className="col-sm-5">Interesses sugeridos</dt>
                  <dd className="col-sm-7">
                    {facebookPayload.niche?.interests ? (
                      <span>{facebookPayload.niche.interests}</span>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </dd>
                  <dt className="col-sm-5">Filtros demográficos</dt>
                  <dd className="col-sm-7">
                    {facebookPayload.niche?.demographicFilters ? (
                      <span>{facebookPayload.niche.demographicFilters}</span>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </dd>
                  <dt className="col-sm-5">Hipótese (persona)</dt>
                  <dd className="col-sm-7">{facebookPayload.hypothesis?.persona ?? "—"}</dd>
                  <dt className="col-sm-5">Promessa central</dt>
                  <dd className="col-sm-7">{facebookPayload.hypothesis?.promise ?? "—"}</dd>
                  <dt className="col-sm-5">Mecanismo</dt>
                  <dd className="col-sm-7">{facebookPayload.hypothesis?.mechanism ?? "—"}</dd>
                  <dt className="col-sm-5">Mecanismo único</dt>
                  <dd className="col-sm-7">{facebookPayload.hypothesis?.uniqueMechanism ?? "—"}</dd>
                </dl>
              </div>
              <div className="col-12">
                <h6 className="text-uppercase text-muted small">Segmentação aprovada</h6>
                {facebookPayload.targeting ? (
                  <div className="row row-cols-1 row-cols-lg-3 g-3">
                    {renderTargetingGroup("Interesses", facebookPayload.targeting.interests)}
                    {renderTargetingGroup("Cargos", facebookPayload.targeting.jobTitles)}
                    {renderTargetingGroup("Comportamentos", facebookPayload.targeting.behaviors)}
                  </div>
                ) : (
                  <p className="text-muted mb-0">
                    Nenhum elemento aprovado foi localizado para este experimento.
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <h5 className="mb-1">Prompt enviado ao modelo</h5>
          <p className="text-muted small mb-0">
            Este é o prompt utilizado pelo Worker IA para gerar o formulário no Meta Ads.
          </p>
        </div>
        <div className="card-body">
          {instantForm.prompt ? (
            <pre className="bg-body-tertiary rounded-3 p-3 small mb-0" style={{ whiteSpace: "pre-wrap" }}>
              {instantForm.prompt}
            </pre>
          ) : (
            <p className="text-muted mb-0">Nenhum prompt registrado para este instant form.</p>
          )}
        </div>
      </section>
    </div>
  );
}
