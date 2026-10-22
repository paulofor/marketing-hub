import { Link } from "react-router-dom";
import type { Product } from "../../api/product/useProducts";

export default function ProductBigIdeaSummary({
  product,
}: {
  product: Pick<Product, "id" | "bigIdea" | "bigIdeaRevision">;
}) {
  return (
    <section
      className="card card-body border-primary-subtle mb-3"
      aria-label="Big Idea do produto"
    >
      <div className="d-flex flex-wrap justify-content-between gap-2 align-items-center">
        <strong>
          Big Idea{" "}
          {product.bigIdeaRevision ? `· v${product.bigIdeaRevision}` : ""}
        </strong>
        <Link
          to={`/products/${product.id}/big-idea`}
          className="btn btn-sm btn-outline-primary"
        >
          Definir ou evoluir Big Idea
        </Link>
      </div>
      <p className="mb-0 mt-2" style={{ overflowWrap: "anywhere" }}>
        {product.bigIdea ||
          "Ainda não definida. Registre a ideia central que orientará este produto."}
      </p>
    </section>
  );
}
