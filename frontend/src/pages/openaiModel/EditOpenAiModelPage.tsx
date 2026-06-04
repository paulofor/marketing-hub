import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useOpenAiModel } from "../../api/openAiModel/useOpenAiModel";
import { useUpdateOpenAiModel } from "../../api/openAiModel/useUpdateOpenAiModel";
import PageTitle from "../../components/PageTitle";
import OpenAiModelForm, { OpenAiModelFormValues } from "./OpenAiModelForm";

function toFormValues(
  model?: ReturnType<typeof useOpenAiModel>["data"],
): OpenAiModelFormValues {
  return {
    name: model?.name ?? "",
    code: model?.code ?? "",
    priceInputStandard: model?.priceInputStandard?.toString() ?? "",
    priceInputCachedStandard: model?.priceInputCachedStandard?.toString() ?? "",
    priceOutputStandard: model?.priceOutputStandard?.toString() ?? "",
    priceInputBatch: model?.priceInputBatch?.toString() ?? "",
    priceInputCachedBatch: model?.priceInputCachedBatch?.toString() ?? "",
    priceOutputBatch: model?.priceOutputBatch?.toString() ?? "",
    acceptsImageInput: model?.acceptsImageInput ?? false,
  };
}

function toPayload(values: OpenAiModelFormValues, id: number) {
  return {
    id,
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

export default function EditOpenAiModelPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading } = useOpenAiModel(id);
  const update = useUpdateOpenAiModel();

  const formValues = useMemo(() => toFormValues(data), [data]);

  if (isLoading || !id) return <p>Carregando...</p>;
  if (!data) return <p>Modelo não encontrado.</p>;

  const handleSubmit = (values: OpenAiModelFormValues) => {
    update.mutate(toPayload(values, Number(id)), {
      onSuccess: () => navigate("/openai-models"),
    });
  };

  return (
    <div>
      <PageTitle>Editar modelo da OpenAI</PageTitle>
      <OpenAiModelForm
        initialValues={formValues}
        onSubmit={handleSubmit}
        isSubmitting={update.isPending}
      />
    </div>
  );
}
