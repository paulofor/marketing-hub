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

const privateReadingSignals = [
  ["EXPERIENCE_STARTED", "Iniciou a experiência"],
  ["VALUE_MOMENT", "Chegou ao momento de valor"],
  ["READY_RESULT_USED", "Usou o resultado pronto"],
  ["PREFERRED_OVER_FREE", "Preferiu ao melhor caminho gratuito"],
  ["CHECKOUT_STARTED", "Escolheu avançar no checkout simulado"],
] as const;

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
  if (control.workspaceCode === "PDE_PRIVATE_PROTOTYPE_ACCEPTANCE") {
    return (
      <PrivatePrototypeAcceptanceForm
        activity={activity}
        executing={executing}
        onExecute={onExecute}
      />
    );
  }
  if (control.workspaceCode === "PDE_PRIVATE_READING") {
    return (
      <PrivateReadingDecisionForm
        activity={activity}
        executing={executing}
        onExecute={onExecute}
      />
    );
  }
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

const prototypeConfirmations = [
  ["privateAccessConfirmed", "O acesso está restrito à validação privada"],
  ["paymentDisabled", "O pagamento real está desativado"],
  ["publicationDisabled", "O produto não está publicado ao público"],
  ["noMediaSpendConfirmed", "Não houve mídia nem qualquer gasto"],
  [
    "firstPartyEventsConfirmed",
    "Os cinco eventos próprios estão instrumentados",
  ],
  ["desktopValidated", "A jornada foi testada no desktop"],
  ["mobileValidated", "A jornada foi testada no celular"],
] as const;

/** Aceita somente um endereço HTTP(S) completo para o revisor abrir o protótipo. */
function isHttpUrl(value: string) {
  try {
    const url = new URL(value);
    return (
      Boolean(url.hostname) &&
      (url.protocol === "http:" || url.protocol === "https:")
    );
  } catch {
    return false;
  }
}

/** Confirma a versão utilizável antes de expor o protótipo às duas leituras privadas. */
function PrivatePrototypeAcceptanceForm({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  const [operatorName, setOperatorName] = useState("");
  const [prototypeVersion, setPrototypeVersion] = useState("");
  const [privateAccessUrl, setPrivateAccessUrl] = useState("");
  const [instrumentationReference, setInstrumentationReference] = useState("");
  const [sourceEvidenceReference, setSourceEvidenceReference] = useState("");
  const [sourceEvaluatedAt, setSourceEvaluatedAt] = useState("");
  const [justification, setJustification] = useState("");
  const [evidenceReference, setEvidenceReference] = useState("");
  const [confirmations, setConfirmations] = useState<Record<string, boolean>>(
    Object.fromEntries(prototypeConfirmations.map(([code]) => [code, false])),
  );
  const allConfirmed = prototypeConfirmations.every(
    ([code]) => confirmations[code],
  );
  const versionValid = /^[a-z0-9][a-z0-9._-]{2,63}$/.test(
    prototypeVersion.trim(),
  );
  const urlValid = isHttpUrl(privateAccessUrl.trim());
  const sourceTimestamp = Date.parse(sourceEvaluatedAt);
  const sourceDateValid =
    Number.isFinite(sourceTimestamp) && sourceTimestamp <= Date.now();
  const formReady =
    control.actionAvailable &&
    operatorName.trim().length >= 3 &&
    versionValid &&
    urlValid &&
    instrumentationReference.trim().length >= 3 &&
    sourceEvidenceReference.trim().length >= 3 &&
    sourceDateValid &&
    justification.trim().length >= 10 &&
    evidenceReference.trim().length >= 3 &&
    allConfirmed &&
    Boolean(control.confirmationToken);

  if (!control.actionAvailable) return null;

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formReady || executing || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision: "APPROVE",
        operatorName: operatorName.trim(),
        justification: justification.trim(),
        evidenceReference: evidenceReference.trim(),
        confirmationToken: control.confirmationToken,
        structuredEvidence: {
          prototypeVersion: prototypeVersion.trim(),
          privateAccessUrl: privateAccessUrl.trim(),
          instrumentationReference: instrumentationReference.trim(),
          sourceEvidenceReference: sourceEvidenceReference.trim(),
          sourceEvaluatedAt: new Date(sourceEvaluatedAt).toISOString(),
          ...confirmations,
        },
      },
    });
  };

  return (
    <form className="product-process-human-decision" onSubmit={submit}>
      <h4>{control.confirmationTitle || "Confirmar protótipo privado"}</h4>
      <p>
        Este gate comprova uma versão utilizável e instrumentada. Ele não
        publica, não cobra e não autoriza mídia.
      </p>
      <div className="product-process-human-decision__grid">
        <label>
          Responsável pela validação <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={operatorName}
            onChange={(event) => setOperatorName(event.target.value)}
            minLength={3}
            maxLength={191}
            required
          />
        </label>
        <label>
          Versão do protótipo <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={prototypeVersion}
            onChange={(event) => setPrototypeVersion(event.target.value)}
            placeholder="Ex.: private-v1"
            pattern="[a-z0-9][a-z0-9._-]{2,63}"
            minLength={3}
            maxLength={64}
            required
          />
        </label>
      </div>
      <label>
        URL privada acessível aos revisores <span aria-hidden="true">*</span>
        <input
          className="form-control"
          type="url"
          value={privateAccessUrl}
          onChange={(event) => setPrivateAccessUrl(event.target.value)}
          placeholder="https://ambiente-privado.exemplo/prototipo"
          required
        />
      </label>
      <div className="product-process-human-decision__grid">
        <label>
          Referência da instrumentação <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={instrumentationReference}
            onChange={(event) =>
              setInstrumentationReference(event.target.value)
            }
            placeholder="Painel ou execução dos cinco eventos"
            minLength={3}
            maxLength={1000}
            required
          />
        </label>
        <label>
          Fonte comercial vigente <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={sourceEvidenceReference}
            onChange={(event) => setSourceEvidenceReference(event.target.value)}
            placeholder="Snapshot ou relatório de fonte"
            minLength={3}
            maxLength={1000}
            required
          />
        </label>
        <label>
          Data da verificação da fonte <span aria-hidden="true">*</span>
          <input
            className="form-control"
            type="datetime-local"
            value={sourceEvaluatedAt}
            onChange={(event) => setSourceEvaluatedAt(event.target.value)}
            required
          />
        </label>
      </div>
      <label>
        Resultado da homologação <span aria-hidden="true">*</span>
        <textarea
          className="form-control"
          rows={3}
          value={justification}
          onChange={(event) => setJustification(event.target.value)}
          placeholder="Descreva a jornada testada e o resultado pronto observado"
          minLength={10}
          maxLength={2000}
          required
        />
      </label>
      <label>
        Evidência auditável da homologação <span aria-hidden="true">*</span>
        <input
          className="form-control"
          value={evidenceReference}
          onChange={(event) => setEvidenceReference(event.target.value)}
          placeholder="Relatório, teste ou pacote de capturas"
          minLength={3}
          maxLength={1000}
          required
        />
      </label>
      <fieldset>
        <legend className="h6">Travas obrigatórias</legend>
        {prototypeConfirmations.map(([code, label]) => (
          <label
            key={code}
            className="product-process-human-decision__confirmation"
          >
            <input
              className="form-check-input"
              type="checkbox"
              checked={confirmations[code]}
              onChange={(event) =>
                setConfirmations((current) => ({
                  ...current,
                  [code]: event.target.checked,
                }))
              }
              required
            />
            <span>{label}</span>
          </label>
        ))}
      </fieldset>
      <button
        className="btn btn-success"
        type="submit"
        disabled={!formReady || executing}
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
        {executing ? "Registrando..." : "Confirmar protótipo privado"}
      </button>
    </form>
  );
}

/** Registra os cinco sinais sem coletar nome, e-mail ou telefone da pessoa. */
function PrivateReadingDecisionForm({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  const [operatorName, setOperatorName] = useState("");
  const [participantReference, setParticipantReference] = useState("");
  const [justification, setJustification] = useState("");
  const [evidenceReference, setEvidenceReference] = useState("");
  const [consentConfirmed, setConsentConfirmed] = useState(false);
  const [firstPartyEvidenceConfirmed, setFirstPartyEvidenceConfirmed] =
    useState(false);
  const [signals, setSignals] = useState<Record<string, "" | "YES" | "NO">>(
    Object.fromEntries(privateReadingSignals.map(([code]) => [code, ""])),
  );
  const allSignalsAnswered = privateReadingSignals.every(
    ([code]) => signals[code] === "YES" || signals[code] === "NO",
  );
  const pseudonymousReference = /^PV-[A-F0-9]{12}$/.test(
    participantReference.trim(),
  );
  const formReady =
    control.actionAvailable &&
    operatorName.trim().length >= 3 &&
    pseudonymousReference &&
    justification.trim().length >= 10 &&
    evidenceReference.trim().length >= 3 &&
    consentConfirmed &&
    firstPartyEvidenceConfirmed &&
    allSignalsAnswered &&
    Boolean(control.confirmationToken);

  if (!control.actionAvailable) return null;

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formReady || executing || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision: "APPROVE",
        operatorName: operatorName.trim(),
        justification: justification.trim(),
        evidenceReference: evidenceReference.trim(),
        confirmationToken: control.confirmationToken,
        structuredEvidence: {
          participantReference: participantReference.trim(),
          consentConfirmed: true,
          firstPartyEvidenceConfirmed: true,
          signals: Object.fromEntries(
            privateReadingSignals.map(([code]) => [
              code,
              signals[code] === "YES",
            ]),
          ),
        },
      },
    });
  };

  return (
    <form className="product-process-human-decision" onSubmit={submit}>
      <h4>{control.confirmationTitle || "Registrar leitura privada"}</h4>
      <p>
        Use um código aleatório criado para esta leitura. Não informe nome,
        e-mail, telefone ou outro dado pessoal. Os cinco sinais precisam ser
        “Sim” para o gate avançar; respostas negativas ficam registradas como
        tentativa bloqueada e repetível após ajuste do protótipo.
      </p>
      <div className="product-process-human-decision__grid">
        <label>
          Responsável pelo registro <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={operatorName}
            onChange={(event) => setOperatorName(event.target.value)}
            minLength={3}
            maxLength={191}
            required
          />
        </label>
        <label>
          Código pseudonimizado da pessoa <span aria-hidden="true">*</span>
          <input
            className="form-control"
            value={participantReference}
            onChange={(event) => setParticipantReference(event.target.value)}
            placeholder="Ex.: PV-A1B2C3D4E5F6"
            pattern="PV-[A-F0-9]{12}"
            minLength={15}
            maxLength={15}
            required
          />
        </label>
      </div>
      <fieldset>
        <legend className="h6">Sinais observados</legend>
        {privateReadingSignals.map(([code, label]) => (
          <label key={code} className="d-block mb-2">
            {label} <span aria-hidden="true">*</span>
            <select
              className="form-select"
              aria-label={label}
              value={signals[code]}
              onChange={(event) =>
                setSignals((current) => ({
                  ...current,
                  [code]: event.target.value as "" | "YES" | "NO",
                }))
              }
              required
            >
              <option value="">Selecione</option>
              <option value="YES">Sim, observado</option>
              <option value="NO">Não observado</option>
            </select>
          </label>
        ))}
      </fieldset>
      <label>
        Observação da leitura <span aria-hidden="true">*</span>
        <textarea
          className="form-control"
          rows={3}
          value={justification}
          onChange={(event) => setJustification(event.target.value)}
          placeholder="Descreva comportamento, esforço, reação e objeções observadas"
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
          placeholder="Referência interna, arquivo ou gravação consentida"
          minLength={3}
          maxLength={1000}
          required
        />
      </label>
      <label className="product-process-human-decision__confirmation">
        <input
          className="form-check-input"
          type="checkbox"
          checked={consentConfirmed}
          onChange={(event) => setConsentConfirmed(event.target.checked)}
          required
        />
        <span>{control.confirmationMessage}</span>
      </label>
      <label className="product-process-human-decision__confirmation">
        <input
          className="form-check-input"
          type="checkbox"
          checked={firstPartyEvidenceConfirmed}
          onChange={(event) =>
            setFirstPartyEvidenceConfirmed(event.target.checked)
          }
          required
        />
        <span>
          Confirmo que os cinco sinais vieram dos eventos próprios desta versão
          do protótipo.
        </span>
      </label>
      <button
        className="btn btn-success"
        type="submit"
        disabled={!formReady || executing}
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
        {executing ? "Registrando..." : "Registrar leitura privada"}
      </button>
    </form>
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
