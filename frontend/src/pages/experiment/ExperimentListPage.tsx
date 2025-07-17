import { Link } from "react-router-dom";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useNiches } from "../../api/niche/useNiches";
import PageTitle from "../../components/PageTitle";
import { useState } from "react";

export default function ExperimentListPage() {
  const { data, isLoading } = useExperiments();
  const { data: niches } = useNiches();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [niche, setNiche] = useState("");
  const experiments = Array.isArray(data) ? data : [];
  const filtered = experiments.filter(
    (e) =>
      (!search || e.name.toLowerCase().includes(search.toLowerCase())) &&
      (!status || e.status === status) &&
      (!niche || e.nicheId === Number(niche)),
  );
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Testes de Nicho</PageTitle>
      <Link className="btn btn-primary mb-3" to="/experiments/new">
        Novo Teste
      </Link>
      <div className="row g-2 mb-3">
        <div className="col">
          <input
            className="form-control"
            placeholder="Buscar"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="col">
          <select className="form-select" value={niche} onChange={(e) => setNiche(e.target.value)}>
            <option value="">Todos Nichos</option>
            {Array.isArray(niches) &&
              niches.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.name}
                </option>
              ))}
          </select>
        </div>
        <div className="col">
          <select className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todos Status</option>
            <option value="PLANNED">PLANNED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="PAUSED">PAUSED</option>
            <option value="FINISHED">FINISHED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Nicho</th>
              <th>KPI alvo</th>
              <th>Status</th>
              <th>Início</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((e) => (
              <tr key={e.id}>
                <td>{e.name}</td>
                <td>{niches?.find((n) => n.id === e.nicheId)?.name}</td>
                <td>{e.kpiTarget}</td>
                <td>{e.status}</td>
                <td>{e.startDate}</td>
                <td>
                  <Link
                    className="btn btn-sm btn-outline-primary"
                    to={`/experiments/${e.id}`}
                  >
                    Visualizar
                  </Link>
                  <Link
                    className="btn btn-sm btn-outline-secondary ms-1"
                    to={`/experiments/${e.id}`}
                  >
                    Duplicar
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
