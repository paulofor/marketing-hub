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
  const angleMap = new Map<number, string>(
    Array.isArray(angles) ? angles.map((a) => [a.id, a.name]) : [],
  );
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: niche?.name || "...", to: `/niches/${nicheId}` },
    { label: data?.title || "..." },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const list = Array.isArray(experiments) ? experiments : [];
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
      <dl className="row">
        <dt className="col-sm-3">Ângulo</dt>
        <dd className="col-sm-9">
          {angleMap.get(data.premiseAngleId ?? 0) || "-"}
        </dd>
        <dt className="col-sm-3">Promessa</dt>
        <dd className="col-sm-9">{data.promise || "-"}</dd>
        <dt className="col-sm-3">Problema</dt>
        <dd className="col-sm-9">{data.problem || "-"}</dd>
        <dt className="col-sm-3">Persona</dt>
        <dd className="col-sm-9">{data.persona || "-"}</dd>
        <dt className="col-sm-3">Regra de sucesso</dt>
        <dd className="col-sm-9">{data.successRule || "-"}</dd>
        <dt className="col-sm-3">Oferta</dt>
        <dd className="col-sm-9">
          {data.offerType === "TRIPWIRE" ? "Tripwire" : "Lead Magnet"}
        </dd>
        {data.offerType === "TRIPWIRE" && (
          <>
            <dt className="col-sm-3">Preço</dt>
            <dd className="col-sm-9">R$ {data.price ?? ""}</dd>
          </>
        )}
        <dt className="col-sm-3">KPI</dt>
        <dd className="col-sm-9">{data.kpiTargetCpl}</dd>
        <dt className="col-sm-3">Status</dt>
        <dd className="col-sm-9">{data.status}</dd>
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
    </div>
  );
}
