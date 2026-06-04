import { useNavigate } from "react-router-dom";
import { useCreateOpenAiModel } from "../../api/openAiModel/useCreateOpenAiModel";
import PageTitle from "../../components/PageTitle";
import OpenAiModelForm, { OpenAiModelFormValues } from "./OpenAiModelForm";

function toPayload(values: OpenAiModelFormValues) {
  return {
    name: values.name,
    code: values.code,
    priceInputStandard: Number(values.priceInputStandard || 0),
    priceInputCachedStandard: Number(values.priceInputCachedStandard || 0),
    priceOutputStandard: Number(values.priceOutputStandard || 0),
    priceInputBatch: Number(values.priceInputBatch || 0),
    priceInputCachedBatch: Number(values.priceInputCachedBatch || 0),
    priceOutputBatch: Number(values.priceOutputBatch || 0),
    acceptsImageInput: values.acceptsImageInput,
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
      />
    </div>
  );
}
