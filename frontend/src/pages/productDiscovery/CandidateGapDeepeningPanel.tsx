import { useState, type FormEvent } from "react";
import {
  type CreateProductDiscoveryCustomerInterviewPayload,
  type ProductDiscoveryInterviewOutcome,
  type ProductDiscoveryOpportunity,
  useCreateProductDiscoveryCustomerInterview,
  useAdoptProductDiscoveryPublicEvidence,
  useResumeProductDiscoveryPublicResearch,
  useProductDiscoveryGapDeepening,
} from "../../api/productDiscovery/useProductDiscovery";

type Props = {
  cycleId: number;
  opportunities: ProductDiscoveryOpportunity[];
};

type FormState = {
  opportunityId: string;
  anonymousParticipantCode: string;
  outcome: ProductDiscoveryInterviewOutcome;
  occurredOn: string;
  purchaseSituation: string;
  desiredResult: string;
  difficulty: string;
  alternativeTried: string;
  amountSpent: string;
  currency: string;
  remainingDifficulty: string;
  consentConfirmed: boolean;
  noPersonalDataConfirmed: boolean;
};

function initialForm(opportunityId?: number): FormState {
  return {
    opportunityId: opportunityId ? String(opportunityId) : "",
    anonymousParticipantCode: "",
    outcome: "PURCHASED",
    occurredOn: "",
    purchaseSituation: "",
    desiredResult: "",
    difficulty: "",
    alternativeTried: "",
    amountSpent: "",
    currency: "BRL",
    remainingDifficulty: "",
    consentConfirmed: false,
    noPersonalDataConfirmed: false,
  };
}

export default function CandidateGapDeepeningPanel({
  cycleId,
  opportunities,
}: Props) {
  const gapQuery = useProductDiscoveryGapDeepening(cycleId);
  const createInterview = useCreateProductDiscoveryCustomerInterview(cycleId);
  const adoptPublicEvidence = useAdoptProductDiscoveryPublicEvidence(cycleId);
  const resumePublicResearch = useResumeProductDiscoveryPublicResearch(cycleId);
  const [form, setForm] = useState<FormState>(
    initialForm(opportunities[0]?.id),
  );
  const gap = gapQuery.data;

  if (gapQuery.isLoading) {
    return <div className="text-secondary">Carregando aprofundamento...</div>;
  }
  if (gapQuery.isError) {
    return (
      <div className="alert alert-danger mb-0">
        Não foi possível carregar a atividade de aprofundamento.
      </div>
    );
  }
  if (!gap?.applicable) return null;

  const publicResearch = gap.evidencePolicy === "PUBLIC_SOURCES_V1";
  const awaitingInterviews =
    !publicResearch && gap.cycleStatus === "AWAITING_CUSTOMER_EVIDENCE";
  const missingNames = opportunities
    .filter((item) => gap.missingOpportunityIds.includes(item.id))
    .map((item) => item.name);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const payload: CreateProductDiscoveryCustomerInterviewPayload = {
      opportunityId: Number(form.opportunityId),
      anonymousParticipantCode: form.anonymousParticipantCode.trim(),
      outcome: form.outcome,
      occurredOn: form.occurredOn,
      purchaseSituation: form.purchaseSituation.trim(),
      desiredResult: form.desiredResult.trim(),
      difficulty: form.difficulty.trim(),
      alternativeTried: form.alternativeTried.trim(),
      remainingDifficulty: form.remainingDifficulty.trim(),
      consentConfirmed: form.consentConfirmed,
      noPersonalDataConfirmed: form.noPersonalDataConfirmed,
      ...(form.amountSpent.trim()
        ? {
            amountSpent: Number(form.amountSpent),
            currency: form.currency.trim().toUpperCase(),
          }
        : {}),
    };
    try {
      await createInterview.mutateAsync(payload);
      setForm(initialForm(opportunities[0]?.id));
    } catch {
      // O estado da mutation mantém a causa visível sem apagar o formulário preenchido.
    }
  }

  return (
    <section className="card border-0 shadow-sm">
      <div className="card-body d-flex flex-column gap-4">
        <div>
          <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-2">Aprofundar lacunas das candidatas</h2>
              <p className="text-secondary mb-0">{gap.guidance}</p>
            </div>
            <div className="d-flex flex-wrap gap-2 align-content-start">
              {publicResearch ? (
                <span className="badge text-bg-primary">
                  Pesquisa pública automatizada
                </span>
              ) : (
                <>
                  <span className="badge text-bg-primary">
                    {gap.interviewCount}/{gap.minimumInterviews} entrevistas
                    mínimas
                  </span>
                  <span className="badge text-bg-light">
                    {gap.purchasedCount} compra(s)
                  </span>
                  <span className="badge text-bg-light">
                    {gap.abandonedCount} desistência(s)
                  </span>
                </>
              )}
            </div>
          </div>
          <p className="small text-secondary mt-3 mb-0">
            {publicResearch
              ? "Relatos públicos orientam hipóteses; não são entrevistas nem vendas comprovadas do nosso produto. "
              : `Amostra exploratória de ${gap.minimumInterviews}–${gap.maximumInterviews} pessoas: orienta hipóteses, mas não estima o mercado. `}
            Argos fica limitado a {gap.maximumPublicQueriesPerAttempt}
            buscas por tentativa, {gap.maximumAttempts} tentativas e US${" "}
            {Number(gap.maximumSearchCostUsd).toFixed(2)} estimados de busca. O
            aprofundamento aceita no máximo {gap.maximumModelInvocations}
            chamadas de modelo; seu custo não é presumido e fica auditado na
            tarefa após o callback. Fonte de preço da busca:{" "}
            <a href={gap.searchPricingSource} target="_blank" rel="noreferrer">
              Brave Search API
            </a>
            , observada em {gap.searchPricingObservedOn}.
          </p>
          {!publicResearch && missingNames.length > 0 ? (
            <div className="alert alert-warning mt-3 mb-0">
              Ainda falta uma situação para: {missingNames.join(", ")}.
            </div>
          ) : null}
        </div>

        {gap.canAdoptPublicEvidence ? (
          <div className="alert alert-info mb-0">
            <p>
              Sem disponibilidade para entrevistas? Argos pode aprofundar as
              mesmas candidatas com avaliações, reclamações e relatos públicos
              verificáveis, preservando histórico e limites. Isso inicia a
              pesquisa dentro dos limites acima; o custo do modelo será auditado
              separadamente.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              disabled={adoptPublicEvidence.isPending}
              onClick={() => adoptPublicEvidence.mutate()}
            >
              {adoptPublicEvidence.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                />
              ) : null}
              Usar pesquisa pública automatizada
            </button>
            {adoptPublicEvidence.isError ? (
              <p role="alert" className="text-danger mt-2">
                {adoptPublicEvidence.error.message}
              </p>
            ) : null}
          </div>
        ) : null}

        {gap.canResumePublicResearch ? (
          <div className="alert alert-warning mb-0">
            <p>
              Após corrigir a causa registrada, retome somente o aprofundamento.
              A pesquisa inicial e a tentativa bloqueada serão preservadas. A
              nova tentativa usa os limites acima e pode consumir buscas e
              modelo.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              disabled={resumePublicResearch.isPending}
              onClick={() => resumePublicResearch.mutate()}
            >
              {resumePublicResearch.isPending
                ? "Retomando..."
                : "Retomar aprofundamento após correção"}
            </button>
            {resumePublicResearch.isError ? (
              <p role="alert" className="text-danger mt-2">
                {resumePublicResearch.error.message}
              </p>
            ) : null}
          </div>
        ) : null}

        {gap.interviews.length > 0 ? (
          <div>
            <h3 className="h6">Relatos registrados</h3>
            <div className="row g-3">
              {gap.interviews.map((interview) => (
                <div className="col-12 col-lg-6" key={interview.id}>
                  <article className="border rounded-3 p-3 h-100">
                    <div className="d-flex justify-content-between gap-3 mb-2">
                      <strong>{interview.opportunityName}</strong>
                      <span className="badge text-bg-light">
                        {interview.outcome === "PURCHASED"
                          ? "Comprou"
                          : "Desistiu"}
                      </span>
                    </div>
                    <p className="small text-secondary mb-2">
                      Código {interview.anonymousParticipantCode} · situação de{" "}
                      {interview.occurredOn}
                    </p>
                    <p className="mb-2">{interview.purchaseSituation}</p>
                    <dl className="row small mb-0">
                      <dt className="col-5">Resultado buscado</dt>
                      <dd className="col-7">{interview.desiredResult}</dd>
                      <dt className="col-5">Dificuldade</dt>
                      <dd className="col-7">{interview.difficulty}</dd>
                      <dt className="col-5">Alternativa</dt>
                      <dd className="col-7">{interview.alternativeTried}</dd>
                      <dt className="col-5">Gasto conhecido</dt>
                      <dd className="col-7">
                        {interview.amountSpent == null
                          ? "Não informado"
                          : `${interview.currency} ${Number(interview.amountSpent).toFixed(2)}`}
                      </dd>
                    </dl>
                    <p className="mb-0 mt-2">
                      <strong>O que continuou difícil:</strong>{" "}
                      {interview.remainingDifficulty}
                    </p>
                  </article>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {awaitingInterviews ? (
          <form className="d-flex flex-column gap-3" onSubmit={handleSubmit}>
            <div>
              <h3 className="h6 mb-1">Registrar entrevista consentida</h3>
              <p className="small text-secondary mb-0">
                Pergunte sobre a última situação real, sem sugerir resposta.
                Veja a{" "}
                <a
                  href="https://www.gov.uk/service-manual/user-research/using-in-depth-interviews"
                  target="_blank"
                  rel="noreferrer"
                >
                  orientação de entrevistas em profundidade
                </a>
                . Não cadastre nome, e-mail, telefone ou contato.
              </p>
            </div>

            <div className="row g-3">
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="gap-opportunity">
                  Candidata <span aria-hidden="true">*</span>
                </label>
                <select
                  className="form-select"
                  id="gap-opportunity"
                  required
                  value={form.opportunityId}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      opportunityId: event.target.value,
                    }))
                  }
                >
                  <option value="">Selecione</option>
                  {opportunities.map((opportunity) => (
                    <option value={opportunity.id} key={opportunity.id}>
                      {opportunity.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-md-3">
                <label className="form-label" htmlFor="gap-participant-code">
                  Código anônimo <span aria-hidden="true">*</span>
                </label>
                <input
                  className="form-control"
                  id="gap-participant-code"
                  maxLength={7}
                  pattern="[A-Za-z]{1,3}[0-9]{1,4}"
                  placeholder="Ex.: P01"
                  required
                  value={form.anonymousParticipantCode}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      anonymousParticipantCode: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="col-12 col-md-3">
                <label className="form-label" htmlFor="gap-outcome">
                  Decisão passada <span aria-hidden="true">*</span>
                </label>
                <select
                  className="form-select"
                  id="gap-outcome"
                  required
                  value={form.outcome}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      outcome: event.target
                        .value as ProductDiscoveryInterviewOutcome,
                    }))
                  }
                >
                  <option value="PURCHASED">Comprou</option>
                  <option value="ABANDONED">Desistiu</option>
                </select>
              </div>
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="gap-occurred-on">
                  Data da situação <span aria-hidden="true">*</span>
                </label>
                <input
                  className="form-control"
                  id="gap-occurred-on"
                  type="date"
                  max={new Date().toISOString().slice(0, 10)}
                  required
                  value={form.occurredOn}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      occurredOn: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="gap-amount">
                  Quanto gastou (opcional)
                </label>
                <input
                  className="form-control"
                  id="gap-amount"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.amountSpent}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      amountSpent: event.target.value,
                    }))
                  }
                />
              </div>
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="gap-currency">
                  Moeda{" "}
                  {form.amountSpent ? <span aria-hidden="true">*</span> : null}
                </label>
                <input
                  className="form-control text-uppercase"
                  id="gap-currency"
                  maxLength={3}
                  minLength={3}
                  required={Boolean(form.amountSpent)}
                  value={form.currency}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      currency: event.target.value,
                    }))
                  }
                />
              </div>
            </div>

            {[
              [
                "purchaseSituation",
                "Conte a última vez em que isso aconteceu",
                "Ocasião, gatilho e prazo concretos.",
              ],
              [
                "desiredResult",
                "Que resultado buscava",
                "Resultado desejado nas palavras resumidas da pessoa.",
              ],
              [
                "difficulty",
                "O que estava difícil",
                "Dificuldade observada antes da decisão.",
              ],
              [
                "alternativeTried",
                "O que tentou ou considerou",
                "Alternativa gratuita ou paga realmente lembrada.",
              ],
              [
                "remainingDifficulty",
                "O que continuou difícil",
                "Obstáculo residual depois da compra ou desistência.",
              ],
            ].map(([field, label, placeholder]) => (
              <div key={field}>
                <label className="form-label" htmlFor={`gap-${field}`}>
                  {label} <span aria-hidden="true">*</span>
                </label>
                <textarea
                  className="form-control"
                  id={`gap-${field}`}
                  maxLength={3000}
                  placeholder={placeholder}
                  required
                  rows={3}
                  value={form[field as keyof FormState] as string}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      [field]: event.target.value,
                    }))
                  }
                />
              </div>
            ))}

            <div className="form-check">
              <input
                className="form-check-input"
                id="gap-consent"
                type="checkbox"
                required
                checked={form.consentConfirmed}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    consentConfirmed: event.target.checked,
                  }))
                }
              />
              <label className="form-check-label" htmlFor="gap-consent">
                A pessoa consentiu com o uso anônimo deste resumo. *
              </label>
            </div>
            <div className="form-check">
              <input
                className="form-check-input"
                id="gap-no-personal-data"
                type="checkbox"
                required
                checked={form.noPersonalDataConfirmed}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    noPersonalDataConfirmed: event.target.checked,
                  }))
                }
              />
              <label
                className="form-check-label"
                htmlFor="gap-no-personal-data"
              >
                Confirmei que o resumo não contém dados pessoais ou contato. *
              </label>
            </div>

            {createInterview.isError ? (
              <div className="alert alert-danger mb-0" role="alert">
                {createInterview.error.message}
              </div>
            ) : null}
            {createInterview.isSuccess ? (
              <div className="alert alert-success mb-0" role="status">
                Entrevista registrada. Os critérios foram recalculados pelo
                backend.
              </div>
            ) : null}

            <div>
              <button
                className="btn btn-primary"
                type="submit"
                disabled={createInterview.isPending}
              >
                {createInterview.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                    Salvando...
                  </>
                ) : (
                  "Registrar entrevista"
                )}
              </button>
            </div>
          </form>
        ) : (
          <div className="alert alert-info mb-0">
            {publicResearch
              ? "A pesquisa usa fontes públicas, sem entrevistas obrigatórias. Sua liberação não comprova comportamento de compra; confira as evidências e lacunas no relatório do ciclo."
              : gap.readyForResearch
                ? "Os critérios comportamentais foram atendidos. Argos pode aprofundar as lacunas sem repetir a pesquisa inicial."
                : "Esta atividade não aceita novas entrevistas no estado atual; o histórico permanece preservado."}
          </div>
        )}
      </div>
    </section>
  );
}
