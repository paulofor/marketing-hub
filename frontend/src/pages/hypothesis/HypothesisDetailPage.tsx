import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { normalizeFramework } from "../../api/hypothesis/types";
import "./HypothesisDetailPage.css";

type SectionRow = {
  label: string;
  value?: string | number | string[] | null;
  defaultOpen?: boolean;
};

type DetailSection = {
  title: string;
  summary?: string;
  tone: "pain" | "result" | "mechanism" | "proof" | "offer";
  rows: SectionRow[];
};

function hasReadableValue(value?: string | number | string[] | null) {
  if (Array.isArray(value)) return value.some((item) => item.trim());
  if (typeof value === "number") return true;
  return Boolean(value?.trim());
}

function renderReadableValue(value?: string | number | string[] | null) {
  if (!hasReadableValue(value)) {
    return <span className="text-muted">Sem informação registrada.</span>;
  }

  if (Array.isArray(value)) {
    return (
      <ul className="hypothesis-detail__list">
        {value
          .filter((item) => item.trim())
          .map((item) => (
            <li key={item}>{item}</li>
          ))}
      </ul>
    );
  }

  return String(value)
    .split("\n")
    .filter((line) => line.trim())
    .map((line) => <p key={line}>{line}</p>);
}

function HypothesisSectionCard({ section }: { section: DetailSection }) {
  const filledRows = section.rows.filter((row) => hasReadableValue(row.value));
  const rows = filledRows.length > 0 ? filledRows : section.rows;

  return (
    <article
      className={`hypothesis-detail__section hypothesis-detail__section--${section.tone}`}
    >
      <div className="hypothesis-detail__section-header">
        <div>
          <span className="hypothesis-detail__eyebrow">{section.title}</span>
          <h2>{section.summary?.trim() || "Resumo ainda não gerado"}</h2>
        </div>
      </div>
      <div className="hypothesis-detail__collapses">
        {rows.map((row, index) => (
          <details
            key={row.label}
            className="hypothesis-detail__collapse"
            open={row.defaultOpen ?? index < 2}
          >
            <summary>
              <span>{row.label}</span>
              <span className="hypothesis-detail__collapse-action">Ver</span>
            </summary>
            <div className="hypothesis-detail__collapse-body">
              {renderReadableValue(row.value)}
            </div>
          </details>
        ))}
      </div>
    </article>
  );
}

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
  const sections: DetailSection[] = [
    {
      title: "Dor",
      summary: framework.pain.summary,
      tone: "pain",
      rows: [
        {
          label: "Dor superficial",
          value: framework.pain.surface,
          defaultOpen: true,
        },
        { label: "Dor raiz", value: framework.pain.root, defaultOpen: true },
        { label: "Dor emocional", value: framework.pain.emotional },
        { label: "Dor social", value: framework.pain.social },
        { label: "Custo da inação", value: framework.pain.cost },
        { label: "Evidências", value: framework.pain.evidenceSignals },
      ],
    },
    {
      title: "Resultado",
      summary: framework.result.summary,
      tone: "result",
      rows: [
        {
          label: "Resultado desejado",
          value: framework.result.desiredResult,
          defaultOpen: true,
        },
        {
          label: "Antes e depois",
          value: framework.result.desiredIdentity,
          defaultOpen: true,
        },
        {
          label: "Resultado de negócio",
          value: framework.result.businessOutcome,
        },
        { label: "Sinal de sucesso", value: framework.result.successSignal },
        { label: "Evidências", value: framework.result.evidenceSignals },
      ],
    },
    {
      title: "Mecanismo",
      summary: framework.mechanism.summary,
      tone: "mechanism",
      rows: [
        {
          label: "Mecanismo central",
          value: framework.mechanism.core,
          defaultOpen: true,
        },
        {
          label: "Nome / diferencial",
          value: framework.mechanism.unique,
          defaultOpen: true,
        },
        { label: "Como funciona", value: framework.mechanism.visible },
        {
          label: "Fator de credibilidade",
          value: framework.mechanism.believability,
        },
        { label: "Evidências", value: framework.mechanism.evidenceSignals },
      ],
    },
    {
      title: "Prova",
      summary: framework.proof.summary,
      tone: "proof",
      rows: [
        {
          label: "Tipo de prova",
          value: framework.proof.type,
          defaultOpen: true,
        },
        {
          label: "Ativo de prova",
          value: framework.proof.asset,
          defaultOpen: true,
        },
        { label: "Mensagem", value: framework.proof.message },
        {
          label: "Como coletar / entregar",
          value: framework.proof.deliveryStage,
        },
        { label: "Evidências", value: framework.proof.evidenceSignals },
      ],
    },
    {
      title: "Oferta",
      summary: framework.offer.summary,
      tone: "offer",
      rows: [
        {
          label: "Nome da oferta",
          value: framework.offer.name,
          defaultOpen: true,
        },
        {
          label: "Promessa central",
          value: framework.offer.corePromise,
          defaultOpen: true,
        },
        { label: "Entregáveis", value: framework.offer.deliverables },
        { label: "Reversão de risco", value: framework.offer.riskReversal },
        { label: "Lógica de preço", value: framework.offer.priceLogic },
        { label: "Preço", value: framework.offer.priceAmount },
        { label: "Tipo da oferta", value: framework.offer.offerType },
        { label: "Call to action", value: framework.offer.cta },
        { label: "Evidências", value: framework.offer.evidenceSignals },
      ],
    },
  ];

  const buildFrameworkSectionMarkdown = (
    title: string,
    fields: SectionRow[],
    summary?: string,
  ) => {
    const fieldsMd = fields
      .map(({ label, value }) => {
        const displayValue = Array.isArray(value)
          ? value.filter((item) => item.trim()).join("; ")
          : value;
        return `- **${label}:** ${displayValue ?? ""}`;
      })
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
    <div className="hypothesis-detail">
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

      <section
        className="hypothesis-detail__hero"
        aria-label="Resumo da hipótese"
      >
        <div>
          <span>Hipótese comercial</span>
          <h1>{framework.offer.name || data.title}</h1>
          <p>
            {framework.offer.corePromise ||
              framework.result.desiredResult ||
              framework.pain.summary ||
              "Framework comercial ainda em construção."}
          </p>
        </div>
        <div className="hypothesis-detail__meta">
          <div>
            <strong>{data.status || "—"}</strong>
            <span>Status</span>
          </div>
          <div>
            <strong>
              {typeof data.costUsd === "number"
                ? `$ ${data.costUsd.toFixed(2)}`
                : "—"}
            </strong>
            <span>Custo IA</span>
          </div>
          <div>
            <strong>
              {framework.offer.priceAmount
                ? `R$ ${framework.offer.priceAmount.toFixed(2)}`
                : "—"}
            </strong>
            <span>Preço sugerido</span>
          </div>
        </div>
      </section>

      <section className="hypothesis-detail__grid">
        {sections.map((section) => (
          <HypothesisSectionCard section={section} key={section.title} />
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
