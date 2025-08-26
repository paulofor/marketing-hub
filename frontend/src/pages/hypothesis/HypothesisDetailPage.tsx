import { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useExperimentsByHypothesis } from "../../api/experiment/useExperimentsByHypothesis";
import { useAngles } from "../../api/angle/useAngles";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";

export default function HypothesisDetailPage() {
  const { nicheId, hypothesisId } = useParams();
  const { data: niche } = useNiche(Number(nicheId));
  const { data, isLoading } = useHypothesis(nicheId, hypothesisId);
  const { data: experiments } = useExperimentsByHypothesis(
    nicheId,
    hypothesisId,
  );
  const { data: angles } = useAngles();
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: niche?.name || "...", to: `/niches/${nicheId}` },
    { label: data?.title || "..." },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const list = Array.isArray(experiments) ? experiments : [];
  const angleName = angles?.find((a) => a.id === data.premiseAngleId)?.name;
  const rows = [
    { label: "Promessa", value: data.promise },
    { label: "Problema", value: data.problem },
    { label: "Persona", value: data.persona },
    { label: "Mecanismo", value: data.mechanism },
    { label: "Mecanismo único", value: data.uniqueMechanism },
    { label: "Regra de sucesso", value: data.successRule },
    { label: "Ângulo", value: angleName },
    {
      label: "Oferta",
      value:
        data.offerType === "TRIPWIRE"
          ? `Tripwire R$ ${data.price ?? ""}`
          : "Lead Magnet",
    },
    { label: "KPI", value: data.kpiTargetCpl },
    { label: "Status", value: data.status },
  ];
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center">
        <PageTitle>{data.title}</PageTitle>
        <div className="d-flex gap-2">
          {data.status === "BACKLOG" && (
            <Link
              className="btn btn-outline-secondary"
              to={`/niches/${nicheId}/hypotheses/${hypothesisId}/edit`}
            >
              Editar
            </Link>
          )}
          <Link
            className="btn btn-primary"
            to={`/experiments/new?nicheId=${nicheId}&hypothesisId=${hypothesisId}`}
          >
            Criar Experimento
          </Link>
        </div>
      </div>
      <dl className="row mb-0">
        {rows.map((r, idx) => (
          <Fragment key={r.label}>
            <dt className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}>
              {r.label}
            </dt>
            <dd className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}>
              {r.value}
            </dd>
          </Fragment>
        ))}
      </dl>
      {list.length === 0 ? (
        <p>Nenhum experimento ainda. Crie um agora.</p>
      ) : (
        <div className="table-responsive">
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Plataforma</th>
                <th>Status</th>
                <th>KPI</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.map((e) => (
                <tr key={e.id}>
                  <td>{e.name}</td>
                  <td>{e.platform}</td>
                  <td>{e.status}</td>
                  <td>{e.kpiTarget}</td>
                  <td>
                    <Link
                      className="btn btn-sm btn-outline-primary"
                      to={`/experiments/${e.id}`}
                    >
                      Abrir
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data.createdAt && (
        <div className="mt-4">
          <h5>Data de criação</h5>
          <p>{new Date(data.createdAt).toLocaleString("pt-BR")}</p>
        </div>
      )}
      {data.prompt && (
        <div className="mt-4">
          <h5>Prompt de criação</h5>
          <pre>{data.prompt}</pre>
        </div>
      )}
    </div>
  );
}
