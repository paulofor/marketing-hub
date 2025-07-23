import { useHypothesisBoard } from "../../api/hypothesis/useHypothesisBoard";
import HypothesisBoard from "./HypothesisBoard";
import NewHypothesisModal from "./NewHypothesisModal";
import { useSearchParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";

export default function HypothesesPage() {
  const [params] = useSearchParams();
  const experimentId = params.get("experimentId") ?? "1";
  const { data } = useHypothesisBoard(experimentId);
  return (
    <div>
      <PageTitle>Hipóteses</PageTitle>
      <NewHypothesisModal experimentId={experimentId} />
      {data && <HypothesisBoard board={data} experimentId={experimentId} />}
    </div>
  );
}
