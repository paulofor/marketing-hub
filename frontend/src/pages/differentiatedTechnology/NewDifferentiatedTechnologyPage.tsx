import { useNavigate } from "react-router-dom";
import { useCreateDifferentiatedTechnology } from "../../api/differentiatedTechnology/useCreateDifferentiatedTechnology";
import PageTitle from "../../components/PageTitle";
import DifferentiatedTechnologyForm, {
  DifferentiatedTechnologyFormState,
} from "./DifferentiatedTechnologyForm";

export default function NewDifferentiatedTechnologyPage() {
  const navigate = useNavigate();
  const create = useCreateDifferentiatedTechnology();

  const handleSubmit = (values: DifferentiatedTechnologyFormState) => {
    create.mutate(values, {
      onSuccess: () => navigate("/differentiated-technologies"),
    });
  };

  return (
    <div>
      <PageTitle>Nova tecnologia diferenciada</PageTitle>
      <DifferentiatedTechnologyForm
        initialValues={{ name: "", description: "", promptText: "" }}
        onSubmit={handleSubmit}
        isSubmitting={create.isPending}
        submitLabel="Salvar"
      />
    </div>
  );
}
