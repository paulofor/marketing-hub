import { Link } from "react-router-dom";
import { useDeferredValue, useMemo, useState, type CSSProperties } from "react";
import type { Product } from "../../api/product/useProducts";
import { formatCommercialStatus } from "../../api/product/productStatus";
import { useProductValueChainPositions } from "../../api/product/useProductValueChainPositions";
import {
  BookOpen,
  Clapperboard,
  GitBranch,
  CircleDollarSign,
  Eye,
  FileText,
  GitCompare,
  Image as ImageIcon,
  Loader2,
  Megaphone,
  Pencil,
  PlaySquare,
  Search,
  Video,
  Workflow,
} from "lucide-react";
import { parsePdePersuasiveJourney } from "../../api/product/pdePersuasiveJourney";
import { useApplyDefaultPdePersuasiveJourney } from "../../api/product/useApplyDefaultPdePersuasiveJourney";
import { useProducts } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";
import ProductValueChainPosition from "../../components/ProductValueChainPosition";

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

function normalizeProductStatus(value?: string) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toUpperCase();
}

function isCommercialValidationProduct(
  product: Pick<Product, "commercialStatus">,
) {
  return (
    normalizeProductStatus(product.commercialStatus) === "VALIDACAO_COMERCIAL"
  );
}

function getProductActivityTime(
  product: Pick<Product, "updatedAt" | "createdAt">,
) {
  const rawActivityTime = product.updatedAt || product.createdAt;
  if (!rawActivityTime) return 0;
  const activityTime = Date.parse(rawActivityTime);
  return Number.isNaN(activityTime) ? 0 : activityTime;
}

function getAssociatedExperimentCount(
  product: Pick<Product, "associatedExperiments">,
) {
  return splitText(product.associatedExperiments).length;
}

function compareProductsByCommercialActivity(a: Product, b: Product) {
  const validationDelta =
    Number(isCommercialValidationProduct(b)) -
    Number(isCommercialValidationProduct(a));
  if (validationDelta !== 0) return validationDelta;

  const activityDelta = getProductActivityTime(b) - getProductActivityTime(a);
  if (activityDelta !== 0) return activityDelta;

  const experimentsDelta =
    getAssociatedExperimentCount(b) - getAssociatedExperimentCount(a);
  if (experimentsDelta !== 0) return experimentsDelta;

  return b.id - a.id;
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

function isPdeProduct(product: {
  slug?: string;
  name?: string;
  productType?: string;
  pdeExperienceJson?: string;
}) {
  const slug = product.slug?.toLowerCase() ?? "";
  const type = product.productType?.toLowerCase() ?? "";
  return (
    slug.includes("pde") ||
    type.includes("pde") ||
    Boolean(product.pdeExperienceJson?.trim()) ||
    isMusaProduct(product)
  );
}

function cleanJourneyItem(value: string) {
  return value.replace(/^- /, "").replace(/\*\*/g, "");
}

function buildPreviewQaUrl(product: { publicUrl?: string; slug?: string }) {
  if (!product.publicUrl) return "";
  try {
    const url = new URL(product.publicUrl);
    url.searchParams.set("mh_preview", "qa");
    url.searchParams.set("pde_analytics", "off");
    url.searchParams.set("utm_source", "internal");
    url.searchParams.set("utm_medium", "qa");
    url.searchParams.set(
      "utm_campaign",
      `${product.slug || "produto"}_preview_qa`,
    );
    url.searchParams.set("utm_content", "product_card");
    return url.toString();
  } catch {
    return "";
  }
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

function getJourneyStepLabel(step: {
  stageNumber?: number;
  stageName?: string;
  aidaLabel?: string;
  stage?: string;
}) {
  const name = step.stageName || step.aidaLabel || step.stage || "Etapa";
  return step.stageNumber ? `Estágio ${step.stageNumber}: ${name}` : name;
}

function getJourneyTrackedSections(step: {
  trackedSectionIds?: string[];
  trackedSectionId?: string;
}) {
  if (step.trackedSectionIds?.length) return step.trackedSectionIds;
  return step.trackedSectionId ? [step.trackedSectionId] : [];
}

export default function ProductListPage() {
  const [identityQuery, setIdentityQuery] = useState("");
  const deferredIdentityQuery = useDeferredValue(identityQuery.trim());
  const { data, isLoading, isFetching } = useProducts(deferredIdentityQuery);
  const valueChainPositions = useProductValueChainPositions();
  const applyDefaultJourney = useApplyDefaultPdePersuasiveJourney();
  const products = useMemo(
    () =>
      Array.isArray(data)
        ? [...data].sort(compareProductsByCommercialActivity)
        : [],
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

      <div className="product-catalog-search mb-4">
        <label className="form-label" htmlFor="product-identity-search">
          Localizar produto por qualquer nome
        </label>
        <div className="input-group">
          <span className="input-group-text" aria-hidden="true">
            <Search size={18} />
          </span>
          <input
            id="product-identity-search"
            className="form-control"
            type="search"
            value={identityQuery}
            onChange={(event) => setIdentityQuery(event.target.value)}
            placeholder="Nome comercial, nome interno, apelido ou slug"
          />
        </div>
        <div className="form-text" aria-live="polite">
          {isFetching
            ? "Buscando no catálogo..."
            : `${products.length} produto${products.length === 1 ? "" : "s"} encontrado${products.length === 1 ? "" : "s"}.`}
        </div>
      </div>

      <div className="row g-3">
        {products.length === 0 && (
          <div className="col-12">
            <p className="text-muted mb-0">
              {deferredIdentityQuery
                ? "Nenhum produto corresponde a esse nome ou apelido."
                : "Nenhum produto comercial cadastrado."}
            </p>
          </div>
        )}
        {products.map((product) => {
          const displayName =
            product.name || product.niche || `Produto ${product.id}`;
          const internalName = product.internalName?.trim();
          const showInternalName =
            Boolean(internalName) && internalName !== product.name?.trim();
          const colors = buildIdentityColors(product);
          const previewQaUrl = buildPreviewQaUrl(product);
          const persuasiveJourney = parsePdePersuasiveJourney(
            product.pdeExperienceJson,
          );
          const showPdeJourneyAction = isPdeProduct(product);
          const persuasiveJourneySteps = persuasiveJourney?.steps ?? [];
          const isApplyingJourney =
            applyDefaultJourney.isPending &&
            applyDefaultJourney.variables === product.id;
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
                      Status comercial:{" "}
                      {formatCommercialStatus(product.commercialStatus)}
                    </span>
                    <h2 className="h4 mb-1">{displayName}</h2>
                    <p className="text-muted mb-1">{product.slug}</p>
                    {showInternalName && (
                      <p className="product-catalog-card__internal-name">
                        <strong>Nome interno:</strong> {internalName}
                      </p>
                    )}
                    {Boolean(product.aliases?.length) && (
                      <div
                        className="product-catalog-card__aliases"
                        aria-label={`Apelidos internos de ${displayName}`}
                      >
                        <span>Apelidos:</span>
                        {product.aliases?.map((alias) => (
                          <span
                            className="badge text-bg-light border"
                            key={alias}
                          >
                            {alias}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="product-catalog-card__price">
                    {product.currentPriceBrl != null
                      ? money.format(product.currentPriceBrl)
                      : "Preço aberto"}
                  </div>
                </div>

                <ProductValueChainPosition
                  productName={
                    product.name || product.niche || `Produto ${product.id}`
                  }
                  position={valueChainPositionByProductId.get(product.id)}
                  isLoading={valueChainPositions.isLoading}
                  isError={valueChainPositions.isError}
                />

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
                        <dt>Logo</dt>
                        <dd>
                          {product.logoUrl ? (
                            <a
                              href={product.logoUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {product.logoUrl}
                            </a>
                          ) : (
                            "Não informado"
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
                      <div>
                        <dt>CTA principal</dt>
                        <dd>{product.primaryCta || "Não definido"}</dd>
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
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/organic-videos`}
                      >
                        <PlaySquare size={16} aria-hidden="true" />
                        Vídeos orgânicos
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/video-images`}
                      >
                        <ImageIcon size={16} aria-hidden="true" />
                        Imagens Para Vídeos
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/pde-videos`}
                      >
                        <Clapperboard size={16} aria-hidden="true" />
                        Vídeos HLS
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/ads`}
                      >
                        <Megaphone size={16} aria-hidden="true" />
                        Anúncios
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/financial`}
                      >
                        <CircleDollarSign size={16} aria-hidden="true" />
                        Financeiro
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/scientific-articles`}
                      >
                        <BookOpen size={16} aria-hidden="true" />
                        Artigos científicos
                      </Link>
                      <Link
                        className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                        to={`/products/${product.id}/experiment-comparison`}
                      >
                        <GitCompare size={16} aria-hidden="true" />
                        Comparar experimentos
                      </Link>
                      {showPdeJourneyAction && (
                        <Link
                          className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                          to={`/products/${product.id}/pde-versions`}
                        >
                          <GitBranch size={16} aria-hidden="true" />
                          Versões PDE
                        </Link>
                      )}
                      {showPdeJourneyAction && (
                        <Link
                          className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                          to="/pde-copy"
                        >
                          <FileText size={16} aria-hidden="true" />
                          Copy PDE
                        </Link>
                      )}
                      {previewQaUrl && (
                        <a
                          className="product-catalog-card__action-button product-catalog-card__action-button--qa"
                          href={previewQaUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          <Eye size={16} aria-hidden="true" />
                          Preview/QA sem métricas
                        </a>
                      )}
                      {showPdeJourneyAction && (
                        <button
                          type="button"
                          className="product-catalog-card__action-button product-catalog-card__action-button--secondary"
                          disabled={isApplyingJourney}
                          onClick={() => applyDefaultJourney.mutate(product.id)}
                        >
                          {isApplyingJourney ? (
                            <Loader2
                              size={16}
                              aria-hidden="true"
                              className="product-editor__button-icon spinning"
                            />
                          ) : (
                            <Workflow size={16} aria-hidden="true" />
                          )}
                          {persuasiveJourney
                            ? "Atualizar jornada PDE"
                            : "Inserir jornada PDE"}
                        </button>
                      )}
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
                      {persuasiveJourney && (
                        <>
                          <h3 className="h6 mt-3">Jornada comercial PDE</h3>
                          <p className="small text-muted mb-2">
                            {persuasiveJourney.framework ||
                              "Funil experiencial PDE"}{" "}
                            · {persuasiveJourney.version || "sem versão"}
                          </p>
                          <ol className="product-catalog-card__journey">
                            {persuasiveJourneySteps.map((step) => {
                              const trackedSections =
                                getJourneyTrackedSections(step);
                              return (
                                <li key={`${step.stage}-${trackedSections[0]}`}>
                                  <strong>{getJourneyStepLabel(step)}</strong>
                                  {step.psychologicalRole ? (
                                    <small className="d-block text-muted">
                                      apoio: {step.psychologicalRole}
                                    </small>
                                  ) : null}
                                  {step.commercialFunction
                                    ? `: ${step.commercialFunction}`
                                    : ""}
                                  {trackedSections.length ? (
                                    <small className="d-block text-muted">
                                      seções: {trackedSections.join(", ")}
                                    </small>
                                  ) : null}
                                </li>
                              );
                            })}
                          </ol>
                        </>
                      )}
                      {product.sevenDayJourney && (
                        <>
                          <h3 className="h6 mt-3">Jornada de 7 dias</h3>
                          <ul className="product-catalog-card__journey">
                            {splitText(product.sevenDayJourney).map((item) => (
                              <li key={item}>{cleanJourneyItem(item)}</li>
                            ))}
                          </ul>
                        </>
                      )}
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
