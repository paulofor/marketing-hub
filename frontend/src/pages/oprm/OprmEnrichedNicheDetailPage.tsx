import { Link, useParams } from "react-router-dom";
import {
  buildOprmEnrichedNichePipelineMarkdownUrl,
  useOprmEnrichedNicheMaterializerProfileDetail,
} from "../../api/oprm/useOprmEnrichedNicheMaterializerDetail";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";

function formatDate(value?: string | null) {
  if (!value) {
    return "Não informado";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function SummaryCard({ title, text }: { title: string; text?: string | null }) {
  return (
    <section className="card h-100">
      <div className="card-body">
        <h2 className="h6 text-uppercase text-secondary">{title}</h2>
        <p className="mb-0" style={{ whiteSpace: "pre-line" }}>
          {text || "Não informado"}
        </p>
      </div>
    </section>
  );
}

export default function OprmEnrichedNicheDetailPage() {
  const { profileId } = useParams();
  const numericProfileId = profileId ? Number(profileId) : undefined;
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: "Nicho enriquecido" },
  ]);
  const { data, isLoading, error } =
    useOprmEnrichedNicheMaterializerProfileDetail(
      Number.isFinite(numericProfileId) ? numericProfileId : undefined,
    );

  if (isLoading) {
    return <p>Carregando nicho enriquecido...</p>;
  }

  if (error || !data) {
    return (
      <div>
        <PageTitle>Nicho enriquecido</PageTitle>
        <div className="alert alert-warning" role="alert">
          Não foi possível carregar os dados do nicho enriquecido.
        </div>
        <Link className="btn btn-outline-secondary" to="/niches">
          Voltar para nichos
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex align-items-center justify-content-between gap-3 mb-3">
        <PageTitle>Nicho enriquecido</PageTitle>
        <div className="d-flex gap-2">
          {data.marketNicheId ? (
            <Link
              className="btn btn-success"
              to={`/niches/${data.marketNicheId}/hypotheses/new`}
            >
              Criar hipótese
            </Link>
          ) : (
            <button
              type="button"
              className="btn btn-success"
              disabled
              title="Nicho não informado para criar hipótese"
            >
              Criar hipótese
            </button>
          )}
          <a
            className="btn btn-primary"
            href={buildOprmEnrichedNichePipelineMarkdownUrl(
              data.enrichedNicheProfileId,
            )}
            download={`oprm-nicho-enriquecido-${data.enrichedNicheProfileId}.md`}
          >
            Baixar pesquisa Markdown
          </a>
          <Link className="btn btn-outline-secondary" to="/niches">
            Voltar para nichos
          </Link>
        </div>
      </div>

      <section className="card mb-3">
        <div className="card-body">
          <h2 className="h5 mb-3">{data.nicheName || "Nicho sem nome"}</h2>
          <dl className="row mb-0">
            <dt className="col-sm-3">Perfil enriquecido</dt>
            <dd className="col-sm-9">#{data.enrichedNicheProfileId}</dd>
            <dt className="col-sm-3">Nicho</dt>
            <dd className="col-sm-9">#{data.marketNicheId}</dd>
            <dt className="col-sm-3">CNAE</dt>
            <dd className="col-sm-9">{data.cnaeCode || "Não informado"}</dd>
            <dt className="col-sm-3">Qualidade</dt>
            <dd className="col-sm-9">
              {data.qualityStatus || "Não informado"}
            </dd>
            <dt className="col-sm-3">Materializado em</dt>
            <dd className="col-sm-9">{formatDate(data.materializedAt)}</dd>
            <dt className="col-sm-3">Fontes</dt>
            <dd className="col-sm-9">
              {data.sourceDomains || "Não informado"}
            </dd>
          </dl>
        </div>
      </section>

      <div className="row g-3">
        <div className="col-md-6">
          <SummaryCard title="Rotina" text={data.routineSummary} />
        </div>
        <div className="col-12">
          <SummaryCard title="Evidências" text={data.evidenceSummary} />
        </div>
      </div>
    </div>
  );
}
