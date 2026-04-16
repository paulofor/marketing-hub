import { useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";

export default function OprmRoutinePlaceholderPage() {
  const { occupationSeedRef } = useParams();

  return (
    <div className="d-flex flex-column gap-3">
      <PageTitle>OPRM · Rotina</PageTitle>
      <p className="text-secondary mb-0">
        A visualização da rotina será entregue na Sprint UI-2. Ocupação selecionada:
        <strong> {occupationSeedRef ?? "não informada"}</strong>.
      </p>
    </div>
  );
}
