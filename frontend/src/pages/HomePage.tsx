import { Link } from "react-router-dom";
import {
  ArrowRight,
  CircleDollarSign,
  Package,
  Sparkles,
  Video,
} from "lucide-react";
import { useMemo, type CSSProperties } from "react";
import { useProducts, type Product } from "../api/product/useProducts";
import PageTitle from "../components/PageTitle";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

function normalizeStatus(value?: string) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toUpperCase();
}

function isActiveProduct(product: Product) {
  const status = normalizeStatus(product.commercialStatus);
  return ![
    "ARQUIVADO",
    "ARCHIVED",
    "INATIVO",
    "INACTIVE",
    "PAUSADO",
    "PAUSED",
    "ENCERRADO",
    "REJECTED",
  ].includes(status);
}

function getProductName(product: Product) {
  return product.name || product.niche || `Produto ${product.id}`;
}

function getPrimaryPromise(product: Product) {
  return (
    product.primaryHypothesis ||
    product.promise ||
    product.tripwire ||
    "Promessa comercial ainda não definida."
  );
}

function getProductPrimaryColor(product: Product) {
  const color = product.colorPalette?.match(/#[0-9a-fA-F]{6}\b/)?.[0];
  return color || "#7a2444";
}

function sortByRecentActivity(a: Product, b: Product) {
  const aTime = Date.parse(a.updatedAt || a.createdAt || "");
  const bTime = Date.parse(b.updatedAt || b.createdAt || "");
  return (
    (Number.isNaN(bTime) ? 0 : bTime) -
    (Number.isNaN(aTime) ? 0 : aTime)
  );
}

export default function HomePage() {
  const { data, isLoading } = useProducts();
  const activeProducts = useMemo(
    () =>
      Array.isArray(data)
        ? data.filter(isActiveProduct).sort(sortByRecentActivity)
        : [],
    [data],
  );

  return (
    <div className="home-page">
      <div className="home-page__header">
        <div>
          <PageTitle>Início</PageTitle>
          <p className="text-muted mb-0">
            Produtos ativos prontos para operação, campanha, vídeo e melhoria
            de conversão.
          </p>
        </div>
        <Link className="btn btn-outline-primary" to="/products">
          Ver tela de produtos
        </Link>
      </div>

      {isLoading ? (
        <p className="text-muted">Carregando produtos ativos...</p>
      ) : activeProducts.length === 0 ? (
        <section className="home-products-empty">
          <Package size={24} aria-hidden="true" />
          <div>
            <h2 className="h5 mb-1">Nenhum produto ativo encontrado</h2>
            <p className="text-muted mb-0">
              Cadastre ou reative um produto para priorizar campanhas e
              produção comercial.
            </p>
          </div>
        </section>
      ) : (
        <section className="home-products-grid" aria-label="Produtos ativos">
          {activeProducts.map((product) => {
            const primaryColor = getProductPrimaryColor(product);
            return (
              <article
                className="home-product-card"
                key={product.id}
                style={{ "--product-primary": primaryColor } as CSSProperties}
              >
                <div className="home-product-card__topline">
                  <span>{product.commercialStatus || "Ativo"}</span>
                  <strong>
                    {product.currentPriceBrl != null
                      ? money.format(product.currentPriceBrl)
                      : "Preço aberto"}
                  </strong>
                </div>

                <h2>{getProductName(product)}</h2>
                <p className="home-product-card__slug">
                  {product.slug || "sem-slug"}
                </p>
                <p className="home-product-card__promise">
                  {getPrimaryPromise(product)}
                </p>

                <dl className="home-product-card__facts">
                  <div>
                    <dt>Público</dt>
                    <dd>
                      {product.targetAudience ||
                        product.avatar ||
                        "Não informado"}
                    </dd>
                  </div>
                  <div>
                    <dt>CTA</dt>
                    <dd>{product.primaryCta || "Não definido"}</dd>
                  </div>
                </dl>

                <div className="home-product-card__actions">
                  <Link
                    to={`/products/${product.id}/edit`}
                    aria-label={`Abrir ${getProductName(product)}`}
                  >
                    <Sparkles size={16} aria-hidden="true" />
                    Produto
                  </Link>
                  <Link to={`/products/${product.id}/sales-videos`}>
                    <Video size={16} aria-hidden="true" />
                    Vídeos
                  </Link>
                  <Link to={`/products/${product.id}/financial`}>
                    <CircleDollarSign size={16} aria-hidden="true" />
                    Financeiro
                  </Link>
                  <Link
                    className="home-product-card__arrow"
                    to="/products"
                    aria-label="Ver todos os produtos"
                  >
                    <ArrowRight size={18} aria-hidden="true" />
                  </Link>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}
