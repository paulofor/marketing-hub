import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import {
  useCreateFacebookSuccessor,
  useFacebookSuccessorReadiness,
} from "../../api/experiment/useFacebookSuccessor";
import type { Experiment } from "../../api/experiment/useExperiments";

function utcDateOffset(days: number) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

interface ExperimentFacebookSuccessorPanelProps {
  experiment: Experiment;
}

export default function ExperimentFacebookSuccessorPanel({
  experiment,
}: ExperimentFacebookSuccessorPanelProps) {
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState(false);
  const readiness = useFacebookSuccessorReadiness(experiment.id);
  const createSuccessor = useCreateFacebookSuccessor(experiment.id);
  const shouldLoadMetaAssets = readiness.data?.available === true && expanded;
  const facebookPages = useAllFacebookPages(shouldLoadMetaAssets);
  const instagramAccounts = useInstagramAccounts(shouldLoadMetaAssets);
  const [dailyBudget, setDailyBudget] = useState("20.00");
  const [mediaSpendLimit, setMediaSpendLimit] = useState("100.00");
  const [startDate, setStartDate] = useState(utcDateOffset(0));
  const [endDate, setEndDate] = useState(utcDateOffset(4));
  const [facebookPageId, setFacebookPageId] = useState("");
  const [instagramAccountId, setInstagramAccountId] = useState("");

  useEffect(() => {
    if ((facebookPages.data?.length ?? 0) === 1) {
      setFacebookPageId(String(facebookPages.data?.[0].id ?? ""));
    }
  }, [facebookPages.data]);

  useEffect(() => {
    if ((instagramAccounts.data?.length ?? 0) === 1) {
      setInstagramAccountId(String(instagramAccounts.data?.[0].id ?? ""));
    }
  }, [instagramAccounts.data]);

  if (readiness.isLoading) {
    return null;
  }

  const decision = readiness.data;
  if (!decision) {
    return null;
  }

  if (decision.existingSuccessorId) {
    return (
      <div className="alert alert-info mt-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
        <span>
          O sucessor Facebook deste experimento já existe e mantém execução
          separada.
        </span>
        <button
          type="button"
          className="btn btn-sm btn-outline-primary"
          onClick={() =>
            navigate(`/experiments/${decision.existingSuccessorId}`)
          }
        >
          Abrir sucessor Facebook
        </button>
      </div>
    );
  }

  if (!decision.available) {
    return null;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (
      !window.confirm(
        "Criar um experimento Facebook separado? Esta ação não publica campanha nem inicia gasto.",
      )
    ) {
      return;
    }
    try {
      const successor = await createSuccessor.mutateAsync({
        dailyBudget: Number(dailyBudget),
        mediaSpendLimit: Number(mediaSpendLimit),
        startDate,
        endDate,
        facebookPageId: Number(facebookPageId),
        instagramAccountId: Number(instagramAccountId),
      });
      toast.success("Sucessor Facebook criado com teto financeiro protegido.");
      navigate(`/experiments/${successor.id}`);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message || error.message
        : "Não foi possível criar o sucessor Facebook.";
      toast.error(message);
    }
  };

  return (
    <section className="card border-primary-subtle mt-3">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h2 className="h6 mb-1">Novo teste em Facebook Ads</h2>
            <p className="text-muted small mb-0">
              Preserva este experimento e cria outro sem campanhas, métricas ou
              aprovações herdadas.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setExpanded((current) => !current)}
            aria-expanded={expanded}
          >
            {expanded ? "Fechar configuração" : "Criar sucessor Facebook"}
          </button>
        </div>

        {expanded ? (
          <form className="row g-3 mt-1" onSubmit={handleSubmit}>
            {facebookPages.isError || instagramAccounts.isError ? (
              <div className="col-12">
                <div className="alert alert-danger mb-0" role="alert">
                  Não foi possível carregar as identidades Meta. Recarregue a
                  configuração antes de criar o experimento.
                </div>
              </div>
            ) : null}
            <div className="col-12 col-md-6 col-xl-3">
              <label className="form-label" htmlFor="successorDailyBudget">
                Orçamento diário (R$) *
              </label>
              <input
                id="successorDailyBudget"
                className="form-control"
                type="number"
                min="0.01"
                step="0.01"
                required
                value={dailyBudget}
                onChange={(event) => setDailyBudget(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <label className="form-label" htmlFor="successorSpendLimit">
                Teto total de mídia (R$) *
              </label>
              <input
                id="successorSpendLimit"
                className="form-control"
                type="number"
                min="0.01"
                step="0.01"
                required
                value={mediaSpendLimit}
                onChange={(event) => setMediaSpendLimit(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <label className="form-label" htmlFor="successorStartDate">
                Início *
              </label>
              <input
                id="successorStartDate"
                className="form-control"
                type="date"
                required
                value={startDate}
                onChange={(event) => setStartDate(event.target.value)}
              />
            </div>
            <div className="col-12 col-md-6 col-xl-3">
              <label className="form-label" htmlFor="successorEndDate">
                Término *
              </label>
              <input
                id="successorEndDate"
                className="form-control"
                type="date"
                required
                value={endDate}
                onChange={(event) => setEndDate(event.target.value)}
              />
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="successorFacebookPage">
                Página do Facebook *
              </label>
              <select
                id="successorFacebookPage"
                className="form-select"
                required
                value={facebookPageId}
                onChange={(event) => setFacebookPageId(event.target.value)}
                disabled={facebookPages.isLoading}
              >
                <option value="">Selecione a página</option>
                {(facebookPages.data ?? []).map((page) => (
                  <option key={page.id} value={page.id}>
                    {page.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="successorInstagram">
                Conta do Instagram *
              </label>
              <select
                id="successorInstagram"
                className="form-select"
                required
                value={instagramAccountId}
                onChange={(event) => setInstagramAccountId(event.target.value)}
                disabled={instagramAccounts.isLoading}
              >
                <option value="">Selecione a conta</option>
                {(instagramAccounts.data ?? []).map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.handle} — {account.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 d-flex justify-content-end">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={
                  createSuccessor.isPending ||
                  facebookPages.isLoading ||
                  instagramAccounts.isLoading ||
                  facebookPages.isError ||
                  instagramAccounts.isError
                }
              >
                {createSuccessor.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                    Criando...
                  </>
                ) : (
                  "Criar experimento com teto"
                )}
              </button>
            </div>
          </form>
        ) : null}
      </div>
    </section>
  );
}
