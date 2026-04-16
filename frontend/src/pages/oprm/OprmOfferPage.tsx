import { useMemo, useState } from "react";
import { AlertCircle } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmRoutineWorkspaceData } from "../../api/oprm/useOprmRoutineWorkspaceData";

function readSignalList(value: unknown): Record<string, unknown>[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter(
    (item): item is Record<string, unknown> =>
      typeof item === "object" && item !== null,
  );
}

function readSignalLabel(signal: Record<string, unknown>, primary: string, fallback: string) {
  return typeof signal[primary] === "string"
    ? String(signal[primary])
    : typeof signal[fallback] === "string"
      ? String(signal[fallback])
      : "Sinal sem descrição";
}

export default function OprmOfferPage() {
  const { occupationSeedRef } = useParams();
  const navigate = useNavigate();
  const routineQuery = useOprmRoutineWorkspaceData(occupationSeedRef);

  const [selectedPain, setSelectedPain] = useState("");
  const [selectedOutcome, setSelectedOutcome] = useState("");
  const [selectedMechanism, setSelectedMechanism] = useState("");
  const [proofText, setProofText] = useState("");
  const [offerText, setOfferText] = useState("");

  const pains = useMemo(
    () => routineQuery.data?.painSignals ?? [],
    [routineQuery.data?.painSignals],
  );

  const outcomes = useMemo(
    () => routineQuery.data?.desiredOutcomeSignals ?? [],
    [routineQuery.data?.desiredOutcomeSignals],
  );

  const mechanisms = useMemo(
    () => routineQuery.data?.mechanismOpportunitySignals ?? [],
    [routineQuery.data?.mechanismOpportunitySignals],
  );

  const canBuild = selectedPain && selectedOutcome && selectedMechanism;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Oferta</PageTitle>
        <p className="text-secondary mb-0">
          Construa a proposta comercial baseada nos sinais reais da rotina da ocupação.
        </p>
      </header>

      <OprmModuleNavigation occupationSeedRef={occupationSeedRef} />

      {routineQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando dados para oferta...</span>
          </div>
        </div>
      ) : null}

      {routineQuery.isError ? (
        <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar os sinais para o builder.</strong>
            <p className="mb-0">Garanta que a rotina já foi publicada para esta ocupação.</p>
          </div>
        </div>
      ) : null}

      {!routineQuery.isLoading && !routineQuery.isError && !routineQuery.data?.frameworkInputPayload ? (
        <div className="alert alert-secondary mb-0" role="status">
          Nenhum artifact `dorResultadoOfertaMecanismoProvaInput` disponível para esta ocupação.
          <div className="mt-2">
            <Link to={`/oprm/routine/${encodeURIComponent(occupationSeedRef ?? "")}`}>Voltar para rotina</Link>
          </div>
        </div>
      ) : null}

      {!routineQuery.isLoading && !routineQuery.isError && routineQuery.data?.frameworkInputPayload ? (
        <>
          <section className="row g-3">
            <div className="col-12 col-xl-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <label className="form-label fw-semibold mb-0">Dor principal *</label>
                  {pains.length > 0 ? pains.map((pain, index) => {
                    const label = readSignalLabel(pain, "painLabel", "painSummary");
                    return (
                      <div className="form-check" key={`pain-${index}`}>
                        <input
                          className="form-check-input"
                          type="radio"
                          name="oprm-pain"
                          id={`oprm-pain-${index}`}
                          checked={selectedPain === label}
                          onChange={() => setSelectedPain(label)}
                        />
                        <label className="form-check-label" htmlFor={`oprm-pain-${index}`}>
                          {label}
                        </label>
                      </div>
                    );
                  }) : <p className="mb-0 text-secondary">Sem dores disponíveis.</p>}
                </div>
              </div>
            </div>

            <div className="col-12 col-xl-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <label className="form-label fw-semibold mb-0">Resultado desejado *</label>
                  {outcomes.length > 0 ? outcomes.map((outcome, index) => {
                    const label = readSignalLabel(outcome, "outcomeLabel", "outcomeSummary");
                    return (
                      <div className="form-check" key={`outcome-${index}`}>
                        <input
                          className="form-check-input"
                          type="radio"
                          name="oprm-outcome"
                          id={`oprm-outcome-${index}`}
                          checked={selectedOutcome === label}
                          onChange={() => setSelectedOutcome(label)}
                        />
                        <label className="form-check-label" htmlFor={`oprm-outcome-${index}`}>
                          {label}
                        </label>
                      </div>
                    );
                  }) : <p className="mb-0 text-secondary">Sem resultados disponíveis.</p>}
                </div>
              </div>
            </div>

            <div className="col-12 col-xl-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <label className="form-label fw-semibold mb-0">Mecanismo sugerido *</label>
                  {mechanisms.length > 0 ? mechanisms.map((mechanism, index) => {
                    const label = readSignalLabel(mechanism, "mechanismLabel", "mechanismSummary");
                    return (
                      <div className="form-check" key={`mechanism-${index}`}>
                        <input
                          className="form-check-input"
                          type="radio"
                          name="oprm-mechanism"
                          id={`oprm-mechanism-${index}`}
                          checked={selectedMechanism === label}
                          onChange={() => setSelectedMechanism(label)}
                        />
                        <label className="form-check-label" htmlFor={`oprm-mechanism-${index}`}>
                          {label}
                        </label>
                      </div>
                    );
                  }) : <p className="mb-0 text-secondary">Sem mecanismos disponíveis.</p>}
                </div>
              </div>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <div>
                <label className="form-label" htmlFor="oprm-proof">Prova inicial *</label>
                <textarea
                  id="oprm-proof"
                  className="form-control"
                  rows={3}
                  value={proofText}
                  onChange={(event) => setProofText(event.target.value)}
                  placeholder="Ex.: Evidência operacional ou social proof relacionada"
                />
              </div>
              <div>
                <label className="form-label" htmlFor="oprm-offer">Oferta proposta *</label>
                <textarea
                  id="oprm-offer"
                  className="form-control"
                  rows={5}
                  value={offerText}
                  onChange={(event) => setOfferText(event.target.value)}
                  placeholder="Descreva a proposta no formato comercial"
                />
              </div>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-2">
              <h2 className="h5 mb-0">Preview estruturado</h2>
              <dl className="row mb-0">
                <dt className="col-sm-3">Dor</dt>
                <dd className="col-sm-9">{selectedPain || "—"}</dd>
                <dt className="col-sm-3">Resultado</dt>
                <dd className="col-sm-9">{selectedOutcome || "—"}</dd>
                <dt className="col-sm-3">Oferta</dt>
                <dd className="col-sm-9">{offerText || "—"}</dd>
                <dt className="col-sm-3">Mecanismo</dt>
                <dd className="col-sm-9">{selectedMechanism || "—"}</dd>
                <dt className="col-sm-3">Prova</dt>
                <dd className="col-sm-9">{proofText || "—"}</dd>
              </dl>
            </div>
          </section>

          <div className="d-flex flex-wrap gap-2">
            <button
              type="button"
              className="btn btn-primary"
              disabled={!canBuild}
              onClick={() => navigate("/hypotheses")}
            >
              Enviar para hipótese
            </button>
            <button
              type="button"
              className="btn btn-outline-primary"
              disabled={!canBuild}
              onClick={() => navigate("/landings")}
            >
              Enviar para landing
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary"
              disabled={!canBuild}
              onClick={() => navigate("/experiments/new")}
            >
              Enviar para experimento
            </button>
          </div>
        </>
      ) : null}
    </div>
  );
}
