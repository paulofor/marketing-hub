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

function sortByUpdatedAtDesc<T extends { updatedAt?: string | null }>(list: T[]) {
  return [...list].sort((a, b) => {
    const aDate = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
    const bDate = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
    return bDate - aDate;
  });
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
      <div className="row row-cols-1 row-cols-lg-2 g-3">
        {deliverableList.map((deliverable) => (
          <div key={deliverable.id} className="col">
            <article className="card h-100 shadow-sm">
              <div className="card-body d-flex flex-column gap-2">
                <div className="d-flex justify-content-between align-items-start gap-2">
                  <h6 className="mb-0">{deliverable.title}</h6>
                  {deliverable.model ? (
                    <span className="badge text-bg-light text-dark">{deliverable.model}</span>
                  ) : null}
                </div>
                {deliverable.description ? (
                  <p className="text-muted small mb-0">{deliverable.description}</p>
                ) : null}
                {deliverable.content ? (
                  <pre className="bg-body-tertiary rounded-3 p-2 small mb-0 text-muted">
                    {deliverable.content}
                  </pre>
                ) : null}
                <details>
                  <summary className="small fw-semibold text-primary">Ver prompt utilizado</summary>
                  <pre className="bg-body-tertiary rounded-3 p-2 small mt-2 text-muted">{deliverable.prompt}</pre>
                </details>
              </div>
              <div className="card-footer text-muted small">
                {`Atualizado em ${
                  deliverable.updatedAt ? new Date(deliverable.updatedAt).toLocaleString("pt-BR") : "-"
                }`}
              </div>
            </article>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="mt-3">
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

      <section className="card mb-4 mt-4">
        <div className="card-header d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h5 className="mb-0">Entregáveis do nicho</h5>
            <p className="text-muted small mb-0">
              Utilize as referências aprovadas para montar pacotes e guiar a produção criativa.
            </p>
          </div>
          <span className="badge text-bg-secondary align-self-center">{deliverableList.length}</span>
        </div>
        <div className="card-body">{renderDeliverableList()}</div>
      </section>

      <section className="card border-0 shadow-sm rounded-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start flex-wrap gap-3">
            <div>
              <h5 className="card-title mb-0">Pacotes de entregáveis</h5>
              <p className="text-muted mb-0">
                Agrupe entregáveis do nicho para acompanhar os materiais aprovados pela IA.
              </p>
            </div>
            <span className="badge text-bg-light text-dark">{packageList.length} pacote(s)</span>
          </div>
          <form className="mt-3" onSubmit={onCreatePackage}>
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
            <div className="d-flex justify-content-end mt-3">
              <button type="submit" className="btn btn-primary" disabled={createPackage.isPending}>
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
            <p className="text-muted small mt-3 mb-0">Carregando pacotes cadastrados...</p>
          ) : packageList.length === 0 ? (
            <p className="text-muted small mt-3 mb-0">
              Nenhum pacote cadastrado ainda. Selecione entregáveis para formar um conjunto coerente.
            </p>
          ) : (
            <div className="mt-3 d-flex flex-column gap-3">
              {packageList.map((pkg) => (
                <div key={pkg.id} className="border rounded-3 p-3 bg-body-tertiary">
                  <div className="d-flex justify-content-between align-items-start flex-wrap gap-3">
                    <div>
                      <h6 className="mb-1">{pkg.name}</h6>
                      {pkg.description ? (
                        <p className="text-muted small mb-2">{pkg.description}</p>
                      ) : null}
                    </div>
                    <span className="badge text-bg-light text-dark">
                      {pkg.deliverables.length} entregável(is)
                    </span>
                  </div>
                  {pkg.deliverables.length > 0 ? (
                    <ul className="small ps-3 mb-2 mt-2">
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
                  <details className="mt-2">
                    <summary className="small fw-semibold text-primary">Ver prompt utilizado</summary>
                    <pre className="bg-body-secondary rounded-3 p-2 mt-2 small text-muted">{pkg.prompt}</pre>
                  </details>
                  <div className="text-muted small mt-2">
                    {`Atualizado em ${
                      pkg.updatedAt ? new Date(pkg.updatedAt).toLocaleString("pt-BR") : "-"
                    }`}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
