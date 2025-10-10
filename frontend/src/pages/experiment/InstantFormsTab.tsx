import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import type {
  Experiment,
  FacebookInstantFormSummary,
} from "../../api/experiment/useExperiments";
import { useInstantFormsByHypothesis } from "../../api/hypothesis/useInstantFormsByHypothesis";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import type { JourneyStep } from "../../api/journey/types";
import CreativeLibraryBanner from "./CreativeLibraryBanner";

interface InstantFormsTabProps {
  experiment: Experiment;
  steps: JourneyStep[];
}

type FeedbackVariant = "success" | "error" | "info";

interface FeedbackState {
  variant: FeedbackVariant;
  title: string;
  description?: string;
}

function formatStepBadge(step: JourneyStep) {
  const phase = step.phase ? step.phase.toLowerCase() : "fase";
  const base = phase.charAt(0).toUpperCase() + phase.slice(1);
  return `${base} • Instant form`;
}

function formatDatetime(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR");
}

function describeStatus(status?: string | null) {
  if (!status) return "Status não informado";
  return status;
}

export default function InstantFormsTab({ experiment, steps }: InstantFormsTabProps) {
  const instantFormSteps = useMemo(
    () => steps.filter((step) => step.stimulusType === "INSTANT_FORM"),
    [steps],
  );
  const { data: forms, isLoading, isError } = useInstantFormsByHypothesis(
    experiment.hypothesisId,
  );
  const availableForms = Array.isArray(forms) ? forms : [];
  const updateExperiment = useUpdateExperiment(experiment.id);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedFormId, setSelectedFormId] = useState<string>(
    experiment.facebookInstantForm?.id
      ? String(experiment.facebookInstantForm.id)
      : "",
  );

  useEffect(() => {
    setSelectedFormId(
      experiment.facebookInstantForm?.id
        ? String(experiment.facebookInstantForm.id)
        : "",
    );
  }, [experiment.facebookInstantForm?.id]);

  const requiredCount = instantFormSteps.length;
  const templateRequiresInstantForm = requiredCount > 0;

  const selectedForm = useMemo(() => {
    if (!selectedFormId) {
      return null;
    }
    const parsedId = Number(selectedFormId);
    if (Number.isNaN(parsedId)) {
      return null;
    }
    return (
      availableForms.find((form) => form.id === parsedId) ??
      (experiment.facebookInstantForm &&
      experiment.facebookInstantForm.id === parsedId
        ? experiment.facebookInstantForm
        : null)
    );
  }, [availableForms, experiment.facebookInstantForm, selectedFormId]);

  const hasForms = availableForms.length > 0;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);

    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || experiment.metricPresetId == null) {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar o formulário",
        description:
          "Defina a meta de KPI e o preset de métricas antes de vincular um instant form ao experimento.",
      });
      return;
    }

    const trimmed = selectedFormId.trim();
    const parsedId = trimmed === "" ? null : Number(trimmed);
    if (parsedId !== null && Number.isNaN(parsedId)) {
      setFeedback({
        variant: "error",
        title: "Instant form inválido",
        description: "Selecione um formulário válido da lista disponível.",
      });
      return;
    }

    try {
      await updateExperiment.mutateAsync({
        name: experiment.name,
        hypothesis: experiment.hypothesis,
        kpiTarget: Number(kpiTargetValue),
        metricPresetId: experiment.metricPresetId ?? undefined,
        sampleSize: experiment.sampleSize ?? undefined,
        mde: experiment.mdePercent ?? undefined,
        startDate: experiment.startDate ?? undefined,
        endDate: experiment.endDate ?? undefined,
        creativesToGenerate: experiment.creativesToGenerate ?? undefined,
        facebookPageId: experiment.facebookPage?.id ?? null,
        facebookInstantFormId: parsedId,
        instagramAccountId: experiment.instagramAccount?.id ?? null,
      });

      const savedForm =
        parsedId == null
          ? null
          : availableForms.find((form) => form.id === parsedId) ?? null;

      setFeedback({
        variant: "success",
        title: "Configurações atualizadas",
        description:
          parsedId == null
            ? "Nenhum instant form ficará vinculado a este experimento."
            : savedForm
              ? `O formulário "${savedForm.name}" está pronto para ser utilizado pela jornada.`
              : "O instant form selecionado foi associado com sucesso.",
      });
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar o formulário",
        description: "Tente novamente em instantes.",
      });
    }
  };

  const renderFeedback = () => {
    if (!feedback) return null;
    const variantClass =
      feedback.variant === "success"
        ? "alert-success"
        : feedback.variant === "error"
          ? "alert-danger"
          : "alert-info";
    return (
      <div className={`alert ${variantClass}`} role="alert">
        <h6 className="alert-heading mb-1">{feedback.title}</h6>
        {feedback.description ? <p className="mb-0">{feedback.description}</p> : null}
      </div>
    );
  };

  const renderStepList = () => {
    if (!templateRequiresInstantForm) {
      return (
        <p className="text-muted mb-0">
          O template de jornada selecionado não prevê etapas com formulário instantâneo.
        </p>
      );
    }

    return (
      <ol className="list-group list-group-numbered">
        {instantFormSteps.map((step) => (
          <li key={step.id} className="list-group-item">
            <div className="d-flex flex-column">
              <div className="d-flex align-items-start justify-content-between">
                <div>
                  <h6 className="mb-1">{step.name ?? `Passo ${step.position}`}</h6>
                  {step.description ? (
                    <p className="mb-1 text-muted small">{step.description}</p>
                  ) : null}
                </div>
                <span className="badge text-bg-light text-dark ms-2">
                  {formatStepBadge(step)}
                </span>
              </div>
              <div className="text-muted small">
                Condição de entrada: {step.entryCondition ?? "—"}
              </div>
              <div className="text-muted small">
                Condição de saída: {step.exitCondition ?? "—"}
              </div>
            </div>
          </li>
        ))}
      </ol>
    );
  };

  const renderFormsTable = () => {
    if (isLoading) {
      return <p>Carregando instant forms cadastrados...</p>;
    }

    if (isError) {
      return <p className="text-danger">Não foi possível carregar os instant forms desta hipótese.</p>;
    }

    if (!hasForms) {
      return (
        <p className="text-muted mb-0">
          Nenhum instant form foi gerado ainda para esta hipótese. Acesse a ficha da hipótese e
          solicite ao Worker IA ou cadastre manualmente para liberar a jornada.
        </p>
      );
    }

    return (
      <div className="table-responsive">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Formulário</th>
              <th>Página</th>
              <th>Status</th>
              <th>Engajamento</th>
              <th>Links</th>
            </tr>
          </thead>
          <tbody>
            {availableForms.map((form) => (
              <tr key={form.id}>
                <td style={{ minWidth: 220 }}>
                  <div className="fw-semibold">{form.name}</div>
                  <div className="text-muted small">ID Meta: {form.facebookFormId}</div>
                  {form.model ? (
                    <div className="text-muted small">Modelo: {form.model}</div>
                  ) : null}
                </td>
                <td style={{ minWidth: 200 }}>
                  <div>{form.facebookPageName}</div>
                  <div className="text-muted small">{form.facebookPageExternalId}</div>
                </td>
                <td style={{ minWidth: 160 }}>
                  <div>{describeStatus(form.status)}</div>
                  <div className="text-muted small">
                    Idioma: {form.locale ?? "não informado"}
                  </div>
                </td>
                <td style={{ minWidth: 160 }}>
                  <div className="text-muted small">Leads</div>
                  <div>{form.leadsCount ?? "—"}</div>
                  <div className="text-muted small mt-2">Atualizado</div>
                  <div>{formatDatetime(form.updatedTime) ?? "—"}</div>
                </td>
                <td style={{ minWidth: 200 }}>
                  <div className="d-flex flex-column gap-1">
                    {form.followUpActionUrl ? (
                      <a
                        href={form.followUpActionUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="small"
                      >
                        Página de agradecimento
                      </a>
                    ) : null}
                    {form.privacyPolicyUrl ? (
                      <a
                        href={form.privacyPolicyUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="small"
                      >
                        Política de privacidade
                      </a>
                    ) : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  const renderSelectedFormDetails = (form: FacebookInstantFormSummary | null) => {
    if (!form) {
      return (
        <div className="text-muted small">
          Nenhum formulário vinculado. Sem um instant form, a jornada ficará bloqueada na etapa de
          captura.
        </div>
      );
    }

    return (
      <dl className="row mb-0">
        <dt className="col-sm-4">Formulário</dt>
        <dd className="col-sm-8">{form.name}</dd>
        <dt className="col-sm-4">ID Meta</dt>
        <dd className="col-sm-8">{form.facebookFormId}</dd>
        <dt className="col-sm-4">Página</dt>
        <dd className="col-sm-8">{form.facebookPageName}</dd>
        <dt className="col-sm-4">Status</dt>
        <dd className="col-sm-8">{describeStatus(form.status)}</dd>
        <dt className="col-sm-4">Idioma</dt>
        <dd className="col-sm-8">{form.locale ?? "—"}</dd>
        <dt className="col-sm-4">Links</dt>
        <dd className="col-sm-8">
          <div className="d-flex flex-column gap-1">
            {form.followUpActionUrl ? (
              <a href={form.followUpActionUrl} target="_blank" rel="noreferrer" className="small">
                Página de agradecimento
              </a>
            ) : null}
            {form.privacyPolicyUrl ? (
              <a href={form.privacyPolicyUrl} target="_blank" rel="noreferrer" className="small">
                Política de privacidade
              </a>
            ) : null}
          </div>
        </dd>
      </dl>
    );
  };

  return (
    <div className="mt-3">
      <CreativeLibraryBanner
        experimentId={String(experiment.id)}
        requestedCreatives={experiment.creativesToGenerate}
      />
      <section className="card mb-4">
        <div className="card-header">
          <h5 className="mb-0">Passos que exigem instant form</h5>
          <span className="text-muted small">
            {requiredCount} etapa{requiredCount === 1 ? "" : "s"} planejada{requiredCount === 1 ? "" : "s"}
          </span>
        </div>
        <div className="card-body">{renderStepList()}</div>
      </section>

      {renderFeedback()}

      <section className="card mb-4">
        <div className="card-header">
          <h5 className="mb-0">Selecionar instant form</h5>
        </div>
        <div className="card-body">
          <form onSubmit={handleSubmit} className="row gy-3" noValidate>
            <div className="col-12 col-lg-6">
              <label className="form-label fw-semibold" htmlFor="instantFormSelector">
                Instant form disponível
              </label>
              <select
                id="instantFormSelector"
                className="form-select"
                value={selectedFormId}
                onChange={(event) => setSelectedFormId(event.target.value)}
              >
                <option value="">Sem instant form</option>
                {availableForms.map((form) => (
                  <option key={form.id} value={form.id}>
                    {form.name} · {form.facebookPageName}
                  </option>
                ))}
              </select>
              <p className="form-text">
                Selecione o formulário aprovado que capta consentimento e dados para nutrir a jornada.
              </p>
            </div>
            <div className="col-12 col-lg-6">
              <div className="bg-body-tertiary rounded-3 p-3 h-100">
                <h6 className="fw-semibold">Resumo do formulário vinculado</h6>
                {renderSelectedFormDetails(selectedForm)}
              </div>
            </div>
            <div className="col-12 d-flex align-items-center gap-2">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={updateExperiment.isPending || !templateRequiresInstantForm}
              >
                {updateExperiment.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Salvando...
                  </>
                ) : (
                  "Salvar instant form"
                )}
              </button>
              <Link
                to={`/niches/${experiment.nicheId}/hypotheses/${experiment.hypothesisId}`}
                className="btn btn-outline-secondary"
              >
                Abrir hipótese
              </Link>
            </div>
          </form>
        </div>
      </section>

      <section className="card">
        <div className="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 className="mb-0">Instant forms da hipótese</h5>
            <p className="text-muted small mb-0">
              Reaproveite formulários gerados anteriormente para acelerar a ativação.
            </p>
          </div>
          <span className="badge text-bg-secondary">{availableForms.length}</span>
        </div>
        <div className="card-body">{renderFormsTable()}</div>
      </section>
    </div>
  );
}
