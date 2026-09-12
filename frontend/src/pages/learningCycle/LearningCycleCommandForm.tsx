import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  cycleError,
  useCycleMutation,
  type CycleCatalog,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

import type { DecisionProposal } from "../../api/learningCycle/useDecisionProposal";

type Field = [
  string,
  string,
  "text" | "number" | "checkbox" | "datetime-local",
];
const byStage: Record<string, Field[]> = {
  LEARNING: [
    ["learning", "Aprendizado e limites da evidência anterior", "text"],
    ["competingExplanation", "Explicação concorrente", "text"],
  ],
  PLANNING: [
    ["planReference", "Plano comercial e versão", "text"],
    ["stopRule", "Regra de parada", "text"],
  ],
  ADJUSTMENT: [
    ["productVersion", "Versão do produto", "text"],
    ["changeEvidence", "Evidência do ajuste útil ou da comunicação", "text"],
  ],
  VIDEO_BRIEF: [
    ["briefReference", "Briefing de Íris e versão", "text"],
    ["campaignGoal", "Objetivo do vídeo de campanha", "text"],
    ["campaignCta", "CTA do vídeo de campanha", "text"],
    ["campaignMetric", "Métrica do vídeo de campanha", "text"],
    ["pdeGoal", "Benefício demonstrado na entrada do PDE", "text"],
    ["pdeCta", "CTA após a demonstração", "text"],
    ["pdeMetric", "Métrica do vídeo de entrada", "text"],
    [
      "controlledVariables",
      "Hipótese principal e demais variáveis mantidas",
      "text",
    ],
    [
      "productionBudgetReference",
      "Referência do teto de produção e avaliação de Plutus",
      "text",
    ],
  ],
  CAMPAIGN_VIDEO: [
    ["campaignVideoAssetId", "Vídeo AD deste experimento", "number"],
    ["productionEvidence", "Evidência da produção no Estúdio", "text"],
  ],
  PDE_ENTRY_VIDEO: [
    ["pdeVideoAssetId", "Vídeo LANDING_HERO deste experimento", "number"],
    ["productionEvidence", "Evidência da demonstração da versão real", "text"],
  ],
  VIDEO_APPROVAL: [
    ["creativeId", "Criativo de campanha aprovado", "number"],
    ["pdeSlotId", "Versão PDE com vídeo integrado em rascunho", "number"],
    [
      "technicalEvidence",
      "Evidência de reprodução, fallback e desempenho",
      "text",
    ],
    [
      "customerReviewEvidence",
      "Evidência da avaliação independente de compreensão e utilidade",
      "text",
    ],
    ["captionsVerified", "Legendas e áudio revisados", "checkbox"],
    ["mobileVerified", "Reprodução e layout validados no celular", "checkbox"],
    [
      "optionalPlaybackVerified",
      "Vídeo opcional e CTA acessível sem assistir",
      "checkbox",
    ],
    [
      "testDataExcluded",
      "Testes segregados das métricas comerciais",
      "checkbox",
    ],
  ],
  VALIDATION: [
    ["approvalInstanceId", "Parecer multiagente aprovado", "number"],
    ["journeyEvidence", "Evidência de aprovação da jornada", "text"],
    [
      "humanObservationEvidence",
      "Observação humana consentida, se realizada (opcional)",
      "text",
    ],
    [
      "instrumentationVerified",
      "Instrumentação e segregação dos testes verificadas",
      "checkbox",
    ],
  ],
  AUTHORIZATION: [
    ["productVersion", "Versão homologada", "text"],
    ["budgetLimitBrl", "Teto total autorizado (R$)", "number"],
    [
      "confirmed",
      "Confirmo a autorização desta versão, orçamento e janela",
      "checkbox",
    ],
  ],
  PUBLICATION: [],
};
export default function LearningCycleCommandForm({
  cycle,
  catalog,
  onUpdated,
  decisionProposal,
}: {
  cycle: LearningCycle;
  catalog: CycleCatalog;
  decisionProposal?: DecisionProposal;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const [registerPrototype, setRegisterPrototype] = useState(false);
  const mutation = useCycleMutation(cycle.productId, cycle.id);
  const [action, setAction] = useState(
    decisionProposal?.proposal?.action ?? cycle.commands[0]?.action ?? "",
  );
  const [requestKey, setRequestKey] = useState(() => createCycleRequestKey());
  const command = cycle.commands.find((item) => item.action === action);
  const returnRequired = action === "ADJUST" || action === "REWORK";
  let fields: Field[] =
    action === "COMPLETE" ? (byStage[cycle.stage] ?? []) : [];
  if (returnRequired)
    fields = [
      ["rootCause", "Causa e impacto comercial", "text"],
      ...(action === "ADJUST"
        ? ([
            ["learning", "Aprendizado preservado", "text"],
            ["nextHypothesis", "Hipótese para o sucessor", "text"],
          ] as Field[])
        : ([
            ["productVersion", "Nova versão a homologar", "text"],
            [
              "technicalOnly",
              "Correção exclusivamente técnica: hipótese, oferta e aquisição permanecem iguais",
              "checkbox",
            ],
          ] as Field[])),
    ];
  if (action === "FIX_MEASUREMENT")
    fields = [
      ["rootCause", "Falha da medição", "text"],
      ["correctionPlan", "Correção e forma de validar", "text"],
    ];
  if (action === "SCALE")
    fields = [["scaleHypothesis", "Motivo e hipótese de expansão", "text"]];
  if (action === "AUTHORIZE_SCALE")
    fields = [
      ...byStage.AUTHORIZATION,
      ["windowEnd", "Novo fim da janela", "datetime-local"],
    ];
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const evidence: Record<string, unknown> = decisionProposal
      ? { decisionProposalId: decisionProposal.id, humanApproved: true }
      : {};
    fields.forEach(([key, , type]) => {
      const value = form.get(key);
      evidence[key] =
        type === "checkbox"
          ? form.has(key)
          : type === "number"
            ? Number(value)
            : type === "datetime-local"
              ? new Date(String(value)).toISOString()
              : String(value ?? "");
    });
    if (action === "REWORK" && registerPrototype) {
      evidence.privatePrototype = {
        prototypeVersion: evidence.productVersion,
        privateAccessUrl: form.get("privateAccessUrl"),
        image: form.get("prototypeImage"),
        evidenceReference: form.get("prototypeEvidence"),
        observedAt: new Date(
          String(form.get("prototypeObservedAt")),
        ).toISOString(),
        desktopValidated: form.has("desktopValidated"),
        mobileValidated: form.has("mobileValidated"),
        firstResultValidated: form.has("firstResultValidated"),
        resumeValidated: form.has("resumeValidated"),
        failuresValidated: form.has("failuresValidated"),
        testDataExcluded: form.has("testDataExcluded"),
        noExternalSideEffects: form.has("noExternalSideEffects"),
      };
    }
    if (returnRequired) {
      const [process, activity] = String(form.get("returnTarget")).split(":");
      Object.assign(evidence, {
        returnProcessId: Number(process),
        returnActivityId: activity,
      });
    }
    try {
      onUpdated(
        await mutation.mutateAsync({
          requestKey,
          expectedRevision: cycle.revision,
          action,
          operatorName: form.get("operatorName"),
          summary: form.get("summary"),
          evidenceReference: form.get("evidenceReference"),
          evidence,
        }),
      );
    } catch {
      /* Erro contratual visível no formulário. */
    }
  }
  if (!cycle.commands.length) return null;
  return (
    <form
      className="card card-body mb-3"
      onSubmit={submit}
      aria-label="Decisão do ciclo"
    >
      <h3 className="h5">Próxima ação</h3>
      <p>{cycle.nextAction}</p>
      <p className="small">Responsável: {cycle.responsible}</p>
      {cycle.workLinks?.length ? (
        <div className="mb-3">
          <p>
            Use o experimento #{cycle.experimentId} e a versão{" "}
            {cycle.productVersion}. O Estúdio mantém os gates de custo e
            aprovação.
          </p>
          <nav
            className="d-flex flex-wrap gap-2"
            aria-label="Produção e integração dos vídeos"
          >
            {cycle.workLinks.map((link) => (
              <Link
                key={link.url}
                className="btn btn-outline-primary btn-sm"
                to={link.url}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      ) : null}
      <label className="form-label">
        Decisão *
        <select
          name="action"
          className="form-select"
          value={action}
          onChange={(event) => {
            setAction(event.target.value);
            setRequestKey(createCycleRequestKey());
            mutation.reset();
          }}
        >
          {cycle.commands.map((item) => (
            <option
              key={item.action}
              value={item.action}
              disabled={!item.available}
            >
              {item.label}
              {item.available ? "" : " · bloqueado"}
            </option>
          ))}
        </select>
      </label>
      {cycle.commands
        .filter((item) => !item.available)
        .map((item) => (
          <p className="small text-body-secondary" key={item.action}>
            {item.label}: {item.reason}
          </p>
        ))}
      <div
        className="cycle-form-grid"
        key={
          decisionProposal ? decisionProposal.id : `${cycle.revision}-${action}`
        }
      >
        <label className="form-label">
          Responsável pela decisão *
          <input
            required
            name="operatorName"
            defaultValue={decisionProposal?.operatorName}
            className="form-control"
            maxLength={160}
          />
        </label>
        <label className="form-label">
          Síntese e justificativa *
          <textarea
            required
            name="summary"
            defaultValue={decisionProposal?.proposal?.summary}
            className="form-control"
            maxLength={4000}
          />
        </label>
        <label className="form-label">
          Referência da evidência *
          <input
            required
            name="evidenceReference"
            defaultValue={decisionProposal?.proposal?.evidenceReference}
            className="form-control"
            maxLength={1200}
            placeholder="Link do relatório, tarefa ou documento"
          />
        </label>
        {returnRequired ? (
          <label className="form-label">
            Atividade que corrigirá a causa *
            <select
              required
              name="returnTarget"
              className="form-select"
              defaultValue={
                decisionProposal?.proposal?.returnProcessId
                  ? `${decisionProposal.proposal.returnProcessId}:${decisionProposal.proposal.returnActivityId}`
                  : ""
              }
            >
              <option value="">Selecione o destino do retorno</option>
              {catalog.returnTargets.map((target) => (
                <option
                  key={`${target.processDefinitionId}:${target.activityId}`}
                  value={`${target.processDefinitionId}:${target.activityId}`}
                >
                  {target.processName} → {target.activityName} · {target.owner}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        {action === "REWORK" && (
          <fieldset className="border rounded p-3">
            <legend className="h6">Protótipo executável desta versão</legend>
            <label className="form-check">
              <input
                type="checkbox"
                checked={registerPrototype}
                onChange={(e) => setRegisterPrototype(e.target.checked)}
                className="form-check-input"
              />
              A nova versão já foi implementada e testada
            </label>
            {registerPrototype && (
              <>
                <p className="small">
                  Registre as provas da versão privada. Isso libera a correção
                  para revisão; a homologação multiagente continua obrigatória.
                </p>
                <label className="form-label">
                  URL privada sem parâmetros *
                  <input
                    className="form-control"
                    type="url"
                    name="privateAccessUrl"
                    required
                  />
                </label>
                <label className="form-label">
                  Imagem Docker testada *
                  <input
                    className="form-control"
                    name="prototypeImage"
                    required
                  />
                </label>
                <label className="form-label">
                  Relatório dos testes *
                  <textarea
                    className="form-control"
                    name="prototypeEvidence"
                    required
                    minLength={20}
                  />
                </label>
                <label className="form-label">
                  Data da verificação *
                  <input
                    className="form-control"
                    type="datetime-local"
                    name="prototypeObservedAt"
                    required
                  />
                </label>
                {[
                  ["desktopValidated", "Desktop validado"],
                  ["mobileValidated", "Celular validado"],
                  [
                    "firstResultValidated",
                    "Primeiro resultado aplicável validado",
                  ],
                  ["resumeValidated", "Salvar e retomar validados"],
                  ["failuresValidated", "Falhas e recuperação validadas"],
                  ["testDataExcluded", "Dados de teste segregados"],
                  [
                    "noExternalSideEffects",
                    "Sem cobrança, campanha ou gasto de mídia",
                  ],
                ].map(([key, label]) => (
                  <label className="form-check" key={key}>
                    <input
                      className="form-check-input"
                      type="checkbox"
                      name={key}
                      required
                    />
                    {label} *
                  </label>
                ))}
              </>
            )}
          </fieldset>
        )}
        {fields.map(([key, label, type]) => (
          <label
            className={
              type === "checkbox" ? "form-check cycle-check" : "form-label"
            }
            key={key}
          >
            {type !== "checkbox"
              ? `${label}${key === "humanObservationEvidence" ? "" : " *"}`
              : null}
            {[
              "approvalInstanceId",
              "campaignVideoAssetId",
              "pdeVideoAssetId",
              "creativeId",
              "pdeSlotId",
            ].includes(key) ? (
              <select
                required
                name={key}
                className="form-select"
                defaultValue=""
              >
                <option value="">
                  Selecione um registro elegível desta versão
                </option>
                {(key === "approvalInstanceId"
                  ? cycle.approvalOptions
                  : cycle.evidenceOptions?.[key]
                )?.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>
            ) : decisionProposal && type === "text" ? (
              <textarea
                className="form-control"
                name={key}
                required
                rows={3}
                maxLength={4000}
                defaultValue={String(
                  decisionProposal.proposal?.[
                    key as keyof NonNullable<DecisionProposal["proposal"]>
                  ] ?? "",
                )}
              />
            ) : (
              <input
                className={
                  type === "checkbox" ? "form-check-input" : "form-control"
                }
                name={key}
                type={type}
                required={
                  (type !== "checkbox" && key !== "humanObservationEvidence") ||
                  key === "confirmed" ||
                  key === "instrumentationVerified" ||
                  (cycle.stage === "VIDEO_APPROVAL" && action === "COMPLETE")
                }
                step={
                  type === "number"
                    ? key.endsWith("Brl")
                      ? "0.01"
                      : "1"
                    : undefined
                }
                min={
                  type === "number" && key !== "contributionBrl"
                    ? "0"
                    : undefined
                }
                maxLength={4000}
                readOnly={
                  key === "productionBudgetReference" &&
                  Boolean(cycle.videoBudget)
                }
                defaultValue={
                  key === "productionBudgetReference" && cycle.videoBudget
                    ? cycle.videoBudget.reference
                    : decisionProposal?.proposal &&
                        key in decisionProposal.proposal
                      ? String(
                          decisionProposal.proposal[
                            key as keyof typeof decisionProposal.proposal
                          ] ?? "",
                        )
                      : key === "productVersion" && action !== "REWORK"
                        ? cycle.productVersion
                        : key === "budgetLimitBrl" &&
                            action !== "AUTHORIZE_SCALE"
                          ? cycle.budgetLimitBrl
                          : undefined
                }
              />
            )}
            {type === "checkbox"
              ? `${label}${key === "confirmed" || key === "instrumentationVerified" || (cycle.stage === "VIDEO_APPROVAL" && action === "COMPLETE") ? " *" : ""}`
              : null}
          </label>
        ))}
      </div>
      {mutation.isError ? (
        <p role="alert" className="alert alert-danger">
          {cycleError(mutation.error)}
        </p>
      ) : null}
      <button
        className="btn btn-primary align-self-start"
        type="submit"
        disabled={mutation.isPending || !command?.available}
      >
        {mutation.isPending ? (
          <span
            className="spinner-border spinner-border-sm me-2"
            role="status"
            aria-label="Registrando"
          />
        ) : null}
        {decisionProposal
          ? "Aprovar decisão e registrar no BPM"
          : command?.label || "Registrar decisão"}
      </button>
    </form>
  );
}
