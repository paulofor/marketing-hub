import { useMemo } from "react";
import { useFunnel } from "../../api/funnel/useFunnel";

interface FunnelPreviewModalProps {
  funnelId: string;
  fallbackName?: string | null;
  onClose: () => void;
}

function formatLabel(value?: string | null) {
  if (!value) return "";
  return value
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(" ");
}

export default function FunnelPreviewModal({
  funnelId,
  fallbackName,
  onClose,
}: FunnelPreviewModalProps) {
  const { data, isLoading } = useFunnel(funnelId);
  const steps = useMemo(() => {
    if (!data?.steps) return [];
    return [...data.steps].sort((a, b) => a.orderIdx - b.orderIdx);
  }, [data?.steps]);

  return (
    <>
      <div className="modal d-block" tabIndex={-1} role="dialog">
        <div className="modal-dialog modal-lg modal-dialog-centered" role="document">
          <div className="modal-content shadow-lg border-0">
            <div className="modal-header border-0 pb-0">
              <div>
                <h5 className="modal-title mb-1">Funil de Vendas</h5>
                <p className="text-muted mb-0 small">
                  {data?.name || fallbackName || "Visualização do funil"}
                </p>
              </div>
              <button
                type="button"
                className="btn-close"
                aria-label="Fechar"
                onClick={onClose}
              />
            </div>
            <div className="modal-body">
              {data?.objective && (
                <p className="text-muted small mb-4">{data.objective}</p>
              )}
              {isLoading ? (
                <p className="text-center text-muted my-5">Carregando funil...</p>
              ) : steps.length === 0 ? (
                <div className="alert alert-info mb-0">
                  Este funil ainda não possui etapas cadastradas.
                </div>
              ) : (
                <ol className="list-unstyled mb-0 d-flex flex-column gap-2">
                  {steps.map((step, index) => {
                    const stimulusLabel = formatLabel(step.stimulusType);
                    return (
                      <li key={step.id} className="card border-0 shadow-sm rounded-3">
                        <div className="card-body p-3">
                          <div className="d-flex flex-wrap justify-content-between gap-3">
                            <div className="d-flex flex-column flex-grow-1 gap-2">
                              <div className="d-flex align-items-center flex-wrap gap-2">
                                <span className="badge rounded-pill text-bg-secondary">
                                  {index + 1}
                                </span>
                                {stimulusLabel && (
                                  <span className="badge bg-primary bg-opacity-10 text-primary fw-semibold text-uppercase small">
                                    {stimulusLabel}
                                  </span>
                                )}
                              </div>
                              <div className="fs-5 fw-semibold text-dark">
                                {formatLabel(step.expectedAction)}
                              </div>
                              {step.note && (
                                <p className="text-muted small mb-0">{step.note}</p>
                              )}
                            </div>
                            {step.scoreInc != null && step.scoreInc !== 0 && (
                              <div className="d-flex align-items-start">
                                <span className="badge bg-success rounded-pill">
                                  +{step.scoreInc}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      </li>
                    );
                  })}
                </ol>
              )}
            </div>
            <div className="modal-footer border-0 pt-0">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show" />
    </>
  );
}
