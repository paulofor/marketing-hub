import { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { normalizeFramework } from "../../api/hypothesis/types";

export default function HypothesisDetailPage() {
  const { nicheId, hypothesisId } = useParams();
  const nicheNumericId = Number(nicheId);
  const { data: niche } = useNiche(nicheNumericId);
  const { data, isLoading } = useHypothesis(nicheId, hypothesisId);
  useBreadcrumbs([
    {
      label: niche?.name || "...",
      to: `/niches/${nicheId}`,
      icon: nicheIcon,
    },
    { label: data?.title || "...", icon: hypothesisIcon },
  ]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const framework = normalizeFramework(data.framework);
  const sections = [
    {
      title: "Dor",
      summary: framework.pain.summary,
      rows: [
        { label: "Dor superficial", value: framework.pain.surface },
        { label: "Dor raiz", value: framework.pain.root },
        { label: "Dor emocional", value: framework.pain.emotional },
        { label: "Dor social", value: framework.pain.social },
        { label: "Custo da inação", value: framework.pain.cost },
      ],
    },
    {
      title: "Resultado",
      summary: framework.result.summary,
      rows: [
        {
          label: "Resultado desejado",
          value: framework.result.desiredResult,
        },
        {
          label: "Identidade desejada",
          value: framework.result.desiredIdentity,
        },
        {
          label: "Resultado de negócio",
          value: framework.result.businessOutcome,
        },
        { label: "Sinal de sucesso", value: framework.result.successSignal },
      ],
    },
    {
      title: "Mecanismo",
      summary: framework.mechanism.summary,
      rows: [
        { label: "Mecanismo central", value: framework.mechanism.core },
        { label: "Mecanismo único", value: framework.mechanism.unique },
        { label: "Evidência visível", value: framework.mechanism.visible },
        {
          label: "Fator de credibilidade",
          value: framework.mechanism.believability,
        },
      ],
    },
    {
      title: "Prova",
      summary: framework.proof.summary,
      rows: [
        { label: "Tipo de prova", value: framework.proof.type },
        { label: "Ativo de prova", value: framework.proof.asset },
        { label: "Mensagem", value: framework.proof.message },
        { label: "Estágio de entrega", value: framework.proof.deliveryStage },
      ],
    },
    {
      title: "Oferta",
      summary: framework.offer.summary,
      rows: [
        { label: "Nome da oferta", value: framework.offer.name },
        { label: "Promessa central", value: framework.offer.corePromise },
        { label: "Entregáveis", value: framework.offer.deliverables },
        { label: "Reversão de risco", value: framework.offer.riskReversal },
        { label: "Lógica de preço", value: framework.offer.priceLogic },
        { label: "Preço", value: framework.offer.priceAmount },
        { label: "Tipo da oferta", value: framework.offer.offerType },
        { label: "Call to action", value: framework.offer.cta },
      ],
    },
  ];

  const buildFrameworkSectionMarkdown = (
    title: string,
    fields: Array<{ label: string; value?: string | number | null }>,
    summary?: string,
  ) => {
    const fieldsMd = fields
      .map(({ label, value }) => `- **${label}:** ${value ?? ""}`)
      .join("\n");

    return (
      `## ${title}\n\n` +
      `${fieldsMd}\n\n` +
      `**Resumo do item:**\n${summary ?? ""}\n`
    );
  };

  const handleSaveMarkdown = () => {
    const md =
      `# Hipótese: ${data.title}\n\n` +
      `**Nicho:** ${niche?.name ?? ""}\n\n` +
      sections
        .map((section) =>
          buildFrameworkSectionMarkdown(
            section.title,
            section.rows,
            section.summary,
          ),
        )
        .join("\n");
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
      <div className="d-flex justify-content-between align-items-center mb-4">
        <PageTitle icon={hypothesisIcon}>{data.title}</PageTitle>
        <div className="d-flex gap-2">
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

      <section className="row row-cols-1 row-cols-xl-2 g-3">
        {sections.map((section) => (
          <div className="col" key={section.title}>
            <article className="card h-100">
              <div className="card-header">
                <h2 className="h5 mb-0">{section.title}</h2>
              </div>
              <div className="card-body">
                {section.summary ? (
                  <p className="fw-semibold">{section.summary}</p>
                ) : null}
                <dl className="row mb-0">
                  {section.rows.map((row, idx) => (
                    <Fragment key={row.label}>
                      <dt
                        className={`col-sm-4 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {row.label}
                      </dt>
                      <dd
                        className={`col-sm-8 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {row.value ?? "—"}
                      </dd>
                    </Fragment>
                  ))}
                </dl>
              </div>
            </article>
          </div>
        ))}
      </section>

      {data.createdAt && (
        <div className="card mt-4">
          <div className="card-body">
            <dl className="row mb-0">
              <dt className="col-sm-3 py-2 bg-light">Data de criação</dt>
              <dd className="col-sm-9 py-2 bg-light">
                {new Date(data.createdAt).toLocaleString("pt-BR")}
              </dd>
            </dl>
          </div>
        </div>
      )}
    </div>
  );
}
