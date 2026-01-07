import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useDifferentiatedTechnology } from "../../api/differentiatedTechnology/useDifferentiatedTechnology";
import { useUpdateDifferentiatedTechnology } from "../../api/differentiatedTechnology/useUpdateDifferentiatedTechnology";
import PageTitle from "../../components/PageTitle";
import DifferentiatedTechnologyForm, {
  DifferentiatedTechnologyFormState,
} from "./DifferentiatedTechnologyForm";

export default function EditDifferentiatedTechnologyPage() {
  const { id } = useParams<{ id: string }>();
  const technologyId = Number(id);
  const navigate = useNavigate();
  const { data, isLoading } = useDifferentiatedTechnology(technologyId);
  const update = useUpdateDifferentiatedTechnology(technologyId);

  const initialValues = useMemo<DifferentiatedTechnologyFormState>(() => ({
    name: data?.name || "",
    description: data?.description || "",
    promptText: data?.promptText || "",
  }), [data]);

  const handleSubmit = (values: DifferentiatedTechnologyFormState) => {
    update.mutate(values, {
      onSuccess: () => navigate("/differentiated-technologies"),
    });
  };

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p className="text-danger">Tecnologia não encontrada.</p>;

  return (
    <div>
      <PageTitle>Editar tecnologia diferenciada</PageTitle>
      <DifferentiatedTechnologyForm
        initialValues={initialValues}
        onSubmit={handleSubmit}
        isSubmitting={update.isPending}
        submitLabel="Salvar alterações"
      />
    </div>
  );
}
