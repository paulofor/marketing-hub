import { Link } from "react-router-dom";
import {
  ArrowRight,
  CircleDollarSign,
  Gem,
  Package,
  Sparkles,
  Star,
  Video,
} from "lucide-react";
import { useMemo, type CSSProperties } from "react";
import { formatCommercialStatus } from "../api/product/productStatus";
import { useProductValueChainPositions } from "../api/product/useProductValueChainPositions";
import { useProducts, type Product } from "../api/product/useProducts";
import PageTitle from "../components/PageTitle";
import ProductValueChainPosition from "../components/ProductValueChainPosition";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

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

export default function HomePage() {
  const { data, isLoading } = useProducts(undefined, true);
  const valueChainPositions = useProductValueChainPositions();
  const productsInPlay = useMemo(
    () => (Array.isArray(data) ? data : []),
    [data],
  );
  const valueChainPositionByProductId = useMemo(
    () =>
      new Map(
        (valueChainPositions.data ?? []).map((position) => [
          position.productId,
          position,
        ]),
      ),
    [valueChainPositions.data],
  );

  return (
    <div className="home-page">
      <div className="home-page__header">
        <div>
          <PageTitle>Início</PageTitle>
          <p className="text-muted mb-0">
            Produtos em PLAY prontos para operação, campanha, vídeo e melhoria de
            conversão.
          </p>
        </div>
        <Link className="btn btn-outline-primary" to="/products">
          Ver tela de produtos
        </Link>
      </div>

      {isLoading ? (
        <p className="text-muted">Carregando produtos em PLAY...</p>
      ) : productsInPlay.length === 0 ? (
        <section className="home-products-empty">
          <Package size={24} aria-hidden="true" />
          <div>
            <h2 className="h5 mb-1">Nenhum produto em PLAY encontrado</h2>
            <p className="text-muted mb-0">
              Retome um produto no catálogo para priorizar campanhas e produção
              comercial.
            </p>
          </div>
        </section>
      ) : (
        <section className="home-products-grid" aria-label="Produtos em PLAY">
          {productsInPlay.map((product) => {
            const primaryColor = getProductPrimaryColor(product);
            return (
              <article
                className="home-product-card"
                key={product.id}
                style={{ "--product-primary": primaryColor } as CSSProperties}
              >
                <div className="home-product-card__topline">
                  <span>
                    Status: {formatCommercialStatus(product.commercialStatus)}
                  </span>
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
                <section
                  className="home-product-card__internal-identity"
                  aria-label={`Identidade interna de ${getProductName(product)}`}
                >
                  <span className="home-product-card__internal-identity-title">
                    Identidade interna
                  </span>
                  <div className="home-product-card__internal-identity-items">
                    <div>
                      <Star size={18} aria-hidden="true" />
                      <span>
                        <small>Produto · estrela</small>
                        <strong>
                          {product.internalName?.trim() || "Pendente"}
                        </strong>
                      </span>
                    </div>
                    <div>
                      <Gem size={18} aria-hidden="true" />
                      <span>
                        <small>Tipo · mineral</small>
                        <strong>
                          {product.productTypeInternalName?.trim() ||
                            "Pendente"}
                        </strong>
                      </span>
                    </div>
                  </div>
                </section>
                <ProductValueChainPosition
                  compact
                  productId={product.id}
                  productName={getProductName(product)}
                  position={valueChainPositionByProductId.get(product.id)}
                  isLoading={valueChainPositions.isLoading}
                  isError={valueChainPositions.isError}
                />
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
