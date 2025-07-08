import { Link } from "react-router-dom";
import { useAssets } from "../../api/media/useAssets";
import PageTitle from "../../components/PageTitle";

export default function MediaListPage() {
  const { data, isLoading } = useAssets();
  if (isLoading) return <p>Loading...</p>;
  return (
    <div>
      <PageTitle>Media Assets</PageTitle>
      <Link className="btn btn-primary mb-3" to="/media/new">
        New Media
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((a) => (
            <tr key={a.id}>
              <td>{a.id}</td>
              <td>{a.type}</td>
              <td>{a.status}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/media/${a.id}`}
                >
                  View
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
