import { Link } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";

export default function ProductListPage() {
  const { data, isLoading } = useProducts();
  if (isLoading) return <p>Loading...</p>;
  return (
    <div>
      <Link className="btn btn-primary mb-3" to="/products/new">
        New Product
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Niche</th>
            <th>Avatar</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.niche}</td>
              <td>{p.avatar}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
