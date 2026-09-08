import { useSearchParams } from "react-router-dom";
import { useLearningCycleEntry } from "../../api/learningCycle/useLearningCycles";
import LearningCycleBlueprint from "./LearningCycleBlueprint";

export default function LearningCycleProcessEntry({
  processDefinitionId,
  productId,
}: {
  processDefinitionId?: number;
  productId?: number;
}) {
  const [params] = useSearchParams();
  const entry = useLearningCycleEntry(
    processDefinitionId,
    productId ?? (Number(params.get("productId")) || undefined),
    Number(params.get("chainId")) || undefined,
  );
  if (entry.isError)
    return (
      <p className="alert alert-warning" role="alert">
        Não foi possível consultar o vínculo deste processo com o ciclo.
        Atualize a página para recuperar a orientação oficial.
      </p>
    );
  if (!entry.data) return null;
  return <LearningCycleBlueprint entry={entry.data} />;
}
