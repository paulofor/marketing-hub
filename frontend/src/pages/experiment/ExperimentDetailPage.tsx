import { Fragment } from "react";
import { useParams } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import PageTitle from "../../components/PageTitle";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = Number(id);
  const { data, isLoading } = useExperiment(expId);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const rows = [
    { label: "Hipótese", value: data.hypothesis },
    { label: "KPI", value: data.kpiGoal },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
    { label: "Status", value: data.status },
  ];

  return (
    <div>
      <PageTitle>Teste {data.id}</PageTitle>
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
    </div>
  );
}
