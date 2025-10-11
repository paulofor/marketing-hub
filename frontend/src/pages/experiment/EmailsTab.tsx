import { Fragment, useEffect, useMemo, useState } from "react";
import type { JourneyStep } from "../../api/journey/types";
import { useJourney } from "../../api/journey/useJourney";
import { useUpdateJourney } from "../../api/journey/useUpdateJourney";
import { useRequestEmails } from "../../api/experiment/useRequestEmails";
import WorkerRequestBanner from "./WorkerRequestBanner";

type EmailStatus = "draft" | "review" | "approved" | "";

interface EmailsTabProps {
  experimentId: string;
  requestedEmails?: number | null;
  journeyId?: number | null;
  steps: JourneyStep[];
  experimentName: string;
}

interface EmailConfig {
  subject: string;
  templateId: string;
  status: EmailStatus;
  notes: string;
}

const STATUS_OPTIONS: Array<{ value: EmailStatus; label: string; description: string }> = [
  {
    value: "",
    label: "Sem status",
    description: "Use quando o conteúdo ainda não foi iniciado.",
  },
  {
    value: "draft",
    label: "Rascunho",
    description: "Texto inicial pronto para revisão.",
  },
  {
    value: "review",
    label: "Em revisão",
    description: "Conteúdo em ajustes com o time responsável.",
  },
  {
    value: "approved",
    label: "Aprovado",
    description: "E-mail validado para disparo na jornada.",
  },
];

const STATUS_BADGES: Record<Exclude<EmailStatus, "">, string> = {
  draft: "text-bg-warning",
  review: "text-bg-info",
  approved: "text-bg-success",
};

function getMetadataKey(stepId: number, field: keyof EmailConfig) {
  return `email.step.${stepId}.${field}`;
}

function buildInitialConfig(stepId: number, metadata?: Record<string, string>): EmailConfig {
  return {
    subject: metadata?.[getMetadataKey(stepId, "subject")] ?? "",
    templateId: metadata?.[getMetadataKey(stepId, "templateId")] ?? "",
    status: (metadata?.[getMetadataKey(stepId, "status")] as EmailStatus) ?? "",
    notes: metadata?.[getMetadataKey(stepId, "notes")] ?? "",
  };
}

function normaliseConfig(config: EmailConfig): EmailConfig {
  return {
    subject: config.subject.trim(),
    templateId: config.templateId.trim(),
    status: config.status,
    notes: config.notes.trim(),
  };
}

function describeStatus(status: EmailStatus) {
  if (!status) return { label: "Sem status", className: "text-bg-secondary" };
  return {
    label: STATUS_OPTIONS.find((option) => option.value === status)?.label ?? "Status",
    className: STATUS_BADGES[status] ?? "text-bg-secondary",
  };
}

function renderStepMetadata(metadata: Record<string, string>) {
  const entries = Object.entries(metadata).filter(([, value]) => value != null && value !== "");
  if (!entries.length) {
    return (
      <p className="text-muted small mb-0">
        O template não definiu metadados adicionais para este e-mail.
      </p>
    );
  }

  return (
    <dl className="row gy-2 small mb-0">
      {entries.map(([key, value]) => (
        <Fragment key={key}>
          <dt className="col-sm-4 text-uppercase text-muted">{key}</dt>
          <dd className="col-sm-8 mb-0">{value}</dd>
        </Fragment>
      ))}
    </dl>
  );
}

export default function EmailsTab({
  experimentId,
  requestedEmails,
  journeyId,
  steps,
  experimentName,
}: EmailsTabProps) {
  const emailSteps = useMemo(
    () => steps.filter((step) => step.stimulusType === "EMAIL"),
    [steps],
  );
  const { data: journey, isLoading, isError } = useJourney(journeyId ?? undefined);
  const updateJourney = useUpdateJourney(journeyId ?? 0);
  const requestEmails = useRequestEmails(experimentId);
  const [configs, setConfigs] = useState<Record<number, EmailConfig>>({});
  const [savingStepId, setSavingStepId] = useState<number | null>(null);

  useEffect(() => {
    if (!journey) return;
    setConfigs(() => {
      const next: Record<number, EmailConfig> = {};
      emailSteps.forEach((step) => {
        next[step.id] = buildInitialConfig(step.id, journey.metadata);
      });
      return next;
    });
  }, [journey, emailSteps]);

  const canEdit = typeof journeyId === "number" && journeyId > 0;
  const approvedCount = emailSteps.reduce((total, step) => {
    const status = configs[step.id]?.status;
    return status === "approved" ? total + 1 : total;
  }, 0);

  const handleChange = (
    stepId: number,
    field: keyof EmailConfig,
    value: string,
  ) => {
    setConfigs((prev) => ({
      ...prev,
      [stepId]: {
        ...prev[stepId],
        [field]: value,
      },
    }));
  };

  const buildMetadataPayload = (stepId: number, config: EmailConfig) => {
    const metadata = journey?.metadata ?? {};
    const keysToOverride = (Object.keys(config) as Array<keyof EmailConfig>).map((field) =>
      getMetadataKey(stepId, field),
    );
    const entries = Object.entries(metadata).filter(
      ([key]) => !keysToOverride.includes(key as ReturnType<typeof getMetadataKey>),
    );
    const normalised = normaliseConfig(config);
    (Object.keys(normalised) as Array<keyof EmailConfig>).forEach((field) => {
      const key = getMetadataKey(stepId, field);
      const value = normalised[field];
      if (value) {
        entries.push([key, value]);
      }
    });

    return entries.reduce<Record<string, string>>((acc, [key, value]) => {
      acc[key] = value;
      return acc;
    }, {});
  };

  const handleSave = async (stepId: number) => {
    if (!journey || !canEdit) return;
    const config = configs[stepId] ?? buildInitialConfig(stepId, journey.metadata);
    setSavingStepId(stepId);
    try {
      const metadata = buildMetadataPayload(stepId, config);
      await updateJourney.mutateAsync({ metadata });
    } finally {
      setSavingStepId(null);
    }
  };

  const handleClear = (stepId: number) => {
    setConfigs((prev) => ({
      ...prev,
      [stepId]: {
        subject: "",
        templateId: "",
        status: "",
        notes: "",
      },
    }));
  };

  if (!emailSteps.length) {
    return (
      <div className="mt-3">
        <p className="text-muted">
          O template selecionado não possui disparos de e-mail. Aproveite o blueprint atual para
          revisar outros estímulos da jornada.
        </p>
      </div>
    );
  }

  return (
    <div className="mt-3">
      <WorkerRequestBanner
        title="E-mails planejados"
        subtitle="Peça ao Worker IA para redigir os e-mails das etapas desta jornada."
        resourceName="e-mail"
        resourceNamePlural="e-mails"
        existingLabel="E-mails aprovados"
        existingCount={approvedCount}
        requestedCount={requestedEmails}
        defaultQuantity={Math.max(1, emailSteps.length)}
        helperText="Informe quantos e-mails deseja que o Worker IA crie para avançar com a jornada."
        buttonLabel="Gerar e-mails"
        successMessage={(quantity) => (
          `Solicitamos ${quantity} ${quantity === 1 ? "e-mail" : "e-mails"} ao Worker IA. Ajuste os status conforme eles ficarem prontos.`
        )}
        onRequest={(quantity) => requestEmails.mutateAsync(quantity)}
        isRequesting={requestEmails.isPending}
      />
      <section className="card mb-4">
        <div className="card-header d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
          <div>
            <h5 className="mb-1">Planejamento dos e-mails</h5>
            <p className="text-muted small mb-0">
              {experimentName} seguirá {emailSteps.length} etapas de e-mail previstas no template.
            </p>
          </div>
          <span className="badge text-bg-secondary">
            {approvedCount} aprovado{approvedCount === 1 ? "" : "s"} · {emailSteps.length} etapa
            {emailSteps.length === 1 ? "" : "s"}
          </span>
        </div>
        <div className="card-body">
          <p className="mb-0 text-muted small">
            Registre assunto, template do provedor e observações de copy para cada e-mail. As
            informações ficam salvas nos metadados da jornada, mantendo o histórico junto às
            execuções do worker.
          </p>
        </div>
      </section>

      {!canEdit ? (
        <div className="alert alert-warning" role="alert">
          <h6 className="alert-heading mb-1">Crie a jornada para editar os e-mails</h6>
          <p className="mb-0">
            Gere a jornada a partir deste experimento antes de preencher os campos abaixo. Isso
            garante que os metadados sejam vinculados à instância correta.
          </p>
        </div>
      ) : null}

      {isLoading && canEdit ? <p>Carregando jornada associada...</p> : null}
      {isError && canEdit ? (
        <p className="text-danger">Não foi possível carregar a jornada vinculada a este experimento.</p>
      ) : null}

      {emailSteps.map((step) => {
        const config = configs[step.id] ?? buildInitialConfig(step.id, journey?.metadata);
        const statusDescriptor = describeStatus(config.status);
        const disableActions = updateJourney.isPending || !canEdit;

        return (
          <section key={step.id} className="card mb-4">
            <div className="card-header d-flex flex-column flex-lg-row justify-content-between align-items-lg-start gap-3">
              <div>
                <h5 className="mb-1">{step.name ?? `Passo ${step.position}`}</h5>
                <p className="text-muted mb-0">{step.description ?? "Detalhe o posicionamento deste e-mail."}</p>
              </div>
              <div className="d-flex align-items-center gap-2">
                <span className="badge text-bg-light text-dark">{step.phase}</span>
                <span className={`badge ${statusDescriptor.className}`}>{statusDescriptor.label}</span>
              </div>
            </div>
            <div className="card-body">
              <div className="mb-4">
                <h6 className="fw-semibold">Diretrizes do template</h6>
                {renderStepMetadata(step.metadata ?? {})}
              </div>
              <form
                className="row g-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  handleSave(step.id);
                }}
                noValidate
              >
                <div className="col-12 col-lg-6">
                  <label className="form-label fw-semibold" htmlFor={`email-status-${step.id}`}>
                    Status do conteúdo
                  </label>
                  <select
                    id={`email-status-${step.id}`}
                    className="form-select"
                    value={config.status}
                    onChange={(event) => handleChange(step.id, "status", event.target.value)}
                    disabled={!canEdit}
                  >
                    {STATUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <p className="form-text">
                    {STATUS_OPTIONS.find((option) => option.value === config.status)?.description ??
                      "Indique o estágio atual para alinhar o time."}
                  </p>
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label fw-semibold" htmlFor={`email-template-${step.id}`}>
                    Template no provedor (ex.: SendGrid)
                  </label>
                  <input
                    id={`email-template-${step.id}`}
                    className="form-control"
                    value={config.templateId}
                    onChange={(event) => handleChange(step.id, "templateId", event.target.value)}
                    placeholder="d-1234567890"
                    disabled={!canEdit}
                  />
                  <p className="form-text">
                    Informe o identificador oficial do template no provedor para rastreabilidade.
                  </p>
                </div>
                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor={`email-subject-${step.id}`}>
                    Assunto aprovado
                  </label>
                  <input
                    id={`email-subject-${step.id}`}
                    className="form-control"
                    value={config.subject}
                    onChange={(event) => handleChange(step.id, "subject", event.target.value)}
                    placeholder="[Lançamento] Seu acesso está liberado"
                    disabled={!canEdit}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label fw-semibold" htmlFor={`email-notes-${step.id}`}>
                    Observações e próximos passos
                  </label>
                  <textarea
                    id={`email-notes-${step.id}`}
                    className="form-control"
                    rows={4}
                    value={config.notes}
                    onChange={(event) => handleChange(step.id, "notes", event.target.value)}
                    placeholder="Resumo da copy, CTA principal, segmentação alvo e testes planejados."
                    disabled={!canEdit}
                  />
                </div>
                <div className="col-12 d-flex flex-wrap gap-2">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={disableActions}
                  >
                    {updateJourney.isPending && savingStepId === step.id ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                          aria-hidden="true"
                        />
                        Salvando...
                      </>
                    ) : (
                      "Salvar e-mail"
                    )}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => handleClear(step.id)}
                    disabled={!canEdit}
                  >
                    Limpar campos
                  </button>
                </div>
              </form>
            </div>
          </section>
        );
      })}
    </div>
  );
}
