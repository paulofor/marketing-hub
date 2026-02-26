import { useEffect, useMemo, useState } from "react";
import { Activity, Briefcase, Target } from "lucide-react";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import type {
  TargetingCandidateStatus,
  TargetingCandidateType,
  TargetingElement,
  TargetingElementType,
} from "../../api/targeting/types";
import { TargetingElementCard } from "../../components/TargetingElementCard";
import { useNiche } from "../../api/niche/useNiche";
import {
  useExperimentTargetingSelections,
  useRunSimpleAudienceFlow,
  useSaveExperimentTargetingSelections,
} from "../../api/experiment/useExperimentTargetingSelections";
import { useExperimentSimpleFlowStatus } from "../../api/experiment/useExperimentSimpleFlowStatus";

interface TargetingTabProps {
  nicheId?: number;
  hypothesisId?: string;
  experimentId?: number;
  nicheName?: string | null;
  hypothesisTitle?: string | null;
}

const CANDIDATE_TYPE_LABEL: Record<TargetingCandidateType, string> = {
  INTEREST: "Interesse",
  WORK_POSITION: "Cargo",
  BEHAVIOR: "Comportamento",
};

const CANDIDATE_STATUS_CONFIG: Record<TargetingCandidateStatus, { label: string; badge: string }> = {
  PENDING_FACEBOOK_MATCH: { label: "Buscando na Meta", badge: "warning" },
  VALIDATED: { label: "Validado", badge: "success" },
  NO_MATCH: { label: "Sem correspondência", badge: "secondary" },
};

function formatDateTime(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toLocaleString();
}

const TYPE_CONFIGS: Array<{
  type: TargetingElementType;
  title: string;
  description: string;
  icon: typeof Target;
}> = [
  {
    type: "INTEREST",
    title: "Interesses",
    description: "Tópicos, paixões e temas salvos no Meta Ads.",
    icon: Target,
  },
  {
    type: "JOB_TITLE",
    title: "Cargos",
    description: "Funções profissionais ideais para o experimento.",
    icon: Briefcase,
  },
  {
    type: "BEHAVIOR",
    title: "Comportamentos",
    description: "Ações e hábitos relevantes detectados no Meta.",
    icon: Activity,
  },
];

export default function TargetingTab({
  nicheId,
  hypothesisId,
  experimentId,
  nicheName,
  hypothesisTitle,
}: TargetingTabProps) {
  const nicheIdAsString = nicheId != null ? String(nicheId) : undefined;
  const { data } = useNiche(Number(nicheId));
  const { data: savedSelections } = useExperimentTargetingSelections(experimentId);
  const saveSelections = useSaveExperimentTargetingSelections(experimentId);
  const runSimpleFlow = useRunSimpleAudienceFlow(experimentId);
  const {
    data: targetingElements,
    isLoading,
    isFetching,
    isError,
  } = useTargetingElementsByNiche(nicheIdAsString);
  const { data: simpleFlowStatus, isLoading: isSimpleFlowStatusLoading, isFetching: isSimpleFlowStatusFetching } =
    useExperimentSimpleFlowStatus(experimentId);

  const [selectedTerms, setSelectedTerms] = useState<Set<string>>(new Set());

  const nicheOptions = useMemo(
    () => [
      ...(data?.interestList ?? []).map((term) => ({ candidateType: "INTEREST" as TargetingCandidateType, term })),
      ...(data?.roleList ?? []).map((term) => ({ candidateType: "WORK_POSITION" as TargetingCandidateType, term })),
      ...(data?.behaviorList ?? []).map((term) => ({ candidateType: "BEHAVIOR" as TargetingCandidateType, term })),
    ],
    [data?.behaviorList, data?.interestList, data?.roleList],
  );

  useEffect(() => {
    const initial = new Set(
      (savedSelections ?? []).map((item) => `${item.candidateType}::${item.term}`),
    );
    setSelectedTerms(initial);
  }, [savedSelections]);

  if (nicheIdAsString == null || !hypothesisId) {
    return (
      <div className="mt-3">
        <p className="text-muted">
          Este experimento não possui nicho ou hipótese associados para exibir a segmentação.
        </p>
      </div>
    );
  }

  const list = Array.isArray(targetingElements) ? targetingElements : [];
  const updating = isFetching && !isLoading;
  const flowRequest = simpleFlowStatus?.request ?? null;
  const resolutionSummary = simpleFlowStatus?.resolution ?? null;
  const pendingJobs = (resolutionSummary?.pending ?? 0) + (resolutionSummary?.processing ?? 0);
  const simpleFlowLoading = isSimpleFlowStatusLoading || (isSimpleFlowStatusFetching && !!flowRequest);
  const summaryItems = [
    { label: "Pendentes", value: resolutionSummary?.pending ?? 0, variant: "secondary" },
    { label: "Em processamento", value: resolutionSummary?.processing ?? 0, variant: "info" },
    { label: "Concluídos", value: resolutionSummary?.completed ?? 0, variant: "success" },
    { label: "Falhas", value: resolutionSummary?.failed ?? 0, variant: "danger" },
  ];

  const onToggle = (candidateType: TargetingCandidateType, term: string) => {
    const key = `${candidateType}::${term}`;
    setSelectedTerms((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const onSave = async () => {
    const items = Array.from(selectedTerms).map((entry) => {
      const [candidateType, term] = entry.split("::");
      return { candidateType: candidateType as TargetingCandidateType, term };
    });
    await saveSelections.mutateAsync({ items });
  };

  const onRunSimpleFlow = async () => {
    await onSave();
    await runSimpleFlow.mutateAsync();
    alert("Fluxo simples executado. A resolução dos IDs no Facebook foi enfileirada.");
  };

  return (
    <div className="mt-3">
      {updating && <p className="text-muted small">Atualizando elementos...</p>}

      <section className="mb-4 card">
        <div className="card-body">
          <h6>Fluxo simples de público (novo)</h6>
          <p className="text-body-secondary small mb-3">
            Selecione interesses, cargos e comportamentos salvos no nicho para criar o público da campanha.
          </p>
          <div className="row g-2">
            {nicheOptions.map((item) => {
              const key = `${item.candidateType}::${item.term}`;
              return (
                <div className="col-12 col-md-6" key={key}>
                  <label className="form-check border rounded p-2 w-100">
                    <input
                      className="form-check-input me-2"
                      type="checkbox"
                      checked={selectedTerms.has(key)}
                      onChange={() => onToggle(item.candidateType, item.term)}
                    />
                    <span className="form-check-label">
                      <strong>{item.term}</strong>
                      <span className="text-body-secondary ms-2">({item.candidateType})</span>
                    </span>
                  </label>
                </div>
              );
            })}
          </div>
          <div className="d-flex gap-2 mt-3">
            <button className="btn btn-outline-primary btn-sm" onClick={onSave} disabled={saveSelections.isPending}>
              {saveSelections.isPending ? <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" /> : null}
              <span className="ms-1">Salvar seleção</span>
            </button>
            <button className="btn btn-primary btn-sm" onClick={onRunSimpleFlow} disabled={runSimpleFlow.isPending || saveSelections.isPending}>
              {runSimpleFlow.isPending ? <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" /> : null}
              <span className="ms-1">Executar fluxo simples</span>
            </button>
          </div>
        </div>
      </section>

      <section className="mb-4 card">
        <div className="card-body">
          <div className="d-flex align-items-center justify-content-between mb-2">
            <div>
              <h6 className="mb-0">Status do fluxo simples</h6>
              <p className="text-body-secondary small mb-0">
                Monitoramos automaticamente a busca dos IDs oficiais na Meta Ads. Execute novamente se houver falhas.
              </p>
            </div>
            {simpleFlowLoading ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : pendingJobs > 0 ? (
              <span className="badge text-bg-warning">Processando</span>
            ) : null}
          </div>
          {flowRequest ? (
            <>
              <div className="d-flex flex-wrap gap-2 mb-3">
                <span className="badge text-bg-light">
                  Última execução: {formatDateTime(flowRequest.updatedAt ?? flowRequest.createdAt) ?? "—"}
                </span>
                <span className="badge text-bg-light">Termos enviados: {flowRequest.candidates?.length ?? 0}</span>
                <span className={`badge text-bg-${pendingJobs > 0 ? "warning" : "success"}`}>
                  {pendingJobs > 0 ? "Resolvendo na Meta" : "Resolução concluída"}
                </span>
              </div>
              <div className="d-flex flex-wrap gap-2 mb-3">
                {summaryItems.map((item) => (
                  <span key={item.label} className={`badge text-bg-${item.variant}`}>
                    {item.label}: {item.value}
                  </span>
                ))}
              </div>
              {resolutionSummary?.last_error ? (
                <div className="alert alert-danger py-2">
                  <strong>Último erro:</strong> {resolutionSummary.last_error}
                </div>
              ) : null}
              <div className="d-flex flex-column gap-3">
                {(flowRequest.candidates ?? []).map((candidate) => {
                  const key = candidate.id ?? `${candidate.tipo}-${candidate.seed ?? candidate.texto_sugerido}`;
                  const statusConfig = candidate.status ? CANDIDATE_STATUS_CONFIG[candidate.status] : undefined;
                  const badgeClass = statusConfig ? statusConfig.badge : "light";
                  const label = statusConfig?.label ?? "Sem status";
                  const primaryLabel = candidate.seed ?? candidate.texto_sugerido ?? "Termo sem descrição";
                  const options = candidate.options ?? [];
                  const noOptionMessage = candidate.status === "NO_MATCH"
                    ? "A Meta não encontrou correspondências para este termo. Tente ajustar o texto."
                    : pendingJobs > 0
                      ? "Ainda estamos buscando opções para este termo..."
                      : "Nenhuma opção retornada. Considere editar o termo e executar novamente.";
                  return (
                    <div key={key} className="border rounded p-2">
                      <div className="d-flex flex-wrap align-items-start justify-content-between gap-2">
                        <div>
                          <strong>{primaryLabel}</strong>
                          <p className="text-body-secondary small mb-1">
                            {CANDIDATE_TYPE_LABEL[candidate.tipo] ?? "Segmento"}
                          </p>
                        </div>
                        <span className={`badge text-bg-${badgeClass}`}>{label}</span>
                      </div>
                      {options.length > 0 ? (
                        <ul className="list-unstyled small mb-0">
                          {options.slice(0, 3).map((option) => (
                            <li key={option.id ?? option.facebook_id}>
                              <strong>{option.name}</strong>
                              {option.audience_size ? (
                                <span className="text-body-secondary ms-1">
                                  • {option.audience_size.toLocaleString("pt-BR")}
                                </span>
                              ) : null}
                            </li>
                          ))}
                          {options.length > 3 ? (
                            <li className="text-body-secondary">
                              +{options.length - 3} outras sugestões registradas
                            </li>
                          ) : null}
                        </ul>
                      ) : (
                        <p className="text-body-secondary small mb-0">{noOptionMessage}</p>
                      )}
                    </div>
                  );
                })}
              </div>
            </>
          ) : (
            <p className="text-muted mb-0">
              Nenhuma execução registrada. Salve as segmentações acima e clique em “Executar fluxo simples”.
            </p>
          )}
        </div>
      </section>

      {isError ? <p className="text-danger">Não foi possível carregar os elementos de segmentação.</p> : null}
      {TYPE_CONFIGS.map(({ type, title, description, icon: Icon }) => {
        const ofType = list.filter((element) => element.type === type);
        const relatedToHypothesis = ofType.filter((element) => element.hypothesisId === hypothesisId);
        const relatedToNiche = ofType.filter((element) => element.hypothesisId !== hypothesisId);

        return (
          <section key={type} className="mb-5">
            <div className="d-flex align-items-start gap-2 mb-2">
              <Icon size={18} className="text-primary mt-1" />
              <div>
                <h5 className="mb-0">{title}</h5>
                <p className="text-body-secondary small mb-0">{description}</p>
              </div>
              <span className="badge text-bg-secondary ms-auto">{ofType.length}</span>
            </div>
            <p className="text-body-secondary small">
              Elementos aprovados para {nicheName ?? "o nicho"}. Priorize os associados à hipótese
              {hypothesisTitle ? ` “${hypothesisTitle}”` : ""}.
            </p>

            <TargetingList
              title="Relacionados à hipótese"
              elements={relatedToHypothesis}
              emptyMessage="Nenhum elemento vinculado diretamente a esta hipótese."
              badgeLabel="Hipótese"
            />
            <TargetingList
              title="Disponíveis no nicho"
              elements={relatedToNiche}
              emptyMessage="Nenhum outro elemento cadastrado para este nicho."
              badgeLabel="Nicho"
            />
          </section>
        );
      })}
    </div>
  );
}

interface TargetingListProps {
  title: string;
  elements: TargetingElement[];
  emptyMessage: string;
  badgeLabel: string;
}

function TargetingList({ title, elements, emptyMessage, badgeLabel }: TargetingListProps) {
  const list = Array.isArray(elements) ? elements : [];

  return (
    <div className="mb-4">
      <div className="d-flex align-items-center mb-2">
        <h6 className="mb-0">{title}</h6>
        <span className="badge text-bg-light ms-2">{list.length}</span>
      </div>
      {list.length === 0 ? (
        <p className="text-muted">{emptyMessage}</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-3">
          {list.map((element) => (
            <div key={element.id} className="col">
              <TargetingElementCard element={element} badgeLabel={badgeLabel} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
