import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { CoursePlan } from "../../api/course/useCoursePlans";
import PageTitle from "../../components/PageTitle";

export default function CoursePlanDetailPage() {
  const { id } = useParams();
  const { data, isLoading } = useQuery({
    queryKey: ["coursePlan", id],
    queryFn: async () => {
      const { data } = await axios.get<CoursePlan>(`/api/courses/${id}`);
      return data;
    },
  });
  if (isLoading) return <p>Loading...</p>;
  if (!data) return <p>Not found</p>;
  return (
    <div>
      <PageTitle>Course Plan {id}</PageTitle>
      <h3>Modules</h3>
      <pre>{data.modules}</pre>
      <h3>Objectives</h3>
      <pre>{data.objectives}</pre>
      <h3>Resources</h3>
      <pre>{data.resources}</pre>
    </div>
  );
}
