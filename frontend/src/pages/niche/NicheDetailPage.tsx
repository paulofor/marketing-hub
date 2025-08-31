import { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const { data, isLoading } = useNiche(Number(nicheId));
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: data?.name || "..." },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const handleSaveMarkdown = () => {
    const md = `# Nicho: ${data.name}\n\n` +
      `**ID:** ${data.id}\n\n` +
      `**Descrição:**\n${data.description}\n\n` +
      `**Volume de Demanda:**\n${data.demandVolume}\n\n` +
      `**Promessas:**\n${data.promises}\n\n` +
      `**Ofertas:**\n${data.offers}\n\n` +
      `**Segmentação-base (Brasil):**\n${data.baseSegmentation}\n\n` +
      `**Principais interesses / comportamentos:**\n${data.interests}\n\n` +
      `**Filtros demográficos & cargos:**\n${data.demographicFilters}\n\n` +
      `**Dicas extras:**\n${data.extraTips}\n`;
    const blob = new Blob([md], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${data.name}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const list = Array.isArray(hypotheses)
    ? [...hypotheses].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const rows = [
    { label: "Descrição", value: data.description },
    { label: "Volume de demanda", value: data.demandVolume },
    { label: "Promessas", value: data.promises },
    { label: "Ofertas", value: data.offers },
    { label: "Hipóteses a gerar", value: data.hypothesesToGenerate },
    { label: "Segmentação base", value: data.baseSegmentation },
    { label: "Interesses", value: data.interests },
    { label: "Filtros demográficos", value: data.demographicFilters },
    { label: "Dicas extras", value: data.extraTips },
    {
      label: "Chat Dialog",
      value: chatDialog ? (
        <a href={chatDialog.url} target="_blank" rel="noopener noreferrer">
          {chatDialog.description}
        </a>
      ) : undefined,
    },
    {
      label: "Criado em",
      value: data.createdAt
        ? new Date(data.createdAt).toLocaleString("pt-BR")
        : undefined,
    },
    {
      label: "Atualizado em",
      value: data.updatedAt
        ? new Date(data.updatedAt).toLocaleString("pt-BR")
        : undefined,
    },
  ];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center">
        <PageTitle>{data.name}</PageTitle>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          onClick={handleSaveMarkdown}
        >
          Salvar em Markdown
        </button>
      </div>
      <dl className="row mb-4">
        {rows.map((r, idx) => (
          <Fragment key={r.label}>
            <dt className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}>
              {r.label}
            </dt>
            <dd className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}>
              <span className="text-break" style={{ whiteSpace: "pre-wrap" }}>
                {r.value ?? "-"}
              </span>
            </dd>
          </Fragment>
        ))}
      </dl>
      {list.length === 0 ? (
        <p>Nenhuma hipótese ainda.</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-4">
          {list.map((h) => (
            <div key={h.id} className="col">
              <div className="card h-100 rounded-3">
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
                <div className="card-footer text-muted">
                  {`Gerado com ${h.model || "-"} em ${h.createdAt ? new Date(h.createdAt).toLocaleString("pt-BR") : "-"}`}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
