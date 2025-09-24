import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import HypothesisList from "./HypothesisList";

export default function HypothesisListPage() {
  return (
    <div>
      <PageTitle icon={hypothesisIcon}>Hipóteses</PageTitle>
      <HypothesisList />
    </div>
  );
}
