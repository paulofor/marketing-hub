import { useHypothesisBoard } from "../../api/hypothesis/useHypothesisBoard";
import HypothesisBoard from "./HypothesisBoard";
import NewHypothesisModal from "./NewHypothesisModal";
import { useSearchParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useState } from "react";
import Button from "../../components/ui/Button";

export default function HypothesesPage() {
  const [params] = useSearchParams();
  const nicheId = params.get("nicheId") ?? "1";
  const { data } = useHypothesisBoard(nicheId);
  const [open, setOpen] = useState(params.get("open") === "new");
  return (
    <div>
      <PageTitle icon={hypothesisIcon}>Hipóteses</PageTitle>
      <div className="mb-3">
        <Button onClick={() => setOpen(true)}>Nova Hipótese</Button>
      </div>
      <NewHypothesisModal
        marketNicheId={nicheId}
        open={open}
        onOpenChange={setOpen}
      />
      {data && <HypothesisBoard board={data} nicheId={nicheId} />}
    </div>
  );
}
