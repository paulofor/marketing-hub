import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  BadgeDollarSign,
  Clapperboard,
  FileText,
  PlayCircle,
  RefreshCcw,
  Save,
  Video,
} from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import { TenantContextBanner } from "../../components/TenantContextBanner";
import { useAsset } from "../../api/media/useAsset";
import { useProduct } from "../../api/product/useProduct";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useCreateSalesVideoProfile } from "../../api/salesVideo/useCreateSalesVideoProfile";
import { useProductSalesVideoJobs } from "../../api/salesVideo/useProductSalesVideoJobs";
import { useApproveSalesVideoScript } from "../../api/salesVideo/useApproveSalesVideoScript";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useTenantContext } from "../../utils/tenantContext";
import {
  SalesVideoJob,
  SalesVideoKind,
  SalesVideoProfile,
} from "../../api/salesVideo/types";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
  SALES_VIDEO_PROVIDER_OPTIONS,
} from "../../api/salesVideo/videoProviderCatalog";
import "./ProductSalesVideoPage.css";

const VIDEO_KIND_OPTIONS: SalesVideoKind[] = ["HERO", "OBJECTION", "PROOF"];
const USD_TO_BRL_RATE = 5;

const DEFAULT_SCRIPT = [
  "Hook: voce sente que sua imagem ainda nao comunica a presenca que voce quer?",
  "Dor: nao e falta de roupa. E falta de clareza sobre os sinais que sua imagem envia.",
  "Mecanismo: o Metodo MUSA cria um plano de 7 dias com microacoes para ajustar presenca, elegancia e intencao.",
  "Desejo: pequenos sinais visuais mudam como voce se percebe e como entra nos ambientes.",
  "CTA: veja agora seu plano MUSA personalizado.",
].join("\n\n");

type ProfileFormState = {
  videoKind: SalesVideoKind;
  title: string;
  personaName: string;
  personaStyle: string;
  voiceStyle: string;
  language: string;
  targetDurationSeconds: string;
  landingPageId: string;
};

function emptyProfileForm(): ProfileFormState {
  return {
    videoKind: "HERO",
    title: "",
    personaName: "",
    personaStyle: "",
    voiceStyle: "",
    language: "pt-BR",
    targetDurationSeconds: "30",
    landingPageId: "",
  };
}

export default function ProductSalesVideoPage() {
  const { productId } = useParams();
  const tenantContext = useTenantContext();
  const { data: product, isLoading: productLoading } = useProduct(productId);
  const { data: profiles, isLoading: profilesLoading } =
    useSalesVideoProfiles(productId);
  const { data: jobs, isLoading: jobsLoading } =
    useProductSalesVideoJobs(productId);
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [selectedProviderName, setSelectedProviderName] = useState(
    DEFAULT_SALES_VIDEO_PROVIDER.providerName,
  );
  const [profileForm, setProfileForm] =
    useState<ProfileFormState>(emptyProfileForm);
  const [scriptText, setScriptText] = useState(DEFAULT_SCRIPT);
  const createProfile = useCreateSalesVideoProfile(productId);
  const approveScript = useApproveSalesVideoScript(
    selectedProfileId || undefined,
  );
  const requestRender = useRequestVideoRender(selectedProfileId || undefined);

  const profileList = useMemo(() => profiles ?? [], [profiles]);
  const jobList = useMemo(() => jobs ?? [], [jobs]);
  const existingVideoJobs = useMemo(
    () => jobList.filter(isExistingVideoJob),
    [jobList],
  );
  const selectedVideoJob = useMemo(() => {
    if (selectedJobId) {
      return existingVideoJobs.find((job) => String(job.id) === selectedJobId);
    }
    return existingVideoJobs[0];
  }, [existingVideoJobs, selectedJobId]);
  const selectedProfile = useMemo(() => {
    if (selectedProfileId) {
      return profileList.find(
        (profile) => String(profile.id) === selectedProfileId,
      );
    }
    if (selectedVideoJob) {
      return profileList.find(
        (profile) => profile.id === selectedVideoJob.profileId,
      );
    }
    return profileList[0];
  }, [profileList, selectedProfileId, selectedVideoJob]);
  const effectiveProfileId = selectedProfile ? String(selectedProfile.id) : "";
  const selectedProvider =
    findSalesVideoProviderOption(selectedProviderName) ??
    DEFAULT_SALES_VIDEO_PROVIDER;
  const selectedVideoObjective = selectedVideoJob
    ? describeVideoObjective(selectedProfile, selectedVideoJob)
    : undefined;
  const productCost = useMemo(
    () => summarizeProductVideoCost(jobList),
    [jobList],
  );
  const funnelMetrics = useMemo(
    () => summarizeFunnel(profileList, jobList),
    [profileList, jobList],
  );

  useEffect(() => {
    if (!selectedJobId && existingVideoJobs.length > 0) {
      setSelectedJobId(String(existingVideoJobs[0].id));
    }
    if (
      selectedJobId &&
      !existingVideoJobs.some((job) => String(job.id) === selectedJobId)
    ) {
      setSelectedJobId(
        existingVideoJobs[0] ? String(existingVideoJobs[0].id) : "",
      );
    }
  }, [existingVideoJobs, selectedJobId]);

  if (!productId) {
    return (
      <div>
        <PageTitle>Vídeos do Produto</PageTitle>
        <p>Informe um produto válido para visualizar os vídeos.</p>
      </div>
    );
  }

  const handleCreateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!profileForm.title.trim()) {
      toast.error("Informe um título para o vídeo");
      return;
    }
    const duration = parseOptionalNumber(profileForm.targetDurationSeconds);
    const landingPageId = parseOptionalNumber(profileForm.landingPageId);
    try {
      const created = await createProfile.mutateAsync({
        videoKind: profileForm.videoKind,
        title: profileForm.title.trim(),
        personaName: profileForm.personaName.trim() || undefined,
        personaStyle: profileForm.personaStyle.trim() || undefined,
        voiceStyle: profileForm.voiceStyle.trim() || undefined,
        language: profileForm.language.trim() || undefined,
        targetDurationSeconds: duration,
        landingPageId,
      });
      setSelectedProfileId(String(created.id));
      setProfileForm(emptyProfileForm());
      toast.success("Vídeo criado no produto");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar vídeo",
      );
    }
  };

  const handleSaveScript = async () => {
    if (!effectiveProfileId) {
      toast.error("Crie ou selecione um vídeo antes do roteiro");
      return;
    }
    if (!scriptText.trim()) {
      toast.error("O roteiro precisa de texto");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText,
        hookText: firstLine(scriptText),
        ctaText: "Ver meu plano MUSA de 7 dias",
        captionText:
          "Diagnostico MUSA: entenda o que sua imagem comunica hoje.",
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Roteiro salvo e aprovado");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao salvar roteiro",
      );
    }
  };

  const handleRequestRender = async () => {
    if (!effectiveProfileId) {
      toast.error("Crie ou selecione um vídeo antes de gerar");
      return;
    }
    try {
      await requestRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: selectedProvider.providerFamily,
        providerName: selectedProvider.providerName,
        executionMode: "TEST",
        metadataJson: buildSalesVideoRenderMetadata(selectedProvider),
      });
      toast.success("Geração de vídeo solicitada");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao solicitar geração",
      );
    }
  };

  if (productLoading || profilesLoading || jobsLoading) {
    return (
      <div>
        <PageTitle>Vídeos do Produto #{productId}</PageTitle>
        <p>Carregando...</p>
      </div>
    );
  }

  return (
    <div className="product-video-page">
      <div className="product-video-page__header">
        <div>
          <PageTitle>Vídeos do produto</PageTitle>
          <p>
            Central única para criar, gerar, revisar e medir custo dos vídeos
            comerciais do produto.
          </p>
        </div>
        <Link to="/products" className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>

      <TenantContextBanner className="mb-3" />

      <section className="product-video-page__context">
        <div>
          <span>Produto</span>
          <strong>{product?.name || product?.slug || `#${productId}`}</strong>
        </div>
        <div>
          <span>Nicho</span>
          <strong>{product?.niche || "Nao informado"}</strong>
        </div>
        <div>
          <span>Avatar</span>
          <strong>{product?.avatar || "Nao informado"}</strong>
        </div>
      </section>

      <section className="product-video-page__metrics">
        <Metric label="Perfis de vídeo" value={String(profileList.length)} />
        <Metric label="Jobs de produção" value={String(jobList.length)} />
        <Metric label="Vídeos prontos" value={String(funnelMetrics.ready)} />
        <Metric label="Em produção" value={String(funnelMetrics.running)} />
        <Metric label="Falhas" value={String(funnelMetrics.failed)} />
        <Metric
          label="Custo total"
          value={formatUsdWithBrl(productCost.total)}
        />
      </section>

      <section className="product-video-page__layout">
        <aside className="product-video-page__sidebar">
          <div className="product-video-page__panel">
            <div className="product-video-page__panel-heading">
              <Video size={18} aria-hidden="true" />
              <strong>Vídeos cadastrados</strong>
            </div>
            <div className="product-video-page__profile-list">
              {existingVideoJobs.length === 0 ? (
                <p className="product-video-page__empty">
                  Nenhum vídeo pronto encontrado para este produto.
                </p>
              ) : (
                existingVideoJobs.map((job) => {
                  const profile = findProfile(profileList, job.profileId);
                  const objective = describeVideoObjective(profile, job);
                  return (
                    <button
                      key={job.id}
                      type="button"
                      className={
                        String(job.id) === String(selectedVideoJob?.id)
                          ? "product-video-page__profile product-video-page__profile--active"
                          : "product-video-page__profile"
                      }
                      onClick={() => {
                        setSelectedJobId(String(job.id));
                        setSelectedProfileId(String(job.profileId));
                      }}
                    >
                      <strong>
                        {profileTitle(profileList, job.profileId)}
                      </strong>
                      <span>
                        Job #{job.id} · {job.providerName ?? job.providerFamily}
                      </span>
                      <span>{formatUsdWithBrl(readJobCost(job))}</span>
                      <em>{objective.stage}</em>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          <form
            className="product-video-page__panel"
            onSubmit={handleCreateProfile}
          >
            <div className="product-video-page__panel-heading">
              <Clapperboard size={18} aria-hidden="true" />
              <strong>Novo vídeo</strong>
            </div>
            <label className="form-label" htmlFor="video-kind">
              Tipo
            </label>
            <select
              id="video-kind"
              className="form-select"
              value={profileForm.videoKind}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  videoKind: event.target.value as SalesVideoKind,
                }))
              }
            >
              {VIDEO_KIND_OPTIONS.map((kind) => (
                <option key={kind} value={kind}>
                  {kind}
                </option>
              ))}
            </select>
            <label className="form-label" htmlFor="video-title">
              Título interno
            </label>
            <input
              id="video-title"
              className="form-control"
              value={profileForm.title}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  title: event.target.value,
                }))
              }
              placeholder="Hero falado MUSA"
            />
            <label className="form-label" htmlFor="video-persona">
              Persona
            </label>
            <input
              id="video-persona"
              className="form-control"
              value={profileForm.personaName}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  personaName: event.target.value,
                }))
              }
              placeholder="Visitante MUSA"
            />
            <label className="form-label" htmlFor="video-style">
              Estilo visual
            </label>
            <input
              id="video-style"
              className="form-control"
              value={profileForm.personaStyle}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  personaStyle: event.target.value,
                }))
              }
              placeholder="Premium acessivel, realista"
            />
            <label className="form-label" htmlFor="video-voice">
              Voz
            </label>
            <input
              id="video-voice"
              className="form-control"
              value={profileForm.voiceStyle}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  voiceStyle: event.target.value,
                }))
              }
              placeholder="Feminina, natural, consultiva"
            />
            <div className="product-video-page__inline-fields">
              <div>
                <label className="form-label" htmlFor="video-duration">
                  Duração
                </label>
                <input
                  id="video-duration"
                  className="form-control"
                  type="number"
                  min="1"
                  value={profileForm.targetDurationSeconds}
                  onChange={(event) =>
                    setProfileForm((prev) => ({
                      ...prev,
                      targetDurationSeconds: event.target.value,
                    }))
                  }
                />
              </div>
              <div>
                <label className="form-label" htmlFor="video-language">
                  Idioma
                </label>
                <input
                  id="video-language"
                  className="form-control"
                  value={profileForm.language}
                  onChange={(event) =>
                    setProfileForm((prev) => ({
                      ...prev,
                      language: event.target.value,
                    }))
                  }
                />
              </div>
            </div>
            <button
              className="btn btn-primary w-100"
              type="submit"
              disabled={createProfile.isPending}
            >
              <Save size={16} aria-hidden="true" />
              {createProfile.isPending ? "Criando..." : "Criar vídeo"}
            </button>
          </form>
        </aside>

        <main className="product-video-page__main">
          <section className="product-video-page__hero-panel">
            <div className="product-video-page__hero-copy">
              <span>Vídeo selecionado</span>
              <h2>{selectedProfile?.title ?? "Nenhum vídeo selecionado"}</h2>
              <p>
                Use esta área para roteiro, geração e acompanhamento.
                Experimentos devem consumir vídeos aprovados daqui, sem criar
                vídeos próprios.
              </p>
              {selectedVideoObjective ? (
                <div className="product-video-page__objective">
                  <span>Objetivo do vídeo na estrada de compra</span>
                  <strong>{selectedVideoObjective.stage}</strong>
                  <p>{selectedVideoObjective.goal}</p>
                  <small>{selectedVideoObjective.evidence}</small>
                </div>
              ) : null}
              <div className="product-video-page__hero-actions">
                {selectedProfile ? (
                  <Link
                    className="btn btn-outline-primary"
                    to={`/sales-videos/profiles/${selectedProfile.id}`}
                  >
                    Abrir detalhes
                  </Link>
                ) : null}
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleRequestRender}
                  disabled={!effectiveProfileId || requestRender.isPending}
                >
                  <PlayCircle size={16} aria-hidden="true" />
                  {requestRender.isPending ? "Solicitando..." : "Gerar vídeo"}
                </button>
              </div>
            </div>
            <LatestVideoPreview job={selectedVideoJob} />
          </section>

          <section className="product-video-page__workflow">
            <div className="product-video-page__script">
              <div className="product-video-page__panel-heading">
                <FileText size={18} aria-hidden="true" />
                <strong>Roteiro comercial</strong>
              </div>
              <textarea
                className="form-control"
                rows={12}
                value={scriptText}
                onChange={(event) => setScriptText(event.target.value)}
              />
              <button
                type="button"
                className="btn btn-outline-primary"
                onClick={handleSaveScript}
                disabled={!effectiveProfileId || approveScript.isPending}
              >
                <Save size={16} aria-hidden="true" />
                {approveScript.isPending
                  ? "Salvando..."
                  : "Salvar roteiro aprovado"}
              </button>
            </div>

            <div className="product-video-page__render">
              <div className="product-video-page__panel-heading">
                <RefreshCcw size={18} aria-hidden="true" />
                <strong>Geração</strong>
              </div>
              <label className="form-label" htmlFor="video-provider">
                Provider
              </label>
              <select
                id="video-provider"
                className="form-select"
                value={selectedProvider.providerName}
                onChange={(event) =>
                  setSelectedProviderName(event.target.value)
                }
              >
                {SALES_VIDEO_PROVIDER_OPTIONS.map((provider) => (
                  <option key={provider.key} value={provider.providerName}>
                    {provider.label}
                  </option>
                ))}
              </select>
              <div className="product-video-page__provider-note">
                <strong>{selectedProvider.label}</strong>
                <span>{selectedProvider.recommendedUse}</span>
              </div>
              <div className="product-video-page__strategy">
                <strong>Direção comercial</strong>
                <span>VEO para blocos curtos falados e anúncios.</span>
                <span>
                  Luma/Kling para visual premium quando houver pós-produção.
                </span>
                <span>
                  Vídeo final deve conduzir dor, mecanismo, desejo e CTA.
                </span>
              </div>
            </div>
          </section>

          <section className="product-video-page__panel">
            <div className="product-video-page__table-heading">
              <div className="product-video-page__panel-heading">
                <BadgeDollarSign size={18} aria-hidden="true" />
                <strong>Jobs e custos do produto</strong>
              </div>
              <span>
                {productCost.knownCount}/{jobList.length} com custo conhecido
              </span>
            </div>
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Vídeo</th>
                    <th>Tipo</th>
                    <th>Status</th>
                    <th>Provider</th>
                    <th>Custo</th>
                    <th>Asset</th>
                    <th>Atualizado</th>
                  </tr>
                </thead>
                <tbody>
                  {jobList.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-muted text-center">
                        Nenhum job registrado para este produto.
                      </td>
                    </tr>
                  ) : (
                    jobList.map((job) => (
                      <tr key={job.id}>
                        <td>#{job.id}</td>
                        <td>{profileTitle(profileList, job.profileId)}</td>
                        <td>{job.jobType}</td>
                        <td>
                          <span className="badge text-bg-info">
                            {job.status}
                          </span>
                        </td>
                        <td>{job.providerName ?? job.providerFamily}</td>
                        <td>{formatUsdWithBrl(readJobCost(job))}</td>
                        <td>{job.assetId ? `#${job.assetId}` : "—"}</td>
                        <td>
                          {formatDate(
                            job.updatedAt ?? job.finishedAt ?? job.requestedAt,
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </section>
    </div>
  );
}

function LatestVideoPreview({ job }: { job?: SalesVideoJob }) {
  const { data: asset } = useAsset(job?.assetId ?? undefined);
  const assetUrl = asset?.publicUrl ?? "";
  const playbackUrl = job?.streamPlaybackUrl?.trim() || assetUrl;

  return (
    <div className="product-video-page__preview">
      {playbackUrl ? (
        <AdaptiveVideoPlayer
          src={playbackUrl}
          fallbackSrc={assetUrl}
          controls
        />
      ) : (
        <div className="product-video-page__preview-empty">
          <PlayCircle size={44} aria-hidden="true" />
          <strong>Sem vídeo pronto</strong>
          <span>Gere um vídeo para preencher o preview do produto.</span>
        </div>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="product-video-page__metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function summarizeFunnel(profiles: SalesVideoProfile[], jobs: SalesVideoJob[]) {
  return {
    ready: jobs.filter(isExistingVideoJob).length,
    running: jobs.filter((job) =>
      [
        "VIDEO_REQUESTED",
        "VIDEO_PROCESSING",
        "SCRIPT_PENDING",
        "STORYBOARD_PENDING",
      ].includes(job.status),
    ).length,
    failed: jobs.filter((job) => job.status === "VIDEO_FAILED").length,
    scripted: profiles.filter(
      (profile) => profile.latestScript?.status === "APPROVED",
    ).length,
  };
}

function isExistingVideoJob(job: SalesVideoJob) {
  return (
    job.status === "VIDEO_READY" &&
    (Boolean(job.streamPlaybackUrl?.trim()) || Boolean(job.assetId))
  );
}

function summarizeProductVideoCost(jobs: SalesVideoJob[]) {
  return jobs.reduce(
    (acc, job) => {
      const cost = readJobCost(job);
      if (cost == null) {
        return acc;
      }
      return {
        total: acc.total + cost,
        knownCount: acc.knownCount + 1,
      };
    },
    { total: 0, knownCount: 0 },
  );
}

function readJobCost(job: SalesVideoJob) {
  const metadataCost = readNumericJsonField(job.metadataJson, [
    "cost_usd",
    "costUsd",
  ]);
  if (metadataCost != null) {
    return metadataCost;
  }
  return readNumericJsonField(job.auditSnapshotJson, ["cost_usd", "costUsd"]);
}

function readNumericJsonField(
  json: string | null | undefined,
  fields: string[],
) {
  if (!json) {
    return null;
  }
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    for (const field of fields) {
      const value = parsed[field];
      if (typeof value === "number" && Number.isFinite(value)) {
        return value;
      }
      if (typeof value === "string" && value.trim()) {
        const numeric = Number(value);
        if (Number.isFinite(numeric)) {
          return numeric;
        }
      }
    }
    return null;
  } catch {
    return null;
  }
}

function profileTitle(profiles: SalesVideoProfile[], profileId: number) {
  return findProfile(profiles, profileId)?.title ?? `Profile #${profileId}`;
}

function findProfile(profiles: SalesVideoProfile[], profileId: number) {
  return profiles.find((profile) => profile.id === profileId);
}

function describeVideoObjective(
  profile: SalesVideoProfile | undefined,
  job: SalesVideoJob,
) {
  const profileTitleText = profile?.title?.toLowerCase() ?? "";
  const plannedObjective = describeKnownMusaPlannedVideo(profileTitleText);
  if (plannedObjective) {
    return plannedObjective;
  }

  const searchableText = [
    profile?.title,
    profile?.videoKind,
    profile?.personaStyle,
    profile?.voiceStyle,
    job.providerName,
    job.metadataJson,
    job.auditSnapshotJson,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  if (
    containsAny(searchableText, ["espelho", "dor", "hook", "apagada", "#5"])
  ) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Abrir com uma cena reconhecivel para a visitante pensar: isso acontece comigo.",
      evidence:
        "Criterio da estrada: situacao reconhecivel antes de explicar o produto.",
    };
  }

  if (
    containsAny(searchableText, [
      "microacoes",
      "microações",
      "mecanismo",
      "7 dias",
      "#7",
    ])
  ) {
    return {
      stage: "Curiosidade -> mecanismo plausivel",
      goal: "Mostrar como pequenas decisoes guiadas produzem uma presenca mais intencional.",
      evidence:
        "Criterio da estrada: entrada da cliente, mecanismo simples e resultado imaginavel.",
    };
  }

  if (
    containsAny(searchableText, [
      "plano",
      "personalizado",
      "diagnostico",
      "diagnóstico",
      "cta",
      "#8",
    ])
  ) {
    return {
      stage: "Desejo -> compra",
      goal: "Conectar o valor percebido ao proximo passo: ver e continuar o plano MUSA.",
      evidence:
        "Criterio da estrada: oferta como continuidade do resultado, nao como compra fria.",
    };
  }

  if (
    containsAny(searchableText, [
      "presenca",
      "presença",
      "luxo",
      "sofisticacao",
      "sofisticação",
      "#6",
    ])
  ) {
    return {
      stage: "Confianca -> desejo",
      goal: "Ajudar a cliente a se imaginar com mais elegancia sem depender de luxo caro.",
      evidence:
        "Criterio da estrada: simular a transformacao futura em uma trajetoria possivel.",
    };
  }

  if (profile?.videoKind === "PROOF") {
    return {
      stage: "Valor pessoal -> confianca",
      goal: "Reforcar prova de produto, mecanismo e casos semelhantes antes da decisao.",
      evidence:
        "Criterio da estrada: transformar interesse inicial em confianca no produto.",
    };
  }

  if (profile?.videoKind === "OBJECTION") {
    return {
      stage: "Reducao de risco",
      goal: "Diminuir incerteza, risco e esforco percebido antes do checkout.",
      evidence:
        "Criterio da estrada: remover resistencias de uso, valor, risco, tradicao e imagem.",
    };
  }

  return {
    stage: "Relevancia -> compreensao",
    goal: "Conectar a dor de imagem ao Metodo MUSA com clareza suficiente para continuar.",
    evidence:
      "Criterio da estrada: aumentar relevancia, valor, adequacao pessoal e confianca.",
  };
}

function describeKnownMusaPlannedVideo(profileTitleText: string) {
  if (profileTitleText.includes("#5")) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Abrir com a dor do espelho para a visitante reconhecer a propria inseguranca visual.",
      evidence:
        "Criterio da estrada: comecar por situacao reconhecivel, problema percebido e atencao.",
    };
  }

  if (profileTitleText.includes("#6")) {
    return {
      stage: "Confianca -> desejo",
      goal: "Mostrar a presenca elegante acessivel como transformacao desejavel sem luxo caro.",
      evidence:
        "Criterio da estrada: ajudar a cliente a simular uma nova situacao futura.",
    };
  }

  if (profileTitleText.includes("#7")) {
    return {
      stage: "Curiosidade -> mecanismo plausivel",
      goal: "Explicar as microacoes de 7 dias para reduzir esforco e tornar o resultado possivel.",
      evidence:
        "Criterio da estrada: entrada da cliente, mecanismo simples e resultado imaginavel.",
    };
  }

  if (profileTitleText.includes("#8")) {
    return {
      stage: "Desejo -> compra",
      goal: "Levar a visitante do desejo ao plano MUSA personalizado como proximo passo natural.",
      evidence:
        "Criterio da estrada: oferta como continuidade do resultado, nao como compra fria.",
    };
  }

  if (profileTitleText.includes("#9")) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Usar fala curta para capturar atencao e fazer a cliente reconhecer rapidamente a dor.",
      evidence:
        "Criterio da estrada: situacao reconhecivel antes de pedir acao comercial.",
    };
  }

  return null;
}

function containsAny(value: string, needles: string[]) {
  return needles.some((needle) => value.includes(needle));
}

function parseOptionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function firstLine(value: string) {
  return value
    .split("\n")
    .find((line) => line.trim())
    ?.trim();
}

function formatUsd(value?: number | null) {
  if (value == null) {
    return "—";
  }
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 4,
  }).format(value);
}

function formatUsdWithBrl(value?: number | null) {
  if (value == null) {
    return "—";
  }
  return `${formatUsd(value)} · ${formatBrl(value * USD_TO_BRL_RATE)}`;
}

function formatBrl(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
