import { useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { AudienceApprovalCard } from "../../components/AudienceApprovalCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";
import { useForm } from "react-hook-form";
import { useRequestAudiences } from "../../api/niche/useRequestAudiences";
import { useRequestHypotheses } from "../../api/niche/useRequestHypotheses";
import { useExperimentsByNiche } from "../../api/experiment/useExperimentsByNiche";
import { useDeliverablesByNiche } from "../../api/deliverable/useDeliverablesByNiche";
import { useCreateDeliverable } from "../../api/deliverable/useCreateDeliverable";
import {
  ArrowUpRight,
  Clock3,
  FileDown,
  Lightbulb,
  Package,
  Sparkles,
  Users,
} from "lucide-react";
import "./NicheDetailPage.css";

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const id = Number(nicheId);
  const { data, isLoading, isFetching } = useNiche(id);
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  const { data: audiences } = useAudiencesByNiche(nicheId);
  const { data: experiments } = useExperimentsByNiche(nicheId);
  const { data: deliverables } = useDeliverablesByNiche(nicheId);
  const requestAudiences = useRequestAudiences(id);
  const requestHypotheses = useRequestHypotheses(id);
  const {
    register: registerAudienceQuantity,
    handleSubmit: handleSubmitAudienceQuantity,
    reset: resetAudienceQuantity,
  } = useForm<{ quantity: number }>({
    defaultValues: { quantity: 1 },
  });
  const {
    register: registerHypothesisQuantity,
    handleSubmit: handleSubmitHypothesisQuantity,
    reset: resetHypothesisQuantity,
  } = useForm<{ quantity: number }>({
    defaultValues: { quantity: 1 },
  });
  const {
    register: registerDeliverable,
    handleSubmit: handleSubmitDeliverable,
    reset: resetDeliverable,
  } = useForm<{
    title: string;
    description?: string;
    content?: string;
    model?: string;
    prompt: string;
  }>({
    defaultValues: {
      title: "",
      description: "",
      content: "",
      model: "",
      prompt: "",
    },
  });
  const createDeliverable = useCreateDeliverable(id);
  useBreadcrumbs([{ label: data?.name || "...", icon: nicheIcon }]);

  const scrollToSection = useCallback((sectionId: string) => {
    if (typeof document === "undefined") return;
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, []);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const handleSaveMarkdown = () => {
    const md =
      `# Nicho: ${data.name}\n\n` +
      `**ID:** ${data.id}\n\n` +
      `**Descrição:**\n${data.description}\n\n` +
      `**Categoria de interesse:**\n${data.interestCategory}\n\n` +
      `**Categoria de cargo:**\n${data.roleCategory}\n\n` +
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
  const experimentsList = Array.isArray(experiments) ? experiments : [];
  const deliverableList = Array.isArray(deliverables)
    ? [...deliverables].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const experimentsCountByHypothesis = experimentsList.reduce<
    Record<string, number>
  >((acc, experiment) => {
    const key = experiment.hypothesisId;
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  const createdAtLabel = data.createdAt
    ? new Date(data.createdAt).toLocaleString("pt-BR")
    : undefined;
  const updatedAtLabel = data.updatedAt
    ? new Date(data.updatedAt).toLocaleString("pt-BR")
    : undefined;
  const infoCards = [
    { label: "Descrição", value: data.description },
    { label: "Categoria de interesse", value: data.interestCategory },
    { label: "Categoria de cargo", value: data.roleCategory },
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
        <a
          className="niche-detail__card-link"
          href={chatDialog.url}
          target="_blank"
          rel="noopener noreferrer"
        >
          {chatDialog.description}
        </a>
      ) : undefined,
    },
  ];
  const stats = [
    {
      icon: Users,
      label: "Públicos",
      value: `${audienceList.length}`,
      helper: `Meta: ${data.audiencesToGenerate ?? 0}`,
      targetId: "niche-audiences",
    },
    {
      icon: Lightbulb,
      label: "Hipóteses",
      value: `${list.length}`,
      helper: `Meta: ${data.hypothesesToGenerate ?? 0}`,
      targetId: "niche-hypotheses",
    },
    {
      icon: Package,
      label: "Entregáveis",
      value: `${deliverableList.length}`,
      helper:
        deliverableList.length === 0
          ? "Nenhum gerado ainda"
          : `${deliverableList.length} prontos para uso`,
      targetId: "niche-deliverables",
    },
    {
      icon: Clock3,
      label: "Atualizado em",
      value: updatedAtLabel ?? "-",
      helper: createdAtLabel ? `Criado em ${createdAtLabel}` : undefined,
    },
  ];
  const audienceStatusLabel =
    requestAudiences.isPending || (isFetching && !isLoading)
      ? "Atualizando públicos..."
      : `Solicitados ao Worker: ${data.audiencesToGenerate ?? 0}`;
  const hypothesisStatusLabel =
    requestHypotheses.isPending || (isFetching && !isLoading)
      ? "Atualizando hipóteses..."
      : `Solicitadas ao Worker: ${data.hypothesesToGenerate ?? 0}`;
  const onRequestAudiences = handleSubmitAudienceQuantity(
    async ({ quantity }) => {
      if (!quantity || quantity <= 0) return;
      try {
        await requestAudiences.mutateAsync(quantity);
        alert("Solicitação enviada!");
        resetAudienceQuantity({ quantity: 1 });
      } catch {
        alert("Erro ao solicitar públicos");
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );
  const onRequestHypotheses = handleSubmitHypothesisQuantity(
    async ({ quantity }) => {
      if (!quantity || quantity <= 0) return;
      try {
        await requestHypotheses.mutateAsync(quantity);
        alert("Solicitação enviada!");
        resetHypothesisQuantity({ quantity: 1 });
      } catch {
        alert("Erro ao solicitar hipóteses");
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );

  const onCreateDeliverable = handleSubmitDeliverable(
    async (values) => {
      try {
        await createDeliverable.mutateAsync({
          title: values.title,
          description: values.description,
          content: values.content,
          model: values.model,
          prompt: values.prompt,
        });
        resetDeliverable({ title: "", description: "", content: "", model: "", prompt: "" });
      } catch (error) {
        console.error("Failed to create deliverable", error);
        alert("Não foi possível salvar o entregável. Tente novamente.");
      }
    },
    (errors) => {
      console.log("Deliverable form errors", errors);
    },
  );

  return (
    <div className="niche-detail">
      <div className="niche-detail__header">
        <div className="niche-detail__title">
          <span className="niche-detail__badge">Nicho</span>
          <PageTitle icon={nicheIcon}>{data.name}</PageTitle>
          <p className="niche-detail__subtitle">
            {`Nicho #${data.id}`}
            {updatedAtLabel ? ` • Atualizado em ${updatedAtLabel}` : ""}
          </p>
        </div>
        <div className="niche-detail__actions">
          <button
            type="button"
            className="btn btn-outline-secondary niche-detail__export-btn"
            onClick={handleSaveMarkdown}
          >
            <FileDown size={18} />
            <span>Salvar em Markdown</span>
          </button>
        </div>
      </div>
      <ul className="niche-detail__stats">
        {stats.map((stat) => {
          const isInteractive = Boolean(stat.targetId);
          const targetId = stat.targetId;
          const className = `niche-detail__stat${
            isInteractive ? " niche-detail__stat--interactive" : ""
          }`;
          return (
            <li
              key={stat.label}
              className={className}
              onClick={
                isInteractive && targetId
                  ? () => scrollToSection(targetId)
                  : undefined
              }
              role={isInteractive ? "button" : undefined}
              tabIndex={isInteractive ? 0 : undefined}
              onKeyDown={
                isInteractive && targetId
                  ? (event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        scrollToSection(targetId);
                      }
                    }
                  : undefined
              }
            >
              <span className="niche-detail__stat-icon">
                <stat.icon size={20} strokeWidth={1.75} />
              </span>
              <div className="niche-detail__stat-content">
                <span className="niche-detail__stat-value">{stat.value}</span>
                <span className="niche-detail__stat-label">{stat.label}</span>
                {stat.helper ? (
                  <span className="niche-detail__stat-helper">{stat.helper}</span>
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>
      <section aria-label="Informações do nicho">
        <div className="niche-detail__grid">
          {infoCards.map((card) => (
            <article key={card.label} className="niche-detail__card">
              <h3 className="niche-detail__card-title">{card.label}</h3>
              <div className="niche-detail__card-content">
                {card.value === null || card.value === undefined || card.value === ""
                  ? (
                      <span className="niche-detail__card-empty">-</span>
                    )
                  : typeof card.value === "string" || typeof card.value === "number"
                    ? (
                        <span className="niche-detail__card-text">{card.value}</span>
                      )
                    : (
                        card.value
                      )}
              </div>
            </article>
          ))}
        </div>
      </section>
      <section className="niche-section" aria-labelledby="niche-deliverables">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-deliverables">
              Entregáveis
            </h2>
            <p className="niche-section__subtitle">
              Centralize os materiais gerados para validar este nicho.
            </p>
          </div>
          <span className="badge text-bg-light text-dark niche-deliverables__badge">
            {deliverableList.length} item(s)
          </span>
        </div>
        <form className="niche-deliverables__form" onSubmit={onCreateDeliverable}>
          <div className="row g-3">
            <div className="col-md-4">
              <label htmlFor="deliverable-title" className="form-label">
                Título *
              </label>
              <input
                id="deliverable-title"
                type="text"
                className="form-control"
                placeholder="Resumo do entregável"
                disabled={createDeliverable.isPending}
                {...registerDeliverable("title", { required: true })}
              />
            </div>
            <div className="col-md-4">
              <label htmlFor="deliverable-model" className="form-label">
                Modelo de IA
              </label>
              <input
                id="deliverable-model"
                type="text"
                className="form-control"
                placeholder="ex: gpt-4.1"
                disabled={createDeliverable.isPending}
                {...registerDeliverable("model")}
              />
            </div>
            <div className="col-12">
              <label htmlFor="deliverable-prompt" className="form-label">
                Prompt utilizado *
              </label>
              <textarea
                id="deliverable-prompt"
                className="form-control"
                rows={2}
                placeholder="Cole aqui o prompt enviado ao modelo"
                disabled={createDeliverable.isPending}
                {...registerDeliverable("prompt", { required: true })}
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="deliverable-description" className="form-label">
                Descrição
              </label>
              <textarea
                id="deliverable-description"
                className="form-control"
                rows={2}
                placeholder="Resumo rápido do entregável"
                disabled={createDeliverable.isPending}
                {...registerDeliverable("description")}
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="deliverable-content" className="form-label">
                Conteúdo detalhado
              </label>
              <textarea
                id="deliverable-content"
                className="form-control"
                rows={2}
                placeholder="Cole aqui o conteúdo completo"
                disabled={createDeliverable.isPending}
                {...registerDeliverable("content")}
              />
            </div>
          </div>
          <div className="d-flex justify-content-end mt-3">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={createDeliverable.isPending}
            >
              {createDeliverable.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Salvar entregável</span>
            </button>
          </div>
        </form>
        {deliverableList.length === 0 ? (
          <p className="niche-section__empty">Nenhum entregável cadastrado ainda.</p>
        ) : (
          <div className="niche-section__grid niche-deliverables__grid">
            {deliverableList.map((deliverable) => (
              <article key={deliverable.id} className="card niche-section__card">
                <div className="card-body niche-deliverable-card__body">
                  <div className="niche-deliverable-card__head">
                    <h3 className="niche-deliverable-card__title">{deliverable.title}</h3>
                    {deliverable.model ? (
                      <span className="badge text-bg-light text-dark">
                        {deliverable.model}
                      </span>
                    ) : null}
                  </div>
                  {deliverable.description ? (
                    <p className="niche-deliverable-card__description">
                      {deliverable.description}
                    </p>
                  ) : null}
                  {deliverable.content ? (
                    <pre className="niche-deliverable-card__content">
                      {deliverable.content}
                    </pre>
                  ) : null}
                  <details className="niche-deliverable-card__prompt">
                    <summary>Ver prompt utilizado</summary>
                    <pre>{deliverable.prompt}</pre>
                  </details>
                </div>
                <div className="card-footer niche-deliverable-card__footer">
                  {`Atualizado em ${
                    deliverable.updatedAt
                      ? new Date(deliverable.updatedAt).toLocaleString("pt-BR")
                      : "-"
                  }`}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <section className="niche-section" aria-labelledby="niche-audiences">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-audiences">
              Públicos
            </h2>
            <p className="niche-section__subtitle">
              {`${audienceList.length}/${data.audiencesToGenerate ?? 0} gerados ou pendentes`}
            </p>
            <p className="niche-section__status">{audienceStatusLabel}</p>
          </div>
          <form
            className="niche-section__actions"
            onSubmit={onRequestAudiences}
          >
            <label htmlFor="audience-quantity" className="visually-hidden">
              Quantidade de públicos que o Worker IA irá gerar
            </label>
            <input
              id="audience-quantity"
              type="number"
              min={1}
              className="form-control"
              title="Quantidade de públicos que o Worker IA irá gerar"
              disabled={requestAudiences.isPending}
              {...registerAudienceQuantity("quantity", { valueAsNumber: true })}
            />
            <button
              type="submit"
              className="btn btn-secondary"
              disabled={requestAudiences.isPending}
            >
              {requestAudiences.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Gerar Públicos</span>
            </button>
          </form>
        </div>
        {audienceList.length === 0 ? (
          <p className="niche-section__empty">Nenhum público ainda.</p>
        ) : (
          <div className="niche-section__grid">
            {audienceList.map((a) => (
              <AudienceApprovalCard
                key={a.id}
                audience={a}
                nicheId={nicheId}
                className="niche-section__card"
              />
            ))}
          </div>
        )}
      </section>
      <section className="niche-section" aria-labelledby="niche-hypotheses">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-hypotheses">
              Hipóteses
            </h2>
            <p className="niche-section__subtitle">
              {list.length === 0
                ? "As hipóteses aparecerão aqui quando forem geradas pela IA."
                : "Principais ângulos sugeridos pela IA para o nicho."}
            </p>
            <p className="niche-section__status">{hypothesisStatusLabel}</p>
          </div>
          <form
            className="niche-section__actions"
            onSubmit={onRequestHypotheses}
          >
            <label htmlFor="hypothesis-quantity" className="visually-hidden">
              Quantidade de hipóteses que o Worker IA irá gerar
            </label>
            <input
              id="hypothesis-quantity"
              type="number"
              min={1}
              className="form-control"
              title="Quantidade de hipóteses que o Worker IA irá gerar"
              disabled={requestHypotheses.isPending}
              {...registerHypothesisQuantity("quantity", { valueAsNumber: true })}
            />
            <button
              type="submit"
              className="btn btn-secondary"
              disabled={requestHypotheses.isPending}
            >
              {requestHypotheses.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Gerar Hipóteses</span>
            </button>
          </form>
        </div>
        {list.length === 0 ? (
          <p className="niche-section__empty">Nenhuma hipótese ainda.</p>
        ) : (
          <div className="niche-section__grid">
            {list.map((h) => {
              const experimentCount = experimentsCountByHypothesis[h.id] ?? 0;
              const experimentLabel =
                experimentCount === 1
                  ? "1 experimento criado"
                  : `${experimentCount} experimentos criados`;
              const fields = [
                { label: "Promessa", value: h.promise },
                { label: "Problema", value: h.problem },
                { label: "Mecanismo", value: h.mechanism },
                { label: "Mecanismo único", value: h.uniqueMechanism },
                { label: "Persona", value: h.persona },
                { label: "Entrega", value: h.entrega },
              ];
              return (
                <div key={h.id} className="card h-100 niche-section__card">
                  <div className="card-body niche-hypothesis-card__body">
                    <div className="niche-hypothesis-card__head">
                      <h3 className="niche-hypothesis-card__title">{h.title}</h3>
                      <span className="niche-hypothesis-card__counter">
                        {experimentLabel}
                      </span>
                    </div>
                    <div className="niche-hypothesis-card__fields">
                      {fields.map((field) => (
                        <div
                          key={field.label}
                          className="niche-hypothesis-card__field"
                        >
                          <span className="niche-hypothesis-card__field-label">
                            {`${field.label}:`}
                          </span>
                          <p className="niche-hypothesis-card__field-value">
                            {field.value || "-"}
                          </p>
                        </div>
                      ))}
                    </div>
                    <div className="niche-hypothesis-card__actions">
                      <Link
                        className="btn btn-sm btn-outline-primary"
                        to={`hypotheses/${h.id}`}
                      >
                        <span>Ver detalhes</span>
                        <ArrowUpRight size={16} />
                      </Link>
                    </div>
                  </div>
                  <div className="card-footer niche-hypothesis-card__footer">
                    {`Gerado com ${h.model || "-"} em ${
                      h.createdAt
                        ? new Date(h.createdAt).toLocaleString("pt-BR")
                        : "-"
                    }`}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
