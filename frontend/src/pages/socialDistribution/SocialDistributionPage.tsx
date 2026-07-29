import { FormEvent, useMemo, useState } from "react";
import { CheckCircle2, Radio, Send, Youtube } from "lucide-react";
import { toast } from "react-toastify";
import {
  SocialPlatform,
  SocialVideoFormat,
  useCreateSocialAccount,
  useCreateSocialPublication,
  useMarkSocialPublicationPublished,
  useQueueSocialPublication,
  useRecordSocialPublicationMetric,
  useSocialAccounts,
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
      toast.success("Publicação criada em rascunho.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar publicação",
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
                  <label className="form-label">Nome</label>
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
              <button className="btn btn-primary mt-3" type="submit">
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
                <div className="col-12 col-md-4">
                  <label className="form-label">Produto</label>
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
                  <label className="form-label">Título</label>
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
              <button className="btn btn-primary mt-3" type="submit">
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
                          onClick={() => handleQueue(publication.id)}
                        >
                          Fila
                        </button>
                        <button
                          className="btn btn-outline-success btn-sm"
                          type="button"
                          onClick={() => handleMarkPublished(publication.id)}
                        >
                          Link
                        </button>
                        <button
                          className="btn btn-outline-secondary btn-sm"
                          type="button"
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
