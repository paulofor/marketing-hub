import { useMemo } from "react";
import { Sparkles } from "lucide-react";
import { useForm } from "react-hook-form";
import WorkerRequestBanner from "./WorkerRequestBanner";
import { useDeliverablesByNiche } from "../../api/deliverable/useDeliverablesByNiche";
import { useDeliverablePackagesByExperiment } from "../../api/deliverable/useDeliverablePackagesByExperiment";
import { useCreateDeliverablePackage } from "../../api/deliverable/useCreateDeliverablePackage";
import { useRequestDeliverables } from "../../api/experiment/useRequestDeliverables";
import type { Deliverable } from "../../api/deliverable/types";
import type { Experiment } from "../../api/experiment/useExperiments";
import "./DeliverablesTab.css";

interface DeliverablesTabProps {
  experiment: Experiment;
  nicheName?: string;
}

type DeliverablePackageForm = {
  name: string;
  description?: string;
  model?: string;
  prompt: string;
  deliverableIds: string[];
};

type ContentListItem = {
  text: string;
  prefix?: string;
};

type ContentBlock =
  | {
      type: "paragraph";
      text: string;
    }
  | {
      type: "list";
      items: ContentListItem[];
    };

function normalizeDeliverableContent(content: string) {
  return content
    .replace(/\r\n/g, "\n")
    .replace(/\\n/g, "\n")
    .replace(/\/n/g, "\n")
    .replace(/\/r/g, "\n")
    .replace(/[\t\f\v]+/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function sortByUpdatedAtDesc<T extends { updatedAt?: string | null }>(list: T[]) {
  return [...list].sort((a, b) => {
    const aDate = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
    const bDate = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
    return bDate - aDate;
  });
}

function formatUpdatedAt(updatedAt?: string | null) {
  if (!updatedAt) {
    return "-";
  }

  return new Date(updatedAt).toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function parseDeliverableContent(content: string): ContentBlock[] {
  const normalized = normalizeDeliverableContent(content);
  const lines = normalized.split(/\r?\n/);
  const blocks: ContentBlock[] = [];
  let currentList: ContentListItem[] | null = null;

  const flushList = () => {
    if (currentList && currentList.length > 0) {
      blocks.push({ type: "list", items: currentList });
    }
    currentList = null;
  };

  for (const line of lines) {
    const trimmed = line.trim();

    if (!trimmed) {
      flushList();
      continue;
    }

    const listMatch = trimmed.match(
      /^((?:\d+|[a-zA-Z])(?:[\.\)]|\s*[–—-])|[-–—•●◦▪*])\s+(.*)$/, 
    );

    if (listMatch) {
      if (!currentList) {
        currentList = [];
      }

      const [, prefix, itemText] = listMatch;
      const cleanedPrefix = /[-–—*]/.test(prefix)
        ? "•"
        : prefix
            .replace(/\s*[–—-]$/, "")
            .replace(/\s+/g, "")
            .trim();
      currentList.push({
        prefix: cleanedPrefix,
        text: itemText.trim(),
      });
      continue;
    }

    flushList();
    blocks.push({ type: "paragraph", text: trimmed });
  }

  flushList();
  return blocks;
}

interface DeliverableContentProps {
  content: string;
}

function DeliverableContent({ content }: DeliverableContentProps) {
  const blocks = useMemo(() => parseDeliverableContent(content), [content]);

  return (
    <div className="deliverable-card__content">
      <span className="deliverable-card__eyebrow">Referência aprovada</span>
      <div className="deliverable-card__snippet" aria-live="polite">
        {blocks.map((block, index) => {
          if (block.type === "list") {
            return (
              <ul key={`list-${index}`} className="deliverable-card__list">
                {block.items.map((item, itemIndex) => (
                  <li key={`list-${index}-item-${itemIndex}`}>
                    {item.prefix ? (
                      <span className="deliverable-card__list-bullet" aria-hidden="true">
                        {item.prefix.replace(/[\.]/g, "")}
                      </span>
                    ) : null}
                    <span>{item.text}</span>
                  </li>
                ))}
              </ul>
            );
          }

          const highlighted = block.text.match(/^([^:]{1,80}):\s*(.*)$/);

          return (
            <p key={`paragraph-${index}`} className="deliverable-card__paragraph">
              {highlighted ? (
                <>
                  <strong>{highlighted[1]}:</strong> {highlighted[2]}
                </>
              ) : (
                block.text
              )}
            </p>
          );
        })}
      </div>
    </div>
  );
}

export default function DeliverablesTab({ experiment, nicheName }: DeliverablesTabProps) {
  const { register, handleSubmit, reset } = useForm<DeliverablePackageForm>({
    defaultValues: {
      name: "",
      description: "",
      model: "",
      prompt: "",
      deliverableIds: [],
    },
  });

  const { data: deliverables, isLoading: isLoadingDeliverables } = useDeliverablesByNiche(
    experiment.nicheId,
  );
  const { data: packages, isLoading: isLoadingPackages } = useDeliverablePackagesByExperiment(
    experiment.id,
  );
  const createPackage = useCreateDeliverablePackage(experiment.id);
  const requestDeliverables = useRequestDeliverables(experiment.id, experiment.nicheId);

  const deliverableList = useMemo(() => {
    if (!Array.isArray(deliverables)) {
      return [] as Deliverable[];
    }
    return sortByUpdatedAtDesc(deliverables);
  }, [deliverables]);

  const packageList = useMemo(() => {
    if (!Array.isArray(packages)) {
      return [];
    }
    return sortByUpdatedAtDesc(packages);
  }, [packages]);

  const onCreatePackage = handleSubmit(
    async (values) => {
      try {
        await createPackage.mutateAsync({
          name: values.name,
          description: values.description?.trim() ? values.description : undefined,
          model: values.model?.trim() ? values.model : undefined,
          prompt: values.prompt,
          deliverableIds: (values.deliverableIds ?? []).map((value) => Number(value)),
        });
        reset({
          name: "",
          description: "",
          model: "",
          prompt: "",
          deliverableIds: [],
        });
      } catch (error) {
        console.error("Failed to create deliverable package", error);
      }
    },
    (validationErrors) => {
      console.log("Deliverable package form errors", validationErrors);
    },
  );

  const renderDeliverableList = () => {
    if (isLoadingDeliverables) {
      return <p>Carregando entregáveis do nicho...</p>;
    }

    if (deliverableList.length === 0) {
      return (
        <p className="text-muted mb-0">
          Nenhum entregável cadastrado ainda{nicheName ? ` para ${nicheName}` : ""}. Cadastre na ficha do nicho para liberar
          curadorias neste experimento.
        </p>
      );
    }

    return (
      <div className="deliverable-grid">
        {deliverableList.map((deliverable) => (
          <article key={deliverable.id} className="deliverable-card">
            <header className="deliverable-card__header">
              <div className="deliverable-card__title-group">
                <h6 className="deliverable-card__title">{deliverable.title}</h6>
                {deliverable.description ? (
                  <p className="deliverable-card__description">{deliverable.description}</p>
                ) : null}
              </div>
              <div className="deliverable-card__tags">
                {deliverable.model ? (
                  <span className="chip chip-muted">{deliverable.model}</span>
                ) : null}
                <span className="chip">Atualizado {formatUpdatedAt(deliverable.updatedAt)}</span>
              </div>
            </header>
            {deliverable.content ? (
              <DeliverableContent
                content={deliverable.content}
              />
            ) : null}
            <details className="deliverable-card__details">
              <summary>Ver prompt utilizado</summary>
              <pre className="deliverable-card__prompt">{deliverable.prompt}</pre>
            </details>
          </article>
        ))}
      </div>
    );
  };

  return (
    <div className="mt-3 deliverables-tab">
      <WorkerRequestBanner
        title="Entregáveis planejados"
        subtitle={
          nicheName
            ? `Solicite ao Worker IA novas definições alinhadas ao nicho ${nicheName}.`
            : "Solicite ao Worker IA novas definições alinhadas a este nicho."
        }
        resourceName="entregável"
        resourceNamePlural="entregáveis"
        buttonLabel="Solicitar entregáveis"
        existingLabel="Entregáveis disponíveis"
        existingCount={deliverableList.length}
        requestedCount={experiment.deliverablesToGenerate}
        defaultQuantity={Math.max(1, experiment.deliverablesToGenerate ?? 3)}
        helperText="Informe quantos entregáveis o Worker IA deve sugerir para enriquecer este experimento."
        onRequest={(quantity) => requestDeliverables.mutateAsync(quantity)}
        isRequesting={requestDeliverables.isPending}
      />

      <section className="card deliverables-panel deliverables-panel--niche mb-4 mt-4">
        <div className="card-header d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h5 className="mb-1">Entregáveis do nicho</h5>
            <p className="text-muted mb-0">
              Visualize rapidamente as definições aprovadas e utilize-as como base para novas curadorias.
            </p>
          </div>
          <span className="chip chip-emphasis align-self-center">{deliverableList.length} disponível(is)</span>
        </div>
        <div className="card-body">
          {isLoadingDeliverables || deliverableList.length === 0 ? (
            <div className="deliverables-panel__empty">{renderDeliverableList()}</div>
          ) : (
            renderDeliverableList()
          )}
        </div>
      </section>

      <section className="card deliverables-panel deliverables-panel--packages border-0">
        <div className="card-body">
          <div className="deliverables-panel__header">
            <div>
              <h5 className="card-title mb-1">Pacotes de entregáveis</h5>
              <p className="text-muted mb-0">
                Monte coleções estratégicas para compartilhar com a equipe e acelerar a produção criativa.
              </p>
            </div>
            <span className="chip chip-muted">{packageList.length} pacote(s)</span>
          </div>
          <form className="mt-4 deliverable-package-form" onSubmit={onCreatePackage}>
            <div className="row g-3">
              <div className="col-md-4">
                <label htmlFor="package-name" className="form-label">
                  Nome do pacote *
                </label>
                <input
                  id="package-name"
                  type="text"
                  className="form-control"
                  placeholder="Pacote inicial"
                  disabled={createPackage.isPending}
                  {...register("name", { required: true })}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="package-model" className="form-label">
                  Modelo de IA
                </label>
                <input
                  id="package-model"
                  type="text"
                  className="form-control"
                  placeholder="ex: gpt-4.1"
                  disabled={createPackage.isPending}
                  {...register("model")}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="package-deliverables" className="form-label">
                  Entregáveis vinculados
                </label>
                <div className="form-floating-select">
                  <select
                    id="package-deliverables"
                    multiple
                    className="form-select"
                    disabled={
                      createPackage.isPending || isLoadingDeliverables || deliverableList.length === 0
                    }
                    {...register("deliverableIds")}
                  >
                    {deliverableList.length === 0 ? (
                      <option disabled value="">
                        Nenhum entregável disponível
                      </option>
                    ) : (
                      deliverableList.map((deliverable) => (
                        <option key={deliverable.id} value={deliverable.id}>
                          {deliverable.title}
                        </option>
                      ))
                    )}
                  </select>
                  <small className="text-muted">Use Ctrl/Cmd + clique para selecionar múltiplos.</small>
                </div>
              </div>
              <div className="col-12">
                <label htmlFor="package-prompt" className="form-label">
                  Prompt utilizado *
                </label>
                <textarea
                  id="package-prompt"
                  className="form-control"
                  rows={2}
                  placeholder="Cole aqui o prompt enviado ao modelo"
                  disabled={createPackage.isPending}
                  {...register("prompt", { required: true })}
                />
              </div>
              <div className="col-12">
                <label htmlFor="package-description" className="form-label">
                  Descrição
                </label>
                <textarea
                  id="package-description"
                  className="form-control"
                  rows={2}
                  placeholder="Observações sobre a curadoria"
                  disabled={createPackage.isPending}
                  {...register("description")}
                />
              </div>
            </div>
            <div className="d-flex justify-content-end mt-4">
              <button type="submit" className="btn btn-primary btn-lg" disabled={createPackage.isPending}>
                {createPackage.isPending ? (
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                ) : (
                  <Sparkles size={18} />
                )}
                <span className="ms-2">{createPackage.isPending ? "Salvando..." : "Salvar pacote"}</span>
              </button>
            </div>
          </form>
          {isLoadingPackages ? (
            <p className="text-muted small mt-4 mb-0">Carregando pacotes cadastrados...</p>
          ) : packageList.length === 0 ? (
            <p className="text-muted small mt-4 mb-0">
              Nenhum pacote cadastrado ainda. Selecione entregáveis para formar um conjunto coerente.
            </p>
          ) : (
            <div className="mt-4 d-flex flex-column gap-3">
              {packageList.map((pkg) => (
                <article key={pkg.id} className="deliverable-package">
                  <header className="deliverable-package__header">
                    <div>
                      <h6 className="deliverable-package__title">{pkg.name}</h6>
                      {pkg.description ? (
                        <p className="deliverable-package__description">{pkg.description}</p>
                      ) : null}
                    </div>
                    <span className="chip chip-muted">
                      {pkg.deliverables.length} entregável(is)
                    </span>
                  </header>
                  {pkg.deliverables.length > 0 ? (
                    <ul className="deliverable-package__list">
                      {pkg.deliverables.map((deliverable) => (
                        <li key={deliverable.id}>
                          <strong>{deliverable.title}</strong>
                          {deliverable.description ? ` – ${deliverable.description}` : ""}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-muted small mb-2">Nenhum entregável associado ainda.</p>
                  )}
                  <details className="deliverable-package__details">
                    <summary>Ver prompt utilizado</summary>
                    <pre className="deliverable-package__prompt">{pkg.prompt}</pre>
                  </details>
                  <div className="deliverable-package__footer text-muted small">
                    Atualizado {formatUpdatedAt(pkg.updatedAt)}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
