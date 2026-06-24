import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  useExperimentTargetingSelections,
  useSaveExperimentTargetingSelections,
} from "../../api/experiment/useExperimentTargetingSelections";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import type {
  TargetingCandidateType,
  TargetingElement,
} from "../../api/targeting/types";

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

  const quantifiedMetaOptions = useMemo(
    () => availableOptions.filter((item) => formatMetaAudienceRange(item)),
    [availableOptions],
  );

  const grouped = useMemo(() => {
    return {
      INTEREST: availableOptions.filter((item) => item.type === "INTEREST"),
      JOB_TITLE: availableOptions.filter((item) => item.type === "JOB_TITLE"),
      BEHAVIOR: availableOptions.filter((item) => item.type === "BEHAVIOR"),
    };
  }, [availableOptions]);

  const selectedWithoutMeta = useMemo(
    () =>
      availableOptions.filter(
        (item) => selected.has(buildKey(item)) && !isMetaUsable(item),
      ),
    [availableOptions, selected],
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
    const selectedItems = availableOptions.filter((item) =>
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
    <div className="card mt-3">
      <div className="card-body d-flex flex-column gap-3">
        <div>
          <h5 className="card-title mb-1">Público</h5>
          <p className="text-muted mb-0 small">
            Marque somente públicos aprovados e com ID oficial da Meta. Itens
            sem ID Meta aparecem para diagnóstico, mas não devem ser usados em
            campanha.
          </p>
          {alterationLocked ? (
            <div className="alert alert-secondary mt-3 mb-0" role="status">
              Público bloqueado para alteração porque o experimento já foi
              liberado ou está em execução.
            </div>
          ) : null}
          <div className="alert alert-info mt-3 mb-0 small" role="status">
            <strong>Pronto para Meta</strong> significa que o item possui
            identificador oficial da Meta e pode entrar no targeting da
            campanha.
            <strong className="ms-1">Sem ID Meta</strong> precisa ser resolvido
            antes de publicar.
          </div>
          {selectedWithoutMeta.length > 0 ? (
            <div className="alert alert-warning mt-3 mb-0 small" role="alert">
              Remova {selectedWithoutMeta.length} item
              {selectedWithoutMeta.length === 1 ? "" : "s"} sem ID Meta antes de
              salvar o público.
            </div>
          ) : null}
          {quantifiedMetaOptions.length > 0 ? (
            <div className="alert alert-success mt-3 mb-0 small" role="status">
              <div className="fw-semibold mb-2">
                Públicos com alcance quantificado pela Meta
              </div>
              <div className="d-flex flex-wrap gap-2">
                {quantifiedMetaOptions.map((item) => (
                  <span
                    className="badge text-bg-light border"
                    key={buildKey(item)}
                  >
                    {item.term}: {formatMetaAudienceRange(item)}
                  </span>
                ))}
              </div>
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
          <div className="row g-3">
            {(["INTEREST", "JOB_TITLE", "BEHAVIOR"] as const).map((type) => (
              <div key={type} className="col-12 col-lg-4">
                <div className="border rounded p-3 h-100">
                  <h6 className="mb-2">{TYPE_LABEL[type]}</h6>
                  <div className="d-flex flex-column gap-2">
                    {grouped[type].map((item) => {
                      const key = buildKey(item);
                      const itemSelected = selected.has(key);
                      const metaUsable = isMetaUsable(item);
                      return (
                        <div className="form-check" key={key}>
                          <input
                            id={key}
                            className="form-check-input"
                            type="checkbox"
                            checked={itemSelected}
                            onChange={() => toggleSelection(item)}
                            disabled={
                              saveSelections.isPending ||
                              alterationLocked ||
                              (!metaUsable && !itemSelected)
                            }
                          />
                          <label htmlFor={key} className="form-check-label">
                            {item.term}
                            <span
                              className={`badge ms-2 ${
                                metaUsable
                                  ? "text-bg-success"
                                  : "text-bg-warning"
                              }`}
                            >
                              {metaUsable ? "Pronto para Meta" : "Sem ID Meta"}
                            </span>
                            {metaUsable && item.metaKey ? (
                              <span className="text-muted small ms-2">
                                {item.metaKey}
                              </span>
                            ) : null}
                            {formatMetaAudienceRange(item) ? (
                              <span className="badge text-bg-light border ms-2">
                                Meta: {formatMetaAudienceRange(item)}
                              </span>
                            ) : null}
                          </label>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="d-flex justify-content-end">
          <button
            className="btn btn-primary"
            onClick={handleSave}
            disabled={
              saveSelections.isPending ||
              isLoading ||
              alterationLocked ||
              selectedWithoutMeta.length > 0
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
