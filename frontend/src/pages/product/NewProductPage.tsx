import { useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useCreateProduct } from "../../api/product/useCreateProduct";
import ProductForm from "./ProductForm";

export default function NewProductPage() {
  const navigate = useNavigate();
  const create = useCreateProduct();

  return (
    <div>
      <PageTitle>Novo Produto Comercial</PageTitle>
      <ProductForm
        isSaving={create.isPending}
        onSubmit={(payload) =>
          create.mutate(payload, { onSuccess: () => navigate("/products") })
        }
      />
    </div>
  );
}
