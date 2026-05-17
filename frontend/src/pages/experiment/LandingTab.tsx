import { useMemo, useState } from "react";
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

function resolveLandingTypeOrder(type: string): number {
  const normalizedType = type.trim().toUpperCase();
  if (normalizedType === "LEAD") {
    return 1;
  }
  if (normalizedType === "PRESALE") {
    return 2;
  }
  return 99;
}

export default function LandingTab({ experiment }: LandingTabProps) {
  const { data: landingPages, isLoading, isError } = useLandingPages(experiment.id);
  const updateExperiment = useUpdateExperiment(experiment.id);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedLandingId, setSelectedLandingId] = useState<number | null>(null);

  const sortedLandingPages = useMemo(() => {
    if (!Array.isArray(landingPages)) {
      return [];
    }

    return [...landingPages].sort((a, b) => {
      const typeDiff = resolveLandingTypeOrder(a.type) - resolveLandingTypeOrder(b.type);
      if (typeDiff !== 0) {
        return typeDiff;
      }
      return a.id - b.id;
    });
  }, [landingPages]);

  const selectedDestinationUrl = normalizeUrl(experiment.followUpActionUrl);
  const resolvedSelectedLanding = useMemo(
    () => sortedLandingPages.find((landing) => landing.id === selectedLandingId) ?? null,
    [selectedLandingId, sortedLandingPages],
  );

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
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card border-0 shadow-sm">
        <div className="card-body">
          <h5 className="card-title mb-1">Landing do experimento</h5>
          <p className="text-muted mb-0">Selecione uma landing e aprove no botão final da aba.</p>
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
                    <div className="form-check">
                      <input
                        id={`landing-choice-${landing.id}`}
                        className="form-check-input"
                        type="radio"
                        name="landing-choice"
                        checked={selectedLandingId === landing.id}
                        onChange={() => setSelectedLandingId(landing.id)}
                      />
                      <label className="form-check-label small" htmlFor={`landing-choice-${landing.id}`}>
                        Selecionar para campanha
                      </label>
                    </div>
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

      <div className="d-flex justify-content-end">
        <button
          type="button"
          className="btn btn-success"
          onClick={() =>
            resolvedSelectedLanding
              ? handleApproveLanding(resolvedSelectedLanding.id, resolvedSelectedLanding.url)
              : undefined
          }
          disabled={updateExperiment.isPending || !resolvedSelectedLanding}
        >
          {updateExperiment.isPending ? (
            <span className="d-inline-flex align-items-center gap-2">
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
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
