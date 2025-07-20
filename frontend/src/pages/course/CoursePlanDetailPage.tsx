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
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  return (
    <div>
      <PageTitle>{`Plano de Curso ${id ?? ''}`}</PageTitle>
      <h3>Módulos</h3>
      <pre>{data.modules}</pre>
      <h3>Objetivos</h3>
      <pre>{data.objectives}</pre>
      <h3>Recursos</h3>
      <pre>{data.resources}</pre>
    </div>
  );
}
