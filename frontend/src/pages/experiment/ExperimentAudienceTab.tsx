import { useEffect, useMemo, useState } from "react";
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
  const { data: savedSelections } =
    useExperimentTargetingSelections(experimentId);
  const saveSelections = useSaveExperimentTargetingSelections(experimentId);

  const availableOptions = useMemo(
    () =>
      (elements ?? []).filter(
        (item) => item.type === "JOB_TITLE" || item.type === "BEHAVIOR",
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

  const grouped = useMemo(() => {
    return {
      JOB_TITLE: availableOptions.filter((item) => item.type === "JOB_TITLE"),
      BEHAVIOR: availableOptions.filter((item) => item.type === "BEHAVIOR"),
    };
  }, [availableOptions]);

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
            Marque somente cargos e comportamentos já aprovados para o nicho.
          </p>
          {alterationLocked ? (
            <div className="alert alert-secondary mt-3 mb-0" role="status">
              Público bloqueado para alteração porque o experimento já foi
              liberado ou está em execução.
            </div>
          ) : null}
        </div>

        {isLoading ? (
          <div className="text-muted small">Carregando opções do nicho...</div>
        ) : availableOptions.length === 0 ? (
          <div className="text-muted small">
            Nenhum cargo/comportamento aprovado foi encontrado para este nicho.
          </div>
        ) : (
          <div className="row g-3">
            {(["JOB_TITLE", "BEHAVIOR"] as const).map((type) => (
              <div key={type} className="col-12 col-lg-6">
                <div className="border rounded p-3 h-100">
                  <h6 className="mb-2">{TYPE_LABEL[type]}</h6>
                  <div className="d-flex flex-column gap-2">
                    {grouped[type].map((item) => {
                      const key = buildKey(item);
                      return (
                        <div className="form-check" key={key}>
                          <input
                            id={key}
                            className="form-check-input"
                            type="checkbox"
                            checked={selected.has(key)}
                            onChange={() => toggleSelection(item)}
                            disabled={
                              saveSelections.isPending || alterationLocked
                            }
                          />
                          <label htmlFor={key} className="form-check-label">
                            {item.term}
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
            disabled={saveSelections.isPending || isLoading || alterationLocked}
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
