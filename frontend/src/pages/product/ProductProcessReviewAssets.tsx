import { ExternalLink, Loader2 } from "lucide-react";
import { useState } from "react";
import type { ProductProcessReviewAsset } from "../../api/businessProcess/types";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import "./ProductProcessReviewAssets.css";

/** Identifica o arquivo exato para não reutilizar carregamento de outra peça. */
export function reviewAssetKey(asset: ProductProcessReviewAsset) {
  return `${asset.taskId}:${asset.artifactId}:${asset.sha256}:${asset.contentUrl}`;
}

/** Mostra os arquivos indicados pelo backend sem escolher outra versão ou registrar aceite. */
export default function ProductProcessReviewAssets({
  assets,
  onLoadChange,
}: {
  assets: ProductProcessReviewAsset[];
  onLoadChange: (key: string, loaded: boolean) => void;
}) {
  if (assets.length === 0) return null;
  return (
    <section
      className="product-process-review-assets"
      aria-label="Criativos para sua decisão"
    >
      <h4>Veja os criativos antes de decidir</h4>
      <p>
        Estas são as peças revisadas por Psique e Têmis. Abra a imagem para
        conferir os detalhes.
      </p>
      <div className="product-process-review-assets__grid">
        {assets.map((asset) => (
          <ReviewAsset
            key={reviewAssetKey(asset)}
            asset={asset}
            onLoadChange={onLoadChange}
          />
        ))}
      </div>
    </section>
  );
}

/** Exibe a imagem inteira, permite abrir o original e recupera falhas de carregamento. */
function ReviewAsset({
  asset,
  onLoadChange,
}: {
  asset: ProductProcessReviewAsset;
  onLoadChange: (key: string, loaded: boolean) => void;
}) {
  const [status, setStatus] = useState<"loading" | "loaded" | "error">(
    "loading",
  );
  const [attempt, setAttempt] = useState(0);
  const url = resolveAssetUrl(asset.contentUrl);
  const src = attempt
    ? `${url}${url.includes("?") ? "&" : "?"}previewRetry=${attempt}`
    : url;
  return (
    <figure className="product-process-review-assets__card">
      <a
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Ampliar ${asset.title.toLowerCase()}`}
      >
        <img
          key={attempt}
          src={src}
          alt={`Prévia de ${asset.title}`}
          hidden={status === "error"}
          onLoad={() => {
            setStatus("loaded");
            onLoadChange(reviewAssetKey(asset), true);
          }}
          onError={() => {
            setStatus("error");
            onLoadChange(reviewAssetKey(asset), false);
          }}
        />
      </a>
      <figcaption>
        <strong>{asset.title}</strong>
        {status === "loading" && (
          <p role="status">
            <Loader2
              className="spinner-border spinner-border-sm"
              size={16}
              aria-hidden="true"
            />{" "}
            Carregando imagem…
          </p>
        )}
        {status === "error" && (
          <div role="alert">
            <p>
              Não foi possível carregar a imagem. Tente novamente antes de
              aprovar.
            </p>
            <button
              className="btn btn-outline-primary btn-sm"
              type="button"
              onClick={() => {
                onLoadChange(reviewAssetKey(asset), false);
                setStatus("loading");
                setAttempt((value) => value + 1);
              }}
            >
              Tentar carregar novamente
            </button>
          </div>
        )}
        <a
          className="btn btn-outline-primary"
          href={url}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`Abrir ${asset.title.toLowerCase()} em tamanho original`}
        >
          <ExternalLink size={17} aria-hidden="true" /> Abrir criativo em
          tamanho original
        </a>
      </figcaption>
    </figure>
  );
}
