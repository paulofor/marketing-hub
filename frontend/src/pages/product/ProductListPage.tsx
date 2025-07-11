import { Link } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";

export default function ProductListPage() {
  const { data, isLoading } = useProducts();
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Produtos</PageTitle>
      <Link className="btn btn-primary mb-3" to="/products/new">
        Novo Produto
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Niche</th>
            <th>Avatar</th>
            <th>Instagram</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.niche}</td>
              <td>{p.avatar}</td>
              <td>{p.instagramAccountId}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
