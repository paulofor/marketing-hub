import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { usePrompt } from "../../api/promptTemplate/usePrompt";
import { useUpdatePrompt } from "../../api/promptTemplate/useUpdatePrompt";
import { usePromptDomains } from "../../api/promptDomain/usePromptDomains";
import PageTitle from "../../components/PageTitle";
import PromptForm, { PromptFormValues } from "./PromptForm";

export default function EditPromptPage() {
  const { id } = useParams();
  const promptId = id ?? "";
  const { data, isLoading } = usePrompt(promptId);
  const { data: domains, isLoading: isLoadingDomains } = usePromptDomains();
  const updatePrompt = useUpdatePrompt(promptId);
  const navigate = useNavigate();

  async function handleSubmit(values: PromptFormValues) {
    await updatePrompt.mutateAsync(values);
    toast.success("Prompt atualizado");
    navigate("/prompts");
  }

  if (!promptId) return <p>Prompt não encontrado.</p>;
  if (isLoading || isLoadingDomains) return <p>Carregando...</p>;
  if (!data) return <p>Prompt não encontrado.</p>;

  return (
    <div className="d-flex flex-column gap-3">
      <PageTitle>Editar prompt</PageTitle>
      <PromptForm
        domains={domains ?? []}
        initialValues={{
          name: data.name,
          domain: data.domain,
          template: data.template,
          active: data.active,
        }}
        isSubmitting={updatePrompt.isPending}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
