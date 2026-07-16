import { Link } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

function splitText(value?: string) {
  if (!value) return [];
  return value
    .split(/[;\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function isMusaProduct(product: { slug?: string; name?: string }) {
  const slug = product.slug?.toLowerCase() ?? "";
  const name = product.name?.toLowerCase() ?? "";
  return slug === "metodo-musa-7-dias" || name.includes("método musa") || name.includes("metodo musa");
}

function extractHexColor(value: string) {
  return value.match(/#[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3})?\b/)?.[0];
}

export default function ProductListPage() {
  const { data, isLoading } = useProducts();
  const products = (Array.isArray(data) ? data : []).filter(isMusaProduct);
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Cadastro de Produtos</PageTitle>
          <p className="text-muted mb-0">
            Fonte comercial dos produtos que o Marketing Hub vende, entrega e escala.
          </p>
        </div>
        <Link className="btn btn-primary" to="/products/new">
          Novo Produto
        </Link>
      </div>

      <div className="row g-3">
        {products.map((product) => {
          const colors = splitText(product.colorPalette);
          return (
            <div className="col-12" key={product.id}>
              <section className="product-catalog-card">
                <div className="product-catalog-card__header">
                  <div>
                    <span className="badge text-bg-light border mb-2">
                      {product.commercialStatus || "Sem status"}
                    </span>
                    <h2 className="h4 mb-1">
                      {product.name || product.niche || `Produto ${product.id}`}
                    </h2>
                    <p className="text-muted mb-0">{product.slug}</p>
                  </div>
                  <div className="product-catalog-card__price">
                    {product.currentPriceBrl != null
                      ? money.format(product.currentPriceBrl)
                      : "Preço aberto"}
                  </div>
                </div>

                <div className="row g-3 mt-1">
                  <div className="col-12 col-xl-7">
                    <dl className="product-catalog-card__facts">
                      <div>
                        <dt>URL pública</dt>
                        <dd>
                          {product.publicUrl ? (
                            <a href={product.publicUrl} target="_blank" rel="noreferrer">
                              {product.publicUrl}
                            </a>
                          ) : (
                            "Não informada"
                          )}
                        </dd>
                      </div>
                      <div>
                        <dt>Público alvo</dt>
                        <dd>{product.targetAudience || product.avatar || "Não informado"}</dd>
                      </div>
                      <div>
                        <dt>Hipótese/oferta principal</dt>
                        <dd>{product.primaryHypothesis || product.promise || "Não informada"}</dd>
                      </div>
                      <div>
                        <dt>Experimentos associados</dt>
                        <dd>{product.associatedExperiments || "Nenhum experimento vinculado"}</dd>
                      </div>
                    </dl>
                  </div>
                  <div className="col-12 col-xl-5">
                    <div className="product-catalog-card__panel">
                      <h3 className="h6">Linguagem</h3>
                      <p>{product.languageStyle || product.storytelling || "Não informada"}</p>
                      {colors.length > 0 && (
                        <>
                          <h3 className="h6 mt-3">Cores</h3>
                          <div className="product-catalog-card__colors">
                            {colors.map((color) => {
                              const hexColor = extractHexColor(color);
                              return (
                                <span key={color}>
                                  {hexColor && (
                                    <i
                                      aria-hidden="true"
                                      style={{ backgroundColor: hexColor }}
                                    />
                                  )}
                                  {color}
                                </span>
                              );
                            })}
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="product-catalog-card__actions">
                  <Link
                    className="btn btn-sm btn-outline-primary"
                    to={`/products/${product.id}/sales-videos`}
                  >
                    Vídeos de venda
                  </Link>
                </div>
              </section>
            </div>
          );
        })}
      </div>
    </div>
  );
}
