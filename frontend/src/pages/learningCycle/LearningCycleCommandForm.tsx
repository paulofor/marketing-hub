import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { useState, type FormEvent } from "react";
import {
  cycleError,
  useCycleMutation,
  type CycleCatalog,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

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
const metrics: Field[] = [
  ["source", "Fonte da leitura e critério de atribuição", "text"],
  ["periodStart", "Início do período medido", "datetime-local"],
  ["periodEnd", "Fim do período medido", "datetime-local"],
  ["observedAt", "Horário da conciliação", "datetime-local"],
  ["sessions", "Sessões atribuídas", "number"],
  ["starts", "Experiências iniciadas", "number"],
  ["firstResults", "Primeiros resultados entregues", "number"],
  ["checkouts", "Checkouts", "number"],
  ["netSales", "Vendas líquidas", "number"],
  ["refunds", "Reembolsos", "number"],
  ["spendBrl", "Mídia acumulada (R$)", "number"],
  ["revenueBrl", "Receita líquida (R$)", "number"],
  ["contributionBrl", "Contribuição após todos os custos (R$)", "number"],
  ["dataValid", "Dados válidos e conciliados", "checkbox"],
  [
    "testDataExcluded",
    "Dados de teste separados da leitura comercial",
    "checkbox",
  ],
  ["deliveryVerified", "Entrega comprovada", "checkbox"],
  ["useVerified", "Uso comprovado", "checkbox"],
  ["satisfactionVerified", "Satisfação comprovada", "checkbox"],
];
export default function LearningCycleCommandForm({
  cycle,
  catalog,
  onUpdated,
}: {
  cycle: LearningCycle;
  catalog: CycleCatalog;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const mutation = useCycleMutation(cycle.productId, cycle.id);
  const [action, setAction] = useState(cycle.commands[0]?.action ?? "");
  const [requestKey, setRequestKey] = useState(() => createCycleRequestKey());
  const command = cycle.commands.find((item) => item.action === action);
  const returnRequired = action === "ADJUST" || action === "REWORK";
  let fields: Field[] =
    action === "COMPLETE" ? (byStage[cycle.stage] ?? []) : [];
  if (action === "MEASURE") fields = metrics;
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
    const evidence: Record<string, unknown> = {};
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
    if (action === "MEASURE")
      Object.assign(evidence, {
        experimentId: cycle.experimentId,
        currency: "BRL",
      });
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
      <div className="cycle-form-grid" key={`${cycle.revision}-${action}`}>
        <label className="form-label">
          Responsável pela decisão *
          <input
            required
            name="operatorName"
            className="form-control"
            maxLength={160}
          />
        </label>
        <label className="form-label">
          Síntese e justificativa *
          <textarea
            required
            name="summary"
            className="form-control"
            maxLength={4000}
          />
        </label>
        <label className="form-label">
          Referência da evidência *
          <input
            required
            name="evidenceReference"
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
              defaultValue=""
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
            {key === "approvalInstanceId" ? (
              <select
                required
                name={key}
                className="form-select"
                defaultValue=""
              >
                <option value="">
                  Selecione uma aprovação real desta versão
                </option>
                {cycle.approvalOptions?.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>
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
                  key === "instrumentationVerified"
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
                defaultValue={
                  key === "productVersion" && action !== "REWORK"
                    ? cycle.productVersion
                    : key === "budgetLimitBrl" && action !== "AUTHORIZE_SCALE"
                      ? cycle.budgetLimitBrl
                      : undefined
                }
              />
            )}
            {type === "checkbox"
              ? `${label}${key === "confirmed" || key === "instrumentationVerified" ? " *" : ""}`
              : null}
          </label>
        ))}
      </div>
      {action === "MEASURE" ? (
        <p className="small">
          Esta leitura ficará identificada como registro do operador, com a
          fonte informada. Não será apresentada como sincronização automática da
          Meta.
        </p>
      ) : null}
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
        {command?.label || "Registrar decisão"}
      </button>
    </form>
  );
}
