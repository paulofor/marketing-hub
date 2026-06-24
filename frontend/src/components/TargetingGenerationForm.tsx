import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { TargetingElementType } from "../api/targeting/types";
import { useRequestTargetingElements } from "../api/targeting/useRequestTargetingElements";
import type { OpenAiModel } from "../api/openAiModel/useOpenAiModels";

interface TargetingGenerationFormProps {
  nicheId: number;
  type: TargetingElementType;
  openAiModels?: OpenAiModel[];
  defaultModel?: string;
  requestedTotal?: number | null;
  isLoadingModels?: boolean;
  isFetchingStatus?: boolean;
  ctaLabel?: string;
  statusLabel?: string;
  className?: string;
}

interface FormValues {
  quantity: number;
  model?: string;
}

export function TargetingGenerationForm({
  nicheId,
  type,
  openAiModels,
  defaultModel,
  requestedTotal,
  isLoadingModels,
  isFetchingStatus,
  ctaLabel = "Gerar",
  statusLabel,
  className,
}: TargetingGenerationFormProps) {
  const resolvedDefaultModel = defaultModel ?? openAiModels?.[0]?.code ?? "";
  const request = useRequestTargetingElements(nicheId, type);
  const { register, handleSubmit, reset, setValue } = useForm<FormValues>({
    defaultValues: {
      quantity: 1,
      model: resolvedDefaultModel,
    },
  });

  useEffect(() => {
    setValue("model", resolvedDefaultModel);
  }, [resolvedDefaultModel, setValue]);

  const onSubmit = handleSubmit(async ({ quantity, model }) => {
    if (!quantity || quantity <= 0) return;
    const trimmedModel = model?.trim() || resolvedDefaultModel;
    try {
      await request.mutateAsync({ quantity, model: trimmedModel });
      reset({ quantity: 1, model: trimmedModel });
    } catch (error) {
      console.error("Erro ao solicitar elementos de segmentação", error);
      alert("Não foi possível solicitar este lote. Tente novamente.");
    }
  });

  const cancelRequest = async () => {
    try {
      await request.mutateAsync({ quantity: 0 });
      reset({ quantity: 1, model: resolvedDefaultModel });
    } catch (error) {
      console.error("Erro ao cancelar solicitação de segmentação", error);
      alert("Não foi possível cancelar esta solicitação. Tente novamente.");
    }
  };

  const pendingTotal = requestedTotal ?? 0;
  const hasPendingRequest = pendingTotal > 0;
  const isSubmitting = request.isPending || Boolean(isLoadingModels);
  const progressStatus = hasPendingRequest ? "Aguardando Worker" : "Fila livre";
  const statusMessage =
    statusLabel ??
    (hasPendingRequest
      ? `Há ${pendingTotal} item(ns) solicitado(s). Esta tela atualiza automaticamente a cada 15 segundos até o Worker concluir.`
      : "Nenhuma solicitação pendente no Worker.");

  return (
    <form
      onSubmit={onSubmit}
      className={`d-flex flex-column gap-2 ${className ?? ""}`}
    >
      <div className="d-flex flex-wrap gap-2">
        <input
          type="number"
          min={1}
          className="form-control w-auto"
          style={{ minWidth: 120 }}
          placeholder="Qtd."
          title="Quantidade solicitada ao Worker IA"
          {...register("quantity", { valueAsNumber: true })}
        />
        <select
          className="form-select w-auto"
          style={{ minWidth: 200 }}
          {...register("model")}
          disabled={isLoadingModels}
        >
          {(openAiModels ?? []).map((modelOption) => (
            <option key={modelOption.code} value={modelOption.code}>
              {modelOption.name}
            </option>
          ))}
        </select>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting}
        >
          {isSubmitting && (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          )}
          {ctaLabel}
        </button>
        {hasPendingRequest ? (
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={isSubmitting}
            onClick={cancelRequest}
          >
            Cancelar pendência
          </button>
        ) : null}
      </div>
      <div
        className={`border rounded-3 p-2 small ${
          hasPendingRequest
            ? "border-warning-subtle bg-warning-subtle"
            : "bg-light"
        }`}
        aria-live="polite"
      >
        <div className="d-flex justify-content-between gap-2 flex-wrap">
          <span className="fw-semibold">{progressStatus}</span>
          <span>{pendingTotal} pendente(s)</span>
        </div>
        <div
          className={
            hasPendingRequest ? "text-warning-emphasis" : "text-body-secondary"
          }
        >
          {isFetchingStatus ? "Atualizando solicitações..." : statusMessage}
        </div>
        {hasPendingRequest ? (
          <div className="text-body-secondary mt-1">
            Nova solicitação antes da conclusão substitui a quantidade pendente
            atual; não soma automaticamente.
          </div>
        ) : null}
      </div>
    </form>
  );
}
