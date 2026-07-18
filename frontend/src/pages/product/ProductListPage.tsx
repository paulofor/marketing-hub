import { Link } from "react-router-dom";
import type { CSSProperties } from "react";
import { Pencil, Video } from "lucide-react";
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
  return (
    slug === "metodo-musa-7-dias" ||
    name.includes("método musa") ||
    name.includes("metodo musa")
  );
}

function extractHexColor(value: string) {
  return value.match(/#[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3})?\b/)?.[0];
}

type ProductIdentityColor = {
  role: string;
  label: string;
  hex: string;
  source: "Cadastrada" | "Recomendada";
};

const musaIdentitySupportColors: ProductIdentityColor[] = [
  {
    role: "Texto principal",
    label: "Ink premium",
    hex: "#2b2024",
    source: "Recomendada",
  },
  {
    role: "Texto secundário",
    label: "Vinho suave",
    hex: "#765f66",
    source: "Recomendada",
  },
  {
    role: "Linha/superfície",
    label: "Rosé claro",
    hex: "#ead8cf",
    source: "Recomendada",
  },
  {
    role: "Apoio funcional",
    label: "Verde confiança",
    hex: "#2f5952",
    source: "Recomendada",
  },
];

function normalizeColorRole(rawColor: string, index: number) {
  const text = rawColor
    .replace(/\s*#[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3})?\b\s*/, " ")
    .trim();
  if (text) return text.charAt(0).toUpperCase() + text.slice(1);
  return `Cor ${index + 1}`;
}

function buildIdentityColors(product: {
  slug?: string;
  name?: string;
  colorPalette?: string;
}) {
  const colors = splitText(product.colorPalette).reduce<ProductIdentityColor[]>(
    (identityColors, color, index) => {
      const hex = extractHexColor(color);
      if (!hex) return identityColors;
      identityColors.push({
        role: normalizeColorRole(color, index),
        label: color,
        hex,
        source: "Cadastrada",
      });
      return identityColors;
    },
    [],
  );

  if (!isMusaProduct(product)) return colors;

  const registeredHexColors = new Set(
    colors.map((color) => color.hex.toLowerCase()),
  );
  const supportColors = musaIdentitySupportColors.filter(
    (color) => !registeredHexColors.has(color.hex.toLowerCase()),
  );

  return [...colors, ...supportColors];
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
            Fonte comercial dos produtos que o Marketing Hub vende, entrega e
            escala.
          </p>
        </div>
        <Link className="btn btn-primary" to="/products/new">
          Novo Produto
        </Link>
      </div>

      <div className="row g-3">
        {products.map((product) => {
          const colors = buildIdentityColors(product);
          const registeredColorCount = colors.filter(
            (color) => color.source === "Cadastrada",
          ).length;
          return (
            <div className="col-12" key={product.id}>
              <section
                className="product-catalog-card"
                style={
                  {
                    "--product-primary": colors[0]?.hex ?? "#7a2444",
                    "--product-accent": colors[1]?.hex ?? "#d6a75c",
                    "--product-background": colors[2]?.hex ?? "#fff8f3",
                  } as CSSProperties
                }
              >
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
                            <a
                              href={product.publicUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {product.publicUrl}
                            </a>
                          ) : (
                            "Não informada"
                          )}
                        </dd>
                      </div>
                      <div>
                        <dt>Público alvo</dt>
                        <dd>
                          {product.targetAudience ||
                            product.avatar ||
                            "Não informado"}
                        </dd>
                      </div>
                      <div>
                        <dt>Hipótese/oferta principal</dt>
                        <dd>
                          {product.primaryHypothesis ||
                            product.promise ||
                            "Não informada"}
                        </dd>
                      </div>
                      <div>
                        <dt>Experimentos associados</dt>
                        <dd>
                          {product.associatedExperiments ||
                            "Nenhum experimento vinculado"}
                        </dd>
                      </div>
                    </dl>
                    {product.slug && (
                      <p className="product-catalog-card__description-links">
                        Links de definição do produto:{" "}
                        <a
                          href={`/api/products/public/${product.slug}/marketing-definition`}
                          target="_blank"
                          rel="noreferrer"
                        >
                          definição formatada
                        </a>
                        {" e "}
                        <a
                          href={`/api/products/public/${product.slug}/marketing-definition.md`}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Markdown
                        </a>
                        .
                      </p>
                    )}
                    <div
                      className="product-catalog-card__actions"
                      aria-label={`Ações de ${product.name || product.niche || `Produto ${product.id}`}`}
                    >
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--primary"
                        to={`/products/${product.id}/edit`}
                      >
                        <Pencil size={16} aria-hidden="true" />
                        Editar dados
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/sales-videos`}
                      >
                        <Video size={16} aria-hidden="true" />
                        Vídeos de venda
                      </Link>
                    </div>
                    {!product.slug && (
                      <p className="product-catalog-card__description-links">
                        Links de definição indisponíveis sem slug cadastrado.
                      </p>
                    )}
                  </div>
                  <div className="col-12 col-xl-5">
                    <div className="product-catalog-card__panel">
                      <h3 className="h6">Linguagem</h3>
                      <p>
                        {product.languageStyle ||
                          product.storytelling ||
                          "Não informada"}
                      </p>
                      {colors.length > 0 && (
                        <>
                          <div className="product-catalog-card__color-heading">
                            <h3 className="h6 mt-3">Identidade visual</h3>
                            <span>
                              {registeredColorCount} cadastradas /{" "}
                              {colors.length} no sistema
                            </span>
                          </div>
                          <div className="product-catalog-card__color-system">
                            {colors.map((color) => {
                              return (
                                <div
                                  className="product-catalog-card__color-role"
                                  key={`${color.role}-${color.hex}`}
                                >
                                  <i
                                    aria-hidden="true"
                                    style={{ backgroundColor: color.hex }}
                                  />
                                  <div>
                                    <strong>{color.role}</strong>
                                    <small>
                                      {color.hex} · {color.source}
                                    </small>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </section>
            </div>
          );
        })}
      </div>
    </div>
  );
}
