import { useEffect, useState } from "react";

import { useAudienceDetails } from "../api/audience/useAudienceDetails";
import { useReprocessAudienceTargetingSeeds } from "../api/audience/useReprocessAudienceTargetingSeeds";
import { useUpdateAudienceTargeting } from "../api/audience/useUpdateAudienceTargeting";
import type { Audience } from "../api/audience/useAudiencesByNiche";
import { useUpdateAudienceApproval } from "../api/audience/useUpdateAudienceApproval";

interface AudienceApprovalCardProps {
  audience: Audience;
  nicheId: string | undefined;
  badgeLabel?: string;
  className?: string;
}

export function AudienceApprovalCard({
  audience,
  nicheId,
  badgeLabel,
  className,
}: AudienceApprovalCardProps) {
  const approval = useUpdateAudienceApproval(nicheId);
  const updateTargeting = useUpdateAudienceTargeting(nicheId);
  const reprocessSeeds = useReprocessAudienceTargetingSeeds(nicheId);
  const [showTargeting, setShowTargeting] = useState(false);
  const { data: audienceDetails, isLoading: isLoadingTargeting } =
    useAudienceDetails(audience.id, true, showTargeting);
  const [targetingSpec, setTargetingSpec] = useState("");
  const [targetingStatus, setTargetingStatus] = useState<string>("");
  const [targetingNotes, setTargetingNotes] = useState("");

  const targetingSeeds = audienceDetails?.seeds ?? [];

  useEffect(() => {
    if (!showTargeting || !audienceDetails) return;
    setTargetingSpec(formatTargetingSpec(audienceDetails.targetingSpec));
    setTargetingStatus(audienceDetails.targetingStatus ?? "DRAFT");
    setTargetingNotes(audienceDetails.targetingNotes ?? "");
  }, [audienceDetails, showTargeting]);

  const toggleApproval = () => {
    approval.mutate({ id: audience.id, approved: !audience.approved });
  };

  const handleSaveTargeting = () => {
    const trimmedSpec = targetingSpec.trim();
    updateTargeting.mutate({
      id: audience.id,
      targetingSpec: trimmedSpec.length > 0 ? trimmedSpec : undefined,
      status: targetingStatus || undefined,
      notes: targetingNotes.trim().length > 0 ? targetingNotes.trim() : undefined,
    });
  };

  const handleReprocess = () => {
    reprocessSeeds.mutate({ id: audience.id });
  };

  const isSavingTargeting = updateTargeting.isPending;
  const isReprocessing = reprocessSeeds.isPending;
  const targetingLoading = isLoadingTargeting && showTargeting;
  const hasTargeting = Boolean(audienceDetails?.targetingSpec);
  const targetingStatusLabel = audienceDetails?.targetingStatus ?? "DRAFT";
  const targetingNotesLabel = audienceDetails?.targetingNotes || "—";

  return (
    <>
      <div className={`card h-100 rounded-3 ${className ?? ""}`}>
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <h5 className="card-title mb-0">{audience.name}</h5>
            {badgeLabel && (
              <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">
                {badgeLabel}
              </span>
            )}
          </div>
          <div className="d-flex align-items-center mt-2">
            <span
              className={`badge ${
                audience.approved
                  ? "bg-success-subtle text-success-emphasis border border-success-subtle"
                  : "bg-warning-subtle text-warning-emphasis border border-warning-subtle"
              }`}
            >
              {audience.approved ? "Aprovado" : "Pendente"}
            </span>
            {approval.isPending && (
              <span className="ms-2 text-muted small">Atualizando...</span>
            )}
          </div>
          <p className="card-text mt-2" style={{ whiteSpace: "pre-wrap" }}>
            {audience.description || "—"}
          </p>
          {audience.model && (
            <p className="card-text">
              <small className="text-muted">
                Gerado pelo modelo {audience.model}
              </small>
            </p>
          )}
          <div className="d-flex justify-content-between align-items-center mt-3">
            <button
              type="button"
              className="btn btn-sm btn-outline-primary"
              onClick={() => setShowTargeting(true)}
            >
              Ver targeting
            </button>
            <button
              type="button"
              className={`btn btn-sm ${
                audience.approved
                  ? "btn-outline-secondary"
                  : "btn-outline-success"
              }`}
              onClick={toggleApproval}
              disabled={approval.isPending}
            >
              {approval.isPending && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
              )}
              {audience.approved ? "Revogar aprovação" : "Aprovar"}
            </button>
          </div>
        </div>
      </div>

      {showTargeting && (
        <div className="modal d-block" tabIndex={-1} role="dialog" aria-modal="true">
          <div className="modal-dialog modal-lg modal-dialog-scrollable">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  Targeting • {audience.name}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={() => setShowTargeting(false)}
                />
              </div>
              <div className="modal-body">
                {targetingLoading ? (
                  <p className="text-muted">Carregando targeting...</p>
                ) : (
                  <>
                    <div className="row g-3">
                      <div className="col-12 col-lg-6">
                        <label className="form-label">Status</label>
                        <select
                          className="form-select"
                          value={targetingStatus}
                          onChange={(event) =>
                            setTargetingStatus(event.target.value)
                          }
                        >
                          <option value="DRAFT">Rascunho</option>
                          <option value="NEEDS_REVIEW">Precisa de revisão</option>
                          <option value="READY">Pronto</option>
                        </select>
                        <small className="text-muted d-block mt-1">
                          Status atual: {targetingStatusLabel}
                        </small>
                      </div>
                      <div className="col-12 col-lg-6">
                        <label className="form-label">Notas do targeting</label>
                        <textarea
                          className="form-control"
                          rows={3}
                          value={targetingNotes}
                          onChange={(event) =>
                            setTargetingNotes(event.target.value)
                          }
                        />
                        <small className="text-muted d-block mt-1">
                          Últimas notas registradas: {targetingNotesLabel}
                        </small>
                      </div>
                    </div>

                    <div className="mt-4">
                      <label className="form-label">Targeting Spec</label>
                      <textarea
                        className="form-control font-monospace"
                        rows={8}
                        value={targetingSpec}
                        onChange={(event) =>
                          setTargetingSpec(event.target.value)
                        }
                        placeholder="Cole ou revise o JSON de targeting estruturado."
                      />
                      {!hasTargeting && (
                        <small className="text-muted d-block mt-1">
                          Ainda não há targeting_spec para este público.
                        </small>
                      )}
                    </div>

                    <div className="mt-4">
                      <div className="d-flex align-items-center justify-content-between mb-2">
                        <h6 className="mb-0">Seeds utilizados</h6>
                        <span className="badge bg-secondary">
                          {targetingSeeds.length}
                        </span>
                      </div>
                      {targetingSeeds.length === 0 ? (
                        <p className="text-muted">Nenhum seed registrado.</p>
                      ) : (
                        <div className="table-responsive">
                          <table className="table table-sm align-middle">
                            <thead>
                              <tr>
                                <th>Tipo</th>
                                <th>Valor</th>
                                <th>Status</th>
                                <th>Confiança</th>
                              </tr>
                            </thead>
                            <tbody>
                              {targetingSeeds.map((seed) => (
                                <tr key={seed.id}>
                                  <td>{seed.type}</td>
                                  <td>
                                    <div className="fw-semibold">
                                      {seed.value}
                                    </div>
                                    {(seed.key || seed.metaId) && (
                                      <small className="text-muted">
                                        {seed.key ?? seed.metaId}
                                      </small>
                                    )}
                                  </td>
                                  <td>{seed.status ?? "—"}</td>
                                  <td>
                                    {seed.confidence != null
                                      ? `${seed.confidence}`
                                      : "—"}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowTargeting(false)}
                >
                  Fechar
                </button>
                <button
                  type="button"
                  className="btn btn-outline-warning"
                  onClick={handleReprocess}
                  disabled={isReprocessing}
                  title="Reprocessa os seeds no Worker IA para gerar um novo targeting."
                >
                  {isReprocessing && (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                  )}
                  Reprocessar seeds
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSaveTargeting}
                  disabled={isSavingTargeting}
                >
                  {isSavingTargeting && (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                  )}
                  Salvar revisão
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function formatTargetingSpec(spec?: string | null) {
  if (!spec) return "";
  try {
    return JSON.stringify(JSON.parse(spec), null, 2);
  } catch {
    return spec;
  }
}
