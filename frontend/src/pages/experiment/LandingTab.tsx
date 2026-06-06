import { useMemo, useState } from "react";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useApproveAndPublishLanding } from "../../api/experiment/useApproveAndPublishLanding";

interface LandingTabProps {
  experiment: Experiment;
  alterationLocked?: boolean;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

function resolveStandaloneLandingUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  if (typeof window !== "undefined" && window.location?.origin) {
    return `${window.location.origin}${normalizedPath}`;
  }
  return normalizedPath;
}

function normalizeUrl(url?: string | null) {
  return (url ?? "").trim().replace(/\/$/, "");
}

export function resolveLandingHtml(
  experiment: Pick<Experiment, "htmlGeraLanding" | "landingPageHtml">,
) {
  const raw = experiment.htmlGeraLanding ?? experiment.landingPageHtml;
  return typeof raw === "string" && raw.trim().length > 0 ? raw : null;
}

export function canAttemptLandingApproval(experiment: Pick<Experiment, "id">) {
  return String(experiment.id ?? "").trim().length > 0;
}

export default function LandingTab({
  experiment,
  alterationLocked = false,
}: LandingTabProps) {
  const approveAndPublishLanding = useApproveAndPublishLanding(
    Number(experiment.id),
  );
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const landingHtml = useMemo(
    () => resolveLandingHtml(experiment),
    [experiment.htmlGeraLanding, experiment.landingPageHtml],
  );

  const [publishedUrls, setPublishedUrls] = useState<{
    iframeUrl: string | null;
    standaloneUrl: string | null;
  } | null>(null);
  const canApproveLanding =
    !alterationLocked && canAttemptLandingApproval(experiment);
  const selectedDestinationUrl = normalizeUrl(
    publishedUrls?.standaloneUrl ?? experiment.followUpActionUrl,
  );
  const campaignDestinationUrl = resolveStandaloneLandingUrl(
    `/landing/${experiment.id}`,
  );

  const handleApproveLanding = async () => {
    setFeedback(null);

    try {
      const publication = await approveAndPublishLanding.mutateAsync();
      const primaryVariant = publication.variantLinks?.[0] ?? null;
      setPublishedUrls({
        iframeUrl:
          primaryVariant?.iframeUrl ??
          publication.iframeUrl ??
          publication.publicUrl ??
          null,
        standaloneUrl:
          primaryVariant?.standaloneUrl ?? publication.standaloneUrl ?? null,
      });
      setFeedback({
        variant: "success",
        message:
          publication.message || "Landing aprovada e publicada no Lead Portal.",
      });
    } catch {
      setFeedback({
        variant: "error",
        message:
          "Não foi possível aprovar esta landing agora. Tente novamente em instantes.",
      });
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <h5 className="card-title mb-1">Landing do experimento</h5>
          <p className="text-muted mb-0">
            Pré-visualização do HTML salvo no experimento.
          </p>
        </div>
      </div>

      {feedback ? (
        <div
          className={`alert ${feedback.variant === "success" ? "alert-success" : "alert-danger"}`}
          role="alert"
        >
          {feedback.message}
        </div>
      ) : null}

      {!landingHtml ? (
        <div className="alert alert-warning mb-0" role="alert">
          A prévia não carregou o HTML no navegador, mas a aprovação consulta o
          registro atualizado no backend. Se o HTML já existir em
          <code>html_geralanding</code>, use o botão para publicar a landing.
        </div>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex flex-wrap align-items-center gap-2">
              <span className="fw-semibold">Destino atual:</span>
              {selectedDestinationUrl ? (
                <span className="badge text-bg-success">
                  URL de campanha definida
                </span>
              ) : (
                <span className="badge text-bg-secondary">
                  Sem URL aprovada
                </span>
              )}
            </div>
            <div className="small text-body-secondary">
              <div>
                <strong>URL usada na campanha:</strong>{" "}
                <a
                  href={campaignDestinationUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  {campaignDestinationUrl}
                </a>
              </div>
              {publishedUrls?.iframeUrl ? (
                <div>
                  <strong>URL do iframe (Lead Portal):</strong>{" "}
                  <a
                    href={publishedUrls.iframeUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {publishedUrls.iframeUrl}
                  </a>
                </div>
              ) : null}
              {selectedDestinationUrl ? (
                <div>
                  <strong>URL standalone (usar na campanha):</strong>{" "}
                  <a
                    href={selectedDestinationUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {selectedDestinationUrl}
                  </a>
                </div>
              ) : null}
            </div>
            <iframe
              title="Prévia da landing do experimento"
              srcDoc={landingHtml}
              style={{
                width: "100%",
                minHeight: 560,
                border: "1px solid #dee2e6",
                borderRadius: 8,
              }}
            />
          </div>
        </div>
      )}

      {alterationLocked ? (
        <div className="alert alert-secondary mb-0" role="status">
          Landing bloqueada para alteração porque o experimento já foi liberado
          ou está em execução.
        </div>
      ) : null}

      <div className="d-flex justify-content-end">
        <button
          type="button"
          className="btn btn-success"
          onClick={() => handleApproveLanding()}
          disabled={approveAndPublishLanding.isPending || !canApproveLanding}
        >
          {approveAndPublishLanding.isPending ? (
            <span className="d-inline-flex align-items-center gap-2">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Aprovando...
            </span>
          ) : (
            "Aprovar landing para campanha"
          )}
        </button>
      </div>
    </div>
  );
}
