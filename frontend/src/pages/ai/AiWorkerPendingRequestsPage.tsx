import { useMemo } from "react";
import { Link } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import {
  Sparkles,
  ClipboardList,
  Mail,
  FileText,
  Workflow,
  Users,
  Lightbulb,
  Clock3,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  useExperiments,
  type Experiment as ExperimentSummary,
} from "../../api/experiment/useExperiments";
import { useNiches, type MarketNiche } from "../../api/niche/useNiches";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import "./AiWorkerPendingRequestsPage.css";

type ExperimentPendingKey =
  | "creativesToGenerate"
  | "instantFormsToGenerate"
  | "emailsToGenerate"
  | "deliverablesToGenerate"
  | "leadPortalFlowsToGenerate";

type NichePendingKey = "hypothesesToGenerate" | "audiencesToGenerate";

type PendingConfig<T extends string> = {
  key: T;
  label: string;
  icon: LucideIcon;
};

const EXPERIMENT_PENDING_CONFIG: PendingConfig<ExperimentPendingKey>[] = [
  { key: "creativesToGenerate", label: "Criativos", icon: Sparkles },
  { key: "instantFormsToGenerate", label: "Instant forms", icon: ClipboardList },
  { key: "emailsToGenerate", label: "E-mails", icon: Mail },
  { key: "deliverablesToGenerate", label: "Materiais de entrega", icon: FileText },
  { key: "leadPortalFlowsToGenerate", label: "Fluxos do portal", icon: Workflow },
];

const NICHE_PENDING_CONFIG: PendingConfig<NichePendingKey>[] = [
  { key: "hypothesesToGenerate", label: "Hipóteses", icon: Lightbulb },
  { key: "audiencesToGenerate", label: "Públicos", icon: Users },
];

function normalizeQuantity(value?: number | null) {
  return typeof value === "number" && value > 0 ? value : 0;
}

export default function AiWorkerPendingRequestsPage() {
  useBreadcrumbs([
    { label: "IA" },
    { label: "Solicitações do Worker" },
  ]);

  const {
    data: experiments,
    isLoading: isLoadingExperiments,
    isFetching: isFetchingExperiments,
    error: experimentsError,
  } = useExperiments();

  const {
    data: niches,
    isLoading: isLoadingNiches,
    isFetching: isFetchingNiches,
    error: nichesError,
  } = useNiches();

  const experimentRequests = useMemo(() => {
    return (experiments ?? [])
      .map((experiment: ExperimentSummary) => {
        const items = EXPERIMENT_PENDING_CONFIG.map((config) => {
          const quantity = normalizeQuantity(experiment[config.key]);
          if (quantity === 0) {
            return null;
          }
          return {
            ...config,
            quantity,
          };
        }).filter((value) => value !== null) as Array<
          PendingConfig<ExperimentPendingKey> & { quantity: number }
        >;

        if (items.length === 0) {
          return null;
        }

        const total = items.reduce((sum, item) => sum + item.quantity, 0);

        return {
          id: experiment.id,
          name: experiment.name,
          hypothesis: experiment.hypothesis,
          platform: experiment.platform,
          status: experiment.status,
          items,
          total,
        };
      })
      .filter((value) => value !== null) as Array<{
        id: string;
        name: string;
        hypothesis: string;
        platform: string;
        status: string;
        items: Array<PendingConfig<ExperimentPendingKey> & { quantity: number }>;
        total: number;
      }>;
  }, [experiments]);

  const nicheRequests = useMemo(() => {
    return (niches ?? [])
      .map((niche: MarketNiche) => {
        const items = NICHE_PENDING_CONFIG.map((config) => {
          const quantity = normalizeQuantity(niche[config.key]);
          if (quantity === 0) {
            return null;
          }
          return {
            ...config,
            quantity,
          };
        }).filter((value) => value !== null) as Array<
          PendingConfig<NichePendingKey> & { quantity: number }
        >;

        if (items.length === 0) {
          return null;
        }

        const total = items.reduce((sum, item) => sum + item.quantity, 0);

        return {
          id: niche.id,
          name: niche.name,
          items,
          total,
        };
      })
      .filter((value) => value !== null) as Array<{
        id: number;
        name: string;
        items: Array<PendingConfig<NichePendingKey> & { quantity: number }>;
        total: number;
      }>;
  }, [niches]);

  const totalPending = useMemo(() => {
    const experimentTotal = experimentRequests.reduce(
      (sum, request) => sum + request.total,
      0,
    );
    const nicheTotal = nicheRequests.reduce(
      (sum, request) => sum + request.total,
      0,
    );
    return experimentTotal + nicheTotal;
  }, [experimentRequests, nicheRequests]);

  const totalGroups = experimentRequests.length + nicheRequests.length;
  const isLoading = isLoadingExperiments || isLoadingNiches;
  const isFetching = isFetchingExperiments || isFetchingNiches;
  const error = experimentsError ?? nichesError;

  return (
    <div>
      <PageTitle>Solicitações pendentes do Worker IA</PageTitle>
      <p className="text-body-secondary">
        Acompanhe tudo o que ainda precisa ser processado pelo worker de IA, com
        agrupamento por experimento e nicho. Use esta visão para priorizar quais
        itens revisar ou complementar antes da execução automática.
      </p>

      {isLoading ? (
        <div className="d-flex align-items-center gap-2">
          <span
            className="spinner-border spinner-border-sm text-primary"
            role="status"
            aria-hidden="true"
          />
          <span>Carregando solicitações pendentes...</span>
        </div>
      ) : error ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar as solicitações pendentes. Atualize a página
          para tentar novamente.
        </div>
      ) : totalPending === 0 ? (
        <div className="ai-worker-pending-empty mt-4">
          <div className="ai-worker-pending-empty__icon">
            <Sparkles aria-hidden="true" size={28} />
          </div>
          <div>
            <h2 className="h5 mb-2">Nenhuma solicitação pendente</h2>
            <p className="text-body-secondary mb-0">
              Assim que novas demandas forem enviadas ao worker de IA, elas
              aparecerão aqui para acompanhamento.
            </p>
          </div>
        </div>
      ) : (
        <div className="d-flex flex-column gap-4">
          <section className="card">
            <div className="card-body ai-worker-pending-summary">
              <div className="ai-worker-pending-summary__icon">
                <Clock3 aria-hidden="true" size={24} />
              </div>
              <div>
                <h2 className="h5 mb-1">{totalPending} solicitações ativas</h2>
                <p className="text-body-secondary mb-0">
                  Distribuídas em {totalGroups} grupos. A lista abaixo é
                  atualizada automaticamente{isFetching ? "..." : "."}
                </p>
              </div>
            </div>
          </section>

          {experimentRequests.length > 0 ? (
            <section>
              <div className="d-flex align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">Experimentos</h2>
                <span className="badge text-bg-primary-subtle text-primary-emphasis">
                  {experimentRequests.length}
                </span>
              </div>
              <div className="row row-cols-1 row-cols-lg-2 row-cols-xxl-3 g-3">
                {experimentRequests.map((request) => (
                  <div className="col" key={request.id}>
                    <div className="card h-100">
                      <div className="card-body ai-worker-pending-card">
                        <div className="ai-worker-pending-card__header">
                          <div>
                            <h3 className="h6 mb-1">{request.name}</h3>
                            <p className="mb-0 text-body-secondary">
                              Hipótese: {request.hypothesis}
                            </p>
                          </div>
                          <span className="badge text-bg-warning-subtle text-warning-emphasis text-uppercase fw-semibold">
                            {request.platform}
                          </span>
                        </div>

                        <div className="ai-worker-pending-card__meta">
                          <span className="badge rounded-pill text-bg-secondary-subtle text-secondary-emphasis">
                            {request.status}
                          </span>
                          <span className="badge rounded-pill text-bg-primary-subtle text-primary-emphasis">
                            {request.total} itens
                          </span>
                        </div>

                        <ul className="ai-worker-pending-card__items">
                          {request.items.map((item) => (
                            <li
                              key={item.key}
                              className="ai-worker-pending-card__item"
                            >
                              <span className="ai-worker-pending-card__item-label">
                                <item.icon aria-hidden="true" size={18} />
                                {item.label}
                              </span>
                              <span className="badge text-bg-primary">
                                {item.quantity} pendentes
                              </span>
                            </li>
                          ))}
                        </ul>

                        <div className="ai-worker-pending-card__footer">
                          <Link
                            to={`/experiments/${request.id}`}
                            className="btn btn-outline-primary btn-sm"
                          >
                            Abrir experimento
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ) : null}

          {nicheRequests.length > 0 ? (
            <section>
              <div className="d-flex align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">Nichos</h2>
                <span className="badge text-bg-primary-subtle text-primary-emphasis">
                  {nicheRequests.length}
                </span>
              </div>
              <div className="row row-cols-1 row-cols-lg-2 row-cols-xxl-3 g-3">
                {nicheRequests.map((request) => (
                  <div className="col" key={request.id}>
                    <div className="card h-100">
                      <div className="card-body ai-worker-pending-card">
                        <div className="ai-worker-pending-card__header">
                          <div>
                            <h3 className="h6 mb-1">{request.name}</h3>
                            <p className="mb-0 text-body-secondary">
                              Solicitações relacionadas a hipóteses e públicos
                              do nicho.
                            </p>
                          </div>
                          <span className="badge text-bg-info-subtle text-info-emphasis">
                            Nicho
                          </span>
                        </div>

                        <div className="ai-worker-pending-card__meta">
                          <span className="badge rounded-pill text-bg-primary-subtle text-primary-emphasis">
                            {request.total} itens
                          </span>
                        </div>

                        <ul className="ai-worker-pending-card__items">
                          {request.items.map((item) => (
                            <li
                              key={item.key}
                              className="ai-worker-pending-card__item"
                            >
                              <span className="ai-worker-pending-card__item-label">
                                <item.icon aria-hidden="true" size={18} />
                                {item.label}
                              </span>
                              <span className="badge text-bg-primary">
                                {item.quantity} pendentes
                              </span>
                            </li>
                          ))}
                        </ul>

                        <div className="ai-worker-pending-card__footer">
                          <Link
                            to={`/niches/${request.id}`}
                            className="btn btn-outline-primary btn-sm"
                          >
                            Abrir nicho
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      )}
    </div>
  );
}
