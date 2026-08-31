import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  Loader2,
  PlayCircle,
  UserCheck,
  Workflow,
} from "lucide-react";
import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import type { ProductProcessActivityExecutionCommand } from "../../api/businessProcess/useProductProcessActivityExecutions";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import ExperimentRunPanel from "../experiment/ExperimentRunPanel";

type Props = {
  activity: ProductProcessActivityExecutionGroup;
  productId: number;
  pending: boolean;
  pendingActivityId?: string;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
};

const executorLabels = {
  AGENT: "Agente",
  BACKEND: "Backend",
  HUMAN: "Aprovação humana",
  HISTORICAL: "Histórico",
  UNCONFIGURED: "Sem contrato",
} as const;

/** Apresenta o comando oficial da atividade usando somente o contrato enviado pelo backend. */
export default function ProductProcessActivityExecutionPanel({
  activity,
  productId,
  pending,
  pendingActivityId,
  onExecute,
}: Props) {
  const control = activity.executionControl;
  if (!control) return null;
  const executing = pending && pendingActivityId === activity.activityId;
  const controlCompleted = activity.operationalState === "COMPLETED";

  return (
    <section
      className={`product-process-activity-control product-process-activity-control--${control.executorType.toLowerCase()}`}
      aria-label={`Execução de ${activity.activityName}`}
    >
      <header className="product-process-activity-control__header">
        <div>
          <span className="product-process-activity-control__eyebrow">
            Como executar
          </span>
          <h3>{executorLabels[control.executorType]}</h3>
        </div>
        <ExecutionIcon executorType={control.executorType} />
      </header>

      <p className="product-process-activity-control__description">
        {control.description}
      </p>

      <ActivityRequirements activity={activity} />

      <p
        className={`product-process-activity-control__availability ${control.actionAvailable || controlCompleted ? "is-ready" : "is-pending"}`}
      >
        {control.actionAvailable || controlCompleted ? (
          <CheckCircle2 size={17} aria-hidden="true" />
        ) : (
          <AlertTriangle size={17} aria-hidden="true" />
        )}
        <span>{control.availabilityReason}</span>
      </p>

      {control.interactionType === "APPROVAL" ? (
        <HumanDecisionForm
          activity={activity}
          executing={executing}
          onExecute={onExecute}
        />
      ) : control.interactionType === "SUBPROCESS" ? (
        control.targetProcessDefinitionId && control.actionAvailable ? (
          <Link
            className="btn btn-primary"
            to={`/products/${productId}/value-chain-history/processes/${control.targetProcessDefinitionId}/activities`}
          >
            <Workflow size={17} aria-hidden="true" />
            {control.actionLabel || "Abrir subprocesso"}
          </Link>
        ) : null
      ) : ["COMMAND", "WORKSPACE"].includes(control.interactionType) &&
        control.actionLabel &&
        control.actionAvailable ? (
        <button
          className="btn btn-primary"
          type="button"
          disabled={pending}
          onClick={() => onExecute({ activityId: activity.activityId })}
        >
          {executing ? (
            <Loader2
              className="spinner-border spinner-border-sm"
              size={16}
              aria-hidden="true"
            />
          ) : (
            <PlayCircle size={17} aria-hidden="true" />
          )}
          {executing ? "Executando..." : control.actionLabel}
        </button>
      ) : null}

      {control.workspaceCode === "EXPERIMENT_PREFLIGHT" &&
      control.workspaceReferenceId ? (
        <div className="product-process-activity-control__workspace">
          <ExperimentRunPanel
            experimentId={String(control.workspaceReferenceId)}
          />
        </div>
      ) : null}
    </section>
  );
}

function ExecutionIcon({
  executorType,
}: {
  executorType: ProductProcessActivityExecutionGroup["executionControl"] extends infer T
    ? T extends { executorType: infer E }
      ? E
      : never
    : never;
}) {
  if (executorType === "HUMAN") {
    return <UserCheck size={22} aria-hidden="true" />;
  }
  if (executorType === "BACKEND") {
    return <Workflow size={22} aria-hidden="true" />;
  }
  return <Bot size={22} aria-hidden="true" />;
}

function HumanDecisionForm({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  if (control.decisionMode === "REVIEW_AND_ACCEPT") {
    return (
      <ReviewAndAcceptDecision
        activity={activity}
        executing={executing}
        onExecute={onExecute}
      />
    );
  }
  return (
    <DetailedHumanDecisionForm
      activity={activity}
      executing={executing}
      onExecute={onExecute}
    />
  );
}

function ActivityRequirements({
  activity,
}: {
  activity: ProductProcessActivityExecutionGroup;
}) {
  const control = activity.executionControl!;
  if (control.requirements.length === 0) return null;
  const content = (
    <ul
      className="product-process-activity-control__requirements"
      aria-label="Pré-requisitos da atividade"
    >
      {control.requirements.map((requirement) => (
        <li key={requirement.code}>
          {requirement.satisfied ? (
            <CheckCircle2 size={18} aria-hidden="true" />
          ) : (
            <AlertTriangle size={18} aria-hidden="true" />
          )}
          <div>
            <strong>{requirement.title}</strong>
            <span>{requirement.detail}</span>
            {!requirement.satisfied ? (
              <small>
                <b>Próxima ação:</b> {requirement.recommendation}
              </small>
            ) : null}
          </div>
        </li>
      ))}
    </ul>
  );
  if (control.decisionMode !== "REVIEW_AND_ACCEPT") return content;
  const approved = control.requirements.filter(
    (requirement) => requirement.satisfied,
  ).length;
  return (
    <details className="product-process-activity-control__review-details">
      <summary>
        <CheckCircle2 size={18} aria-hidden="true" />
        {approved}/{control.requirements.length} verificações prontas
        <span>Ver evidências</span>
      </summary>
      {content}
    </details>
  );
}

function ReviewAndAcceptDecision({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  const [showRejection, setShowRejection] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const ready = control.actionAvailable && Boolean(control.confirmationToken);
  const rejectionReady = ready && rejectionReason.trim().length >= 10;

  if (!control.actionAvailable) return null;

  const approve = () => {
    if (!ready || executing || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision: "APPROVE",
        confirmationToken: control.confirmationToken,
      },
    });
  };

  const reject = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!rejectionReady || executing || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision: "REJECT",
        justification: rejectionReason.trim(),
        confirmationToken: control.confirmationToken,
      },
    });
  };

  return (
    <div className="product-process-review-accept">
      <div className="product-process-review-accept__summary">
        <CheckCircle2 size={24} aria-hidden="true" />
        <div>
          <strong>{control.confirmationTitle || "Tudo pronto"}</strong>
          <p>{control.confirmationMessage}</p>
          <small>
            A evidência e a justificativa serão registradas automaticamente.
            Esta autorização não cria campanha paga nem realiza gasto.
          </small>
        </div>
      </div>
      <button
        className="btn btn-success product-process-review-accept__approve"
        type="button"
        disabled={!ready || executing}
        onClick={approve}
      >
        {executing ? (
          <Loader2
            className="spinner-border spinner-border-sm"
            size={16}
            aria-hidden="true"
          />
        ) : (
          <UserCheck size={17} aria-hidden="true" />
        )}
        {executing
          ? "Autorizando..."
          : control.actionLabel || "Li, entendi e autorizo"}
      </button>
      <button
        className="btn btn-link product-process-review-accept__reject-toggle"
        type="button"
        disabled={executing}
        onClick={() => setShowRejection((visible) => !visible)}
      >
        Não autorizar
      </button>
      {showRejection ? (
        <form
          className="product-process-review-accept__rejection"
          onSubmit={reject}
        >
          <label>
            Motivo da não autorização <span aria-hidden="true">*</span>
            <textarea
              className="form-control"
              rows={3}
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              placeholder="Explique o que precisa ser corrigido antes de autorizar"
              minLength={10}
              maxLength={2000}
              required
            />
          </label>
          <button
            className="btn btn-outline-danger"
            type="submit"
            disabled={!rejectionReady || executing}
          >
            Registrar não autorização
          </button>
        </form>
      ) : null}
    </div>
  );
}

function DetailedHumanDecisionForm({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  const [decision, setDecision] = useState<"APPROVE" | "REJECT">("APPROVE");
  const [operatorName, setOperatorName] = useState("");
  const [justification, setJustification] = useState("");
  const [evidenceReference, setEvidenceReference] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const formReady =
    control.actionAvailable &&
    operatorName.trim().length >= 3 &&
    justification.trim().length >= 10 &&
    evidenceReference.trim().length >= 3 &&
    confirmed &&
    Boolean(control.confirmationToken);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formReady || executing || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision,
        operatorName: operatorName.trim(),
        justification: justification.trim(),
        evidenceReference: evidenceReference.trim(),
        confirmationToken: control.confirmationToken,
      },
    });
  };

  if (!control.actionAvailable) return null;

  return (
    <form className="product-process-human-decision" onSubmit={submit}>
      <h4>{control.confirmationTitle || "Registrar decisão humana"}</h4>
      <div className="product-process-human-decision__grid">
        <label>
          Decisão <span aria-hidden="true">*</span>
          <select
            className="form-select"
            value={decision}
            onChange={(event) =>
              setDecision(event.target.value as "APPROVE" | "REJECT")
            }
            required
          >
            <option value="APPROVE">Aprovar</option>
            <option value="REJECT">Reprovar</option>
          </select>
        </label>
        <label>
          Responsável <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={operatorName}
            onChange={(event) => setOperatorName(event.target.value)}
            placeholder="Nome de quem tomou a decisão"
            minLength={3}
            maxLength={191}
            required
          />
        </label>
      </div>
      <label>
        Justificativa <span aria-hidden="true">*</span>
        <textarea
          className="form-control"
          rows={3}
          value={justification}
          onChange={(event) => setJustification(event.target.value)}
          placeholder="Explique a decisão com base nas evidências e no impacto comercial"
          minLength={10}
          maxLength={2000}
          required
        />
      </label>
      <label>
        Evidência auditável <span aria-hidden="true">*</span>
        <input
          className="form-control"
          value={evidenceReference}
          onChange={(event) => setEvidenceReference(event.target.value)}
          placeholder="URL, tarefa, run, documento ou referência verificável"
          minLength={3}
          maxLength={1000}
          required
        />
      </label>
      <label className="product-process-human-decision__confirmation">
        <input
          className="form-check-input"
          type="checkbox"
          checked={confirmed}
          onChange={(event) => setConfirmed(event.target.checked)}
          required
        />
        <span>
          {control.confirmationMessage ||
            "Confirmo que revisei o objetivo e as evidências desta decisão."}
        </span>
      </label>
      <button
        className={`btn ${decision === "APPROVE" ? "btn-success" : "btn-outline-danger"}`}
        type="submit"
        disabled={!formReady || executing}
      >
        {executing ? (
          <Loader2
            className="spinner-border spinner-border-sm"
            size={16}
            aria-hidden="true"
          />
        ) : decision === "APPROVE" ? (
          <UserCheck size={17} aria-hidden="true" />
        ) : (
          <AlertTriangle size={17} aria-hidden="true" />
        )}
        {executing
          ? "Registrando..."
          : decision === "APPROVE"
            ? control.actionLabel || "Aprovar atividade"
            : "Registrar reprovação"}
      </button>
    </form>
  );
}
