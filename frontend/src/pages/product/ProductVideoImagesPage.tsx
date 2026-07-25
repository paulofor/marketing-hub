import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import { Link, useParams } from "react-router-dom";
import {
  CheckCircle2,
  Image as ImageIcon,
  Loader2,
  Sparkles,
} from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useProduct } from "../../api/product/useProduct";
import {
  ProductVideoImage,
  useGenerateProductVideoImages,
  useProductVideoImages,
} from "../../api/product/useProductVideoImages";
import { useUpdateProductVideoSeedImage } from "../../api/product/useUpdateProductVideoSeedImage";

const EXAMPLE_VIDEO_IMAGE_PROMPT =
  "Imagem-base para vídeo comercial do produto: mulher brasileira adulta, expressão natural de clareza e alívio, ambiente claro e cotidiano, estética premium acessível, composição limpa, sem sensualização, sem pose artificial, pronta para virar referência de vídeo curto.";

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? error.message;
  }
  return error instanceof Error
    ? error.message
    : "Não foi possível gerar as imagens.";
}

function formatDate(value?: string) {
  if (!value) return "sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function ProductVideoImagesPage() {
  const { productId } = useParams();
  const { data: product, isLoading: productLoading } = useProduct(productId);
  const { data: images, isLoading: imagesLoading } =
    useProductVideoImages(productId);
  const generateImages = useGenerateProductVideoImages(productId);
  const updateSeedImage = useUpdateProductVideoSeedImage();
  const [prompt, setPrompt] = useState("");
  const [characterName, setCharacterName] = useState("Sofia MUSA");
  const imageList = useMemo(() => images ?? [], [images]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPrompt = prompt.trim();
    if (!normalizedPrompt || generateImages.isPending) {
      return;
    }
    generateImages.mutate(
      { prompt: normalizedPrompt },
      {
        onSuccess: () => {
          toast.success("Imagens geradas para vídeos do produto");
        },
      },
    );
  }

  function approveAsSeedImage(image: ProductVideoImage) {
    if (!productId || !image.assetId || updateSeedImage.isPending) {
      return;
    }
    updateSeedImage.mutate(
      {
        productId: Number(productId),
        assetId: image.assetId,
        characterName,
        reviewStatus: "APPROVED",
        reviewNotes: "Aprovada pela galeria de imagens para vídeos do produto.",
        reviewedBy: "Marketing Hub",
      },
      {
        onSuccess: () => {
          toast.success("Imagem aprovada como semente de vídeo");
        },
      },
    );
  }

  if (!productId) {
    return <p>Informe um produto válido para gerenciar imagens de vídeo.</p>;
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Imagens Para Vídeos</PageTitle>
          <p className="text-muted mb-0">
            Galeria exclusiva de imagens do produto para gerar vídeos comerciais
            consistentes.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Voltar para produtos
        </Link>
      </div>

      <div className="row g-4">
        <div className="col-12 col-xl-4">
          <div className="card">
            <div className="card-body">
              <h2 className="h5 mb-1">
                {productLoading
                  ? "Carregando produto..."
                  : product?.name || `Produto ${productId}`}
              </h2>
              <p className="text-muted small">
                Gere imagens com intenção de vídeo: personagem, cenário,
                linguagem visual e promessa precisam parecer do mesmo produto.
              </p>

              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label
                    className="form-label fw-semibold"
                    htmlFor="product-video-image-prompt"
                  >
                    Prompt da imagem
                  </label>
                  <textarea
                    id="product-video-image-prompt"
                    className="form-control"
                    rows={9}
                    maxLength={4000}
                    value={prompt}
                    onChange={(event) => setPrompt(event.target.value)}
                    placeholder={EXAMPLE_VIDEO_IMAGE_PROMPT}
                  />
                  <div className="form-text">
                    {prompt.length}/4000 caracteres
                  </div>
                </div>

                <div className="mb-3">
                  <label
                    className="form-label fw-semibold"
                    htmlFor="product-video-character-name"
                  >
                    Nome da personagem
                  </label>
                  <input
                    id="product-video-character-name"
                    className="form-control"
                    maxLength={191}
                    value={characterName}
                    onChange={(event) => setCharacterName(event.target.value)}
                  />
                </div>

                {generateImages.isError ? (
                  <div className="alert alert-danger" role="alert">
                    {errorMessage(generateImages.error)}
                  </div>
                ) : null}

                <div className="d-flex flex-wrap gap-2">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={
                      generateImages.isPending || prompt.trim().length === 0
                    }
                  >
                    {generateImages.isPending ? (
                      <>
                        <Loader2
                          size={16}
                          aria-hidden="true"
                          className="product-editor__button-icon spinning"
                        />
                        Gerando...
                      </>
                    ) : (
                      <>
                        <Sparkles size={16} aria-hidden="true" />
                        Gerar imagens
                      </>
                    )}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setPrompt(EXAMPLE_VIDEO_IMAGE_PROMPT)}
                    disabled={generateImages.isPending}
                  >
                    Usar exemplo
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-8">
          <div className="product-video-image-gallery">
            {imagesLoading ? (
              <div className="product-video-image-gallery__empty">
                <Loader2
                  size={20}
                  aria-hidden="true"
                  className="product-editor__button-icon spinning"
                />
                Carregando imagens...
              </div>
            ) : imageList.length === 0 ? (
              <div className="product-video-image-gallery__empty">
                <ImageIcon size={28} aria-hidden="true" />
                Nenhuma imagem criada para vídeos deste produto.
              </div>
            ) : (
              imageList.map((image) => {
                const isSeed =
                  product?.videoSeedImageAssetId != null &&
                  product.videoSeedImageAssetId === image.assetId;
                return (
                  <article
                    className="product-video-image-gallery__item"
                    key={image.id}
                  >
                    <div className="product-video-image-gallery__preview">
                      {image.publicUrl ? (
                        <img
                          src={image.publicUrl}
                          alt="Imagem gerada para vídeo do produto"
                        />
                      ) : (
                        <ImageIcon size={32} aria-hidden="true" />
                      )}
                    </div>
                    <div className="product-video-image-gallery__content">
                      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                          <h2 className="h6 mb-1">
                            {image.model || image.provider || "Imagem gerada"}
                          </h2>
                          <div className="small text-muted">
                            {formatDate(image.createdAt)} · asset{" "}
                            {image.assetId}
                          </div>
                        </div>
                        <span
                          className={`badge ${isSeed ? "text-bg-success" : "text-bg-light border"}`}
                        >
                          {isSeed ? "Imagem semente" : image.reviewStatus}
                        </span>
                      </div>
                      <p className="product-video-image-gallery__prompt">
                        {image.prompt}
                      </p>
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm"
                        disabled={isSeed || updateSeedImage.isPending}
                        onClick={() => approveAsSeedImage(image)}
                      >
                        <CheckCircle2 size={16} aria-hidden="true" />
                        Aprovar como imagem semente
                      </button>
                    </div>
                  </article>
                );
              })
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
