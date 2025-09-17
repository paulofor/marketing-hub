import { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";
import { useForm } from "react-hook-form";
import { useRequestAudiences } from "../../api/niche/useRequestAudiences";

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const id = Number(nicheId);
  const { data, isLoading, isFetching } = useNiche(id);
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  const { data: audiences } = useAudiencesByNiche(nicheId);
  const requestAudiences = useRequestAudiences(id);
  const { register, handleSubmit, reset } = useForm<{ quantity: number }>({
    defaultValues: { quantity: 1 },
  });
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: data?.name || "..." },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const handleSaveMarkdown = () => {
    const md =
      `# Nicho: ${data.name}\n\n` +
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
  const audienceList = Array.isArray(audiences) ? audiences : [];
  const approvedAudienceCount = audienceList.filter((a) => a.approved).length;
  const rows = [
    { label: "Descrição", value: data.description },
    { label: "Volume de demanda", value: data.demandVolume },
    { label: "Promessas", value: data.promises },
    { label: "Ofertas", value: data.offers },
    { label: "Hipóteses a gerar", value: data.hypothesesToGenerate },
    { label: "Públicos a gerar", value: data.audiencesToGenerate },
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
      <h4 className="mt-4">
        Públicos ({audienceList.length}/{data.audiencesToGenerate ?? 0}) · Aprovados: {" "}
        {approvedAudienceCount}
      </h4>
      <div className="d-flex align-items-center mb-2">
        <input
          type="number"
          min={1}
          className="form-control w-auto me-2"
          title="Quantidade de públicos que o Worker IA irá gerar"
          {...register("quantity", { valueAsNumber: true })}
        />
        <button
          type="button"
          className="btn btn-secondary"
          onClick={handleSubmit(
            async ({ quantity }) => {
              if (!quantity || quantity <= 0) return;
              try {
                await requestAudiences.mutateAsync(quantity);
                alert("Solicitação enviada!");
                reset();
              } catch {
                alert("Erro ao solicitar públicos");
              }
            },
            (errors) => {
              console.log("Validation errors", errors);
            },
          )}
          disabled={requestAudiences.isPending}
        >
          Gerar Públicos
        </button>
        <span className="ms-2">
          {requestAudiences.isPending || (isFetching && !isLoading)
            ? "Atualizando..."
            : `Solicitados: ${data.audiencesToGenerate ?? 0}`}
        </span>
      </div>
      {audienceList.length === 0 ? (
        <p>Nenhum público ainda.</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-4 mb-4">
          {audienceList.map((a) => (
            <div key={a.id} className="col">
              <div className="card h-100 rounded-3">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start">
                    <h5 className="card-title mb-0">{a.name}</h5>
                    <span
                      className={`badge ${
                        a.approved ? "bg-success" : "bg-warning text-dark"
                      }`}
                    >
                      {a.approved ? "Aprovado" : "Pendente"}
                    </span>
                  </div>
                  <p className="card-text">{a.description || "-"}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      <h4 className="mt-4">Hipóteses</h4>
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
                  <p className="card-text">
                    <strong>Entrega:</strong> {h.entrega || "-"}
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
