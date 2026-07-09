import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import type { Experiment } from "../../api/experiment/useExperiments";
import {
  ExperimentVideoSlot,
  SalesVideoExecutionMode,
  useExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { useRequestExperimentVeoVideo } from "../../api/experiment/useRequestExperimentVeoVideo";
import { useTenantContext } from "../../utils/tenantContext";

interface ExperimentVideoTabProps {
  experiment: Experiment;
  alterationLocked?: boolean;
}

const VIDEO_SLOT_OPTIONS: ExperimentVideoSlot[] = [
  "AD",
  "LANDING_HERO",
  "FORM_EXPLAINER",
  "PRE_CHECKOUT",
];

const EXECUTION_MODE_OPTIONS: SalesVideoExecutionMode[] = ["TEST", "PRODUCTION"];

function buildInitialScript(experiment: Experiment) {
  const sections = [
    experiment.funnelPromise ? `Promessa: ${experiment.funnelPromise}` : null,
    experiment.singlePain ? `Dor: ${experiment.singlePain}` : null,
    experiment.primaryCta ? `CTA: ${experiment.primaryCta}` : null,
    experiment.adCopy ? `Copy do anúncio:\n${experiment.adCopy}` : null,
    experiment.landingPageCopy ? `Copy da página:\n${experiment.landingPageCopy}` : null,
  ].filter(Boolean);
  return sections.join("\n\n").slice(0, 6000);
}

function parseOptionalNumber(value: string) {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function ExperimentVideoTab({
  experiment,
  alterationLocked = false,
}: ExperimentVideoTabProps) {
  const tenantContext = useTenantContext();
  const { data: videoAssets, isLoading } = useExperimentVideoAssets(experiment.id);
  const requestVeoVideo = useRequestExperimentVeoVideo(experiment.id);
  const [formState, setFormState] = useState({
    slot: "LANDING_HERO" as ExperimentVideoSlot,
    title: `Vídeo VEO - Experimento ${experiment.id}`,
    objective: "Aumentar conversão da página de venda e destravar campanha com vídeo obrigatório.",
    primaryMetric: "CTR, tempo na página e conversão para checkout/lead.",
    personaName: "",
    personaStyle: "consultiva, direta e comercial",
    voiceStyle: "natural, confiante e com urgência moderada",
    language: "pt-BR",
    targetDurationSeconds: "30",
    scriptText: buildInitialScript(experiment),
    hookText: experiment.funnelPromise ?? "",
    ctaText: experiment.primaryCta ?? "Quero acessar agora",
    captionText: "",
    providerName: "VEO",
    executionMode: "TEST" as SalesVideoExecutionMode,
    requiredForRelease: true,
  });

  const sortedAssets = useMemo(() => videoAssets ?? [], [videoAssets]);
  const canSubmit =
    !alterationLocked &&
    formState.title.trim().length > 0 &&
    formState.objective.trim().length > 0 &&
    formState.primaryMetric.trim().length > 0 &&
    formState.scriptText.trim().length > 0 &&
    tenantContext.userEmail.trim().length > 0 &&
    !requestVeoVideo.isPending;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const targetDurationSeconds = parseOptionalNumber(formState.targetDurationSeconds);
    if (formState.targetDurationSeconds.trim() && targetDurationSeconds === undefined) {
      toast.error("Duração alvo inválida");
      return;
    }
    try {
      const created = await requestVeoVideo.mutateAsync({
        slot: formState.slot,
        title: formState.title.trim(),
        objective: formState.objective.trim(),
        primaryMetric: formState.primaryMetric.trim(),
        personaName: formState.personaName.trim() || undefined,
        personaStyle: formState.personaStyle.trim() || undefined,
        voiceStyle: formState.voiceStyle.trim() || undefined,
        language: formState.language.trim() || undefined,
        targetDurationSeconds,
        scriptText: formState.scriptText.trim(),
        hookText: formState.hookText.trim() || undefined,
        ctaText: formState.ctaText.trim() || undefined,
        captionText: formState.captionText.trim() || undefined,
        providerName: formState.providerName.trim() || "VEO",
        executionMode: formState.executionMode,
        requestedBy: tenantContext.userEmail,
        requiredForRelease: formState.requiredForRelease,
      });
      toast.success(
        `Vídeo solicitado. Profile #${created.salesVideoProfileId} · Job #${created.salesVideoJobId}`,
      );
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível solicitar o vídeo VEO.")
        : "Não foi possível solicitar o vídeo VEO.";
      toast.error(message);
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div>
              <h5 className="card-title mb-1">Vídeos do experimento</h5>
              <p className="text-muted small mb-0">
                Gestão operacional dos vídeos necessários para campanha e página.
              </p>
            </div>
            <span className="badge text-bg-secondary">
              {sortedAssets.length} ativo(s)
            </span>
          </div>
          <div className="table-responsive mt-3">
            <table className="table table-sm align-middle">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Slot</th>
                  <th>Status</th>
                  <th>Revisão</th>
                  <th>Provider</th>
                  <th>Profile / Job</th>
                  <th>Obrigatório</th>
                  <th>Atualizado</th>
                  <th>Asset</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={9} className="text-muted">
                      Carregando vídeos...
                    </td>
                  </tr>
                ) : sortedAssets.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="text-muted">
                      Nenhum vídeo registrado para este experimento.
                    </td>
                  </tr>
                ) : (
                  sortedAssets.map((asset) => (
                    <tr key={asset.id}>
                      <td>{asset.id}</td>
                      <td>{asset.slot}</td>
                      <td>
                        <span className="badge text-bg-info">{asset.status}</span>
                      </td>
                      <td>{asset.reviewStatus}</td>
                      <td>{asset.provider}</td>
                      <td>
                        {asset.salesVideoProfileId ? (
                          <Link to={`/sales-videos/profiles/${asset.salesVideoProfileId}`}>
                            Profile #{asset.salesVideoProfileId}
                          </Link>
                        ) : (
                          "—"
                        )}
                        {asset.salesVideoJobId ? ` · Job #${asset.salesVideoJobId}` : ""}
                      </td>
                      <td>{asset.requiredForRelease ? "Sim" : "Não"}</td>
                      <td>{formatDate(asset.updatedAt)}</td>
                      <td>
                        {asset.assetUrl ? (
                          <a href={asset.assetUrl} target="_blank" rel="noreferrer">
                            Abrir
                          </a>
                        ) : (
                          "—"
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <form className="card" onSubmit={handleSubmit}>
        <div className="card-body">
          <h5 className="card-title mb-3">Solicitar vídeo VEO</h5>
          <div className="row g-3">
            <div className="col-md-3">
              <label className="form-label">Slot</label>
              <select
                className="form-select"
                value={formState.slot}
                onChange={(event) =>
                  setFormState((prev) => ({
                    ...prev,
                    slot: event.target.value as ExperimentVideoSlot,
                  }))
                }
              >
                {VIDEO_SLOT_OPTIONS.map((slot) => (
                  <option key={slot} value={slot}>
                    {slot}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-6">
              <label className="form-label">Título interno</label>
              <input
                className="form-control"
                value={formState.title}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, title: event.target.value }))
                }
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Provider</label>
              <input
                className="form-control"
                value={formState.providerName}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, providerName: event.target.value }))
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label">Objetivo</label>
              <input
                className="form-control"
                value={formState.objective}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, objective: event.target.value }))
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label">Métrica primária</label>
              <input
                className="form-control"
                value={formState.primaryMetric}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, primaryMetric: event.target.value }))
                }
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Duração alvo</label>
              <input
                className="form-control"
                type="number"
                min="1"
                value={formState.targetDurationSeconds}
                onChange={(event) =>
                  setFormState((prev) => ({
                    ...prev,
                    targetDurationSeconds: event.target.value,
                  }))
                }
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Modo</label>
              <select
                className="form-select"
                value={formState.executionMode}
                onChange={(event) =>
                  setFormState((prev) => ({
                    ...prev,
                    executionMode: event.target.value as SalesVideoExecutionMode,
                  }))
                }
              >
                {EXECUTION_MODE_OPTIONS.map((mode) => (
                  <option key={mode} value={mode}>
                    {mode}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-6">
              <label className="form-label">Persona / estilo</label>
              <div className="row g-2">
                <div className="col-md-4">
                  <input
                    className="form-control"
                    placeholder="Persona"
                    value={formState.personaName}
                    onChange={(event) =>
                      setFormState((prev) => ({
                        ...prev,
                        personaName: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-4">
                  <input
                    className="form-control"
                    placeholder="Estilo"
                    value={formState.personaStyle}
                    onChange={(event) =>
                      setFormState((prev) => ({
                        ...prev,
                        personaStyle: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-4">
                  <input
                    className="form-control"
                    placeholder="Voz"
                    value={formState.voiceStyle}
                    onChange={(event) =>
                      setFormState((prev) => ({
                        ...prev,
                        voiceStyle: event.target.value,
                      }))
                    }
                  />
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <label className="form-label">Hook</label>
              <input
                className="form-control"
                value={formState.hookText}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, hookText: event.target.value }))
                }
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">CTA</label>
              <input
                className="form-control"
                value={formState.ctaText}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, ctaText: event.target.value }))
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label">Script para VEO</label>
              <textarea
                className="form-control"
                rows={10}
                value={formState.scriptText}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, scriptText: event.target.value }))
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label">Legenda / observações</label>
              <textarea
                className="form-control"
                rows={3}
                value={formState.captionText}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, captionText: event.target.value }))
                }
              />
            </div>
            <div className="col-12">
              <div className="form-check">
                <input
                  id="requiredForRelease"
                  className="form-check-input"
                  type="checkbox"
                  checked={formState.requiredForRelease}
                  onChange={(event) =>
                    setFormState((prev) => ({
                      ...prev,
                      requiredForRelease: event.target.checked,
                    }))
                  }
                />
                <label className="form-check-label" htmlFor="requiredForRelease">
                  Obrigatório para liberar campanha
                </label>
              </div>
            </div>
          </div>
          <div className="mt-3">
            <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
              {requestVeoVideo.isPending ? "Solicitando..." : "Solicitar vídeo VEO"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
