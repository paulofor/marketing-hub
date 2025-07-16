import { Link } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";
import PageTitle from "../../components/PageTitle";

export default function ProductListPage() {
  const { data, isLoading } = useProducts();
  const products = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Produtos</PageTitle>
      <Link className="btn btn-primary mb-3" to="/products/new">
        Novo Produto
      </Link>
      <div className="table-responsive">
        <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Niche</th>
            <th>Avatar</th>
            <th>Instagram</th>
            <th>Custo IA</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.niche}</td>
              <td>{p.avatar}</td>
              <td>{p.instagramAccountId}</td>
              <td>{p.aiCost}</td>
            </tr>
          ))}
        </tbody>
        </table>
      </div>
    </div>
  );
}
