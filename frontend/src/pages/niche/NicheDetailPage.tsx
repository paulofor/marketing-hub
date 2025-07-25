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
      <div className="d-flex justify-content-between align-items-center">
        <PageTitle>{data.name}</PageTitle>
        <Link className="btn btn-primary" to="hypotheses/new">
          Nova Hipótese
        </Link>
      </div>
      {list.length === 0 ? (
        <p>Nenhuma hipótese ainda. <Link to="hypotheses/new">Crie uma agora</Link>.</p>
      ) : (
        <div className="table-responsive">
          <table className="table">
            <thead>
              <tr>
                <th>Título</th>
                <th>Oferta</th>
                <th>Status</th>
                <th>KPI</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.map((h) => (
                <tr key={h.id}>
                  <td>{h.title}</td>
                  <td>
                    {h.offerType === "TRIPWIRE"
                      ? `Tripwire R$ ${h.price ?? ""}`
                      : "Lead Magnet"}
                  </td>
                  <td>{h.status}</td>
                  <td>{h.kpiTargetCpl}</td>
                  <td>
                    <Link
                      className="btn btn-sm btn-outline-primary"
                      to={`hypotheses/${h.id}`}
                    >
                      Ver detalhes
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
