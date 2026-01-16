import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { usePromptDomainObjects } from "../../api/promptDomain/usePromptDomainObjects";
import { useCreatePromptDomain } from "../../api/promptDomain/useCreatePromptDomain";
import PromptDomainForm, { PromptDomainFormValues } from "./PromptDomainForm";

export default function NewPromptDomainPage() {
  const navigate = useNavigate();
  const { data: objects, isLoading } = usePromptDomainObjects();
  const createDomain = useCreatePromptDomain();

  const handleSubmit = async (values: PromptDomainFormValues) => {
    await createDomain.mutateAsync({
      code: values.code,
      name: values.name,
      description: values.description,
      objects: values.objects,
    });
    toast.success("Domínio criado com sucesso");
    navigate("/prompt-domains");
  };

  return (
    <div className="d-flex flex-column gap-3" style={{ maxWidth: 640 }}>
      <PageTitle>Novo domínio de prompt</PageTitle>
      <PromptDomainForm
        objects={objects}
        isLoadingObjects={isLoading}
        isSubmitting={createDomain.isPending}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
