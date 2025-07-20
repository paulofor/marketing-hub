import { Fragment, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useNiche } from "../../api/niche/useNiche";
import PageTitle from "../../components/PageTitle";
import CriativosTab from "../Experiment/CriativosTab";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const { data, isLoading } = useExperiment(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);

  const [tab, setTab] = useState("overview");
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const rows = [
    { label: "Nome", value: data.name },
    {
      label: "Nicho",
      value: <Link to={`/niches/${data.nicheId}/edit`}>{niche?.name}</Link>,
    },
    { label: "Hipótese", value: data.hypothesis },
    { label: "KPI", value: data.kpiTarget },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
  ];
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center">
        <PageTitle>{`Teste ${data.id}`}</PageTitle>
        <span className="badge bg-secondary">{data.status}</span>
      </div>
      <ul className="nav nav-tabs mt-3">
        <li className="nav-item">
          <button
            className={`nav-link${tab === "overview" ? " active" : ""}`}
            onClick={() => setTab("overview")}
          >
            Overview
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link${tab === "creatives" ? " active" : ""}`}
            onClick={() => setTab("creatives")}
          >
            Criativos
          </button>
        </li>
        <li className="nav-item">
          <span className="nav-link">Públicos</span>
        </li>
        <li className="nav-item">
          <span className="nav-link">Métricas</span>
        </li>
      </ul>
      {tab === "overview" && (
        <div className="card">
          <div className="card-body p-0">
            <dl className="row mb-0">
              {rows.map((r, idx) => (
                <Fragment key={r.label}>
                  <dt
                    className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                  >
                    {r.label}
                  </dt>
                  <dd
                    className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                  >
                    {r.value}
                  </dd>
                </Fragment>
              ))}
            </dl>
          </div>
        </div>
      )}
      {tab === "creatives" && <CriativosTab experimentId={expId} />}
    </div>
  );
}
