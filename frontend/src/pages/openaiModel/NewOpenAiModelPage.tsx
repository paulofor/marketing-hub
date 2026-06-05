import { useNavigate } from "react-router-dom";
import { useCreateOpenAiModel } from "../../api/openAiModel/useCreateOpenAiModel";
import PageTitle from "../../components/PageTitle";
import OpenAiModelForm, { OpenAiModelFormValues } from "./OpenAiModelForm";

function toPayload(values: OpenAiModelFormValues) {
  return {
    name: values.name.trim(),
  };
}

export default function NewOpenAiModelPage() {
  const navigate = useNavigate();
  const create = useCreateOpenAiModel();

  const handleSubmit = (values: OpenAiModelFormValues) => {
    create.mutate(toPayload(values), {
      onSuccess: () => navigate("/openai-models"),
    });
  };

  return (
    <div>
      <PageTitle>Novo modelo da OpenAI</PageTitle>
      <OpenAiModelForm
        onSubmit={handleSubmit}
        isSubmitting={create.isPending}
        submitLabel="Buscar na OpenAI e salvar"
        nameOnly
      />
    </div>
  );
}
