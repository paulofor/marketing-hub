import { useParams } from "react-router-dom";
import FunnelBuilder from "../../components/FunnelBuilder";
import { useFunnel } from "../../api/funnel/useFunnel";

export default function EditFunnelPage() {
  const { id } = useParams();
  const { data, isLoading } = useFunnel(id!);
  if (isLoading || !data) return <p>Carregando...</p>;
  const steps =
    data.steps?.map((s) => ({
      id: s.id,
      backendId: s.id,
      stimulus_type: s.stimulusType,
      expected_action: s.expectedAction,
      score_inc: s.scoreInc ?? 0,
    })) ?? [];
  return <FunnelBuilder funnel={{ id: data.id, name: data.name, steps }} />;
}
