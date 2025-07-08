import { Link } from "react-router-dom";
import { useSuccessProducts } from "../../api/successProduct/useSuccessProducts";
import PageTitle from "../../components/PageTitle";

export default function SuccessProductListPage() {
  const { data, isLoading } = useSuccessProducts();
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
          </tr>
        </thead>
        <tbody>
          {data?.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.description}</td>
              <td>{p.novo ? "Yes" : "No"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
