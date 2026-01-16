import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { usePromptDomain } from "../../api/promptDomain/usePromptDomain";
import { usePromptDomainObjects } from "../../api/promptDomain/usePromptDomainObjects";
import { useUpdatePromptDomain } from "../../api/promptDomain/useUpdatePromptDomain";
import PromptDomainForm, { PromptDomainFormValues } from "./PromptDomainForm";

export default function EditPromptDomainPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading } = usePromptDomain(id);
  const { data: objects, isLoading: isLoadingObjects } = usePromptDomainObjects();
  const updateDomain = useUpdatePromptDomain(id ?? "");

  if (!id) return <p>Domínio não encontrado.</p>;
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Domínio não encontrado.</p>;

  const handleSubmit = async (values: PromptDomainFormValues) => {
    await updateDomain.mutateAsync({
      name: values.name,
      description: values.description,
      objects: values.objects,
    });
    toast.success("Domínio atualizado");
    navigate("/prompt-domains");
  };

  return (
    <div className="d-flex flex-column gap-3" style={{ maxWidth: 640 }}>
      <PageTitle>Editar domínio</PageTitle>
      <PromptDomainForm
        initialValues={{
          code: data.code,
          name: data.name,
          description: data.description ?? "",
          objects: data.objects.map((object) => object.slug),
        }}
        objects={objects}
        isLoadingObjects={isLoadingObjects}
        isSubmitting={updateDomain.isPending}
        disableCode
        onSubmit={handleSubmit}
      />
    </div>
  );
}
