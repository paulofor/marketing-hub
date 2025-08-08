import { useParams } from "react-router-dom";
import FunnelBuilder from "../../components/FunnelBuilder";
import { useFunnel } from "../../api/funnel/useFunnel";

export default function EditFunnelPage() {
  const { id } = useParams();
  const { data, isLoading } = useFunnel(id!);
  if (isLoading || !data) return <p>Carregando...</p>;
  const steps = [...(data.steps ?? [])]
    .map((s, index) => ({ ...s, _idx: index }))
    .sort((a, b) => {
      const ao =
        typeof a.orderIdx === "number" && !Number.isNaN(a.orderIdx)
          ? a.orderIdx
          : Number.MAX_SAFE_INTEGER;
      const bo =
        typeof b.orderIdx === "number" && !Number.isNaN(b.orderIdx)
          ? b.orderIdx
          : Number.MAX_SAFE_INTEGER;
      if (ao !== bo) return ao - bo;
      const idCmp = String(a.id).localeCompare(String(b.id));
      if (idCmp !== 0) return idCmp;
      return a._idx - b._idx;
    })
    .map((s) => ({
      id: s.id.toString(),
      backendId: s.id.toString(),
      stimulus_type: s.stimulusType,
      expected_action: s.expectedAction,
      score_inc: s.scoreInc ?? 0,
    }));
  return <FunnelBuilder funnel={{ id: data.id, name: data.name, steps }} />;
}
