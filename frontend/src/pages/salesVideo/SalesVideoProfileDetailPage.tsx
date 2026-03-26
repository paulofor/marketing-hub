import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useSalesVideoProfile } from "../../api/salesVideo/useSalesVideoProfile";
import { useGenerateSalesVideoScript } from "../../api/salesVideo/useGenerateSalesVideoScript";
import { useApproveSalesVideoScript } from "../../api/salesVideo/useApproveSalesVideoScript";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useSalesVideoJobs } from "../../api/salesVideo/useSalesVideoJobs";
import { useSalesVideoJobEvents } from "../../api/salesVideo/useSalesVideoJobEvents";
import { useLandingVideoSlots } from "../../api/salesVideo/useLandingVideoSlots";
import { useCreateLandingVideoSlot } from "../../api/salesVideo/useCreateLandingVideoSlot";
import { useUpdateLandingVideoSlot } from "../../api/salesVideo/useUpdateLandingVideoSlot";
import { useSalesVideoScripts } from "../../api/salesVideo/useSalesVideoScripts";
import { useLandingVideoSlotHistory } from "../../api/salesVideo/useLandingVideoSlotHistory";
import { useRetrySalesVideoJob } from "../../api/salesVideo/useRetrySalesVideoJob";
import {
  LandingVideoSlot,
  SalesVideoJob,
  SalesVideoProviderFamily,
  SalesVideoRetryReason,
} from "../../api/salesVideo/types";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { TenantContextBanner } from "../../components/TenantContextBanner";
import { useTenantContext } from "../../utils/tenantContext";

const PROVIDER_FAMILIES: SalesVideoProviderFamily[] = ["EXTERNAL_VIDEO_MODULE", "OPENAI"];

export default function SalesVideoProfileDetailPage() {
  const { profileId } = useParams();
  const { data: profile, isLoading } = useSalesVideoProfile(profileId);
  const { data: jobs } = useSalesVideoJobs(profileId);
  const [selectedJobId, setSelectedJobId] = useState<number | undefined>(undefined);
  const { data: jobEvents, isLoading: eventsLoading } = useSalesVideoJobEvents(selectedJobId);
  const tenantContext = useTenantContext();
  const { data: scriptHistory } = useSalesVideoScripts(profileId);

  const [scriptForm, setScriptForm] = useState({
    scriptText: "",
    hookText: "",
    ctaText: "",
    captionText: "",
    approvedBy: tenantContext.userEmail,
  });
  const [scriptRequestForm, setScriptRequestForm] = useState({
    requestedBy: tenantContext.userEmail,
    providerName: "openai-gpt-4o",
  });
  const [renderForm, setRenderForm] = useState({
    requestedBy: tenantContext.userEmail,
    providerFamily: "EXTERNAL_VIDEO_MODULE" as SalesVideoProviderFamily,
    providerName: "video-management-service",
  });
  const [slotForm, setSlotForm] = useState({
    slotName: "hero",
    assetId: "",
    posterAssetId: "",
    vttAssetId: "",
    autoplay: true,
    muted: true,
    loopVideo: false,
    controlsEnabled: true,
    lazyLoad: true,
    publishedBy: tenantContext.userEmail,
  });

  const generateScript = useGenerateSalesVideoScript(profileId);
  const approveScript = useApproveSalesVideoScript(profileId);
  const requestRender = useRequestVideoRender(profileId);

  const landingId = profile?.landingPageId ?? undefined;
  const { data: slots } = useLandingVideoSlots(landingId);
  const createSlot = useCreateLandingVideoSlot(landingId);
  const updateSlot = useUpdateLandingVideoSlot(landingId);
  const retryJob = useRetrySalesVideoJob();
  const RETRY_REASONS: SalesVideoRetryReason[] = [
    "MANUAL_INTERVENTION",
    "PROVIDER_FAILURE",
    "ASSET_EXPIRED",
    "QUALITY_ASSURANCE",
    "AUTO_RECOVERY",
    "OTHER",
  ];
  const [retryReason, setRetryReason] = useState<SalesVideoRetryReason>("MANUAL_INTERVENTION");
  const [retryNotes, setRetryNotes] = useState("");
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const { data: slotHistory, isLoading: slotHistoryLoading } = useLandingVideoSlotHistory({
    landingId,
    slotId: selectedSlotId ?? undefined,
  });

  useEffect(() => {
    if (profile?.latestScript) {
      setScriptForm((prev) => ({
        ...prev,
        scriptText: profile.latestScript?.scriptText ?? "",
        hookText: profile.latestScript?.hookText ?? "",
        ctaText: profile.latestScript?.ctaText ?? "",
        captionText: profile.latestScript?.captionText ?? "",
      }));
    }
  }, [profile?.latestScript?.id]);

  useEffect(() => {
    setScriptRequestForm((prev) => ({ ...prev, requestedBy: tenantContext.userEmail }));
    setRenderForm((prev) => ({ ...prev, requestedBy: tenantContext.userEmail }));
    setScriptForm((prev) => ({ ...prev, approvedBy: tenantContext.userEmail }));
    setSlotForm((prev) => ({ ...prev, publishedBy: tenantContext.userEmail }));
  }, [tenantContext.userEmail]);

  useEffect(() => {
    if (!selectedJobId && jobs && jobs.length > 0) {
      setSelectedJobId(jobs[0].id);
    }
  }, [jobs, selectedJobId]);

  useEffect(() => {
    setSelectedSlotId(null);
  }, [landingId]);

  const readyJobsWithAsset = useMemo(() => {
    return (jobs ?? []).filter((job): job is SalesVideoJob & { assetId: number } => Boolean(job.assetId));
  }, [jobs]);

  if (!profileId) {
    return (
      <div>
        <PageTitle>Perfil de Vídeo</PageTitle>
        <p>Informe um perfil válido.</p>
      </div>
    );
  }

  if (isLoading || !profile) {
    return (
      <div>
        <PageTitle>Perfil de Vídeo #{profileId}</PageTitle>
        <p>Carregando...</p>
      </div>
    );
  }

  const handleScriptRequestSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      await generateScript.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerName: scriptRequestForm.providerName.trim() || undefined,
      });
      toast.success("Geração de script solicitada");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao criar job";
      toast.error(message);
    }
  };

  const handleScriptApprovalSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!scriptForm.scriptText.trim()) {
      toast.error("O script precisa de conteúdo");
      return;
    }
    if (!tenantContext.userEmail.trim()) {
      toast.error("Informe quem aprovou o script");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText: scriptForm.scriptText,
        hookText: scriptForm.hookText,
        ctaText: scriptForm.ctaText,
        captionText: scriptForm.captionText,
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Script aprovado");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao aprovar script";
      toast.error(message);
    }
  };

  const handleRenderRequestSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!tenantContext.userEmail.trim()) {
      toast.error("Informe quem está solicitando o render");
      return;
    }
    try {
      await requestRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: renderForm.providerFamily,
        providerName: renderForm.providerName.trim() || undefined,
      });
      toast.success("Render solicitado");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao solicitar render";
      toast.error(message);
    }
  };

  const handleRetrySubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedJobId) {
      toast.error("Selecione um job para reprocessar");
      return;
    }
    try {
      await retryJob.mutateAsync({
        jobId: selectedJobId,
        profileId: profile.id,
        requestedBy: tenantContext.userEmail,
        reason: retryReason,
        notes: retryNotes.trim() || undefined,
      });
      toast.success("Reprocessamento solicitado");
      setRetryNotes("");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao reprocessar job";
      toast.error(message);
    }
  };

  const parseNumber = (value: string) => {
    if (!value.trim()) return undefined;
    const parsed = Number(value);
    if (Number.isNaN(parsed)) {
      return undefined;
    }
    return parsed;
  };

  const handleSlotSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!landingId) {
      toast.error("Associe o perfil a uma landing antes de publicar");
      return;
    }
    const assetId = parseNumber(slotForm.assetId);
    if (!assetId) {
      toast.error("Selecione um asset válido");
      return;
    }
    try {
      await createSlot.mutateAsync({
        profileId: profile.id,
        slotName: slotForm.slotName.trim() || "hero",
        assetId,
        posterAssetId: parseNumber(slotForm.posterAssetId),
        vttAssetId: parseNumber(slotForm.vttAssetId),
        autoplay: slotForm.autoplay,
        muted: slotForm.muted,
        loopVideo: slotForm.loopVideo,
        controlsEnabled: slotForm.controlsEnabled,
        lazyLoad: slotForm.lazyLoad,
        publishedBy: tenantContext.userEmail,
      });
      toast.success("Slot configurado com sucesso");
      setSlotForm((prev) => ({
        ...prev,
        assetId: "",
        posterAssetId: "",
        vttAssetId: "",
        publishedBy: tenantContext.userEmail,
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "Não foi possível salvar o slot";
      toast.error(message);
    }
  };

  const handleSlotToggle = async (
    slot: LandingVideoSlot,
    field: "autoplay" | "muted" | "loopVideo" | "controlsEnabled" | "lazyLoad",
    value: boolean,
  ) => {
    try {
      await updateSlot.mutateAsync({ slotId: slot.id, payload: { [field]: value } });
      toast.success(`Slot ${slot.slotName} atualizado`);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao atualizar slot";
      toast.error(message);
    }
  };

  return (
    <div>
      <PageTitle>Perfil de Vídeo #{profile.id}</PageTitle>
      <TenantContextBanner className="mb-3" />
      <div className="mb-3">
        <Link to={`/products/${profile.productId}/sales-videos`} className="btn btn-link p-0">
          &larr; Voltar para os perfis do produto
        </Link>
      </div>

      <section className="mb-4">
        <div className="card p-3">
          <h2 className="h5">Resumo do perfil</h2>
          <div className="row">
            <div className="col-md-3">
              <strong>Tipo:</strong> {profile.videoKind}
            </div>
            <div className="col-md-3">
              <strong>Status:</strong> {profile.status}
            </div>
            <div className="col-md-3">
              <strong>Persona:</strong> {profile.personaName ?? "—"}
            </div>
            <div className="col-md-3">
              <strong>Landing:</strong> {profile.landingPageId ?? "—"}
            </div>
          </div>
        </div>
      </section>

      <section className="mb-4">
        <div className="row g-4">
          <div className="col-lg-6">
            <div className="card p-3 h-100">
              <h3 className="h6 mb-3">Solicitar geração de script</h3>
              <form onSubmit={handleScriptRequestSubmit} className="d-flex flex-column gap-3">
                <div>
                  <label className="form-label">Solicitante</label>
                  <input
                    className="form-control"
                    value={scriptRequestForm.requestedBy}
                    onChange={(event) =>
                      setScriptRequestForm((prev) => ({ ...prev, requestedBy: event.target.value }))
                    }
                  />
                </div>
                <div>
                  <label className="form-label">Provider (opcional)</label>
                  <input
                    className="form-control"
                    value={scriptRequestForm.providerName}
                    onChange={(event) =>
                      setScriptRequestForm((prev) => ({ ...prev, providerName: event.target.value }))
                    }
                  />
                </div>
                <button className="btn btn-outline-primary" type="submit" disabled={generateScript.isPending}>
                  {generateScript.isPending ? "Enviando..." : "Gerar script"}
                </button>
              </form>
            </div>
          </div>
          <div className="col-lg-6">
            <div className="card p-3 h-100">
              <h3 className="h6 mb-3">Solicitar renderização</h3>
              <form onSubmit={handleRenderRequestSubmit} className="d-flex flex-column gap-3">
                <div>
                  <label className="form-label">Solicitante</label>
                  <input
                    className="form-control"
                    value={renderForm.requestedBy}
                    onChange={(event) =>
                      setRenderForm((prev) => ({ ...prev, requestedBy: event.target.value }))
                    }
                  />
                </div>
                <div>
                  <label className="form-label">Família do provider</label>
                  <select
                    className="form-select"
                    value={renderForm.providerFamily}
                    onChange={(event) =>
                      setRenderForm((prev) => ({
                        ...prev,
                        providerFamily: event.target.value as SalesVideoProviderFamily,
                      }))
                    }
                  >
                    {PROVIDER_FAMILIES.map((family) => (
                      <option key={family} value={family}>
                        {family}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Provider (opcional)</label>
                  <input
                    className="form-control"
                    value={renderForm.providerName}
                    onChange={(event) =>
                      setRenderForm((prev) => ({ ...prev, providerName: event.target.value }))
                    }
                  />
                </div>
                <button className="btn btn-outline-primary" type="submit" disabled={requestRender.isPending}>
                  {requestRender.isPending ? "Solicitando..." : "Solicitar render"}
                </button>
              </form>
            </div>
          </div>
        </div>
      </section>

      <section className="mb-4">
        <div className="card p-3">
          <h2 className="h5">Revisão e aprovação do script</h2>
          <form onSubmit={handleScriptApprovalSubmit} className="row g-3">
            <div className="col-12">
              <label className="form-label">Script completo</label>
              <textarea
                className="form-control"
                rows={6}
                value={scriptForm.scriptText}
                onChange={(event) =>
                  setScriptForm((prev) => ({ ...prev, scriptText: event.target.value }))
                }
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Hook</label>
              <textarea
                className="form-control"
                rows={3}
                value={scriptForm.hookText}
                onChange={(event) =>
                  setScriptForm((prev) => ({ ...prev, hookText: event.target.value }))
                }
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">CTA</label>
              <textarea
                className="form-control"
                rows={3}
                value={scriptForm.ctaText}
                onChange={(event) =>
                  setScriptForm((prev) => ({ ...prev, ctaText: event.target.value }))
                }
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Legenda</label>
              <textarea
                className="form-control"
                rows={3}
                value={scriptForm.captionText}
                onChange={(event) =>
                  setScriptForm((prev) => ({ ...prev, captionText: event.target.value }))
                }
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Aprovado por</label>
              <input
                className="form-control"
                value={scriptForm.approvedBy}
                onChange={(event) =>
                  setScriptForm((prev) => ({ ...prev, approvedBy: event.target.value }))
                }
              />
            </div>
            <div className="col-12">
              <button className="btn btn-success" type="submit" disabled={approveScript.isPending}>
                {approveScript.isPending ? "Salvando..." : "Aprovar script"}
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="mb-4">
        <h2 className="h5">Jobs e eventos</h2>
        <div className="table-responsive mb-3">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>ID</th>
                <th>Tipo</th>
                <th>Provider</th>
                <th>Status</th>
                <th>Tentativas</th>
                <th>Último motivo</th>
                <th>Progresso</th>
                <th>Solicitado em</th>
              </tr>
            </thead>
            <tbody>
              {(jobs ?? []).map((job) => (
                <tr
                  key={job.id}
                  className={selectedJobId === job.id ? "table-primary" : ""}
                  onClick={() => setSelectedJobId(job.id)}
                  style={{ cursor: "pointer" }}
                >
                  <td>{job.id}</td>
                  <td>{job.jobType}</td>
                  <td>{job.providerName ?? job.providerFamily}</td>
                  <td>{job.status}</td>
                  <td>{job.retryAttempt ?? 1}</td>
                  <td>{job.retryReason ?? "—"}</td>
                  <td>{job.progressPercent ?? 0}%</td>
                  <td>{job.requestedAt ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {selectedJobId && (
          <div className="card p-3">
            <h3 className="h6">Eventos do job #{selectedJobId}</h3>
            {eventsLoading && <p>Carregando eventos...</p>}
            {!eventsLoading && (jobEvents?.length ?? 0) === 0 && (
              <p className="text-muted">Nenhum evento registrado.</p>
            )}
            {!eventsLoading && jobEvents && jobEvents.length > 0 && (
              <ul className="list-group list-group-flush">
                {jobEvents.map((event) => (
                  <li key={event.id} className="list-group-item px-0">
                    <div className="d-flex justify-content-between">
                      <strong>{event.eventType}</strong>
                      <span className="text-muted small">{event.createdAt}</span>
                    </div>
                    <div className="small text-muted">
                      {event.oldStatus && event.newStatus
                        ? `${event.oldStatus} → ${event.newStatus}`
                        : event.newStatus ?? ""}
                    </div>
                    {event.message && <div>{event.message}</div>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </section>

      <section>
        <h2 className="h5">Publicação na landing</h2>
        {!landingId && (
          <p className="text-muted">
            Este perfil ainda não está associado a uma landing page. Informe `landing_page_id` no
            cadastro para habilitar a publicação.
          </p>
        )}
        {landingId && (
          <>
            <div className="card p-3 mb-4">
              <h3 className="h6">Configurar slot</h3>
              <form className="row g-3" onSubmit={handleSlotSubmit}>
                <div className="col-md-3">
                  <label className="form-label">Slot</label>
                  <input
                    className="form-control"
                    value={slotForm.slotName}
                    onChange={(event) => setSlotForm((prev) => ({ ...prev, slotName: event.target.value }))}
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Asset de vídeo</label>
                  <select
                    className="form-select mb-2"
                    value={slotForm.assetId}
                    onChange={(event) => setSlotForm((prev) => ({ ...prev, assetId: event.target.value }))}
                  >
                    <option value="">Selecione um job pronto</option>
                    {readyJobsWithAsset.map((job) => (
                      <option key={job.id} value={job.assetId}
                        >{`Job #${job.id} · ${job.status}`}</option>
                    ))}
                  </select>
                  <input
                    className="form-control"
                    type="number"
                    value={slotForm.assetId}
                    onChange={(event) => setSlotForm((prev) => ({ ...prev, assetId: event.target.value }))}
                    placeholder="ID do asset"
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Poster (opcional)</label>
                  <input
                    className="form-control"
                    type="number"
                    value={slotForm.posterAssetId}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, posterAssetId: event.target.value }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Legendas (opcional)</label>
                  <input
                    className="form-control"
                    type="number"
                    value={slotForm.vttAssetId}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, vttAssetId: event.target.value }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Autoplay</label>
                  <input
                    className="form-check-input ms-2"
                    type="checkbox"
                    checked={slotForm.autoplay}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, autoplay: event.target.checked }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Mudo por padrão</label>
                  <input
                    className="form-check-input ms-2"
                    type="checkbox"
                    checked={slotForm.muted}
                    onChange={(event) => setSlotForm((prev) => ({ ...prev, muted: event.target.checked }))}
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Repetir vídeo</label>
                  <input
                    className="form-check-input ms-2"
                    type="checkbox"
                    checked={slotForm.loopVideo}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, loopVideo: event.target.checked }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Controles visíveis</label>
                  <input
                    className="form-check-input ms-2"
                    type="checkbox"
                    checked={slotForm.controlsEnabled}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, controlsEnabled: event.target.checked }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Lazy load</label>
                  <input
                    className="form-check-input ms-2"
                    type="checkbox"
                    checked={slotForm.lazyLoad}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, lazyLoad: event.target.checked }))
                    }
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Publicado por (opcional)</label>
                  <input
                    className="form-control"
                    value={slotForm.publishedBy}
                    onChange={(event) =>
                      setSlotForm((prev) => ({ ...prev, publishedBy: event.target.value }))
                    }
                  />
                </div>
                <div className="col-12">
                  <button className="btn btn-primary" type="submit" disabled={createSlot.isPending}>
                    {createSlot.isPending ? "Salvando..." : "Publicar na landing"}
                  </button>
                </div>
              </form>
            </div>

            <div className="row g-3">
              {(slots ?? []).map((slot) => {
                const assetUrl = slot.assetUrl ? resolveAssetUrl(slot.assetUrl) : undefined;
                const posterUrl = slot.posterAssetUrl ? resolveAssetUrl(slot.posterAssetUrl) : undefined;
                return (
                  <div className="col-md-6" key={slot.id}>
                    <div className="card p-3 h-100">
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                          <h3 className="h6 mb-0">Slot {slot.slotName}</h3>
                          <small className="text-muted">Asset #{slot.assetId}</small>
                        </div>
                        <Link to={`/landing/${slot.landingPageId}`} className="btn btn-link btn-sm">
                          Ver landing
                        </Link>
                      </div>
                      {assetUrl ? (
                        <video
                          src={assetUrl}
                          poster={posterUrl}
                          className="w-100 mb-3"
                          controls={slot.controlsEnabled}
                          loop={slot.loopVideo}
                          muted={slot.muted}
                        />
                      ) : (
                        <p className="text-muted">Asset ainda não possui URL pública.</p>
                      )}
                      <div className="d-flex flex-column gap-1">
                        <label className="form-check-label">
                          <input
                            type="checkbox"
                            className="form-check-input me-2"
                            checked={slot.autoplay}
                            onChange={(event) => handleSlotToggle(slot, "autoplay", event.target.checked)}
                          />
                          Autoplay
                        </label>
                        <label className="form-check-label">
                          <input
                            type="checkbox"
                            className="form-check-input me-2"
                            checked={slot.muted}
                            onChange={(event) => handleSlotToggle(slot, "muted", event.target.checked)}
                          />
                          Iniciar sem áudio
                        </label>
                        <label className="form-check-label">
                          <input
                            type="checkbox"
                            className="form-check-input me-2"
                            checked={slot.loopVideo}
                            onChange={(event) => handleSlotToggle(slot, "loopVideo", event.target.checked)}
                          />
                          Repetir vídeo
                        </label>
                        <label className="form-check-label">
                          <input
                            type="checkbox"
                            className="form-check-input me-2"
                            checked={slot.controlsEnabled}
                            onChange={(event) => handleSlotToggle(slot, "controlsEnabled", event.target.checked)}
                          />
                          Controles visíveis
                        </label>
                        <label className="form-check-label">
                          <input
                            type="checkbox"
                            className="form-check-input me-2"
                            checked={slot.lazyLoad}
                            onChange={(event) => handleSlotToggle(slot, "lazyLoad", event.target.checked)}
                          />
                          Lazy load
                        </label>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </section>
    </div>
  );
}
