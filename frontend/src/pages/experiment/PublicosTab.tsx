import type { Audience } from "../../api/audience/useAudiencesByNiche";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";

interface PublicosTabProps {
  nicheId?: number;
  hypothesisId?: string;
  nicheName?: string | null;
  hypothesisTitle?: string | null;
}

export default function PublicosTab({
  nicheId,
  hypothesisId,
  nicheName,
  hypothesisTitle,
}: PublicosTabProps) {
  const nicheIdAsString = nicheId != null ? String(nicheId) : undefined;
  const {
    data,
    isLoading,
    isFetching,
    isError,
  } = useAudiencesByNiche(nicheIdAsString);

  if (nicheIdAsString == null || !hypothesisId) {
    return (
      <div className="mt-3">
        <p className="text-muted">
          Este experimento não possui nicho ou hipótese associados para exibir públicos.
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="mt-3">
        <p>Carregando públicos...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mt-3">
        <p className="text-danger">Não foi possível carregar os públicos relacionados.</p>
      </div>
    );
  }

  const list = Array.isArray(data) ? data : [];
  const relatedToHypothesis = list.filter((a) => a.hypothesisId === hypothesisId);
  const relatedToNiche = list.filter((a) => a.hypothesisId !== hypothesisId);
  const updating = isFetching && !isLoading;

  const hypothesisTitleSuffix = hypothesisTitle
    ? ` “${hypothesisTitle}”`
    : "";
  const nicheTitleSuffix = nicheName ? ` “${nicheName}”` : "";

  return (
    <div className="mt-3">
      {updating && (
        <p className="text-muted small">Atualizando públicos...</p>
      )}
      <AudienceSection
        title={`Públicos relacionados à hipótese${hypothesisTitleSuffix}`}
        audiences={relatedToHypothesis}
        emptyMessage="Nenhum público relacionado diretamente a esta hipótese."
        badgeLabel="Hipótese"
      />
      <AudienceSection
        title={`Públicos disponíveis no nicho${nicheTitleSuffix}`}
        audiences={relatedToNiche}
        emptyMessage="Nenhum outro público cadastrado para este nicho."
        badgeLabel="Nicho"
      />
    </div>
  );
}

function AudienceSection({
  title,
  audiences,
  emptyMessage,
  badgeLabel,
}: {
  title: string;
  audiences: Audience[];
  emptyMessage: string;
  badgeLabel: string;
}) {
  return (
    <section className="mb-4">
      <div className="d-flex align-items-center mb-3">
        <h5 className="mb-0">{title}</h5>
        <span className="badge bg-secondary ms-2">{audiences.length}</span>
      </div>
      {audiences.length === 0 ? (
        <p className="text-muted">{emptyMessage}</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-4">
          {audiences.map((audience) => (
            <div key={audience.id} className="col">
              <AudienceCard audience={audience} badgeLabel={badgeLabel} />
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function AudienceCard({
  audience,
  badgeLabel,
}: {
  audience: Audience;
  badgeLabel: string;
}) {
  return (
    <div className="card h-100 rounded-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start">
          <h5 className="card-title mb-0">{audience.name}</h5>
          <span className="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">
            {badgeLabel}
          </span>
        </div>
        <p
          className="card-text mt-2"
          style={{ whiteSpace: "pre-wrap" }}
        >
          {audience.description || "—"}
        </p>
        {audience.model && (
          <p className="card-text">
            <small className="text-muted">
              Gerado pelo modelo {audience.model}
            </small>
          </p>
        )}
      </div>
    </div>
  );
}
