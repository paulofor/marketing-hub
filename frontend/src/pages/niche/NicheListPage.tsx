import { Link } from "react-router-dom";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useExperimentsByNiche } from "../../api/experiment/useExperimentsByNiche";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";

export default function NicheListPage() {
  useBreadcrumbs([{ label: "Nichos" }]);
  const { data, isLoading } = useNiches();
  const niches = Array.isArray(data) ? data : [];

  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Nichos de Mercado</PageTitle>
      <Link className="btn btn-primary mb-3" to="/niches/new">
        Novo Nicho
      </Link>
      {niches.length === 0 ? (
        <p>
          Nenhum nicho encontrado.{' '}
          <Link to="/niches/new">Crie um agora</Link>.
        </p>
      ) : (
        <div className="table-responsive">
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Hipóteses</th>
                <th>Experimentos</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {niches.map((n) => (
                <NicheRow key={n.id} niche={n} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function NicheRow({ niche }: { niche: { id: number; name: string } }) {
  const { data: hyps } = useHypothesesByNiche(String(niche.id), "ALL");
  const { data: exps } = useExperimentsByNiche(String(niche.id));
  return (
    <tr>
      <td>
        <Link to={`/niches/${niche.id}`}>{niche.name}</Link>
      </td>
      <td>{Array.isArray(hyps) ? hyps.length : 0}</td>
      <td>{Array.isArray(exps) ? exps.length : 0}</td>
      <td>
        <Link
          className="btn btn-sm btn-outline-secondary me-1"
          to={`/niches/${niche.id}/edit`}
        >
          Editar
        </Link>
      </td>
    </tr>
  );
}
