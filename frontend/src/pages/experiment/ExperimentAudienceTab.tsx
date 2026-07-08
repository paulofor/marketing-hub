import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2, HelpCircle, Users } from "lucide-react";
import {
  useExperimentTargetingSelections,
  useSaveExperimentTargetingSelections,
} from "../../api/experiment/useExperimentTargetingSelections";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import type {
  TargetingCandidateType,
  TargetingElement,
} from "../../api/targeting/types";
import "./ExperimentAudienceTab.css";

interface ExperimentAudienceTabProps {
  experimentId: number;
  nicheId?: number | null;
  alterationLocked?: boolean;
}

const TYPE_LABEL: Record<TargetingElement["type"], string> = {
  INTEREST: "Interesse",
  JOB_TITLE: "Cargo",
  BEHAVIOR: "Comportamento",
};

function toCandidateType(
  type: TargetingElement["type"],
): TargetingCandidateType {
  return type === "JOB_TITLE" ? "WORK_POSITION" : type;
}

function buildKey(element: TargetingElement) {
  return `${element.type}::${element.id}`;
}

function isMetaUsable(element: TargetingElement) {
  return Boolean(element.metaId?.trim());
}

function formatAudienceSize(value: number) {
  return new Intl.NumberFormat("pt-BR").format(value);
}

function formatMetaAudienceRange(element: TargetingElement) {
  const lower = element.metaAudienceSizeLowerBound;
  const upper = element.metaAudienceSizeUpperBound;

  if (typeof lower === "number" && typeof upper === "number") {
    if (lower === upper) {
      return `${formatAudienceSize(lower)} pessoas`;
    }
    return `${formatAudienceSize(lower)} a ${formatAudienceSize(upper)} pessoas`;
  }

  if (typeof lower === "number") {
    return `a partir de ${formatAudienceSize(lower)} pessoas`;
  }

  if (typeof upper === "number") {
    return `até ${formatAudienceSize(upper)} pessoas`;
  }

  return null;
}

function getAudienceBounds(element: TargetingElement) {
  const lower =
    typeof element.metaAudienceSizeLowerBound === "number"
      ? element.metaAudienceSizeLowerBound
      : 0;
  const upper =
    typeof element.metaAudienceSizeUpperBound === "number"
      ? element.metaAudienceSizeUpperBound
      : lower;
  return { lower, upper };
}

function formatCombinedAudienceRange(lower: number, upper: number) {
  if (lower === 0 && upper === 0) {
    return "Sem alcance informado";
  }
  if (lower === upper) {
    return `${formatAudienceSize(lower)} pessoas`;
  }
  return `${formatAudienceSize(lower)} a ${formatAudienceSize(upper)} pessoas`;
}

export function ExperimentAudienceTab({
  experimentId,
  nicheId,
  alterationLocked = false,
}: ExperimentAudienceTabProps) {
  const nicheIdAsString = nicheId != null ? String(nicheId) : undefined;
  const { data: elements, isLoading } = useTargetingElementsByNiche(
    nicheIdAsString,
    { status: "APPROVED" },
  );
  const { data: allElements, isLoading: isLoadingAllElements } =
    useTargetingElementsByNiche(nicheIdAsString);
  const { data: savedSelections } =
    useExperimentTargetingSelections(experimentId);
  const saveSelections = useSaveExperimentTargetingSelections(experimentId);

  const availableOptions = useMemo(
    () =>
      (elements ?? []).filter((item) =>
        ["INTEREST", "JOB_TITLE", "BEHAVIOR"].includes(item.type),
      ),
    [elements],
  );

  const [selected, setSelected] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!savedSelections || availableOptions.length === 0) {
      return;
    }
    const byId = new Set<number>();
    savedSelections.forEach((entry) => {
      if (typeof entry.targetingElementId === "number") {
        byId.add(entry.targetingElementId);
      }
    });
    const next = new Set<string>();
    availableOptions.forEach((item) => {
      if (byId.has(item.id)) next.add(buildKey(item));
    });
    setSelected(next);
  }, [savedSelections, availableOptions]);

  const totalElements = Array.isArray(allElements) ? allElements.length : 0;
  const unavailableElements = Math.max(
    totalElements - availableOptions.length,
    0,
  );

  const readyOptions = useMemo(
    () => availableOptions.filter((item) => isMetaUsable(item)),
    [availableOptions],
  );

  const diagnosticOptions = useMemo(
    () => availableOptions.filter((item) => !isMetaUsable(item)),
    [availableOptions],
  );

  const grouped = useMemo(() => {
    return {
      INTEREST: readyOptions.filter((item) => item.type === "INTEREST"),
      JOB_TITLE: readyOptions.filter((item) => item.type === "JOB_TITLE"),
      BEHAVIOR: readyOptions.filter((item) => item.type === "BEHAVIOR"),
    };
  }, [readyOptions]);

  const selectedWithoutMeta = useMemo(
    () =>
      availableOptions.filter(
        (item) => selected.has(buildKey(item)) && !isMetaUsable(item),
      ),
    [availableOptions, selected],
  );

  const selectedReadyOptions = useMemo(
    () => readyOptions.filter((item) => selected.has(buildKey(item))),
    [readyOptions, selected],
  );

  const selectedAudienceRange = useMemo(
    () =>
      selectedReadyOptions.reduce(
        (acc, item) => {
          const bounds = getAudienceBounds(item);
          return {
            lower: acc.lower + bounds.lower,
            upper: acc.upper + bounds.upper,
          };
        },
        { lower: 0, upper: 0 },
      ),
    [selectedReadyOptions],
  );

  const toggleSelection = (item: TargetingElement) => {
    if (alterationLocked) return;
    setSelected((prev) => {
      const next = new Set(prev);
      const key = buildKey(item);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const handleSave = async () => {
    if (alterationLocked) return;
    const selectedItems = readyOptions.filter((item) =>
      selected.has(buildKey(item)),
    );
    await saveSelections.mutateAsync({
      items: selectedItems.map((item) => ({
        candidateType: toCandidateType(item.type),
        term: item.term,
        targetingElementId: item.id,
      })),
    });
  };

  if (nicheId == null) {
    return (
      <div className="alert alert-warning mt-3 mb-0">
        Experimento sem nicho vinculado.
      </div>
    );
  }

  return (
    <div className="card mt-3 audience-tab">
      <div className="card-body d-flex flex-column gap-4">
        <div className="audience-header">
          <div>
            <h5 className="card-title mb-1">Público da campanha</h5>
            <p className="text-muted mb-0 small">
              Monte um público simples, válido para Meta Ads e fácil de publicar
              no worker.
            </p>
          </div>
          <div className="audience-header__status">
            <CheckCircle2 size={16} aria-hidden="true" />
            {readyOptions.length} pronto{readyOptions.length === 1 ? "" : "s"}
          </div>
        </div>

        <div className="audience-summary-grid" aria-label="Resumo do público">
          <div className="audience-summary-item audience-summary-item--primary">
            <span>Selecionados</span>
            <strong>{selectedReadyOptions.length}</strong>
            <small>itens que entrarão no targeting</small>
          </div>
          <div className="audience-summary-item">
            <span>Prontos para Meta</span>
            <strong>{readyOptions.length}</strong>
            <small>com ID oficial da Meta</small>
          </div>
          <div className="audience-summary-item">
            <span>Alcance selecionado</span>
            <strong>
              {formatCombinedAudienceRange(
                selectedAudienceRange.lower,
                selectedAudienceRange.upper,
              )}
            </strong>
            <small>soma dos intervalos informados</small>
          </div>
          <div className="audience-summary-item">
            <span>Diagnóstico</span>
            <strong>{diagnosticOptions.length}</strong>
            <small>aprovados sem ID Meta</small>
          </div>
        </div>

        <div className="audience-guidance">
          <div className="audience-guidance__icon">
            <HelpCircle size={18} aria-hidden="true" />
          </div>
          <div>
            <strong>Regra prática:</strong> salve pelo menos um item pronto para
            Meta. Interesses, cargos e comportamentos selecionados entram como
            ampliação de alcance para o teste de campanha.
          </div>
        </div>

        <div>
          {alterationLocked ? (
            <div className="alert alert-secondary mt-3 mb-0" role="status">
              Público bloqueado para alteração porque o experimento já foi
              liberado ou está em execução.
            </div>
          ) : null}
          {selectedWithoutMeta.length > 0 ? (
            <div className="alert alert-warning mt-3 mb-0 small" role="alert">
              {selectedWithoutMeta.length} item
              {selectedWithoutMeta.length === 1 ? "" : "s"} salvo
              {selectedWithoutMeta.length === 1 ? "" : "s"} anteriormente sem ID
              Meta não entrará
              {selectedWithoutMeta.length === 1 ? "" : "ão"} no próximo
              salvamento.
            </div>
          ) : null}
        </div>

        {isLoading ? (
          <div className="text-muted small">Carregando opções do nicho...</div>
        ) : availableOptions.length === 0 ? (
          <div className="alert alert-warning mb-0 small" role="status">
            <div className="fw-semibold mb-2">
              Nenhum público aprovado apareceu para este experimento.
            </div>
            <ol className="mb-3 ps-3">
              <li>Abra a tela do nicho e vá em Segmentação Meta Ads.</li>
              <li>Gere interesses, cargos ou comportamentos.</li>
              <li>Depois aprove somente os itens com ID oficial da Meta.</li>
              <li>Volte aqui para selecionar o público do experimento.</li>
            </ol>
            {isLoadingAllElements ? (
              <div className="text-muted mb-3">
                Conferindo se existem públicos pendentes ou recusados...
              </div>
            ) : unavailableElements > 0 ? (
              <div className="mb-3">
                Já existem {unavailableElements} item
                {unavailableElements === 1 ? "" : "s"} neste nicho, mas ainda
                não estão aprovados para campanha.
              </div>
            ) : (
              <div className="mb-3">
                Ainda não existe nenhum item de público gerado para este nicho.
              </div>
            )}
            <Link
              className="btn btn-outline-primary btn-sm"
              to={`/niches/${nicheId}#niche-targeting`}
            >
              Gerar públicos no nicho
            </Link>
          </div>
        ) : (
          <div className="audience-category-grid">
            {(["INTEREST", "JOB_TITLE", "BEHAVIOR"] as const).map((type) => (
              <section key={type} className="audience-category">
                <div className="audience-category__header">
                  <div>
                    <h6>{TYPE_LABEL[type]}</h6>
                    <span>
                      {
                        grouped[type].filter((item) =>
                          selected.has(buildKey(item)),
                        ).length
                      }{" "}
                      selecionado
                      {grouped[type].filter((item) =>
                        selected.has(buildKey(item)),
                      ).length === 1
                        ? ""
                        : "s"}
                    </span>
                  </div>
                  <Users size={18} aria-hidden="true" />
                </div>
                {grouped[type].length === 0 ? (
                  <div className="audience-empty-state">
                    Nenhum item pronto nesta categoria.
                  </div>
                ) : (
                  <div className="audience-option-list">
                    {grouped[type].map((item) => {
                      const key = buildKey(item);
                      const itemSelected = selected.has(key);
                      const audienceRange = formatMetaAudienceRange(item);
                      return (
                        <label
                          className={`audience-option ${
                            itemSelected ? "audience-option--selected" : ""
                          }`}
                          htmlFor={key}
                          key={key}
                        >
                          <input
                            id={key}
                            className="form-check-input"
                            type="checkbox"
                            checked={itemSelected}
                            onChange={() => toggleSelection(item)}
                            disabled={
                              saveSelections.isPending || alterationLocked
                            }
                          />
                          <span className="audience-option__content">
                            <span className="audience-option__title">
                              {item.term}
                            </span>
                            <span className="audience-option__meta">
                              {item.metaKey ? (
                                <span>Meta: {item.metaKey}</span>
                              ) : null}
                              {audienceRange ? (
                                <span>{audienceRange}</span>
                              ) : (
                                <span>Alcance não informado</span>
                              )}
                            </span>
                          </span>
                          <span className="audience-option__badge">Pronto</span>
                        </label>
                      );
                    })}
                  </div>
                )}
              </section>
            ))}
          </div>
        )}

        {diagnosticOptions.length > 0 ? (
          <details className="audience-diagnostic">
            <summary>
              <span>
                <AlertTriangle size={16} aria-hidden="true" />
                Itens aprovados sem ID Meta
              </span>
              <strong>{diagnosticOptions.length}</strong>
            </summary>
            <div className="audience-diagnostic__body">
              <p>
                Estes itens ficam fora da seleção porque ainda não possuem o
                identificador oficial necessário para publicação na Meta.
              </p>
              <div className="audience-diagnostic__list">
                {diagnosticOptions.map((item) => (
                  <div
                    className="audience-diagnostic__item"
                    key={buildKey(item)}
                  >
                    <span>{item.term}</span>
                    <small>{TYPE_LABEL[item.type]}</small>
                  </div>
                ))}
              </div>
            </div>
          </details>
        ) : null}

        {selectedReadyOptions.length > 0 ? (
          <details className="audience-selected-details">
            <summary>Ver público selecionado</summary>
            <div className="audience-selected-details__body">
              {selectedReadyOptions.map((item) => (
                <div
                  className="audience-selected-details__item"
                  key={buildKey(item)}
                >
                  <div>
                    <strong>{item.term}</strong>
                    <span>{TYPE_LABEL[item.type]}</span>
                  </div>
                  <small>
                    {formatMetaAudienceRange(item) ?? "Sem alcance"}
                  </small>
                </div>
              ))}
            </div>
          </details>
        ) : null}

        <div className="audience-actions">
          <div className="text-muted small">
            {selectedReadyOptions.length === 0
              ? "Selecione pelo menos um público pronto para liberar a campanha."
              : `${selectedReadyOptions.length} item${
                  selectedReadyOptions.length === 1 ? "" : "s"
                } pronto${
                  selectedReadyOptions.length === 1 ? "" : "s"
                } para salvar.`}
          </div>
          <button
            className="btn btn-primary"
            onClick={handleSave}
            disabled={
              saveSelections.isPending ||
              isLoading ||
              alterationLocked ||
              selectedReadyOptions.length === 0
            }
          >
            {saveSelections.isPending ? (
              <span className="d-inline-flex align-items-center gap-2">
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />
                Salvando...
              </span>
            ) : (
              "Salvar público"
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
