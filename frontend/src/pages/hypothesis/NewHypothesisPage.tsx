import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";

export default function NewHypothesisPage() {
  const { nicheId } = useParams();

  return (
    <div className="hypothesis-new-page">
      <PageTitle icon={hypothesisIcon}>Nova hipótese</PageTitle>
      <section className="card">
        <div className="card-body">
          <p className="text-muted mb-3">
            Tela reservada para construir a nova hipótese nos próximos passos.
          </p>
          {nicheId && (
            <p className="mb-3">
              <strong>Nicho recebido:</strong> #{nicheId}
            </p>
          )}
          <Link className="btn btn-outline-secondary" to="/niches">
            Voltar para nichos
          </Link>
        </div>
      </section>
    </div>
  );
}
