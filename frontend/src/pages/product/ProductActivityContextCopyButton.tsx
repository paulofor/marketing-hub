import ProductContextCopyButton from "./ProductContextCopyButton";
import {
  activityContext,
  type ActivityContextProps,
} from "./productActivityContext";

/** Copia a identidade versionada da atividade usando o mesmo controle HTTP do processo. */
export default function ProductActivityContextCopyButton(
  props: ActivityContextProps,
) {
  const number = `${props.processSequence ? `${props.processSequence}.` : ""}${props.activity.sequenceNumber}`;
  return (
    <ProductContextCopyButton
      text={activityContext(props)}
      label={`Copiar contexto da atividade ${number}`}
      manualLabel={`Contexto da atividade ${number} para copiar manualmente`}
      loading={props.loading}
    />
  );
}
