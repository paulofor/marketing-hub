import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const { data, isLoading } = useNiche(Number(nicheId));
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: data?.name || "..." },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const list = Array.isArray(hypotheses) ? hypotheses : [];
  return (
    <div>
      <PageTitle>{data.name}</PageTitle>
      {list.length === 0 ? (
        <p>Nenhuma hipótese ainda.</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-4">
          {list.map((h) => (
            <div key={h.id} className="col">
              <div className="card h-100">
                <div className="card-body">
                  <h5 className="card-title">{h.title}</h5>
                  <p className="card-text">
                    <strong>Promessa:</strong> {h.promise || "-"}
                  </p>
                  <p className="card-text">
                    <strong>Problema:</strong> {h.problem || "-"}
                  </p>
                  <p className="card-text">
                    <strong>Mecanismo:</strong> {h.mechanism || "-"}
                  </p>
                  <p className="card-text">
                    <strong>Mecanismo único:</strong> {h.uniqueMechanism || "-"}
                  </p>
                  <p className="card-text">
                    <strong>Persona:</strong> {h.persona || "-"}
                  </p>
                  <Link
                    className="btn btn-sm btn-outline-primary mt-2"
                    to={`hypotheses/${h.id}`}
                  >
                    Ver detalhes
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
