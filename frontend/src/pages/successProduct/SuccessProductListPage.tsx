import { Link } from "react-router-dom";
import { useSuccessProducts } from "../../api/successProduct/useSuccessProducts";
import PageTitle from "../../components/PageTitle";

export default function SuccessProductListPage() {
  const { data, isLoading } = useSuccessProducts();
  const products = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Produtos de Sucesso</PageTitle>
      <Link className="btn btn-primary mb-3" to="/success-products/new">
        Novo Produto de Sucesso
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Descrição</th>
            <th>Novo</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.description}</td>
              <td>{p.novo ? "Yes" : "No"}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/success-products/${p.id}`}
                >
                  Visualizar
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
