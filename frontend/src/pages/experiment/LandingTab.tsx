import { useMemo, useState } from "react";
import axios from "axios";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import { useLandingPages } from "../../api/landing/useLandingPages";

interface LandingTabProps {
  experiment: Experiment;
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

export default function LandingTab({ experiment }: LandingTabProps) {
  const { data: landingPages, isLoading, isError } = useLandingPages(experiment.id);
  const updateExperiment = useUpdateExperiment(experiment.id);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [pendingLandingId, setPendingLandingId] = useState<number | null>(null);
  const [isPublishing, setIsPublishing] = useState(false);

  const sortedLandingPages = useMemo(() => {
    if (!Array.isArray(landingPages)) {
      return [];
    }

    return [...landingPages].sort((a, b) => b.id - a.id);
  }, [landingPages]);

  const selectedDestinationUrl = normalizeUrl(experiment.followUpActionUrl);
  const hasGeneratedLandingHtml = Boolean(experiment.landingPageHtml?.trim());

  const handleApproveAndPublish = async () => {
    if (!hasGeneratedLandingHtml) {
      setFeedback({
        variant: "error",
        message: "Gere o HTML da landing na aba Estrutura de conteúdo antes de aprovar e publicar.",
      });
      return;
    }

    setFeedback(null);
    setIsPublishing(true);
    try {
      const { data } = await axios.post<{
        publicUrl?: string | null;
        facebookPixelId?: string | null;
        pixelAppliedAutomatically?: boolean;
      }>(`/api/experiments/${experiment.id}/pipeline/landing-page-html/approve-and-publish`);

      const pixelFeedback =
        data?.pixelAppliedAutomatically && data.facebookPixelId
          ? ` Pixel do nicho aplicado automaticamente (${data.facebookPixelId}).`
          : " Pixel do nicho será aplicado automaticamente ao ficar disponível.";
      setFeedback({
        variant: "success",
        message: data?.publicUrl
          ? `Landing aprovada e publicada automaticamente em ${data.publicUrl}.${pixelFeedback}`
          : `Landing aprovada e publicação automática iniciada.${pixelFeedback}`,
      });
    } catch {
      setFeedback({
        variant: "error",
        message: "Não foi possível aprovar/publicar a landing agora. Tente novamente em instantes.",
      });
    } finally {
      setIsPublishing(false);
    }
  };

  const handleApproveLanding = async (landingId: number, landingUrl: string) => {
    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || experiment.metricPresetId == null) {
      setFeedback({
        variant: "error",
        message:
          "Defina a meta de KPI e o preset de métricas antes de aprovar a landing como destino da campanha.",
      });
      return;
    }

    const destinationUrl = resolveStandaloneLandingUrl(landingUrl);
    setPendingLandingId(landingId);
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
        leadPortalFlowsToGenerate: experiment.leadPortalFlowsToGenerate ?? undefined,
        journeyTemplateId: experiment.journeyTemplateId ?? undefined,
        facebookPageId: experiment.facebookPage?.id ?? null,
        facebookInstantFormId: experiment.facebookInstantForm?.id ?? null,
        instagramAccountId: experiment.instagramAccount?.id ?? null,
        leadPortalFlowId: experiment.leadPortalFlowId ?? null,
        followUpActionUrl: destinationUrl,
      });

      setFeedback({
        variant: "success",
        message: "Landing aprovada e definida como URL de destino da campanha.",
      });
    } catch {
      setFeedback({
        variant: "error",
        message:
          "Não foi possível aprovar esta landing agora. Tente novamente em instantes.",
      });
    } finally {
      setPendingLandingId(null);
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <h5 className="card-title mb-1">Landing do experimento</h5>
          <p className="text-muted mb-0">
            Aprove a landing que deve ser usada como URL de destino da campanha.
          </p>
          <div className="mt-3">
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleApproveAndPublish}
              disabled={isPublishing || !hasGeneratedLandingHtml}
            >
              {isPublishing ? (
                <span className="d-inline-flex align-items-center gap-2">
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                  Publicando...
                </span>
              ) : (
                "Aprovar e publicar landing"
              )}
            </button>
          </div>
          {!hasGeneratedLandingHtml ? (
            <p className="text-muted small mt-2 mb-0">
              Gere o HTML na aba Estrutura de conteúdo para habilitar a aprovação nesta aba.
            </p>
          ) : null}
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

      {isLoading ? (
        <p className="text-muted">Carregando landings do experimento...</p>
      ) : isError ? (
        <p className="text-danger">Não foi possível carregar as landings deste experimento.</p>
      ) : sortedLandingPages.length === 0 ? (
        <p className="text-muted mb-0">
          Nenhuma landing gerada ainda. Gere o HTML na aba Estrutura de conteúdo para publicar aqui.
        </p>
      ) : (
        <div className="d-flex flex-column gap-3">
          {sortedLandingPages.map((landing) => {
            const standaloneUrl = resolveStandaloneLandingUrl(landing.url);
            const isSelected = normalizeUrl(standaloneUrl) === selectedDestinationUrl;
            const isApproving =
              pendingLandingId === landing.id ||
              (updateExperiment.isPending && pendingLandingId === landing.id);

            return (
              <div key={landing.id} className="card border-0 shadow-sm">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                    <div>
                      <h6 className="mb-1 d-flex align-items-center gap-2">
                        Landing #{landing.id}
                        {isSelected ? (
                          <span className="badge text-bg-success">Destino ativo</span>
                        ) : null}
                      </h6>
                      <p className="text-muted small mb-1">Tipo: {landing.type}</p>
                      <p className="text-muted small mb-0">Status: {landing.status}</p>
                    </div>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => handleApproveLanding(landing.id, landing.url)}
                      disabled={isApproving || updateExperiment.isPending}
                    >
                      {isApproving ? (
                        <span className="spinner-border spinner-border-sm" role="status" />
                      ) : null}
                      {isApproving
                        ? "Aprovando..."
                        : isSelected
                          ? "Landing aprovada"
                          : "Aprovar landing"}
                    </button>
                  </div>

                  <div>
                    <p className="text-muted small mb-1">URL standalone</p>
                    <a
                      href={standaloneUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="small text-break"
                    >
                      {standaloneUrl}
                    </a>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
