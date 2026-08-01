import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BadgeCheck,
  BarChart3,
  Brain,
  Clapperboard,
  FileText,
  GitBranch,
  Goal,
  Lightbulb,
  Link2,
  PlayCircle,
  ShieldCheck,
  Sparkles,
  Video,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useProductPdeProductionSlots } from "../../api/product/usePdeProductionSlots";
import { useProducts } from "../../api/product/useProducts";
import type { Product } from "../../api/product/useProducts";
import { useProductPdeVersionVideos } from "../../api/product/usePdeVersionVideos";
import type { PdeVersionVideoPanel } from "../../api/product/usePdeVersionVideos";
import type { PostDeployPdeProductionSlot } from "../../api/experiment/usePostDeployMonitor";
import { useProductSalesVideoJobs } from "../../api/salesVideo/useProductSalesVideoJobs";
import { useSalesVideoPerformanceSummary } from "../../api/salesVideo/useSalesVideoPerformanceSummary";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useVideoProjects } from "../../api/salesVideo/useVideoProjects";
import type {
  SalesVideoJob,
  SalesVideoPerformanceSummary,
  SalesVideoProfile,
  VideoProject,
} from "../../api/salesVideo/types";
import "./PdeVideoProductionPage.css";

type FunnelSlot = {
  key: string;
  label: string;
  objective: string;
  metric: string;
  kind?: SalesVideoProfile["videoKind"];
};

type SlotReadiness = {
  label: string;
  tone: "blocked" | "warning" | "ready" | "learning";
  nextAction: string;
};

type ProductionStep = {
  key: string;
  title: string;
  icon: typeof Brain;
  description: string;
  actionLabel: string;
  route: string;
  productStudioAnchor?: string;
};

const funnelSlots: FunnelSlot[] = [
  {
    key: "abertura",
    label: "Abertura",
    objective: "Segurar atencao e materializar a dor nos primeiros segundos.",
    metric: "Play e progresso 25%",
    kind: "HERO",
  },
  {
    key: "prova",
    label: "Prova",
    objective: "Mostrar que a mudanca visual e percebida e alcancavel.",
    metric: "Progresso 50% e diagnostico iniciado",
    kind: "PROOF",
  },
  {
    key: "mecanismo",
    label: "Mecanismo",
    objective: "Explicar o microajuste que reduz esforco e aumenta desejo.",
    metric: "Progresso 75%",
  },
  {
    key: "objecao",
    label: "Objecao",
    objective: "Quebrar a resistencia de tempo, roupa, dinheiro ou exposicao.",
    metric: "Paywall e checkout iniciado",
    kind: "OBJECTION",
  },
  {
    key: "cta",
    label: "CTA",
    objective: "Transformar interesse em diagnostico, plano ou compra.",
    metric: "Checkout e compra",
  },
];

const productionSteps: ProductionStep[] = [
  {
    key: "briefing",
    title: "Briefing comercial",
    icon: Brain,
    description:
      "Registrar dor, desejo, objecao, acao esperada e metrica antes de gerar qualquer ativo.",
    actionLabel: "Abrir Estudio",
    route: "/audio-video-studio",
    productStudioAnchor: "roteiro",
  },
  {
    key: "roteiro",
    title: "Roteiro e cenas",
    icon: FileText,
    description:
      "Quebrar a promessa em gancho, dor, mecanismo, recompensa e CTA por cena curta.",
    actionLabel: "Editar roteiro",
    route: "/audio-video-studio/projects",
    productStudioAnchor: "roteiro",
  },
  {
    key: "storyboard",
    title: "Storyboard e prompts",
    icon: Clapperboard,
    description:
      "Definir imagem mestre, movimento, personagem, ambiente, legenda e continuidade visual.",
    actionLabel: "Planejar cenas",
    route: "/audio-video-studio",
    productStudioAnchor: "geracao",
  },
  {
    key: "geracao",
    title: "Geracao e variacoes",
    icon: Sparkles,
    description:
      "Escolher provider pelo objetivo comercial: hero premium, anuncio, teaser, retargeting ou avatar.",
    actionLabel: "Gerar video",
    route: "/videos",
    productStudioAnchor: "geracao",
  },
  {
    key: "qualidade",
    title: "Qualidade comercial",
    icon: ShieldCheck,
    description:
      "Bloquear video sem audio, sem CTA, com artefatos ou sem funcao clara no funil.",
    actionLabel: "Revisar videos",
    route: "/creative-video-review",
    productStudioAnchor: "qualidade",
  },
  {
    key: "vinculo",
    title: "Vinculo ao PDE",
    icon: Link2,
    description:
      "Conectar video aprovado ao produto, versao, slot, HLS e experimento correto.",
    actionLabel: "Ver versoes",
    route: "/products",
    productStudioAnchor: "pos-producao",
  },
  {
    key: "distribuicao",
    title: "Distribuicao",
    icon: GitBranch,
    description:
      "Reaproveitar o video aprovado em PDE, Meta Ads, cortes organicos e teste A/B.",
    actionLabel: "Distribuir",
    route: "/social-distribution",
    productStudioAnchor: "pos-producao",
  },
  {
    key: "aprendizado",
    title: "Aprendizado por metrica",
    icon: Lightbulb,
    description:
      "Decidir escalar, refazer gancho, trocar CTA, mudar slot ou aposentar com base em dados.",
    actionLabel: "Ver saude PDE",
    route: "/ops-monitor/pde",
  },
];

const qualityGates = [
  "Hipotese comercial definida",
  "Roteiro por cena aprovado",
  "Storyboard e prompts consistentes",
  "Video gerado com audio/legenda/CTA",
  "Aprovacao humana registrada",
  "HLS pronto para PDE",
  "Vinculo com produto, versao e experimento",
  "Metrica de aprendizado definida",
];

function isPdeProduct(product: Product) {
  const searchable = [
    product.productType,
    product.pdeExperienceJson,
    product.funnel,
    product.codeModules,
    product.publicUrl,
    product.slug,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  return searchable.includes("pde") || searchable.includes("musa");
}

function productLabel(product: Product) {
  return product.name || product.slug || `Produto #${product.id}`;
}

function formatNumber(value?: number | null) {
  return new Intl.NumberFormat("pt-BR").format(value ?? 0);
}

function formatMoney(value?: number | null) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value ?? 0);
}

function latestJobForProfile(
  profile: SalesVideoProfile,
  jobs: SalesVideoJob[],
) {
  return (
    jobs.find((job) => job.profileId === profile.id) ??
    profile.lastJob ??
    undefined
  );
}

function matchProfileForSlot(slot: FunnelSlot, profiles: SalesVideoProfile[]) {
  const normalizedSlot = slot.label.toLowerCase();
  return profiles.find((profile) => {
    const title = profile.title.toLowerCase();
    return (
      (slot.kind && profile.videoKind === slot.kind) ||
      title.includes(normalizedSlot) ||
      title.includes(slot.key)
    );
  });
}

function matchProjectForSlot(slot: FunnelSlot, projects: VideoProject[]) {
  const normalizedSlot = slot.label.toLowerCase();
  return projects.find((project) => {
    const funnelStage = project.funnelStage?.toLowerCase() ?? "";
    const title = project.title.toLowerCase();
    return funnelStage.includes(slot.key) || title.includes(normalizedSlot);
  });
}

function matchPdePanelForSlot(
  slot: FunnelSlot,
  panels: PdeVersionVideoPanel[],
) {
  const normalizedSlot = slot.label.toLowerCase();
  return panels.find((panel) => {
    const slotText = [
      panel.slot.slotCode,
      panel.slot.experienceVersion,
      panel.slot.notes,
      ...panel.videos.map((video) =>
        [video.objective, video.primaryMetric].join(" "),
      ),
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return slotText.includes(slot.key) || slotText.includes(normalizedSlot);
  });
}

function countHlsVideos(panels: PdeVersionVideoPanel[]) {
  return panels.reduce(
    (total, panel) =>
      total +
      panel.videos.filter((video) => Boolean(video.hlsPlaybackUrl?.trim()))
        .length,
    0,
  );
}

function latestPdePanel(panels: PdeVersionVideoPanel[]) {
  return [...panels].sort((current, next) =>
    next.slot.slotCode.localeCompare(current.slot.slotCode, "pt-BR", {
      numeric: true,
    }),
  )[0];
}

function latestExperienceVersion(
  pdePanels: PdeVersionVideoPanel[],
  slots: PostDeployPdeProductionSlot[],
) {
  return (
    latestPdePanel(pdePanels)?.slot.experienceVersion ??
    latestSlotPanel(slots)?.experienceVersion ??
    "Sem versao carregada"
  );
}

function readinessForSlot({
  profile,
  project,
  job,
  hlsVideos,
}: {
  profile?: SalesVideoProfile;
  project?: VideoProject;
  job?: SalesVideoJob;
  hlsVideos: number;
}): SlotReadiness {
  if (hlsVideos > 0) {
    return {
      label: "Aprendendo",
      tone: "learning",
      nextAction: "Ler retencao, checkout e compra antes de escalar.",
    };
  }
  if (profile?.status === "VIDEO_READY" || job?.status === "VIDEO_READY") {
    return {
      label: "Pronto sem HLS",
      tone: "warning",
      nextAction: "Vincular ao PDE e publicar HLS no slot correto.",
    };
  }
  if (job) {
    return {
      label: "Em producao",
      tone: "warning",
      nextAction: "Acompanhar job e revisar qualidade comercial.",
    };
  }
  if (profile || project) {
    return {
      label: "Briefing iniciado",
      tone: "warning",
      nextAction: "Concluir roteiro, cenas e solicitar geracao.",
    };
  }
  return {
    label: "Sem briefing",
    tone: "blocked",
    nextAction: "Criar briefing comercial antes de gerar video.",
  };
}

function productVideoStudioRoute(
  productId: number | undefined,
  anchor?: string,
  fallbackRoute = "/products",
) {
  if (!productId) {
    return fallbackRoute;
  }
  return `/products/${productId}/sales-videos${anchor ? `#${anchor}` : ""}`;
}

export default function PdeVideoProductionPage() {
  const productsQuery = useProducts();
  const pdeProducts = useMemo(
    () => (productsQuery.data ?? []).filter(isPdeProduct),
    [productsQuery.data],
  );
  const selectableProducts =
    pdeProducts.length > 0 ? pdeProducts : (productsQuery.data ?? []);
  const [selectedProductId, setSelectedProductId] = useState<string>("");

  useEffect(() => {
    if (!selectedProductId && selectableProducts[0]) {
      setSelectedProductId(String(selectableProducts[0].id));
    }
  }, [selectableProducts, selectedProductId]);

  const selectedProduct = selectableProducts.find(
    (product) => String(product.id) === selectedProductId,
  );
  const profilesQuery = useSalesVideoProfiles(selectedProductId || undefined);
  const jobsQuery = useProductSalesVideoJobs(selectedProductId || undefined);
  const productionSlotsQuery = useProductPdeProductionSlots(
    selectedProductId || undefined,
  );
  const pdeVideosQuery = useProductPdeVersionVideos(
    selectedProductId || undefined,
  );
  const projectsQuery = useVideoProjects();

  const profiles = useMemo(
    () => profilesQuery.data ?? [],
    [profilesQuery.data],
  );
  const jobs = useMemo(() => jobsQuery.data ?? [], [jobsQuery.data]);
  const productProjects = useMemo(
    () =>
      (projectsQuery.data ?? []).filter(
        (project) => String(project.productId ?? "") === selectedProductId,
      ),
    [projectsQuery.data, selectedProductId],
  );
  const pdePanels = useMemo(
    () => pdeVideosQuery.data ?? [],
    [pdeVideosQuery.data],
  );
  const productionSlots = useMemo(
    () => productionSlotsQuery.data ?? [],
    [productionSlotsQuery.data],
  );
  const selectedProfileId = profiles[0]?.id;
  const performanceQuery = useSalesVideoPerformanceSummary(selectedProfileId);
  const hlsVideos = countHlsVideos(pdePanels);

  const profileWithReadyVideo = profiles.filter(
    (profile) =>
      profile.status === "VIDEO_READY" ||
      profile.status === "PUBLISHED" ||
      latestJobForProfile(profile, jobs)?.status === "VIDEO_READY",
  ).length;
  const activeJobs = jobs.filter((job) =>
    ["SCRIPT_PENDING", "VIDEO_REQUESTED", "VIDEO_PROCESSING"].includes(
      job.status,
    ),
  ).length;
  const slotsWithBlockers = funnelSlots.filter((slot) => {
    const profile = matchProfileForSlot(slot, profiles);
    const project = matchProjectForSlot(slot, productProjects);
    const panel = matchPdePanelForSlot(slot, pdePanels);
    const job = profile ? latestJobForProfile(profile, jobs) : undefined;
    return (
      readinessForSlot({
        profile,
        project,
        job,
        hlsVideos:
          panel?.videos.filter((video) => Boolean(video.hlsPlaybackUrl?.trim()))
            .length ?? 0,
      }).tone === "blocked"
    );
  }).length;

  return (
    <div className="pde-video-production-page">
      <div className="pde-video-production-page__header">
        <div>
          <PageTitle>Produção de Vídeo PDE</PageTitle>
          <p className="pde-video-production-page__subtitle">
            Cockpit comercial para decidir qual video falta em cada produto PDE,
            acompanhar briefing, roteiro, geracao, qualidade, HLS, distribuicao
            e aprendizado por metrica.
          </p>
        </div>
        <span className="pde-video-production-page__badge">
          <GitBranch size={16} aria-hidden="true" />
          Cockpit por produto
        </span>
      </div>

      <section className="pde-video-production-page__selector">
        <div>
          <label htmlFor="pde-product-select">Produto PDE *</label>
          <select
            id="pde-product-select"
            className="form-select"
            value={selectedProductId}
            onChange={(event) => setSelectedProductId(event.target.value)}
            disabled={productsQuery.isLoading}
          >
            <option value="">Selecione um produto</option>
            {selectableProducts.map((product) => (
              <option value={product.id} key={product.id}>
                {productLabel(product)}
              </option>
            ))}
          </select>
        </div>
        {selectedProduct && (
          <div className="pde-video-production-page__product-context">
            <strong>{productLabel(selectedProduct)}</strong>
            <span>
              {selectedProduct.promise ||
                selectedProduct.primaryCta ||
                "Promessa comercial nao informada"}
            </span>
          </div>
        )}
      </section>

      <section className="pde-video-production-page__summary-grid">
        <MetricCard
          label="Perfis de video"
          value={formatNumber(profiles.length)}
        />
        <MetricCard
          label="Videos prontos/publicados"
          value={formatNumber(profileWithReadyVideo)}
        />
        <MetricCard label="Jobs em producao" value={formatNumber(activeJobs)} />
        <MetricCard label="Videos HLS em PDE" value={formatNumber(hlsVideos)} />
        <MetricCard
          label="Versoes PDE"
          value={formatNumber(productionSlots.length || pdePanels.length)}
          detail={latestExperienceVersion(pdePanels, productionSlots)}
        />
        <MetricCard
          label="Slots sem briefing"
          value={formatNumber(slotsWithBlockers)}
          detail="Prioridade de producao"
        />
      </section>

      <section className="pde-video-production-page__section">
        <div className="pde-video-production-page__section-heading">
          <div>
            <h2>Cockpit do produto PDE</h2>
            <p>
              Cada slot abaixo cruza a necessidade comercial do funil com
              evidencias reais do backend: projeto, perfil, job e HLS.
            </p>
          </div>
          {selectedProduct && (
            <Link
              className="btn btn-primary"
              to={`/products/${selectedProduct.id}/sales-videos`}
            >
              <Video size={16} aria-hidden="true" />
              Produzir videos
            </Link>
          )}
        </div>

        {profilesQuery.isLoading ||
        jobsQuery.isLoading ||
        productionSlotsQuery.isLoading ||
        pdeVideosQuery.isLoading ? (
          <p className="text-muted mb-0">Carregando cockpit de producao...</p>
        ) : (
          <div className="pde-video-production-page__slot-grid">
            {funnelSlots.map((slot) => {
              const profile = matchProfileForSlot(slot, profiles);
              const project = matchProjectForSlot(slot, productProjects);
              const job = profile
                ? latestJobForProfile(profile, jobs)
                : undefined;
              const panel = matchPdePanelForSlot(slot, pdePanels);
              const slotHlsVideos =
                panel?.videos.filter((video) =>
                  Boolean(video.hlsPlaybackUrl?.trim()),
                ).length ?? 0;
              const readiness = readinessForSlot({
                profile,
                project,
                job,
                hlsVideos: slotHlsVideos,
              });
              return (
                <article
                  className="pde-video-production-page__slot"
                  key={slot.key}
                >
                  <div className="pde-video-production-page__slot-top">
                    <span>{slot.label}</span>
                    <small>{slot.metric}</small>
                  </div>
                  <span
                    className={`pde-video-production-page__slot-status pde-video-production-page__slot-status--${readiness.tone}`}
                  >
                    {readiness.label}
                  </span>
                  <p>{slot.objective}</p>
                  <dl>
                    <div>
                      <dt>Projeto</dt>
                      <dd>
                        {project
                          ? `${project.status} · #${project.id}`
                          : "Sem projeto vinculado"}
                      </dd>
                    </div>
                    <div>
                      <dt>Perfil</dt>
                      <dd>
                        {profile
                          ? `${profile.status} · #${profile.id}`
                          : "Sem perfil encontrado"}
                      </dd>
                    </div>
                    <div>
                      <dt>Job</dt>
                      <dd>
                        {job
                          ? `${job.status} · ${job.jobType}`
                          : "Sem job recente"}
                      </dd>
                    </div>
                    <div>
                      <dt>HLS</dt>
                      <dd>
                        {slotHlsVideos > 0
                          ? `${slotHlsVideos} pronto(s)`
                          : "Sem HLS publicado"}
                      </dd>
                    </div>
                  </dl>
                  <div className="pde-video-production-page__next-action">
                    <strong>Proxima acao</strong>
                    <span>{readiness.nextAction}</span>
                  </div>
                  <div className="pde-video-production-page__slot-actions">
                    {profile ? (
                      <Link
                        className="btn btn-outline-primary btn-sm"
                        to={`/sales-videos/profiles/${profile.id}`}
                      >
                        Abrir perfil
                      </Link>
                    ) : selectedProduct ? (
                      <Link
                        className="btn btn-outline-primary btn-sm"
                        to={productVideoStudioRoute(
                          selectedProduct.id,
                          readiness.label === "Pronto sem HLS"
                            ? "pos-producao"
                            : "roteiro",
                        )}
                      >
                        Criar perfil
                      </Link>
                    ) : null}
                    {project && (
                      <Link
                        className="btn btn-outline-secondary btn-sm"
                        to="/audio-video-studio/projects"
                      >
                        Ver projeto
                      </Link>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="pde-video-production-page__section">
        <div className="pde-video-production-page__section-heading">
          <div>
            <h2>Etapas de execucao</h2>
            <p>
              A producao deve seguir gates comerciais antes de gastar geracao,
              aprovar ativo ou publicar no PDE.
            </p>
          </div>
        </div>
        <div className="pde-video-production-page__steps">
          {productionSteps.map((step) => {
            const Icon = step.icon;
            return (
              <article
                className="pde-video-production-page__step"
                key={step.key}
              >
                <div className="pde-video-production-page__step-header">
                  <span className="pde-video-production-page__step-icon">
                    <Icon size={18} aria-hidden="true" />
                  </span>
                  <h3>{step.title}</h3>
                </div>
                <p>{step.description}</p>
                <Link
                  className="btn btn-outline-primary btn-sm"
                  to={productVideoStudioRoute(
                    selectedProduct?.id,
                    step.productStudioAnchor,
                    step.route,
                  )}
                >
                  {step.actionLabel}
                </Link>
              </article>
            );
          })}
        </div>
      </section>

      <section className="pde-video-production-page__section">
        <div className="pde-video-production-page__two-columns">
          <div>
            <h2>Qualidade comercial</h2>
            <p>
              O video so deveria avancar quando tiver funcao comercial,
              consistencia criativa, revisao humana e HLS pronto para PDE.
            </p>
            <ul className="pde-video-production-page__checklist">
              {qualityGates.map((gate) => (
                <li key={gate}>
                  <BadgeCheck size={16} aria-hidden="true" />
                  {gate}
                </li>
              ))}
            </ul>
          </div>
          <PerformancePanel summary={performanceQuery.data} />
        </div>
      </section>

      <section className="pde-video-production-page__section">
        <div className="pde-video-production-page__section-heading">
          <div>
            <h2>Versoes PDE e HLS</h2>
            <p>
              Video sem HLS nao deve ser tratado como pronto para publicacao no
              PDE. Esta area mostra as versoes e os ativos retornados pelo
              backend.
            </p>
          </div>
          {selectedProduct && (
            <Link
              className="btn btn-outline-primary"
              to={`/products/${selectedProduct.id}/pde-videos`}
            >
              <PlayCircle size={16} aria-hidden="true" />
              Abrir videos PDE
            </Link>
          )}
        </div>
        {pdePanels.length === 0 ? (
          <div className="alert alert-light border mb-0">
            Nenhuma versao PDE com videos HLS foi carregada para o produto
            selecionado. Cadastre ou publique o slot produtivo antes de tratar o
            video como pronto para escala.
          </div>
        ) : (
          <div className="pde-video-production-page__version-list">
            {pdePanels.map((panel) => (
              <article
                className="pde-video-production-page__version"
                key={panel.slot.id}
              >
                <div>
                  <strong>{panel.slot.slotCode}</strong>
                  <span>{panel.slot.experienceVersion}</span>
                </div>
                <div>
                  {panel.videos.length} video
                  {panel.videos.length === 1 ? "" : "s"} ·{" "}
                  {panel.videos.filter((video) => video.hlsPlaybackUrl).length}{" "}
                  HLS
                </div>
                <span className="badge text-bg-light">{panel.slot.status}</span>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function latestSlotPanel(slots: PostDeployPdeProductionSlot[]) {
  return [...slots].sort((current, next) =>
    next.slotCode.localeCompare(current.slotCode, "pt-BR", {
      numeric: true,
    }),
  )[0];
}

function MetricCard({
  label,
  value,
  detail,
}: {
  label: string;
  value: string;
  detail?: string;
}) {
  return (
    <article className="pde-video-production-page__metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
      {detail && <small>{detail}</small>}
    </article>
  );
}

function PerformancePanel({
  summary,
}: {
  summary?: SalesVideoPerformanceSummary;
}) {
  return (
    <div className="pde-video-production-page__performance">
      <div className="pde-video-production-page__performance-title">
        <BarChart3 size={18} aria-hidden="true" />
        <h2>Aprendizado por metrica</h2>
      </div>
      <div className="pde-video-production-page__performance-grid">
        <MetricCard label="Views" value={formatNumber(summary?.totalViews)} />
        <MetricCard label="Leads" value={formatNumber(summary?.totalLeads)} />
        <MetricCard
          label="Checkouts"
          value={formatNumber(summary?.totalCheckoutStarted)}
        />
        <MetricCard
          label="Compras"
          value={formatNumber(summary?.totalPurchases)}
        />
        <MetricCard
          label="Receita"
          value={formatMoney(summary?.totalRevenue)}
        />
      </div>
      <div className="pde-video-production-page__decision">
        <Goal size={16} aria-hidden="true" />
        Registrar decisao: escalar, refazer gancho, trocar CTA, mudar slot ou
        aposentar.
      </div>
    </div>
  );
}
