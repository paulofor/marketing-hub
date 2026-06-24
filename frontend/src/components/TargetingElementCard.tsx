import { useEffect, useMemo, useState } from "react";
import type {
  TargetingElement,
  TargetingElementSource,
  TargetingElementStatus,
} from "../api/targeting/types";
import { useUpdateTargetingElement } from "../api/targeting/useUpdateTargetingElement";

const typeLabels: Record<TargetingElement["type"], string> = {
  INTEREST: "Interesse",
  JOB_TITLE: "Cargo",
  BEHAVIOR: "Comportamento",
};

const statusLabels: Record<TargetingElementStatus, string> = {
  DRAFT: "Rascunho",
  NEEDS_REVIEW: "Precisa de revisão",
  APPROVED: "Aprovado",
  REJECTED: "Rejeitado",
};

const statusClasses: Record<TargetingElementStatus, string> = {
  DRAFT:
    "bg-secondary-subtle text-secondary-emphasis border border-secondary-subtle",
  NEEDS_REVIEW:
    "bg-warning-subtle text-warning-emphasis border border-warning-subtle",
  APPROVED:
    "bg-success-subtle text-success-emphasis border border-success-subtle",
  REJECTED: "bg-danger-subtle text-danger-emphasis border border-danger-subtle",
};

const sourceLabels: Record<TargetingElementSource, string> = {
  AI: "IA",
  MANUAL: "Manual",
};

interface TargetingElementCardProps {
  element: TargetingElement;
  badgeLabel?: string;
  className?: string;
}

type FormState = {
  term: string;
  status: TargetingElementStatus;
  source: TargetingElementSource | "";
  description: string;
  prompt: string;
  model: string;
  notes: string;
  lastReviewedBy: string;
  metaId: string;
  metaKey: string;
  confidence: string;
};

const audienceFormatter = new Intl.NumberFormat("pt-BR");

function formatAudienceRange(lower?: number | null, upper?: number | null) {
  if (typeof lower === "number" && typeof upper === "number") {
    if (lower === upper) return audienceFormatter.format(lower);
    return `${audienceFormatter.format(lower)} – ${audienceFormatter.format(upper)}`;
  }
  if (typeof lower === "number") {
    return `≥ ${audienceFormatter.format(lower)}`;
  }
  if (typeof upper === "number") {
    return `≤ ${audienceFormatter.format(upper)}`;
  }
  return "—";
}

function buildFormState(element: TargetingElement): FormState {
  return {
    term: element.term,
    status: element.status,
    source: element.source ?? "",
    description: element.description ?? "",
    prompt: element.prompt ?? "",
    model: element.model ?? "",
    notes: element.notes ?? "",
    lastReviewedBy: element.lastReviewedBy ?? "",
    metaId: element.metaId ?? "",
    metaKey: element.metaKey ?? "",
    confidence:
      typeof element.confidence === "number" ? String(element.confidence) : "",
  };
}

export function TargetingElementCard({
  element,
  badgeLabel,
  className,
}: TargetingElementCardProps) {
  const [showModal, setShowModal] = useState(false);
  const [formState, setFormState] = useState<FormState>(() =>
    buildFormState(element),
  );
  const nicheId = useMemo(
    () => String(element.marketNicheId ?? ""),
    [element.marketNicheId],
  );
  const updateElement = useUpdateTargetingElement(nicheId);
  const audienceRangeLabel = formatAudienceRange(
    element.metaAudienceSizeLowerBound,
    element.metaAudienceSizeUpperBound,
  );

  useEffect(() => {
    if (!showModal) return;
    setFormState(buildFormState(element));
  }, [element, showModal]);

  const handleStatusUpdate = (status: TargetingElementStatus) => {
    updateElement.mutate({ id: element.id, status });
  };

  const handleSave = () => {
    const payload = {
      id: element.id,
      term: formState.term.trim() || element.term,
      status: formState.status,
      source: formState.source || null,
      description: formState.description.trim() || null,
      prompt: formState.prompt.trim() || null,
      model: formState.model.trim() || null,
      notes: formState.notes.trim() || null,
      lastReviewedBy: formState.lastReviewedBy.trim() || null,
      metaId: formState.metaId.trim() || null,
      metaKey: formState.metaKey.trim() || null,
      confidence:
        formState.confidence.trim() === ""
          ? null
          : Number(formState.confidence),
    };
    if (payload.confidence != null && Number.isNaN(payload.confidence)) {
      payload.confidence = null;
    }
    updateElement.mutate(payload, {
      onSuccess: () => setShowModal(false),
    });
  };

  const isMutating = updateElement.isPending;
  const hasOfficialMetaId = Boolean(element.metaId?.trim());
  const canApprove = element.status === "APPROVED" || hasOfficialMetaId;
  const approvalHelpText = hasOfficialMetaId
    ? "ID oficial da Meta encontrado. Pode aprovar para uso em experimento."
    : "Ainda sem ID oficial da Meta. Revise ou reprocese antes de aprovar.";

  const createdAtLabel = element.createdAt
    ? new Date(element.createdAt).toLocaleString("pt-BR")
    : null;
  const updatedAtLabel = element.updatedAt
    ? new Date(element.updatedAt).toLocaleString("pt-BR")
    : null;

  return (
    <>
      <div className={`card h-100 rounded-3 ${className ?? ""}`}>
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex justify-content-between align-items-start gap-3">
            <div>
              <p className="text-uppercase text-muted small mb-1">
                {typeLabels[element.type]}
              </p>
              <h5 className="mb-1">{element.term}</h5>
              {badgeLabel ? (
                <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">
                  {badgeLabel}
                </span>
              ) : null}
            </div>
            <span className={`badge ${statusClasses[element.status]}`}>
              {statusLabels[element.status]}
            </span>
          </div>
          <p
            className="text-body-secondary mb-0"
            style={{ whiteSpace: "pre-wrap" }}
          >
            {element.description?.trim() || "Sem descrição cadastrada."}
          </p>
          {element.status !== "APPROVED" ? (
            <div
              className={`alert ${
                hasOfficialMetaId ? "alert-success" : "alert-warning"
              } py-2 mb-0 small`}
              role="status"
            >
              {approvalHelpText}
            </div>
          ) : null}
          <dl className="row small mb-0">
            <dt className="col-sm-5">Modelo</dt>
            <dd className="col-sm-7">{element.model ?? "—"}</dd>
            <dt className="col-sm-5">Origem</dt>
            <dd className="col-sm-7">
              {element.source ? sourceLabels[element.source] : "Não definido"}
            </dd>
            <dt className="col-sm-5">Meta ID</dt>
            <dd className="col-sm-7">{element.metaId ?? "—"}</dd>
            <dt className="col-sm-5">Meta Key</dt>
            <dd className="col-sm-7">{element.metaKey ?? "—"}</dd>
            <dt className="col-sm-5">Confiança</dt>
            <dd className="col-sm-7">
              {element.confidence != null ? element.confidence : "—"}
            </dd>
          </dl>
          {element.notes ? (
            <div className="alert alert-light border d-flex flex-column gap-2 mb-0">
              <strong>Notas do revisor</strong>
              <span style={{ whiteSpace: "pre-wrap" }}>{element.notes}</span>
            </div>
          ) : null}
          <div className="mt-auto d-flex flex-wrap gap-2 pt-2 border-top">
            <button
              type="button"
              className={`btn btn-sm ${
                element.status === "APPROVED"
                  ? "btn-outline-secondary"
                  : "btn-outline-success"
              }`}
              onClick={() =>
                handleStatusUpdate(
                  element.status === "APPROVED" ? "NEEDS_REVIEW" : "APPROVED",
                )
              }
              disabled={isMutating || !canApprove}
            >
              {isMutating && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
              )}
              {element.status === "APPROVED" ? "Revogar aprovação" : "Aprovar"}
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => setShowModal(true)}
            >
              Editar detalhes
            </button>
          </div>
        </div>
      </div>

      {showModal ? (
        <div
          className="modal d-block"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog modal-lg modal-dialog-scrollable">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  Editar elemento · {typeLabels[element.type]}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowModal(false)}
                  aria-label="Fechar"
                />
              </div>
              <div className="modal-body">
                <div className="row g-3">
                  <div className="col-12">
                    <label className="form-label">Termo</label>
                    <input
                      className="form-control"
                      value={formState.term}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          term: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Status</label>
                    <select
                      className="form-select"
                      value={formState.status}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          status: event.target.value as TargetingElementStatus,
                        }))
                      }
                    >
                      {Object.entries(statusLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Origem</label>
                    <select
                      className="form-select"
                      value={formState.source}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          source: event.target.value as
                            | TargetingElementSource
                            | "",
                        }))
                      }
                    >
                      <option value="">Não informado</option>
                      {Object.entries(sourceLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Modelo</label>
                    <input
                      className="form-control"
                      value={formState.model}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          model: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Confiança</label>
                    <input
                      className="form-control"
                      type="number"
                      step="0.01"
                      value={formState.confidence}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          confidence: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Descrição</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={formState.description}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          description: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Prompt</label>
                    <textarea
                      className="form-control font-monospace"
                      rows={4}
                      value={formState.prompt}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          prompt: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Notas do revisor</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={formState.notes}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          notes: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">
                      Responsável pela revisão
                    </label>
                    <input
                      className="form-control"
                      value={formState.lastReviewedBy}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          lastReviewedBy: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Meta ID</label>
                    <input
                      className="form-control"
                      value={formState.metaId}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          metaId: event.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label">Meta Key</label>
                    <input
                      className="form-control"
                      value={formState.metaKey}
                      onChange={(event) =>
                        setFormState((prev) => ({
                          ...prev,
                          metaKey: event.target.value,
                        }))
                      }
                    />
                  </div>
                </div>
                <div className="mt-4 small text-body-secondary">
                  <p className="mb-1">
                    <strong>ID interno:</strong> {element.id}
                  </p>
                  {createdAtLabel ? (
                    <p className="mb-1">Criado em {createdAtLabel}</p>
                  ) : null}
                  {updatedAtLabel ? (
                    <p className="mb-0">Atualizado em {updatedAtLabel}</p>
                  ) : null}
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowModal(false)}
                  disabled={isMutating}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSave}
                  disabled={isMutating}
                >
                  {isMutating && (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                  )}
                  Salvar alterações
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
