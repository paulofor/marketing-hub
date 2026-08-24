import { useState } from "react";
import type {
  ExperimentRun,
  ExperimentRunGateGroup,
  ExperimentRunGateResult,
  ExperimentRunGateStatus,
  ExperimentRunPreflight,
} from "../../api/experiment/useExperimentRuns";
import {
  useCreateExperimentRun,
  useExperimentRunPreflight,
  useExperimentRuns,
  useRecordExperimentRunHomologation,
  useRunExperimentPreflight,
} from "../../api/experiment/useExperimentRuns";

const gateGroupLabels: Record<ExperimentRunGateGroup, string> = {
  UPSTREAM_QUALITY: "Qualidade upstream",
  COMMERCIAL_EVIDENCE: "Evidência comercial",
  EXPERIMENT_DESIGN: "Desenho experimental",
  ASSET_QUALITY: "Qualidade dos ativos",
  FUNCTIONAL_E2E: "Teste ponta a ponta",
  DISTRIBUTION: "Distribuição",
  META_PUBLICATION: "Publicação Meta",
  MEASUREMENT: "Mensuração",
};

const homologationGateCodes = new Set([
  "LANDING_QUALITY_REVIEW_APPROVED",
  "FORM_CAN_BE_SUBMITTED",
  "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED",
  "META_EFFECTIVE_STATUS_CONFIRMED",
  "DIRECT_CHANNEL_READINESS_CONFIRMED",
  "DATA_FRESHNESS_VALID",
]);

type HomologationDraft = {
  status: Extract<ExperimentRunGateStatus, "PASS" | "FAIL"> | "";
  summary: string;
  evidenceReference: string;
};

const statusLabels: Record<string, string> = {
  DRAFT: "Rascunho",
  PREFLIGHT_PENDING: "Preflight pendente",
  PREFLIGHT_RUNNING: "Preflight em execução",
  PREFLIGHT_FAILED: "Preflight bloqueado",
  READY_TO_PUBLISH: "Pronto para publicar",
  PUBLICATION_PENDING: "Publicação pendente",
  PUBLISHING: "Publicando",
  PUBLISHED_AWAITING_EXPOSURE: "Publicado sem exposição confirmada",
  RUNNING: "Execução comercial",
  PAUSE_REQUESTED: "Pausa solicitada",
  PAUSED: "Pausado",
  STOP_REQUESTED: "Parada solicitada",
  COMPLETED: "Concluído",
  FAILED: "Falhou",
  CANCELLED: "Cancelado",
  NOT_EVALUATED: "Não avaliada",
  TECHNICALLY_INVALID: "Inválida tecnicamente",
  MEASUREMENT_INVALID: "Medição inválida",
  STRATEGICALLY_INVALID: "Inválida estrategicamente",
  INSUFFICIENT_DATA: "Dados insuficientes",
  COMMERCIALLY_VALID: "Comercialmente válida",
  UNKNOWN: "Desconhecida",
  VALID: "Válida",
  WARNING: "Atenção",
  BLOCKED: "Bloqueada",
  STALE: "Desatualizada",
};

function label(value?: string | null) {
  if (!value) return "—";
  return statusLabels[value] ?? value;
}

function badgeClass(value?: string | null) {
  if (!value) return "text-bg-secondary";
  if (
    ["READY_TO_PUBLISH", "COMMERCIALLY_VALID", "VALID", "PASS"].includes(value)
  ) {
    return "text-bg-success";
  }
  if (
    [
      "PREFLIGHT_FAILED",
      "TECHNICALLY_INVALID",
      "MEASUREMENT_INVALID",
      "STRATEGICALLY_INVALID",
      "BLOCKED",
      "FAIL",
    ].includes(value)
  ) {
    return "text-bg-danger";
  }
  if (["WARNING", "INSUFFICIENT_DATA", "PENDING"].includes(value)) {
    return "text-bg-warning text-dark";
  }
  return "text-bg-secondary";
}

function latestRun(runs?: ExperimentRun[]) {
  return [...(runs ?? [])].sort(
    (left, right) => right.runNumber - left.runNumber,
  )[0];
}

function groupGates(gates: ExperimentRunGateResult[]) {
  return gates.reduce<Record<string, ExperimentRunGateResult[]>>(
    (acc, gate) => {
      acc[gate.gateGroup] = [...(acc[gate.gateGroup] ?? []), gate];
      return acc;
    },
    {},
  );
}

type ExperimentRunPanelProps = {
  experimentId: string;
  compact?: boolean;
};

export default function ExperimentRunPanel({
  experimentId,
  compact = false,
}: ExperimentRunPanelProps) {
  const runsQuery = useExperimentRuns(experimentId);
  const currentRun = latestRun(runsQuery.data);
  const preflightQuery = useExperimentRunPreflight(currentRun?.id);
  const createRun = useCreateExperimentRun(experimentId);
  const runPreflight = useRunExperimentPreflight(experimentId);
  const recordHomologation = useRecordExperimentRunHomologation(experimentId);
  const preflight = preflightQuery.data;
  const gateGroups = groupGates(preflight?.gates ?? []);
  const homologationGates = (preflight?.gates ?? []).filter((gate) =>
    homologationGateCodes.has(gate.gateCode),
  );
  const [openGateGroup, setOpenGateGroup] = useState<string | null>(null);
  const [homologationDrafts, setHomologationDrafts] = useState<
    Record<string, HomologationDraft>
  >({});

  const homologationReady =
    homologationGates.length === 4 &&
    homologationGates.every((gate) => {
      const draft = homologationDrafts[gate.gateCode];
      return Boolean(
        draft?.status && draft.summary.trim() && draft.evidenceReference.trim(),
      );
    });

  const handleCreateRun = () => {
    if (!createRun.isPending) {
      createRun.mutate("PRODUCTION");
    }
  };

  const handleRunPreflight = () => {
    if (currentRun && !runPreflight.isPending) {
      runPreflight.mutate(currentRun.id);
    }
  };

  const updateHomologationDraft = (
    gateCode: string,
    field: keyof HomologationDraft,
    value: string,
  ) => {
    setHomologationDrafts((current) => ({
      ...current,
      [gateCode]: {
        status: current[gateCode]?.status ?? "",
        summary: current[gateCode]?.summary ?? "",
        evidenceReference: current[gateCode]?.evidenceReference ?? "",
        [field]: value,
      } as HomologationDraft,
    }));
  };

  const handleRecordHomologation = () => {
    if (!currentRun || !homologationReady || recordHomologation.isPending) {
      return;
    }
    recordHomologation.mutate({
      runId: currentRun.id,
      gates: homologationGates.map((gate) => {
        const draft = homologationDrafts[gate.gateCode];
        return {
          gateCode: gate.gateCode,
          status: draft.status as Extract<
            ExperimentRunGateStatus,
            "PASS" | "FAIL"
          >,
          summary: draft.summary.trim(),
          evidenceReference: draft.evidenceReference.trim(),
        };
      }),
    });
  };

  return (
    <div className="card">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h5 className="card-title mb-1">Execução atual</h5>
            <p className="text-muted small mb-0">
              Verdade operacional do backend sobre a tentativa de colocar este
              experimento no mercado.
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              onClick={handleCreateRun}
              disabled={createRun.isPending || runsQuery.isLoading}
            >
              {createRun.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              Criar run
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleRunPreflight}
              disabled={!currentRun || runPreflight.isPending}
            >
              {runPreflight.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              Rodar preflight
            </button>
          </div>
        </div>

        {runsQuery.isLoading ? (
          <div className="text-muted small mt-3">
            Carregando execução atual...
          </div>
        ) : !currentRun ? (
          <div className="alert alert-info mt-3 mb-0">
            Nenhum run foi criado para este experimento. Crie um run antes de
            interpretar falha técnica como resultado de mercado.
          </div>
        ) : (
          <>
            <div className="row g-3 mt-1">
              <StatusItem label="Run" value={`#${currentRun.runNumber}`} />
              <StatusItem label="Modo" value={currentRun.mode} />
              <StatusItem
                label="Status"
                value={label(currentRun.status)}
                badge={currentRun.status}
              />
              <StatusItem
                label="Validade"
                value={label(currentRun.evidenceValidity)}
                badge={currentRun.evidenceValidity}
              />
              <StatusItem
                label="Dados"
                value={label(currentRun.dataQualityStatus)}
                badge={currentRun.dataQualityStatus}
              />
              <StatusItem
                label="Exposição confirmada"
                value={currentRun.firstVerifiedImpressionAt ?? "—"}
              />
            </div>

            {!compact ? (
              <div className="mt-4">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <h6 className="mb-0">Checklist de preparação</h6>
                  {preflight ? (
                    <span
                      className={`badge ${preflight.hasBlockers ? "text-bg-danger" : "text-bg-success"}`}
                    >
                      {preflight.hasBlockers
                        ? "Com bloqueadores"
                        : "Sem bloqueadores"}
                    </span>
                  ) : null}
                </div>
                {preflightQuery.isLoading ? (
                  <div className="text-muted small">
                    Carregando preflight...
                  </div>
                ) : !preflight || preflight.gates.length === 0 ? (
                  <div className="text-muted small">
                    Preflight ainda não executado.
                  </div>
                ) : (
                  <>
                    <div
                      className="accordion"
                      id={`experiment-run-preflight-${currentRun.id}`}
                    >
                      {Object.entries(gateGroups).map(
                        ([group, gates], index) => (
                          <div className="accordion-item" key={group}>
                            <h2 className="accordion-header">
                              <button
                                className={`accordion-button ${openGateGroup === group || (!openGateGroup && index === 0) ? "" : "collapsed"}`}
                                type="button"
                                aria-expanded={
                                  openGateGroup === group ||
                                  (!openGateGroup && index === 0)
                                }
                                aria-controls={`experiment-run-preflight-${currentRun.id}-${group}`}
                                onClick={() => setOpenGateGroup(group)}
                              >
                                {gateGroupLabels[
                                  group as ExperimentRunGateGroup
                                ] ?? group}
                              </button>
                            </h2>
                            <div
                              id={`experiment-run-preflight-${currentRun.id}-${group}`}
                              className={`accordion-collapse collapse ${openGateGroup === group || (!openGateGroup && index === 0) ? "show" : ""}`}
                            >
                              <div className="accordion-body p-0">
                                <ul className="list-group list-group-flush">
                                  {gates.map((gate) => (
                                    <li
                                      className="list-group-item"
                                      key={gate.gateCode}
                                    >
                                      <div className="d-flex flex-wrap justify-content-between gap-2">
                                        <div>
                                          <div className="fw-semibold">
                                            {gate.gateCode}
                                          </div>
                                          <div className="text-muted small">
                                            {gate.summary}
                                          </div>
                                          {gate.remediationCode ? (
                                            <div className="small text-danger">
                                              Remediação: {gate.remediationCode}
                                            </div>
                                          ) : null}
                                        </div>
                                        <span
                                          className={`badge align-self-start ${badgeClass(gate.status)}`}
                                        >
                                          {gate.status}
                                        </span>
                                      </div>
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            </div>
                          </div>
                        ),
                      )}
                    </div>
                    {homologationGates.length > 0 ? (
                      <section
                        className="card border-primary mt-3"
                        aria-label="Evidências da homologação funcional"
                      >
                        <div className="card-body">
                          <h6 className="mb-1">
                            Evidências da homologação funcional
                          </h6>
                          <p className="text-muted small">
                            Registre os quatro resultados do run. Evidência
                            ausente ou reprovada mantém o preflight bloqueado.
                          </p>
                          <div className="d-flex flex-column gap-3">
                            {homologationGates.map((gate) => {
                              const draft = homologationDrafts[
                                gate.gateCode
                              ] ?? {
                                status: "",
                                summary: "",
                                evidenceReference: "",
                              };
                              return (
                                <fieldset
                                  className="border rounded p-3"
                                  key={gate.gateCode}
                                >
                                  <legend className="float-none w-auto fs-6 px-1">
                                    {gate.gateCode}
                                  </legend>
                                  <div className="row g-2">
                                    <div className="col-12 col-md-3">
                                      <label className="form-label small fw-semibold">
                                        Resultado
                                        <select
                                          className="form-select"
                                          aria-label={`Resultado ${gate.gateCode}`}
                                          value={draft.status}
                                          onChange={(event) =>
                                            updateHomologationDraft(
                                              gate.gateCode,
                                              "status",
                                              event.target.value,
                                            )
                                          }
                                        >
                                          <option value="">Selecione</option>
                                          <option value="PASS">Aprovado</option>
                                          <option value="FAIL">
                                            Reprovado
                                          </option>
                                        </select>
                                      </label>
                                    </div>
                                    <div className="col-12 col-md-9">
                                      <label className="form-label small fw-semibold">
                                        Evidência ou artefato
                                        <input
                                          className="form-control"
                                          aria-label={`Evidência ${gate.gateCode}`}
                                          value={draft.evidenceReference}
                                          onChange={(event) =>
                                            updateHomologationDraft(
                                              gate.gateCode,
                                              "evidenceReference",
                                              event.target.value,
                                            )
                                          }
                                          placeholder="URL, contrato ou referência auditável"
                                        />
                                      </label>
                                    </div>
                                    <div className="col-12">
                                      <label className="form-label small fw-semibold">
                                        Conclusão observada
                                        <textarea
                                          className="form-control"
                                          aria-label={`Conclusão ${gate.gateCode}`}
                                          rows={2}
                                          value={draft.summary}
                                          onChange={(event) =>
                                            updateHomologationDraft(
                                              gate.gateCode,
                                              "summary",
                                              event.target.value,
                                            )
                                          }
                                        />
                                      </label>
                                    </div>
                                  </div>
                                </fieldset>
                              );
                            })}
                          </div>
                          {recordHomologation.isError ? (
                            <div className="alert alert-danger py-2 mt-3 mb-0">
                              Não foi possível registrar a homologação. Revise
                              os quatro gates e tente novamente.
                            </div>
                          ) : null}
                          <button
                            type="button"
                            className="btn btn-primary mt-3"
                            disabled={
                              !homologationReady || recordHomologation.isPending
                            }
                            onClick={handleRecordHomologation}
                          >
                            {recordHomologation.isPending
                              ? "Registrando..."
                              : "Registrar homologação"}
                          </button>
                        </div>
                      </section>
                    ) : null}
                  </>
                )}
              </div>
            ) : null}
          </>
        )}
      </div>
    </div>
  );
}

function StatusItem({
  label,
  value,
  badge,
}: {
  label: string;
  value: string;
  badge?: string;
}) {
  return (
    <div className="col-12 col-md-4 col-xl-2">
      <div className="text-muted small">{label}</div>
      {badge ? (
        <span className={`badge ${badgeClass(badge)}`}>{value}</span>
      ) : (
        <div className="fw-semibold text-break">{value}</div>
      )}
    </div>
  );
}
