import { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useExperimentsByHypothesis } from "../../api/experiment/useExperimentsByHypothesis";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";
import { useInstantFormsByHypothesis } from "../../api/hypothesis/useInstantFormsByHypothesis";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { AudienceApprovalCard } from "../../components/AudienceApprovalCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useForm } from "react-hook-form";
import { useRequestAudiences } from "../../api/niche/useRequestAudiences";

export default function HypothesisDetailPage() {
  const { nicheId, hypothesisId } = useParams();
  const nicheNumericId = Number(nicheId);
  const { data: niche, isFetching: isFetchingNiche } = useNiche(nicheNumericId);
  const { data, isLoading } = useHypothesis(nicheId, hypothesisId);
  const { data: experiments } = useExperimentsByHypothesis(
    nicheId,
    hypothesisId,
  );
  const { data: audiences } = useAudiencesByNiche(nicheId);
  const { data: instantForms, isLoading: isLoadingInstantForms } =
    useInstantFormsByHypothesis(hypothesisId);
  const requestAudiences = useRequestAudiences(nicheNumericId);
  const { register, handleSubmit, reset } = useForm<{ quantity: number }>({
    defaultValues: { quantity: 1 },
  });
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
  const list = Array.isArray(experiments) ? experiments : [];
  const audienceList = Array.isArray(audiences) ? audiences : [];
  const instantFormList = Array.isArray(instantForms) ? instantForms : [];
  const rows = [
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
      `# Hipótese: ${data.title}\n\n` +
      `**Promessa:**\n${data.promise ?? ""}\n\n` +
      `**Problema:**\n${data.problem ?? ""}\n\n` +
      `**Persona:**\n${data.persona ?? ""}\n\n` +
      `**Mecanismo:**\n${data.mechanism ?? ""}\n\n` +
      `**Mecanismo único:**\n${data.uniqueMechanism ?? ""}\n\n` +
      `**Entrega:**\n${data.entrega ?? ""}\n`;
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

      <p className="mb-2">
        Públicos gerados: {audienceList.length}/
        {niche?.audiencesToGenerate ?? 0}
      </p>
      <div className="d-flex align-items-center mb-4">
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
          {requestAudiences.isPending || isFetchingNiche
            ? "Atualizando..."
            : `Solicitados: ${niche?.audiencesToGenerate ?? 0}`}
        </span>
      </div>

      {audienceList.length === 0 ? (
        <p className="text-muted">Nenhum público cadastrado para este nicho.</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-4 mb-4">
          {audienceList.map((a) => (
            <div key={a.id} className="col">
              <AudienceApprovalCard
                audience={a}
                nicheId={nicheId}
                badgeLabel={
                  a.hypothesisId === hypothesisId ? "Hipótese" : "Nicho"
                }
              />
            </div>
          ))}
        </div>
      )}

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
              {instantFormList.length} registro{instantFormList.length > 1 ? "s" : ""}
            </span>
          )}
        </div>
        <div className="card-body">
          {isLoadingInstantForms ? (
            <p>Carregando instant forms...</p>
          ) : instantFormList.length === 0 ? (
            <p className="text-muted">
              Nenhum Instant Form vinculado a esta hipótese. Quando o worker IA gerar um formulário,
              ele ficará disponível aqui para ser reutilizado em diferentes experimentos.
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
                        <div className="text-muted small">ID Meta: {form.facebookFormId ?? "—"}</div>
                        <div className="text-muted small">
                          Modelo: {form.model ? form.model : "—"}
                        </div>
                        {form.prompt && (
                          <details className="small mt-1">
                            <summary>Ver prompt</summary>
                            <pre className="mb-0 text-break" style={{ whiteSpace: "pre-wrap" }}>
                              {form.prompt}
                            </pre>
                          </details>
                        )}
                      </td>
                      <td style={{ minWidth: 180 }}>
                        <div>{form.facebookPageName}</div>
                        <div className="text-muted small">{form.facebookPageExternalId}</div>
                      </td>
                      <td style={{ minWidth: 140 }}>
                        <div>{form.status ?? "—"}</div>
                        <div className="text-muted small">
                          {form.locale ? `Idioma: ${form.locale}` : "Idioma não informado"}
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
                            <span className="text-muted small">Sem link de agradecimento</span>
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
                            <span className="text-muted small">Sem política informada</span>
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
