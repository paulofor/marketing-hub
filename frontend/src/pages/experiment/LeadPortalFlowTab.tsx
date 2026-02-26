import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useLeadPortalFlows } from "../../api/leadPortal/useLeadPortalFlows";
import { useUpdateLeadPortalFlowApproval } from "../../api/leadPortal/useUpdateLeadPortalFlowApproval";
import { useRequestLeadPortalFlows } from "../../api/experiment/useRequestLeadPortalFlows";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import WorkerRequestBanner from "./WorkerRequestBanner";

interface LeadPortalFlowTabProps {
  experiment: Experiment;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

type PreviewViewport = "desktop" | "mobile";

export default function LeadPortalFlowTab({
  experiment,
}: LeadPortalFlowTabProps) {
  const { data: flows, isLoading, isError } = useLeadPortalFlows({ nicheId: experiment.nicheId });
  const requestFlows = useRequestLeadPortalFlows(experiment.id);
  const updateExperiment = useUpdateExperiment(experiment.id);
  const updateApproval = useUpdateLeadPortalFlowApproval();
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [pendingAssignmentId, setPendingAssignmentId] = useState<number | null>(
    null,
  );
  const [pendingApprovalId, setPendingApprovalId] = useState<number | null>(
    null,
  );
  const [previewViewportByFlow, setPreviewViewportByFlow] = useState<
    Record<number, PreviewViewport>
  >({});
  const sortedFlows = useMemo(() => {
    if (!Array.isArray(flows)) return [];
    return [...flows].sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [flows]);

  const requestedCount = experiment.leadPortalFlowsToGenerate ?? 0;
  const assignedFlowId = experiment.leadPortalFlowId ?? null;

  const handleRequestFlows = async (quantity: number) => {
    await requestFlows.mutateAsync(quantity);
    setFeedback({
      variant: "success",
      message:
        quantity === 1
          ? "Solicitamos 1 fluxo do portal ao Worker IA."
          : `Solicitamos ${quantity} fluxos do portal ao Worker IA.`,
    });
  };

  const handleAssignFlow = async (flowId: number | null) => {
    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || experiment.metricPresetId == null) {
      setFeedback({
        variant: "error",
        message:
          "Defina a meta de KPI e o preset de métricas antes de vincular um fluxo do portal ao experimento.",
      });
      return;
    }
    setPendingAssignmentId(flowId ?? 0);
    setFeedback(null);
    try {
      await updateExperiment.mutateAsync({
        name: experiment.name,
        hypothesis: experiment.hypothesis,
        kpiTarget: Number(kpiTargetValue),
        metricPresetId: experiment.metricPresetId ?? undefined,
        sampleSize: experiment.sampleSize ?? undefined,
        mde: experiment.mdePercent ?? undefined,
        startDate: experiment.startDate ?? undefined,
        endDate: experiment.endDate ?? undefined,
        creativesToGenerate: experiment.creativesToGenerate ?? undefined,
        instantFormsToGenerate: experiment.instantFormsToGenerate ?? undefined,
        emailsToGenerate: experiment.emailsToGenerate ?? undefined,
        deliverablesToGenerate: experiment.deliverablesToGenerate ?? undefined,
        leadPortalFlowsToGenerate:
          experiment.leadPortalFlowsToGenerate ?? undefined,
        journeyTemplateId: experiment.journeyTemplateId ?? undefined,
        facebookPageId: experiment.facebookPage?.id ?? null,
        facebookInstantFormId: experiment.facebookInstantForm?.id ?? null,
        instagramAccountId: experiment.instagramAccount?.id ?? null,
        followUpActionUrl: experiment.followUpActionUrl ?? null,
        leadPortalFlowId: flowId,
      });
      setFeedback({
        variant: "success",
        message: flowId
          ? "Fluxo do portal vinculado ao experimento."
          : "O experimento não está mais associado a um fluxo do portal.",
      });
    } catch {
      setFeedback({
        variant: "error",
        message:
          "Não foi possível atualizar o experimento. Tente novamente em instantes.",
      });
    } finally {
      setPendingAssignmentId(null);
    }
  };

  const handleToggleApproval = async (flowId: number, approved: boolean) => {
    setPendingApprovalId(flowId);
    setFeedback(null);
    try {
      await updateApproval.mutateAsync({ id: flowId, approved });
    } catch {
      setFeedback({
        variant: "error",
        message:
          "Não foi possível atualizar a aprovação deste fluxo. Tente novamente.",
      });
    } finally {
      setPendingApprovalId(null);
    }
  };

  const handlePreviewViewportChange = (
    flowId: number,
    viewport: PreviewViewport,
  ) => {
    setPreviewViewportByFlow((current) => ({
      ...current,
      [flowId]: viewport,
    }));
  };

  return (
    <div className="d-flex flex-column gap-3">
      <WorkerRequestBanner
        title="Fluxos de portal do lead"
        subtitle="Solicite sugestões de perguntas para o portal e vincule o fluxo ideal ao experimento."
        resourceName="fluxo"
        resourceNamePlural="fluxos"
        requestedCount={requestedCount}
        existingCount={sortedFlows.length}
        buttonLabel="Gerar fluxos"
        onRequest={handleRequestFlows}
        isRequesting={requestFlows.isPending}
        helperText="Os fluxos gerados ficam disponíveis para revisão e aprovação antes de serem publicados."
      />

      <div className="alert alert-info d-flex flex-wrap justify-content-between align-items-center" role="status">
        <div className="me-3">
          <strong>Formulários simples agora vivem no nicho.</strong>
          <span className="ms-1">Crie e edite os formulários reutilizáveis diretamente na ficha do nicho.</span>
        </div>
        <Link className="btn btn-outline-primary btn-sm" to={`/niches/${experiment.nicheId}`}>
          Abrir nicho
        </Link>
      </div>

      {feedback ? (
        <div
          className={`alert ${feedback.variant === "success" ? "alert-success" : "alert-danger"}`}
          role="alert"
        >
          {feedback.message}
        </div>
      ) : null}

      {isLoading ? (
        <p className="text-muted">Carregando fluxos do portal...</p>
      ) : isError ? (
        <p className="text-danger">
          Não foi possível carregar os fluxos disponíveis.
        </p>
      ) : sortedFlows.length === 0 ? (
        <p className="text-muted">
          Nenhum fluxo do portal disponível no momento.
        </p>
      ) : (
        <div className="d-flex flex-column gap-3">
          {sortedFlows.map((flow) => {
            const isSelected = assignedFlowId === flow.id;
            const isApproving =
              pendingApprovalId === flow.id && updateApproval.isPending;
            const isAssigning =
              pendingAssignmentId === flow.id ||
              (pendingAssignmentId === 0 && flow.id === assignedFlowId);
            const activeViewport = previewViewportByFlow[flow.id] ?? "desktop";
            return (
              <div key={flow.id} className="card border-0 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-3">
                    <div>
                      <h5 className="card-title mb-1 d-flex align-items-center gap-2">
                        {flow.name}
                        {isSelected ? (
                          <span className="badge text-bg-primary">
                            Selecionado
                          </span>
                        ) : null}
                        {flow.approved ? (
                          <span className="badge text-bg-success">
                            Aprovado
                          </span>
                        ) : (
                          <span className="badge text-bg-secondary">
                            Pendente
                          </span>
                        )}
                      </h5>
                      <p className="text-muted small mb-0">Slug: {flow.slug}</p>
                      {flow.publicUrl ? (
                        <p className="text-muted small mt-2 mb-0">
                          URL pública:{" "}
                          <a
                            href={flow.publicUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                          >
                            {flow.publicUrl}
                          </a>
                        </p>
                      ) : null}
                      {flow.description ? (
                        <p className="text-muted small mt-2 mb-0">
                          {flow.description}
                        </p>
                      ) : null}
                    </div>
                    <div className="d-flex flex-column gap-2">
                      <button
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() =>
                          handleToggleApproval(flow.id, !flow.approved)
                        }
                        disabled={isApproving}
                      >
                        {isApproving ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                          />
                        ) : null}
                        {isApproving
                          ? "Atualizando..."
                          : flow.approved
                            ? "Revogar aprovação"
                            : "Aprovar"}
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleAssignFlow(flow.id)}
                        disabled={isAssigning || updateExperiment.isPending}
                      >
                        {isAssigning || updateExperiment.isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                          />
                        ) : null}
                        {isAssigning || updateExperiment.isPending
                          ? "Aplicando..."
                          : isSelected
                            ? "Fluxo selecionado"
                            : "Usar neste experimento"}
                      </button>
                    </div>
                  </div>

                  <div className="mt-3">
                    <div className="border rounded bg-light p-3 mb-3">
                      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                        <h6 className="fw-semibold mb-0">
                          Pré-visualização para o lead
                        </h6>
                        <div
                          className="btn-group btn-group-sm"
                          role="group"
                          aria-label="Alternar visualização"
                        >
                          <button
                            type="button"
                            className={`btn ${activeViewport === "desktop" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() =>
                              handlePreviewViewportChange(flow.id, "desktop")
                            }
                          >
                            Desktop
                          </button>
                          <button
                            type="button"
                            className={`btn ${activeViewport === "mobile" ? "btn-primary" : "btn-outline-primary"}`}
                            onClick={() =>
                              handlePreviewViewportChange(flow.id, "mobile")
                            }
                          >
                            Celular
                          </button>
                        </div>
                      </div>
                      <LeadFlowPreview
                        flowName={flow.name}
                        questions={flow.questions}
                        viewport={activeViewport}
                      />
                    </div>

                    <h6 className="fw-semibold">Perguntas</h6>
                    <ol className="list-group list-group-numbered">
                      {flow.questions.map((question) => (
                        <li key={question.id} className="list-group-item">
                          <div className="d-flex justify-content-between align-items-start gap-3">
                            <div>
                              <div className="fw-semibold">
                                {question.title}
                              </div>
                              <div className="text-muted small">
                                Campo: {question.dataKey} · Tipo:{" "}
                                {question.type.replace(/_/g, " ")}
                              </div>
                              {question.description ? (
                                <p className="text-muted small mb-0">
                                  {question.description}
                                </p>
                              ) : null}
                              {question.options.length > 0 ? (
                                <ul className="text-muted small mb-0 mt-2 ps-3">
                                  {question.options.map((option) => (
                                    <li key={option}>{option}</li>
                                  ))}
                                </ul>
                              ) : null}
                            </div>
                            <span className="badge text-bg-light text-dark align-self-start">
                              {question.required ? "Obrigatória" : "Opcional"}
                            </span>
                          </div>
                        </li>
                      ))}
                    </ol>
                  </div>

                  <div className="mt-3 d-flex flex-wrap gap-3 text-muted small">
                    {flow.model ? <span>Modelo: {flow.model}</span> : null}
                    {flow.approvedAt ? (
                      <span>Aprovado em: {formatDate(flow.approvedAt)}</span>
                    ) : null}
                    <span>Criado em: {formatDate(flow.createdAt)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="d-flex justify-content-end">
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          onClick={() => handleAssignFlow(null)}
          disabled={assignedFlowId == null || updateExperiment.isPending}
        >
          {updateExperiment.isPending ? (
            <span className="spinner-border spinner-border-sm" role="status" />
          ) : null}
          Remover fluxo do experimento
        </button>
      </div>
    </div>
  );
}

interface LeadFlowPreviewProps {
  flowName: string;
  questions: Array<{
    id: number;
    title: string;
    type: string;
    required: boolean;
    options: string[];
    placeholder?: string | null;
  }>;
  viewport: PreviewViewport;
}

function LeadFlowPreview({
  flowName,
  questions,
  viewport,
}: LeadFlowPreviewProps) {
  const isMobile = viewport === "mobile";
  return (
    <div className="d-flex justify-content-center">
      <div
        className="border rounded-4 bg-white shadow-sm p-3"
        style={{
          width: "100%",
          maxWidth: isMobile ? "380px" : "920px",
        }}
      >
        <div className="border-bottom pb-2 mb-3">
          <h6 className="mb-1">{flowName}</h6>
          <p className="text-muted small mb-0">
            Assim o lead verá o formulário no portal.
          </p>
        </div>
        <form
          className="d-flex flex-column gap-3"
          onSubmit={(event) => event.preventDefault()}
        >
          {questions.map((question) => (
            <div key={question.id}>
              <label className="form-label fw-semibold">
                {question.title}
                {question.required ? " *" : ""}
              </label>
              {renderPreviewField(question)}
            </div>
          ))}
          <button type="button" className="btn btn-primary" disabled>
            Enviar respostas
          </button>
        </form>
      </div>
    </div>
  );
}

function renderPreviewField(
  question: LeadFlowPreviewProps["questions"][number],
) {
  switch (question.type) {
    case "TEXTAREA":
      return (
        <textarea
          className="form-control"
          rows={3}
          placeholder={question.placeholder ?? "Digite sua resposta"}
          disabled
        />
      );
    case "SINGLE_CHOICE":
      return (
        <select className="form-select" disabled defaultValue="">
          <option value="" disabled>
            Selecione uma opção
          </option>
          {question.options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      );
    case "MULTIPLE_CHOICE":
      return (
        <div className="d-flex flex-column gap-2">
          {question.options.map((option) => (
            <div className="form-check" key={option}>
              <input
                className="form-check-input"
                type="checkbox"
                value={option}
                id={`${question.id}-${option}`}
                disabled
              />
              <label
                className="form-check-label"
                htmlFor={`${question.id}-${option}`}
              >
                {option}
              </label>
            </div>
          ))}
        </div>
      );
    case "PHONE":
      return (
        <input
          type="tel"
          className="form-control"
          placeholder={question.placeholder ?? "(11) 99999-9999"}
          disabled
        />
      );
    default:
      return (
        <input
          type="text"
          className="form-control"
          placeholder={question.placeholder ?? "Digite sua resposta"}
          disabled
        />
      );
  }
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
