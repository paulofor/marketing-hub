import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useProduct } from "../../api/product/useProduct";
import { useUpdateProduct } from "../../api/product/useUpdateProduct";
import ProductForm from "./ProductForm";

export default function EditProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const productId = Number(id);
  const { data: product, isLoading } = useProduct(productId);
  const update = useUpdateProduct();

  if (isLoading || !product) return <p>Carregando...</p>;

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Editar Produto Comercial</PageTitle>
          <p className="text-muted mb-0">
            Ajuste os dados que orientam oferta, comunicação, identidade e
            escala.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Voltar
        </Link>
      </div>
      <ProductForm
        initialProduct={product}
        isSaving={update.isPending}
        submitLabel="Salvar alterações"
        onSubmit={(payload) =>
          update.mutate(
            { id: productId, data: payload },
            { onSuccess: () => navigate("/products") },
          )
        }
      />
    </div>
  );
}
