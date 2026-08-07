import {
  AlertTriangle,
  BadgeCheck,
  CreditCard,
  ExternalLink,
  RefreshCw,
  ShieldCheck,
  Video,
} from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import {
  useProviderCreditPurchases,
  useRegisterProviderCreditPurchase,
} from "../../api/planning/useProviderCreditPurchases";
import { useSalesVideoProviderScores } from "../../api/salesVideo/useSalesVideoProviderScores";
import {
  SALES_VIDEO_PROVIDER_OPTIONS,
  SalesVideoProviderOption,
} from "../../api/salesVideo/videoProviderCatalog";
import { SalesVideoProviderScore } from "../../api/salesVideo/types";
import "./VideoProviderManagementPage.css";

type ProviderRow = {
  option: SalesVideoProviderOption;
  score?: SalesVideoProviderScore;
};

const RECOMMENDATION_PRIORITY: Record<string, number> = {
  priorizar: 4,
  testar_controlado: 3,
  usar_com_cautela: 2,
  bloquear_ou_regenerar: 1,
};

const RECOMMENDATION_LABELS: Record<string, string> = {
  priorizar: "Priorizar",
  testar_controlado: "Teste controlado",
  usar_com_cautela: "Usar com cautela",
  bloquear_ou_regenerar: "Regenerar e testar",
};

export default function VideoProviderManagementPage() {
  const providerScoresQuery = useSalesVideoProviderScores();
  const [purchaseProvider, setPurchaseProvider] = useState<SalesVideoProviderOption>();

  const rows = useMemo<ProviderRow[]>(() => {
    const scoresByProvider = new Map(
      (providerScoresQuery.data ?? []).map((score) => [
        score.providerName,
        score,
      ]),
    );
    return SALES_VIDEO_PROVIDER_OPTIONS.map((option) => ({
      option,
      score: scoresByProvider.get(option.providerName),
    })).sort(compareProviderRows);
  }, [providerScoresQuery.data]);

  const bestProvider = rows
    .filter((row) => row.score)
    .sort((a, b) => (b.score?.score ?? 0) - (a.score?.score ?? 0))[0];

  return (
    <div className="video-provider-page">
      <div className="video-provider-page__header">
        <div>
          <h1>Provedores de vídeo</h1>
          <p className="video-provider-page__subtitle">
            Gestão de provedores com score baseado no histórico real de jobs,
            aprovação humana e conversão comercial.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-primary"
          onClick={() => providerScoresQuery.refetch()}
          disabled={providerScoresQuery.isFetching}
        >
          <RefreshCw size={16} aria-hidden="true" />
          Atualizar
        </button>
      </div>

      <section className="video-provider-page__summary" aria-label="Resumo">
        <Metric label="Provedores" value={rows.length} />
        <Metric
          label="Com histórico"
          value={rows.filter((row) => row.score).length}
        />
        <Metric
          label="Melhor score"
          value={bestProvider?.score?.score ?? "-"}
        />
        <Metric
          label="Fornecedor líder"
          value={bestProvider?.option.label ?? "Sem dados"}
        />
      </section>

      {providerScoresQuery.isError ? (
        <div className="video-provider-page__notice video-provider-page__notice--error">
          <AlertTriangle size={18} aria-hidden="true" />
          Não foi possível carregar o score dos provedores.
        </div>
      ) : null}

      <section className="video-provider-page__grid" aria-label="Provedores">
        {rows.map(({ option, score }) => (
          <article className="video-provider-page__card" key={option.key}>
            <div className="video-provider-page__card-header">
              <div>
                <span className="video-provider-page__provider-icon">
                  <Video size={18} aria-hidden="true" />
                </span>
                <h2>{option.label}</h2>
              </div>
              <span
                className={[
                  "video-provider-page__recommendation",
                  recommendationClass(score?.recommendation),
                ]
                  .filter(Boolean)
                  .join(" ")}
              >
                {RECOMMENDATION_LABELS[score?.recommendation ?? ""] ??
                  "Sem histórico"}
              </span>
            </div>

            <div className="video-provider-page__score">
              <strong>{score?.score ?? "N/D"}</strong>
              <span>score</span>
              <div
                className="video-provider-page__score-bar"
                aria-hidden="true"
              >
                <span style={{ width: `${score?.score ?? 0}%` }} />
              </div>
            </div>

            <p className="video-provider-page__use">{option.recommendedUse}</p>

            <dl className="video-provider-page__facts">
              <Fact label="Prontos" value={score?.readyJobs ?? 0} />
              <Fact label="Falhas" value={score?.failedJobs ?? 0} />
              <Fact
                label="Falhas operacionais"
                value={score?.operationalFailedJobs ?? 0}
              />
              <Fact label="Aprovados" value={score?.approvedAssets ?? 0} />
              <Fact label="Rejeitados" value={score?.rejectedAssets ?? 0} />
              <Fact label="Leads" value={score?.leads ?? 0} />
              <Fact label="Compras" value={score?.purchases ?? 0} />
              <Fact
                label="Receita"
                value={formatCurrency(score?.revenue ?? 0)}
              />
              <Fact
                label="Duração direta"
                value={`${option.maxDirectDurationSeconds ?? option.clipDurationSeconds}s`}
              />
            </dl>
            {score?.riskMessage ? (
              <p className="video-provider-page__risk">{score.riskMessage}</p>
            ) : null}

            <div className="video-provider-page__capabilities">
              <Capability
                active={option.supportsHeroVideo}
                label="Hero PDE"
              />
              <Capability
                active={option.supportsSceneAssembly}
                label="Montagem"
              />
              <Capability
                active={option.supportsOpenAiReferenceImage}
                label="Imagem base"
              />
            </div>
            {option.creditsUrl ? (
              <div className="video-provider-page__credit-actions">
                <a
                  className="btn btn-outline-primary video-provider-page__credits-link"
                  href={option.creditsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Comprar créditos
                  <ExternalLink size={15} aria-hidden="true" />
                </a>
                <button
                  type="button"
                  className="btn btn-primary"
                  aria-label={`Registrar compra ${option.label}`}
                  onClick={() => setPurchaseProvider(option)}
                >
                  <CreditCard size={15} aria-hidden="true" />
                  Registrar compra
                </button>
              </div>
            ) : null}
          </article>
        ))}
      </section>
      {purchaseProvider ? (
        <CreditPurchaseDialog
          provider={purchaseProvider}
          onClose={() => setPurchaseProvider(undefined)}
        />
      ) : null}
    </div>
  );
}

function CreditPurchaseDialog({
  provider,
  onClose,
}: {
  provider: SalesVideoProviderOption;
  onClose: () => void;
}) {
  const purchasesQuery = useProviderCreditPurchases(provider.providerName);
  const registerPurchase = useRegisterProviderCreditPurchase(provider.providerName);
  const [purchasedAt, setPurchasedAt] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [creditsPurchased, setCreditsPurchased] = useState("");
  const [evidenceReference, setEvidenceReference] = useState("");

  const submit = (event: FormEvent) => {
    event.preventDefault();
    registerPurchase.mutate({
      purchasedAt: new Date(purchasedAt).toISOString(),
      amount: Number(amount),
      currency,
      creditsPurchased: Number(creditsPurchased),
      evidenceReference: evidenceReference.trim() || null,
    });
  };

  return (
    <div className="video-provider-page__dialog-backdrop" role="presentation">
      <section
        className="video-provider-page__dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="credit-purchase-title"
      >
        <div className="video-provider-page__dialog-header">
          <div>
            <h2 id="credit-purchase-title">Registrar créditos — {provider.label}</h2>
            <p>Registra a saída de caixa sem duplicar o custo consumido pelos renders.</p>
          </div>
          <button type="button" className="btn-close" aria-label="Fechar" onClick={onClose} />
        </div>
        <form onSubmit={submit}>
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label" htmlFor="credit-purchased-at">Data e hora *</label>
              <input id="credit-purchased-at" className="form-control" type="datetime-local" required value={purchasedAt} onChange={(event) => setPurchasedAt(event.target.value)} />
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="credit-amount">Valor *</label>
              <input id="credit-amount" className="form-control" type="number" min="0.01" step="0.01" required value={amount} onChange={(event) => setAmount(event.target.value)} />
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="credit-currency">Moeda *</label>
              <select id="credit-currency" className="form-select" required value={currency} onChange={(event) => setCurrency(event.target.value)}>
                <option value="USD">USD</option>
                <option value="BRL">BRL</option>
              </select>
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="credit-quantity">Créditos adquiridos *</label>
              <input id="credit-quantity" className="form-control" type="number" min="1" step="1" required value={creditsPurchased} onChange={(event) => setCreditsPurchased(event.target.value)} />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="credit-evidence">Referência do comprovante</label>
              <input id="credit-evidence" className="form-control" maxLength={500} placeholder="Número da fatura ou URL" value={evidenceReference} onChange={(event) => setEvidenceReference(event.target.value)} />
            </div>
          </div>
          {registerPurchase.isError ? <div className="alert alert-danger mt-3">Não foi possível registrar a compra.</div> : null}
          {registerPurchase.isSuccess ? <div className="alert alert-success mt-3">Compra registrada no Financeiro.</div> : null}
          <div className="video-provider-page__dialog-actions">
            <button type="button" className="btn btn-outline-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={registerPurchase.isPending}>
              {registerPurchase.isPending ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
              Salvar no Financeiro
            </button>
          </div>
        </form>
        <div className="video-provider-page__purchase-history">
          <h3>Histórico</h3>
          {purchasesQuery.isLoading ? <p>Carregando...</p> : null}
          {purchasesQuery.data?.length === 0 ? <p>Nenhuma compra registrada.</p> : null}
          {purchasesQuery.data?.map((purchase) => (
            <p key={purchase.id}>
              <strong>{purchase.currency} {purchase.amount.toFixed(2)}</strong> — {purchase.creditsPurchased.toLocaleString("pt-BR")} créditos em {new Date(purchase.purchasedAt).toLocaleString("pt-BR")}
            </p>
          ))}
        </div>
      </section>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="video-provider-page__metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string | number }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </>
  );
}

function Capability({ active, label }: { active: boolean; label: string }) {
  return (
    <span
      className={[
        "video-provider-page__capability",
        active ? "is-active" : "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {active ? (
        <BadgeCheck size={14} aria-hidden="true" />
      ) : (
        <ShieldCheck size={14} aria-hidden="true" />
      )}
      {label}
    </span>
  );
}

function recommendationClass(recommendation?: string) {
  if (recommendation === "priorizar") {
    return "is-good";
  }
  if (recommendation === "bloquear_ou_regenerar") {
    return "is-warning";
  }
  if (recommendation === "usar_com_cautela") {
    return "is-warning";
  }
  return "";
}

export function compareProviderRows(a: ProviderRow, b: ProviderRow) {
  const scoreDiff = (b.score?.score ?? -1) - (a.score?.score ?? -1);
  if (scoreDiff !== 0) {
    return scoreDiff;
  }

  const recommendationDiff =
    recommendationPriority(b.score?.recommendation) -
    recommendationPriority(a.score?.recommendation);
  if (recommendationDiff !== 0) {
    return recommendationDiff;
  }

  const revenueDiff = (b.score?.revenue ?? 0) - (a.score?.revenue ?? 0);
  if (revenueDiff !== 0) {
    return revenueDiff;
  }

  const purchaseDiff = (b.score?.purchases ?? 0) - (a.score?.purchases ?? 0);
  if (purchaseDiff !== 0) {
    return purchaseDiff;
  }

  const leadDiff = (b.score?.leads ?? 0) - (a.score?.leads ?? 0);
  if (leadDiff !== 0) {
    return leadDiff;
  }

  const readyJobsDiff = (b.score?.readyJobs ?? 0) - (a.score?.readyJobs ?? 0);
  if (readyJobsDiff !== 0) {
    return readyJobsDiff;
  }

  const failedJobsDiff = (a.score?.failedJobs ?? 0) - (b.score?.failedJobs ?? 0);
  if (failedJobsDiff !== 0) {
    return failedJobsDiff;
  }

  return a.option.label.localeCompare(b.option.label, "pt-BR");
}

function recommendationPriority(recommendation?: string) {
  return RECOMMENDATION_PRIORITY[recommendation ?? ""] ?? 0;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(value);
}
