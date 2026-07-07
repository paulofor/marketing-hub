import { useHypothesisBoard } from "../../api/hypothesis/useHypothesisBoard";
import HypothesisBoard from "./HypothesisBoard";
import { useSearchParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";

export default function HypothesesPage() {
  const [params] = useSearchParams();
  const nicheId = params.get("nicheId") ?? "1";
  const { data } = useHypothesisBoard(nicheId);
  return (
    <div>
      <PageTitle icon={hypothesisIcon}>Hipóteses</PageTitle>
      {data && <HypothesisBoard board={data} nicheId={nicheId} />}
    </div>
  );
}
