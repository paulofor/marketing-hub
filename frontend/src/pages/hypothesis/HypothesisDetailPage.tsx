import { Fragment, useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useExperimentsByHypothesis } from "../../api/experiment/useExperimentsByHypothesis";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import { useInstantFormsByHypothesis } from "../../api/hypothesis/useInstantFormsByHypothesis";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { TargetingElementCard } from "../../components/TargetingElementCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import type { TargetingElementType } from "../../api/targeting/types";
import { TargetingGenerationForm } from "../../components/TargetingGenerationForm";
import { TargetingRequestForm } from "../../components/TargetingRequestForm";
import { TargetingRequestStatusPanel } from "../../components/TargetingRequestStatusPanel";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { HypothesisFrameworkTabsView } from "../../components/HypothesisFrameworkTabsView";

export default function HypothesisDetailPage() {
  const { nicheId, hypothesisId } = useParams();
  const nicheNumericId = Number(nicheId);
  const normalizedNicheId = Number.isFinite(nicheNumericId)
    ? nicheNumericId
    : undefined;
  const targetingRequestFilters = useMemo(
    () => ({ limit: 6, nicheId: normalizedNicheId, hypothesisId }),
    [normalizedNicheId, hypothesisId],
  );
  const { data: niche, isFetching: isFetchingNiche } = useNiche(nicheNumericId);
  const { data, isLoading, refetch } = useHypothesis(nicheId, hypothesisId);
  const { data: experiments } = useExperimentsByHypothesis(
    nicheId,
    hypothesisId,
  );
  const { data: targetingElements, isFetching: isFetchingTargeting } =
    useTargetingElementsByNiche(nicheId);
  const { data: instantForms, isLoading: isLoadingInstantForms } =
    useInstantFormsByHypothesis(hypothesisId);
  const { data: openAiModels, isLoading: isLoadingModels } = useOpenAiModels();
  useBreadcrumbs([
    {
      label: niche?.name || "...",
      to: `/niches/${nicheId}`,
      icon: nicheIcon,
    },
    { label: data?.title || "...", icon: hypothesisIcon },
  ]);

  const formatUsd = (value?: number | string | null) => {
    if (value === undefined || value === null) return undefined;
    const num = typeof value === "string" ? Number(value) : value;
    if (Number.isNaN(num)) return undefined;
    return num.toLocaleString("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    });
  };
  const formatBrl = (value?: number | string | null) => {
    if (value === undefined || value === null) return undefined;
    const num = typeof value === "string" ? Number(value) : value;
    if (Number.isNaN(num)) return undefined;
    return num.toLocaleString("pt-BR", {
      style: "currency",
      currency: "BRL",
    });
  };

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const list = Array.isArray(experiments) ? experiments : [];
  const targetingList = Array.isArray(targetingElements)
    ? targetingElements
    : [];
  const instantFormList = Array.isArray(instantForms) ? instantForms : [];
  const targetingByType: Record<TargetingElementType, typeof targetingList> = {
    INTEREST: targetingList.filter((element) => element.type === "INTEREST"),
    JOB_TITLE: targetingList.filter((element) => element.type === "JOB_TITLE"),
    BEHAVIOR: targetingList.filter((element) => element.type === "BEHAVIOR"),
  };
  const targetingConfigs: Array<{
    type: TargetingElementType;
    title: string;
    description: string;
    requested?: number | null;
    model?: string | null;
  }> = [
    {
      type: "INTEREST",
      title: "Interesses",
      description:
        "Segmentos prontos para usar como interesses salvos no Meta Ads.",
      requested: niche?.interestsToGenerate,
      model: niche?.interestModel,
    },
    {
      type: "JOB_TITLE",
      title: "Cargos",
      description: "Funções profissionais associadas à persona desta hipótese.",
      requested: niche?.jobTitlesToGenerate,
      model: niche?.jobTitleModel,
    },
    {
      type: "BEHAVIOR",
      title: "Comportamentos",
      description: "Ações e hábitos que indicam afinidade com a oferta.",
      requested: niche?.behaviorsToGenerate,
      model: niche?.behaviorModel,
    },
  ];
  const costLabel = formatUsd(data.costUsd) ?? "-";
  const costBrlLabel = formatBrl(data.cost) ?? "-";
  const expenseBrlLabel = formatBrl(data.expense) ?? "-";
  const rows = [
    { label: "Modelo", value: data.model ?? "-" },
    { label: "Custo (USD)", value: costLabel },
    { label: "Custo (BRL)", value: costBrlLabel },
    { label: "Despesa (BRL)", value: expenseBrlLabel },
    { label: "Promessa", value: data.promise },
    { label: "Problema", value: data.problem },
    { label: "Persona", value: data.persona },
    { label: "Mecanismo", value: data.mechanism },
    { label: "Mecanismo único", value: data.uniqueMechanism },
    { label: "Entrega", value: data.entrega },
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
      `# Hipótese: ${data.title}

` +
      `**Modelo:**
${data.model ?? ""}

` +
      `**Custo (USD):**
${formatUsd(data.costUsd) ?? ""}

` +
      `**Custo (BRL):**
${formatBrl(data.cost) ?? ""}

` +
      `**Despesa (BRL):**
${formatBrl(data.expense) ?? ""}

` +
      `**Promessa:**
${data.promise ?? ""}

` +
      `**Problema:**
${data.problem ?? ""}

` +
      `**Persona:**
${data.persona ?? ""}

` +
      `**Mecanismo:**
${data.mechanism ?? ""}

` +
      `**Mecanismo único:**
${data.uniqueMechanism ?? ""}

` +
      `**Entrega:**
${data.entrega ?? ""}
`;
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
      <div className="d-flex justify-content-between align-items-center mb-4">
        <PageTitle icon={hypothesisIcon}>{data.title}</PageTitle>
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

      <section className="mb-4">
        <h5 className="mb-2">Segmentação Meta Ads</h5>
        <p className="text-body-secondary">
          Localização fixa: Brasil. Gere e aprove elementos separados por
          interesses, cargos e comportamentos para este nicho.
        </p>
        <TargetingRequestForm
          className="mb-3"
          defaultDescricao={`Hipótese ${data.title} (${niche?.name ?? "N/A"})`}
          defaultIdioma="pt_BR"
          defaultPais="BR"
          defaultPublico="PROSPECT"
          nicheId={normalizedNicheId}
          hypothesisId={hypothesisId}
          queryFilters={targetingRequestFilters}
        />
        <TargetingRequestStatusPanel
          className="mb-4"
          limit={6}
          nicheId={normalizedNicheId}
          hypothesisId={hypothesisId}
        />
        <div className="row row-cols-1 row-cols-lg-3 g-3 mb-3">
          {targetingConfigs.map((config) => (
            <div key={config.type} className="col">
              <div className="border rounded-3 p-3 h-100 d-flex flex-column gap-3">
                <div>
                  <strong>{config.title}</strong>
                  <p className="text-body-secondary small mb-0">
                    {config.description}
                  </p>
                  <span className="badge text-bg-light mt-2">
                    {targetingByType[config.type].length} cadastrados
                  </span>
                </div>
                <TargetingGenerationForm
                  nicheId={nicheNumericId}
                  type={config.type}
                  openAiModels={openAiModels}
                  defaultModel={config.model ?? openAiModels?.[0]?.code}
                  requestedTotal={config.requested}
                  isLoadingModels={isLoadingModels}
                  isFetchingStatus={isFetchingTargeting || isFetchingNiche}
                  ctaLabel={`Gerar ${config.title.toLowerCase()}`}
                />
              </div>
            </div>
          ))}
        </div>
        {targetingConfigs.map((config) => (
          <div key={`${config.type}-list`} className="mb-5">
            <div className="d-flex align-items-center mb-3">
              <h6 className="mb-0">{config.title}</h6>
              <span className="badge text-bg-secondary ms-2">
                {targetingByType[config.type].length}
              </span>
            </div>
            {targetingByType[config.type].length === 0 ? (
              <p className="text-muted">
                Nenhum elemento de {config.title.toLowerCase()} foi cadastrado
                ainda.
              </p>
            ) : (
              <div className="row row-cols-1 row-cols-md-2 g-3">
                {targetingByType[config.type].map((element) => (
                  <div key={element.id} className="col">
                    <TargetingElementCard
                      element={element}
                      badgeLabel={
                        element.hypothesisId === hypothesisId
                          ? "Hipótese"
                          : "Nicho"
                      }
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </section>

      <div className="card mb-4">
        <div className="card-header">
          <h5 className="mb-0">Informações da hipótese</h5>
        </div>
        <div className="card-body">
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
          {data.createdAt && (
            <div className="mt-4">
              <h6>Data de criação</h6>
              <p>{new Date(data.createdAt).toLocaleString("pt-BR")}</p>
            </div>
          )}
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header d-flex justify-content-between align-items-center">
          <h5 className="mb-0">Instant Forms</h5>
          {instantFormList.length > 0 && (
            <span className="text-muted small">
              {instantFormList.length} registro
              {instantFormList.length > 1 ? "s" : ""}
            </span>
          )}
        </div>
        <div className="card-body">
          {isLoadingInstantForms ? (
            <p>Carregando instant forms...</p>
          ) : instantFormList.length === 0 ? (
            <p className="text-muted">
              Nenhum Instant Form vinculado a esta hipótese. Quando o worker IA
              gerar um formulário, ele ficará disponível aqui para ser
              reutilizado em diferentes experimentos.
            </p>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr>
                    <th>Formulário</th>
                    <th>Página</th>
                    <th>Status</th>
                    <th>Leads</th>
                    <th>Datas</th>
                    <th>Links</th>
                  </tr>
                </thead>
                <tbody>
                  {instantFormList.map((form) => (
                    <tr key={form.id}>
                      <td style={{ minWidth: 220 }}>
                        <div className="fw-semibold">{form.name}</div>
                        <div className="text-muted small">
                          ID Meta: {form.facebookFormId ?? "—"}
                        </div>
                        <div className="text-muted small">
                          Modelo: {form.model ? form.model : "—"}
                        </div>
                        {form.prompt && (
                          <details className="small mt-1">
                            <summary>Ver prompt</summary>
                            <pre
                              className="mb-0 text-break"
                              style={{ whiteSpace: "pre-wrap" }}
                            >
                              {form.prompt}
                            </pre>
                          </details>
                        )}
                      </td>
                      <td style={{ minWidth: 180 }}>
                        <div>{form.facebookPageName}</div>
                        <div className="text-muted small">
                          {form.facebookPageExternalId}
                        </div>
                      </td>
                      <td style={{ minWidth: 140 }}>
                        <div>{form.status ?? "—"}</div>
                        <div className="text-muted small">
                          {form.locale
                            ? `Idioma: ${form.locale}`
                            : "Idioma não informado"}
                        </div>
                      </td>
                      <td>{form.leadsCount ?? "—"}</td>
                      <td style={{ minWidth: 200 }}>
                        <div className="text-muted small">Criado</div>
                        <div>
                          {form.createdTime
                            ? new Date(form.createdTime).toLocaleString("pt-BR")
                            : "—"}
                        </div>
                        <div className="text-muted small mt-2">Atualizado</div>
                        <div>
                          {form.updatedTime
                            ? new Date(form.updatedTime).toLocaleString("pt-BR")
                            : "—"}
                        </div>
                      </td>
                      <td style={{ minWidth: 200 }}>
                        <div className="d-flex flex-column gap-1">
                          {form.followUpActionUrl ? (
                            <div className="d-flex flex-column gap-1">
                              <a
                                href={form.followUpActionUrl}
                                target="_blank"
                                rel="noreferrer"
                              >
                                Página de agradecimento
                              </a>
                              <span className="text-muted small text-break">
                                {form.followUpActionUrl}
                              </span>
                            </div>
                          ) : (
                            <span className="text-muted small">
                              Sem link de agradecimento
                            </span>
                          )}
                          {form.privacyPolicyUrl ? (
                            <div className="d-flex flex-column gap-1">
                              <a
                                href={form.privacyPolicyUrl}
                                target="_blank"
                                rel="noreferrer"
                              >
                                Política de privacidade
                              </a>
                              <span className="text-muted small text-break">
                                {form.privacyPolicyUrl}
                              </span>
                            </div>
                          ) : (
                            <span className="text-muted small">
                              Sem política informada
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {hypothesisId && (
        <div className="mb-4">
          <HypothesisFrameworkTabsView
            hypothesisId={hypothesisId}
            nicheId={nicheId}
            nicheName={niche?.name}
            framework={data.framework}
            onRefresh={refetch}
          />
        </div>
      )}

      <div className="card mb-4">
        <div className="card-header">
          <h5 className="mb-0">Experimentos</h5>
        </div>
        <div className="card-body">
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
      </div>

      {data.prompt && (
        <div className="card mb-4">
          <div className="card-header">
            <h5 className="mb-0">Prompt de criação</h5>
          </div>
          <div className="card-body">
            <pre className="text-break" style={{ whiteSpace: "pre-wrap" }}>
              {data.prompt}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
