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
    { label: "Entrega", value: data.entrega },
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

  const handleSaveMarkdown = () => {
    const nicheMd =
      `# Nicho: ${niche?.name ?? ""}\n\n` +
      `**ID:** ${niche?.id ?? ""}\n\n` +
      `**Descrição:**\n${niche?.description ?? ""}\n\n` +
      `**Volume de Demanda:**\n${niche?.demandVolume ?? ""}\n\n` +
      `**Promessas:**\n${niche?.promises ?? ""}\n\n` +
      `**Ofertas:**\n${niche?.offers ?? ""}\n\n` +
      `**Segmentação-base (Brasil):**\n${niche?.baseSegmentation ?? ""}\n\n` +
      `**Principais interesses / comportamentos:**\n${niche?.interests ?? ""}\n\n` +
      `**Filtros demográficos & cargos:**\n${niche?.demographicFilters ?? ""}\n\n` +
      `**Dicas extras:**\n${niche?.extraTips ?? ""}\n`;
    const hypothesisMd =
      `# Hipótese: ${data.title}\n\n` +
      `**Promessa:**\n${data.promise ?? ""}\n\n` +
      `**Problema:**\n${data.problem ?? ""}\n\n` +
      `**Persona:**\n${data.persona ?? ""}\n\n` +
      `**Mecanismo:**\n${data.mechanism ?? ""}\n\n` +
      `**Mecanismo único:**\n${data.uniqueMechanism ?? ""}\n\n` +
      `**Entrega:**\n${data.entrega ?? ""}\n\n` +
      `**Regra de sucesso:**\n${data.successRule ?? ""}\n\n` +
      `**Ângulo:**\n${angleName ?? ""}\n\n` +
      `**Oferta:**\n${
        data.offerType === "TRIPWIRE"
          ? `Tripwire R$ ${data.price ?? ""}`
          : "Lead Magnet"
      }\n\n` +
      `**KPI:**\n${data.kpiTargetCpl ?? ""}\n\n` +
      `**Status:**\n${data.status ?? ""}\n`;
    const md = `${nicheMd}\n\n${hypothesisMd}`;
    const blob = new Blob([md], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${niche?.name ?? "nicho"}-${data.title}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };
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
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={handleSaveMarkdown}
          >
            Salvar em Markdown
          </button>
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
          <pre className="text-break" style={{ whiteSpace: "pre-wrap" }}>
            {data.prompt}
          </pre>
        </div>
      )}
    </div>
  );
}
