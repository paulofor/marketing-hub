import { Fragment, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useExperimentEmailDetail } from "../../api/experiment/useExperimentEmailDetail";
import { useUpdateExperimentEmailApproval } from "../../api/experiment/useUpdateExperimentEmailApproval";
import { useDeleteExperimentEmail } from "../../api/experiment/useDeleteExperimentEmail";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";

const STATUS_LABELS: Record<string, string> = {
  draft: "Rascunho",
  review: "Em revisão",
  approved: "Aprovado",
};

const STATUS_BADGE: Record<string, string> = {
  draft: "text-bg-secondary",
  review: "text-bg-warning",
  approved: "text-bg-success",
};

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

export default function ExperimentEmailDetailPage() {
  const navigate = useNavigate();
  const { id, emailStepId } = useParams();
  const experimentId = id as string;
  const stepId = emailStepId as string;
  const { data: experiment } = useExperiment(experimentId);
  const { data: detail, isLoading, isError } = useExperimentEmailDetail(experimentId, stepId);
  const { data: niche } = useNiche(experiment?.nicheId ?? 0);
  const { data: hypothesis } = useHypothesis(
    experiment ? String(experiment.nicheId) : undefined,
    experiment ? String(experiment.hypothesisId) : undefined,
  );
  const [feedback, setFeedback] = useState<string | null>(null);
  const [pendingApprovalAction, setPendingApprovalAction] = useState<"approve" | "revoke" | null>(null);
  const updateApproval = useUpdateExperimentEmailApproval({
    experimentId,
    stepId,
    journeyId: detail?.journeyId,
  });
  const deleteEmail = useDeleteExperimentEmail({
    experimentId,
    stepId,
    journeyId: detail?.journeyId,
  });

  useBreadcrumbs(
    experiment && detail
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
            label: detail.stepName ?? `E-mail ${detail.stepPosition ?? ""}`,
          },
        ]
      : [],
  );

  const statusBadge = detail?.status ? STATUS_BADGE[detail.status] ?? "text-bg-secondary" : "text-bg-secondary";
  const statusLabel = detail?.status ? STATUS_LABELS[detail.status] ?? detail.status : "Sem status";

  const stepMetadataEntries = useMemo(() => {
    if (!detail?.stepMetadata) return [] as Array<[string, string]>;
    return Object.entries(detail.stepMetadata).filter(([, value]) => value != null && value !== "");
  }, [detail?.stepMetadata]);

  const handleApproval = async (approved: boolean) => {
    try {
      setPendingApprovalAction(approved ? "approve" : "revoke");
      await updateApproval.mutateAsync(approved);
      setFeedback(approved ? "E-mail marcado como aprovado." : "Aprovação removida deste e-mail.");
    } catch (error) {
      setFeedback("Não foi possível atualizar o status. Tente novamente.");
    } finally {
      setPendingApprovalAction(null);
    }
  };

  const handleDelete = async () => {
    if (!confirm("Excluir o conteúdo deste e-mail? Esta ação removerá os metadados salvos.")) return;
    try {
      await deleteEmail.mutateAsync();
      navigate(`/experiments/${experimentId}`);
    } catch (error) {
      setFeedback("Não foi possível excluir o e-mail. Tente novamente.");
    }
  };

  if (isLoading) {
    return <p>Carregando detalhamento do e-mail...</p>;
  }

  if (isError || !detail) {
    return <p>Não foi possível carregar este e-mail.</p>;
  }

  return (
    <div className="mt-3">
      <div className="d-flex justify-content-between align-items-center gap-3">
        <PageTitle>E-mail · {detail.stepName ?? `Etapa ${detail.stepPosition}`}</PageTitle>
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
            <h5 className="mb-1">Resumo do e-mail planejado</h5>
            <p className="text-muted small mb-0">
              Detalhes do conteúdo criado pelo Worker IA para a etapa da jornada.
            </p>
          </div>
          <span className={`badge ${statusBadge}`}>{statusLabel}</span>
        </div>
        <div className="card-body">
          <div className="row gy-3">
            <div className="col-lg-6">
              <dl className="row mb-0">
                <dt className="col-sm-4">Assunto</dt>
                <dd className="col-sm-8">{detail.subject ?? "—"}</dd>
                <dt className="col-sm-4">Template</dt>
                <dd className="col-sm-8">{detail.templateId ?? "—"}</dd>
                <dt className="col-sm-4">Preheader</dt>
                <dd className="col-sm-8">{detail.preheader ?? "—"}</dd>
                <dt className="col-sm-4">Modelo</dt>
                <dd className="col-sm-8">{detail.model ?? "—"}</dd>
              </dl>
            </div>
            <div className="col-lg-6">
              <dl className="row mb-0">
                <dt className="col-sm-5">Etapa</dt>
                <dd className="col-sm-7">
                  <div className="fw-semibold">{detail.stepName ?? "—"}</div>
                  <div className="text-muted small">
                    {detail.stepPhase ? `${detail.stepPhase} • posição ${detail.stepPosition ?? "?"}` : `Posição ${detail.stepPosition ?? "?"}`}
                  </div>
                  {detail.stepDescription ? (
                    <p className="text-muted small mb-0 mt-1" style={{ whiteSpace: "pre-wrap" }}>
                      {detail.stepDescription}
                    </p>
                  ) : null}
                </dd>
                <dt className="col-sm-5">Criado em</dt>
                <dd className="col-sm-7">{formatDate(detail.journeyCreatedAt)}</dd>
                <dt className="col-sm-5">Atualizado em</dt>
                <dd className="col-sm-7">{formatDate(detail.journeyUpdatedAt)}</dd>
                <dt className="col-sm-5">Aprovação</dt>
                <dd className="col-sm-7">{detail.approved ? "Aprovado" : "Pendente"}</dd>
              </dl>
            </div>
          </div>
        </div>
        <div className="card-footer d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-success"
            disabled={updateApproval.isPending || detail.approved}
            onClick={() => handleApproval(true)}
          >
            {updateApproval.isPending && pendingApprovalAction === "approve" ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Atualizando...
              </>
            ) : (
              "Aprovar e-mail"
            )}
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={updateApproval.isPending || !detail.approved}
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
            disabled={deleteEmail.isPending}
            onClick={handleDelete}
          >
            {deleteEmail.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Excluindo...
              </>
            ) : (
              "Excluir metadados"
            )}
          </button>
        </div>
      </section>

      <section className="card mb-4">
        <div className="card-header">
          <h5 className="mb-1">Notas de copy e próximos passos</h5>
          <p className="text-muted small mb-0">
            Utilize estas referências para alinhar o copywriter e o time de CRM.
          </p>
        </div>
        <div className="card-body">
          {detail.notes ? (
            <p className="mb-0" style={{ whiteSpace: "pre-wrap" }}>
              {detail.notes}
            </p>
          ) : (
            <p className="text-muted mb-0">Nenhuma observação registrada.</p>
          )}
        </div>
      </section>

      <section className="card mb-4">
        <div className="card-header">
          <h5 className="mb-1">Metadados da etapa no template</h5>
          <p className="text-muted small mb-0">
            Contexto adicional definido no template da jornada.
          </p>
        </div>
        <div className="card-body">
          {stepMetadataEntries.length ? (
            <dl className="row gy-2 mb-0 small">
              {stepMetadataEntries.map(([key, value]) => (
                <Fragment key={key}>
                  <dt className="col-sm-4 text-uppercase text-muted">{key}</dt>
                  <dd className="col-sm-8 mb-0">{value}</dd>
                </Fragment>
              ))}
            </dl>
          ) : (
            <p className="text-muted mb-0">Nenhum metadado adicional definido para esta etapa.</p>
          )}
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <h5 className="mb-1">Prompt registrado</h5>
          <p className="text-muted small mb-0">
            Histórico completo enviado ao modelo de IA para gerar o plano deste e-mail.
          </p>
        </div>
        <div className="card-body">
          {detail.prompt ? (
            <pre className="bg-body-tertiary rounded-3 p-3 small mb-0" style={{ whiteSpace: "pre-wrap" }}>
              {detail.prompt}
            </pre>
          ) : (
            <p className="text-muted mb-0">Nenhum prompt foi armazenado para esta etapa.</p>
          )}
        </div>
      </section>
    </div>
  );
}
