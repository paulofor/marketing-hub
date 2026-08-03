import { FormEvent, useMemo, useState } from "react";
import { BarChart3, CheckCircle2, Radio, Send, Youtube } from "lucide-react";
import { toast } from "react-toastify";
import {
  SocialPlatform,
  SocialVideoFormat,
  useCreateSocialAccount,
  useApproveSocialGrowthContent,
  useCreateSocialGrowthContent,
  useCreateSocialGrowthPlan,
  useCreateSocialPublication,
  useMarkSocialPublicationPublished,
  useQueueSocialPublication,
  useRecordSocialPublicationMetric,
  useSocialAccounts,
  useSocialGrowthPlans,
  useSocialPublications,
} from "../../api/socialDistribution/useSocialDistribution";
import { useProducts } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";

const platformLabels: Record<SocialPlatform, string> = {
  YOUTUBE: "YouTube",
  INSTAGRAM: "Instagram",
  TIKTOK: "TikTok",
};

const formatByPlatform: Record<SocialPlatform, SocialVideoFormat> = {
  YOUTUBE: "YOUTUBE_SHORT",
  INSTAGRAM: "INSTAGRAM_REELS",
  TIKTOK: "TIKTOK_DRAFT",
};

export default function SocialDistributionPage() {
  const productsQuery = useProducts();
  const accountsQuery = useSocialAccounts();
  const publicationsQuery = useSocialPublications();
  const growthPlansQuery = useSocialGrowthPlans();
  const createGrowthPlan = useCreateSocialGrowthPlan();
  const createGrowthContent = useCreateSocialGrowthContent();
  const approveGrowthContent = useApproveSocialGrowthContent();
  const createAccount = useCreateSocialAccount();
  const createPublication = useCreateSocialPublication();
  const queuePublication = useQueueSocialPublication();
  const markPublished = useMarkSocialPublicationPublished();
  const recordMetric = useRecordSocialPublicationMetric();
  const [accountPlatform, setAccountPlatform] =
    useState<SocialPlatform>("YOUTUBE");
  const [accountName, setAccountName] = useState("");
  const [accountHandle, setAccountHandle] = useState("");
  const [publicationPlatform, setPublicationPlatform] =
    useState<SocialPlatform>("YOUTUBE");
  const [productId, setProductId] = useState("");
  const [socialAccountId, setSocialAccountId] = useState("");
  const [title, setTitle] = useState("");
  const [caption, setCaption] = useState("");
  const [hashtags, setHashtags] = useState("#Shorts");
  const [videoUrl, setVideoUrl] = useState("");
  const [growthContentId, setGrowthContentId] = useState("");
  const [planProductId, setPlanProductId] = useState("");
  const [planName, setPlanName] = useState("");
  const [planAudience, setPlanAudience] = useState("");
  const [planHypothesis, setPlanHypothesis] = useState("");
  const [planObjective, setPlanObjective] = useState("");
  const [planCta, setPlanCta] = useState("");
  const [planDestination, setPlanDestination] = useState("");
  const [planUtm, setPlanUtm] = useState("");
  const [selectedPlanId, setSelectedPlanId] = useState("");
  const [contentType, setContentType] = useState<"SHORT" | "LONG_VIDEO">(
    "SHORT",
  );
  const [contentPillar, setContentPillar] = useState("");
  const [contentTopic, setContentTopic] = useState("");
  const [contentStage, setContentStage] = useState("DESCOBERTA");
  const [publishedUrls, setPublishedUrls] = useState<Record<number, string>>(
    {},
  );
  const [viewsByPublication, setViewsByPublication] = useState<
    Record<number, string>
  >({});

  const accounts = accountsQuery.data ?? [];
  const platformAccounts = useMemo(
    () =>
      accounts.filter((account) => account.platform === publicationPlatform),
    [accounts, publicationPlatform],
  );

  const handleCreateAccount = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createAccount.mutateAsync({
        platform: accountPlatform,
        displayName: accountName,
        handle: accountHandle,
        connectionMode: "OAUTH",
        status: "SETUP_REQUIRED",
      });
      setAccountName("");
      setAccountHandle("");
      toast.success("Conta social cadastrada para conexão.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao cadastrar conta",
      );
    }
  };

  const handleCreatePublication = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createPublication.mutateAsync({
        productId: Number(productId),
        growthContentId: growthContentId ? Number(growthContentId) : undefined,
        socialAccountId: socialAccountId ? Number(socialAccountId) : undefined,
        platform: publicationPlatform,
        videoFormat: formatByPlatform[publicationPlatform],
        title,
        caption,
        hashtags,
        videoUrl,
      });
      setTitle("");
      setCaption("");
      setVideoUrl("");
      setGrowthContentId("");
      toast.success("Publicação criada em rascunho.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar publicação",
      );
    }
  };

  const handleCreateGrowthPlan = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const plan = await createGrowthPlan.mutateAsync({
        productId: Number(planProductId),
        name: planName,
        audience: planAudience,
        commercialHypothesis: planHypothesis,
        commercialObjective: planObjective,
        primaryCta: planCta,
        destinationUrl: planDestination,
        utmCampaign: planUtm,
      });
      setSelectedPlanId(String(plan.id));
      setPlanName("");
      setPlanHypothesis("");
      setPlanObjective("");
      toast.success("Plano criado em rascunho.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar plano",
      );
    }
  };

  const handleCreateGrowthContent = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await createGrowthContent.mutateAsync({
        planId: Number(selectedPlanId),
        request: {
          contentType,
          pillar: contentPillar,
          topic: contentTopic,
          funnelStage: contentStage,
        },
      });
      setContentTopic("");
      toast.success("Pauta criada com URL rastreável.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar pauta",
      );
    }
  };

  const handleQueue = async (publicationId: number) => {
    const publication = await queuePublication.mutateAsync(publicationId);
    if (publication.status === "BLOCKED") {
      toast.warn(publication.failureReason);
    } else {
      toast.success("Publicação entrou na fila.");
    }
  };

  const handleMarkPublished = async (publicationId: number) => {
    const url = publishedUrls[publicationId];
    if (!url) {
      toast.warn("Informe a URL publicada.");
      return;
    }
    await markPublished.mutateAsync({
      publicationId,
      request: { publishedUrl: url },
    });
    toast.success("Link publicado registrado.");
  };

  const handleRecordMetric = async (publicationId: number) => {
    await recordMetric.mutateAsync({
      publicationId,
      request: { views: Number(viewsByPublication[publicationId] || 0) },
    });
    toast.success("Métrica registrada.");
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Distribuição orgânica</PageTitle>
          <p className="text-muted mb-0">
            Fila de vídeos por produto para YouTube, Instagram Reels e TikTok.
          </p>
        </div>
        <span className="badge text-bg-light border">
          YouTube primeiro · Reels e TikTok preparados
        </span>
      </div>

      <section className="card mb-4">
        <div className="card-body">
          <div className="d-flex align-items-center gap-2 mb-3">
            <BarChart3 size={18} aria-hidden="true" />
            <div>
              <h2 className="h5 mb-0">Plano de Crescimento Orgânico</h2>
              <div className="small text-muted">
                Planeje conteúdo, aquecimento e atribuição antes de publicar.
              </div>
            </div>
          </div>
          <form className="row g-3" onSubmit={handleCreateGrowthPlan}>
            <div className="col-12 col-md-4">
              <label className="form-label">Produto *</label>
              <select
                className="form-select"
                required
                value={planProductId}
                onChange={(event) => setPlanProductId(event.target.value)}
              >
                <option value="">Selecione</option>
                {(productsQuery.data ?? []).map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name || product.slug}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Nome do ciclo *</label>
              <input
                className="form-control"
                required
                value={planName}
                onChange={(event) => setPlanName(event.target.value)}
                placeholder="Piloto YouTube · 30 dias"
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Campanha UTM *</label>
              <input
                className="form-control"
                required
                value={planUtm}
                onChange={(event) => setPlanUtm(event.target.value)}
                placeholder="agenda-cheia-youtube-piloto"
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Público *</label>
              <input
                className="form-control"
                required
                value={planAudience}
                onChange={(event) => setPlanAudience(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Hipótese comercial *</label>
              <input
                className="form-control"
                required
                value={planHypothesis}
                onChange={(event) => setPlanHypothesis(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Objetivo mensurável *</label>
              <input
                className="form-control"
                required
                value={planObjective}
                onChange={(event) => setPlanObjective(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label">CTA principal *</label>
              <input
                className="form-control"
                required
                value={planCta}
                onChange={(event) => setPlanCta(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label">URL de destino *</label>
              <input
                className="form-control"
                type="url"
                required
                value={planDestination}
                onChange={(event) => setPlanDestination(event.target.value)}
                placeholder="https://..."
              />
            </div>
            <div className="col-12">
              <button
                className="btn btn-primary"
                type="submit"
                disabled={createGrowthPlan.isPending}
              >
                {createGrowthPlan.isPending && (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                )}
                Criar plano
              </button>
            </div>
          </form>

          <hr className="my-4" />
          <form className="row g-3" onSubmit={handleCreateGrowthContent}>
            <div className="col-12 col-md-3">
              <label className="form-label">Plano *</label>
              <select
                className="form-select"
                required
                value={selectedPlanId}
                onChange={(event) => setSelectedPlanId(event.target.value)}
              >
                <option value="">Selecione</option>
                {(growthPlansQuery.data ?? []).map((plan) => (
                  <option key={plan.id} value={plan.id}>
                    {plan.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-2">
              <label className="form-label">Formato *</label>
              <select
                className="form-select"
                value={contentType}
                onChange={(event) =>
                  setContentType(event.target.value as "SHORT" | "LONG_VIDEO")
                }
              >
                <option value="SHORT">Short</option>
                <option value="LONG_VIDEO">Vídeo longo</option>
              </select>
            </div>
            <div className="col-6 col-md-2">
              <label className="form-label">Etapa *</label>
              <select
                className="form-select"
                value={contentStage}
                onChange={(event) => setContentStage(event.target.value)}
              >
                <option value="DESCOBERTA">Descoberta</option>
                <option value="AQUECIMENTO">Aquecimento</option>
                <option value="CONVERSAO">Conversão</option>
              </select>
            </div>
            <div className="col-12 col-md-2">
              <label className="form-label">Pilar *</label>
              <input
                className="form-control"
                required
                value={contentPillar}
                onChange={(event) => setContentPillar(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label">Pauta *</label>
              <input
                className="form-control"
                required
                value={contentTopic}
                onChange={(event) => setContentTopic(event.target.value)}
              />
            </div>
            <div className="col-12">
              <button
                className="btn btn-outline-primary"
                type="submit"
                disabled={createGrowthContent.isPending}
              >
                {createGrowthContent.isPending && (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                )}
                Adicionar pauta
              </button>
            </div>
          </form>

          <div className="row g-3 mt-2">
            {(growthPlansQuery.data ?? []).map((plan) => (
              <div className="col-12" key={plan.id}>
                <div className="border rounded p-3">
                  <div className="d-flex flex-wrap justify-content-between gap-2">
                    <div>
                      <strong>{plan.name}</strong>
                      <div className="small text-muted">
                        {plan.productName} · {plan.commercialObjective}
                      </div>
                    </div>
                    <span className="badge text-bg-light border">
                      {plan.performance.decision}
                    </span>
                  </div>
                  <div className="small mt-2">
                    {plan.performance.decisionReason}
                  </div>
                  <div className="small text-muted mt-1">
                    {plan.performance.views} views ·{" "}
                    {plan.performance.recurringViewers} recorrentes ·{" "}
                    {plan.performance.landingSessions} visitas ·{" "}
                    {plan.performance.leads} leads ·{" "}
                    {plan.performance.salesApproved} vendas
                  </div>
                  {plan.contents.map((content) => (
                    <div
                      className="d-flex flex-wrap align-items-center gap-2 border-top pt-2 mt-2"
                      key={content.id}
                    >
                      <span className="badge text-bg-secondary">
                        {content.contentType === "SHORT" ? "Short" : "Longo"}
                      </span>
                      <span>{content.topic}</span>
                      <a
                        className="small"
                        href={content.trackingUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        URL rastreável
                      </a>
                      <span className="small text-muted">{content.status}</span>
                      {content.status === "DRAFT" && (
                        <button
                          className="btn btn-outline-success btn-sm"
                          type="button"
                          disabled={approveGrowthContent.isPending}
                          onClick={() =>
                            approveGrowthContent
                              .mutateAsync({
                                planId: plan.id,
                                contentId: content.id,
                              })
                              .then(() =>
                                toast.success(
                                  "Pauta aprovada; a publicação ainda precisa ser criada e enfileirada.",
                                ),
                              )
                          }
                        >
                          {approveGrowthContent.isPending && (
                            <span
                              className="spinner-border spinner-border-sm"
                              aria-hidden="true"
                            />
                          )}
                          Aprovar pauta
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="row g-3 mb-4">
        <div className="col-12 col-xl-5">
          <form className="card h-100" onSubmit={handleCreateAccount}>
            <div className="card-body">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Youtube size={18} aria-hidden="true" />
                <h2 className="h5 mb-0">Conta social</h2>
              </div>
              <div className="row g-3">
                <div className="col-12 col-md-4">
                  <label className="form-label">Rede</label>
                  <select
                    className="form-select"
                    value={accountPlatform}
                    onChange={(event) =>
                      setAccountPlatform(event.target.value as SocialPlatform)
                    }
                  >
                    {Object.entries(platformLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Nome *</label>
                  <input
                    className="form-control"
                    value={accountName}
                    onChange={(event) => setAccountName(event.target.value)}
                    required
                  />
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Handle</label>
                  <input
                    className="form-control"
                    value={accountHandle}
                    onChange={(event) => setAccountHandle(event.target.value)}
                    placeholder="@canal"
                  />
                </div>
              </div>
              <button
                className="btn btn-primary mt-3"
                type="submit"
                disabled={createAccount.isPending}
              >
                {createAccount.isPending && (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                )}
                <CheckCircle2 size={16} aria-hidden="true" />
                Cadastrar conta
              </button>
            </div>
          </form>
        </div>

        <div className="col-12 col-xl-7">
          <form className="card h-100" onSubmit={handleCreatePublication}>
            <div className="card-body">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Radio size={18} aria-hidden="true" />
                <h2 className="h5 mb-0">Nova publicação</h2>
              </div>
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">Pauta aprovada do plano</label>
                  <select
                    className="form-select"
                    value={growthContentId}
                    onChange={(event) => {
                      const value = event.target.value;
                      setGrowthContentId(value);
                      const plan = (growthPlansQuery.data ?? []).find(
                        (candidate) =>
                          candidate.contents.some(
                            (content) => String(content.id) === value,
                          ),
                      );
                      const content = plan?.contents.find(
                        (candidate) => String(candidate.id) === value,
                      );
                      if (plan && content) {
                        setProductId(String(plan.productId));
                        setTitle(content.topic);
                        setCaption(`${content.cta} ${content.trackingUrl}`);
                      }
                    }}
                  >
                    <option value="">Publicação avulsa</option>
                    {(growthPlansQuery.data ?? []).flatMap((plan) =>
                      plan.contents
                        .filter(
                          (content) =>
                            content.status === "APPROVED" &&
                            !content.publicationId,
                        )
                        .map((content) => (
                          <option key={content.id} value={content.id}>
                            {plan.name} · {content.topic}
                          </option>
                        )),
                    )}
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Produto *</label>
                  <select
                    className="form-select"
                    value={productId}
                    onChange={(event) => setProductId(event.target.value)}
                    required
                  >
                    <option value="">Selecione</option>
                    {(productsQuery.data ?? []).map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name ||
                          product.slug ||
                          `Produto ${product.id}`}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Rede</label>
                  <select
                    className="form-select"
                    value={publicationPlatform}
                    onChange={(event) => {
                      setPublicationPlatform(
                        event.target.value as SocialPlatform,
                      );
                      setSocialAccountId("");
                    }}
                  >
                    {Object.entries(platformLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Conta</label>
                  <select
                    className="form-select"
                    value={socialAccountId}
                    onChange={(event) => setSocialAccountId(event.target.value)}
                  >
                    <option value="">Sem conta</option>
                    {platformAccounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.displayName} · {account.status}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-12">
                  <label className="form-label">Título *</label>
                  <input
                    className="form-control"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    required
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">URL pública do vídeo</label>
                  <input
                    className="form-control"
                    value={videoUrl}
                    onChange={(event) => setVideoUrl(event.target.value)}
                    placeholder="https://..."
                  />
                </div>
                <div className="col-12 col-md-8">
                  <label className="form-label">Legenda</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={caption}
                    onChange={(event) => setCaption(event.target.value)}
                  />
                </div>
                <div className="col-12 col-md-4">
                  <label className="form-label">Hashtags</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={hashtags}
                    onChange={(event) => setHashtags(event.target.value)}
                  />
                </div>
              </div>
              <button
                className="btn btn-primary mt-3"
                type="submit"
                disabled={createPublication.isPending}
              >
                {createPublication.isPending && (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                )}
                <Send size={16} aria-hidden="true" />
                Criar rascunho
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card">
        <div className="card-body">
          <h2 className="h5">Fila e resultados</h2>
          <div className="table-responsive">
            <table className="table align-middle">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Rede</th>
                  <th>Título</th>
                  <th>Status</th>
                  <th>Resultado</th>
                  <th>Ação</th>
                </tr>
              </thead>
              <tbody>
                {(publicationsQuery.data ?? []).map((publication) => (
                  <tr key={publication.id}>
                    <td>
                      {publication.productName || publication.productSlug}
                    </td>
                    <td>{platformLabels[publication.platform]}</td>
                    <td>{publication.title}</td>
                    <td>
                      <span className="badge text-bg-light border">
                        {publication.status}
                      </span>
                      {publication.failureReason && (
                        <div className="small text-danger mt-1">
                          {publication.failureReason}
                        </div>
                      )}
                    </td>
                    <td>
                      {publication.publishedUrl ? (
                        <a
                          href={publication.publishedUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Post publicado
                        </a>
                      ) : (
                        <input
                          className="form-control form-control-sm"
                          placeholder="URL publicada"
                          value={publishedUrls[publication.id] ?? ""}
                          onChange={(event) =>
                            setPublishedUrls((values) => ({
                              ...values,
                              [publication.id]: event.target.value,
                            }))
                          }
                        />
                      )}
                      <input
                        className="form-control form-control-sm mt-2"
                        placeholder="Views"
                        type="number"
                        min={0}
                        value={viewsByPublication[publication.id] ?? ""}
                        onChange={(event) =>
                          setViewsByPublication((values) => ({
                            ...values,
                            [publication.id]: event.target.value,
                          }))
                        }
                      />
                      {publication.latestMetric && (
                        <div className="small text-muted mt-1">
                          Última leitura: {publication.latestMetric.views ?? 0}{" "}
                          views
                        </div>
                      )}
                    </td>
                    <td>
                      <div className="d-flex flex-wrap gap-2">
                        <button
                          className="btn btn-outline-primary btn-sm"
                          type="button"
                          disabled={queuePublication.isPending}
                          onClick={() => handleQueue(publication.id)}
                        >
                          Fila
                        </button>
                        <button
                          className="btn btn-outline-success btn-sm"
                          type="button"
                          disabled={markPublished.isPending}
                          onClick={() => handleMarkPublished(publication.id)}
                        >
                          Link
                        </button>
                        <button
                          className="btn btn-outline-secondary btn-sm"
                          type="button"
                          disabled={recordMetric.isPending}
                          onClick={() => handleRecordMetric(publication.id)}
                        >
                          Métrica
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
