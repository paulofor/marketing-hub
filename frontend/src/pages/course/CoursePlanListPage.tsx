import { Link } from "react-router-dom";
import { useCoursePlans } from "../../api/course/useCoursePlans";
import PageTitle from "../../components/PageTitle";

export default function CoursePlanListPage() {
  const { data, isLoading } = useCoursePlans();
  if (isLoading) return <p>Loading...</p>;
  return (
    <div>
      <PageTitle>Course Plans</PageTitle>
      <Link className="btn btn-primary mb-3" to="/courses/new">
        New Course Plan
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Audience</th>
            <th>Transformation</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.targetAudience}</td>
              <td>{p.transformation}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/courses/${p.id}`}
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
