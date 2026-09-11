import { useDeferredValue, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Loader2, Pencil, Play, Search, Square } from "lucide-react";
import { formatCommercialStatus } from "../../api/product/productStatus";
import {
  useProductAutomaticExecution,
  type ProductAutomaticExecutionControl,
} from "../../api/product/useProductAutomaticExecution";
import { useProducts, type Product } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";
import "./AllProductsPage.css";

const productNameCollator = new Intl.Collator("pt-BR", {
  numeric: true,
  sensitivity: "base",
});

type AutomaticExecutionStatus = "PLAY" | "STOP";

type Feedback = {
  tone: "success" | "danger";
  message: string;
};

function cleanName(value?: string) {
  return value?.trim() || "";
}

function getProductDisplayName(product: Product) {
  return (
    cleanName(product.internalName) ||
    cleanName(product.name) ||
    cleanName(product.niche) ||
    `Produto ${product.id}`
  );
}

function getAutomaticExecutionStatus(
  product: Product,
): AutomaticExecutionStatus | null {
  if (
    product.automaticExecutionStatus === "PLAY" ||
    product.automaticExecutionStatus === "STOP"
  ) {
    return product.automaticExecutionStatus;
  }
  if (typeof product.automaticExecutionEnabled === "boolean") {
    return product.automaticExecutionEnabled ? "PLAY" : "STOP";
  }
  return null;
}

function compareProductsAlphabetically(first: Product, second: Product) {
  const byName = productNameCollator.compare(
    getProductDisplayName(first),
    getProductDisplayName(second),
  );
  return byName !== 0 ? byName : first.id - second.id;
}

export default function AllProductsPage() {
  const [identityQuery, setIdentityQuery] = useState("");
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const deferredIdentityQuery = useDeferredValue(identityQuery.trim());
  const productsQuery = useProducts(deferredIdentityQuery);
  const automaticExecution = useProductAutomaticExecution();
  const products = useMemo(
    () =>
      Array.isArray(productsQuery.data)
        ? [...productsQuery.data].sort(compareProductsAlphabetically)
        : [],
    [productsQuery.data],
  );

  function changeAutomaticExecution(
    product: Product,
    currentStatus: AutomaticExecutionStatus,
  ) {
    const displayName = getProductDisplayName(product);
    automaticExecution.reset();
    setFeedback(null);
    automaticExecution.mutate(
      {
        productId: product.id,
        automaticExecutionEnabled: currentStatus === "STOP",
      },
      {
        onSuccess: (control: ProductAutomaticExecutionControl) => {
          setFeedback({
            tone: "success",
            message: `${displayName} agora está em ${control.automaticExecutionStatus}.`,
          });
        },
        onError: () => {
          setFeedback({
            tone: "danger",
            message: `Não foi possível alterar ${displayName}. O estado anterior foi preservado.`,
          });
        },
      },
    );
  }

  return (
    <div className="all-products-page">
      <div className="all-products-page__header">
        <div>
          <PageTitle>Todos os produtos</PageTitle>
          <p className="text-muted mb-0">
            Visão compacta para localizar, editar e controlar produtos em PLAY
            ou STOP.
          </p>
        </div>
        <Link className="btn btn-outline-primary" to="/products">
          Ver operação em PLAY
        </Link>
      </div>

      <div className="all-products-page__search">
        <label className="form-label" htmlFor="all-products-search">
          Localizar produto por qualquer nome
        </label>
        <div className="input-group">
          <span className="input-group-text" aria-hidden="true">
            <Search size={18} />
          </span>
          <input
            id="all-products-search"
            className="form-control"
            type="search"
            value={identityQuery}
            onChange={(event) => setIdentityQuery(event.target.value)}
            placeholder="Nome interno, comercial, apelido ou slug"
          />
        </div>
        {productsQuery.isFetching && !productsQuery.isLoading ? (
          <p className="form-text mb-0" role="status">
            Atualizando a lista...
          </p>
        ) : null}
      </div>

      {feedback ? (
        <div
          className={`alert alert-${feedback.tone}`}
          role="alert"
          aria-live="polite"
        >
          {feedback.message}
        </div>
      ) : null}

      {productsQuery.isLoading ? (
        <p className="all-products-page__state" role="status">
          <span
            className="spinner-border spinner-border-sm"
            aria-hidden="true"
          />
          Carregando todos os produtos...
        </p>
      ) : productsQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          <p className="mb-2">
            Não foi possível carregar os produtos. Nenhum estado foi alterado.
          </p>
          <button
            className="btn btn-sm btn-outline-danger"
            type="button"
            disabled={productsQuery.isFetching}
            onClick={() => productsQuery.refetch()}
          >
            {productsQuery.isFetching ? (
              <span
                className="spinner-border spinner-border-sm me-1"
                aria-hidden="true"
              />
            ) : null}
            Tentar novamente
          </button>
        </div>
      ) : products.length === 0 ? (
        <p className="all-products-page__state">
          {deferredIdentityQuery
            ? "Nenhum produto corresponde a essa busca."
            : "Nenhum produto cadastrado."}
        </p>
      ) : (
        <div
          className="all-products-list"
          aria-label="Todos os produtos em ordem alfabética"
        >
          {products.map((product) => {
            const displayName = getProductDisplayName(product);
            const commercialName = cleanName(product.name);
            const shouldShowCommercialName =
              Boolean(commercialName) && commercialName !== displayName;
            const automaticStatus = getAutomaticExecutionStatus(product);
            const isChangingThisProduct =
              automaticExecution.isPending &&
              automaticExecution.variables?.productId === product.id;
            const nextStatus = automaticStatus === "PLAY" ? "STOP" : "PLAY";

            return (
              <article className="all-products-list__item" key={product.id}>
                <div className="all-products-list__identity">
                  <h2>
                    <Link to={`/products/${product.id}/edit`}>
                      {displayName}
                    </Link>
                  </h2>
                  {shouldShowCommercialName ? <p>{commercialName}</p> : null}
                  <small>
                    ID {product.id}
                    {cleanName(product.slug) ? ` · ${product.slug}` : ""}
                  </small>
                </div>

                <div className="all-products-list__statuses">
                  <span className="all-products-list__commercial-status">
                    {formatCommercialStatus(product.commercialStatus)}
                  </span>
                  <span
                    className={`all-products-list__execution-status all-products-list__execution-status--${automaticStatus?.toLowerCase() || "unknown"}`}
                  >
                    {automaticStatus || "Status indisponível"}
                  </span>
                </div>

                <div
                  className="all-products-list__actions"
                  aria-label={`Ações de ${displayName}`}
                >
                  <Link
                    className="btn btn-sm btn-outline-secondary"
                    to={`/products/${product.id}/edit`}
                  >
                    <Pencil size={15} aria-hidden="true" />
                    Editar
                  </Link>
                  <button
                    className={`btn btn-sm ${nextStatus === "PLAY" ? "btn-success" : "btn-outline-danger"}`}
                    type="button"
                    disabled={!automaticStatus || automaticExecution.isPending}
                    aria-label={
                      automaticStatus
                        ? `Colocar ${displayName} em ${nextStatus}`
                        : `Estado operacional indisponível para ${displayName}`
                    }
                    onClick={() => {
                      if (automaticStatus) {
                        changeAutomaticExecution(product, automaticStatus);
                      }
                    }}
                  >
                    {isChangingThisProduct ? (
                      <Loader2
                        className="all-products-list__spinner"
                        size={15}
                        aria-hidden="true"
                      />
                    ) : nextStatus === "PLAY" ? (
                      <Play size={15} aria-hidden="true" />
                    ) : (
                      <Square size={14} aria-hidden="true" />
                    )}
                    {isChangingThisProduct
                      ? "Alterando..."
                      : `Colocar em ${nextStatus}`}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
