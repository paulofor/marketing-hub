import { Link } from "react-router-dom";
import { useSuccessProducts } from "../../api/successProduct/useSuccessProducts";
import PageTitle from "../../components/PageTitle";

export default function SuccessProductListPage() {
  const { data, isLoading } = useSuccessProducts();
  if (isLoading) return <p>Loading...</p>;
  return (
    <div>
      <PageTitle>Success Products</PageTitle>
      <Link className="btn btn-primary mb-3" to="/success-products/new">
        New Success Product
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Description</th>
            <th>New</th>
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
