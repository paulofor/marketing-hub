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
  const toggleApproval = () => {
    approval.mutate({ id: audience.id, approved: !audience.approved });
  };
  return (
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
            <small className="text-muted">Gerado pelo modelo {audience.model}</small>
          </p>
        )}
        <div className="d-flex justify-content-end">
          <button
            type="button"
            className={`btn btn-sm ${
              audience.approved ? "btn-outline-secondary" : "btn-outline-success"
            }`}
            onClick={toggleApproval}
            disabled={approval.isPending}
          >
            {audience.approved ? "Revogar aprovação" : "Aprovar"}
          </button>
        </div>
      </div>
    </div>
  );
}
