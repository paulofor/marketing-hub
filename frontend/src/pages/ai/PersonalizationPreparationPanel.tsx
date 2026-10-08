import { FormEvent, useEffect, useState } from "react";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";

type Input = {
  occasion: string;
  pieces: string[];
  preferences: string;
  constraints: string;
};
type Context = {
  productId: number;
  productName: string;
  commercialPlanId: number;
  experimentId: number;
  publicUrl: string;
  budget: {
    authorizationHash: string;
    maximumUsd: number;
    knownEstimatedUsd: number;
    availableUsd: number;
    productVersion: string;
    costComplete: boolean;
    blocker: string | null;
  };
};
type Preparation = {
  jobId: string;
  status: string;
  input: Input;
  imageBase64?: string;
  error?: string;
};
type Draft = {
  accessToken: string;
  operationKey: string;
  input: Input;
  jobId?: string;
};
const base = "/pde/visual-personalization/v1";
const active = ["PDE_QUEUED", "PDE_RUNNING", "PDE_RAW_RECEIVED"];

/** Abre a preparação autorizada pelo Hub sem escolher margem, publicar oferta ou chamar outro agente. */
export default function PersonalizationPreparationPanel({
  productId,
  planId,
  experimentId,
}: {
  productId: number;
  planId: number;
  experimentId: number;
}) {
  useBreadcrumbs([{ label: "Preparar entrega visual" }]);
  const storageKey =
    "visual-preparation:" + productId + ":" + planId + ":" + experimentId;
  const [context, setContext] = useState<Context | null>(null);
  const [occasion, setOccasion] = useState("");
  const [pieces, setPieces] = useState("");
  const [preferences, setPreferences] = useState("");
  const [constraints, setConstraints] = useState("");
  const [consent, setConsent] = useState(false);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [preparation, setPreparation] = useState<Preparation | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setContext(null);
    setError("");
    axios
      .get<Context>(base + "/products/" + productId + "/context", {
        params: { commercialPlanId: planId, experimentId },
      })
      .then(({ data }) => {
        if (!cancelled) setContext(data);
      })
      .catch((cause) => {
        if (!cancelled) setError(message(cause));
      });
    const saved = window.sessionStorage.getItem(storageKey);
    if (saved) {
      try {
        const value = JSON.parse(saved) as Draft;
        setDraft(value);
        setOccasion(value.input.occasion);
        setPieces(value.input.pieces.join("\n"));
        setPreferences(value.input.preferences);
        setConstraints(value.input.constraints);
        setConsent(true);
        if (value.jobId) void refresh(value);
      } catch {
        window.sessionStorage.removeItem(storageKey);
      }
    }
    return () => {
      cancelled = true;
    };
  }, [productId, planId, experimentId, storageKey]);

  useEffect(() => {
    if (!draft?.jobId || !preparation || !active.includes(preparation.status))
      return;
    const timer = window.setTimeout(() => {
      void refresh(draft);
    }, 3000);
    return () => window.clearTimeout(timer);
  }, [draft, preparation]);

  async function refresh(value: Draft) {
    if (!value.jobId) return;
    setWorking(true);
    try {
      const { data } = await axios.get<Preparation>(
        base + "/preparations/" + value.jobId,
        { headers: { "X-PDE-Visual-Session": value.accessToken } },
      );
      setPreparation(data);
      setError("");
    } catch (cause) {
      setError(message(cause));
    } finally {
      setWorking(false);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!context || working || !consent || preparation) return;
    const input = draft?.input ?? {
      occasion: occasion.trim(),
      pieces: pieces
        .split("\n")
        .map((s) => s.trim())
        .filter(Boolean),
      preferences: preferences.trim(),
      constraints: constraints.trim(),
    };
    const value = draft ?? {
      accessToken: randomKey(),
      operationKey: randomKey(),
      input,
    };
    window.sessionStorage.setItem(storageKey, JSON.stringify(value));
    setDraft(value);
    setWorking(true);
    setError("");
    try {
      const { data } = await axios.post<Preparation>(
        base + "/products/" + productId + "/preparations",
        {
          commercialPlanId: planId,
          experimentId,
          productVersion: context.budget.productVersion,
          authorizationHash: context.budget.authorizationHash,
          syntheticConsent: consent,
          accessToken: value.accessToken,
          operationKey: value.operationKey,
          input: value.input,
        },
      );
      const registered = { ...value, jobId: data.jobId };
      setDraft(registered);
      window.sessionStorage.setItem(storageKey, JSON.stringify(registered));
      setPreparation(data);
    } catch (cause) {
      setError(message(cause));
      if (axios.isAxiosError(cause) && cause.response?.status === 400) {
        setDraft(null);
        window.sessionStorage.removeItem(storageKey);
      }
    } finally {
      setWorking(false);
    }
  }

  /** Reaplica a resposta recebida quando custo ou aplicação foram recuperados, sem chamar IA. */
  async function reconcile() {
    if (!draft?.jobId || working) return;
    setWorking(true);
    try {
      const { data } = await axios.post<Preparation>(
        base + "/preparations/" + draft.jobId + "/reconcile",
        null,
        { headers: { "X-PDE-Visual-Session": draft.accessToken } },
      );
      setPreparation(data);
      setError("");
    } catch (cause) {
      setError(message(cause));
    } finally {
      setWorking(false);
    }
  }

  const validIdentity = [productId, planId, experimentId].every(
    (id) => Number.isSafeInteger(id) && id > 0,
  );
  const canPrepare =
    validIdentity &&
    context?.budget.costComplete &&
    !context.budget.blocker &&
    context.budget.availableUsd > 0;
  let deliveryUrl = "";
  if (context?.publicUrl && draft?.jobId) {
    const url = new URL(
      "personalization.html",
      context.publicUrl.replace(/\/?$/, "/"),
    );
    url.searchParams.set("preparation", draft.jobId);
    url.hash = new URLSearchParams({ access: draft.accessToken }).toString();
    deliveryUrl = url.toString();
  }
  return (
    <div className="container py-4" style={{ maxWidth: 880 }}>
      <PageTitle>Preparar entrega visual</PageTitle>
      <p>
        Uma ocasião, poucas peças e três opções para escolher. Esta preparação
        interna usa dados simulados e não realiza compra.
      </p>
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      {!validIdentity && (
        <div className="alert alert-warning">
          Informe produto, plano e experimento válidos no contexto da
          preparação.
        </div>
      )}
      {!context && validIdentity && !error && (
        <p role="status">Conferindo a preparação autorizada…</p>
      )}
      {context && (
        <div className="card mb-4">
          <div className="card-body">
            <h2 className="h5">{context.productName}</h2>
            <p>
              IA autorizada para esta preparação: até US${" "}
              {Number(context.budget.maximumUsd).toFixed(2)} no total. Consumo
              conhecido estimado: US${" "}
              {Number(context.budget.knownEstimatedUsd).toFixed(6)}. Não inclui
              mídia ou vídeo pago.
            </p>
            {context.budget.blocker && (
              <p role="alert">{context.budget.blocker}</p>
            )}
          </div>
        </div>
      )}
      {!preparation && (
        <form onSubmit={submit}>
          <fieldset
            disabled={working || !canPrepare || Boolean(draft)}
            className="border-0 p-0"
          >
            <label className="form-label" htmlFor="visual-occasion">
              Ocasião e condições práticas *
            </label>
            <input
              id="visual-occasion"
              className="form-control mb-3"
              value={occasion}
              required
              maxLength={500}
              onChange={(event) => setOccasion(event.target.value)}
              placeholder="Ex.: jantar informal, à tarde, dia quente"
            />
            <label className="form-label" htmlFor="visual-pieces">
              Peças disponíveis, uma por linha *
            </label>
            <textarea
              id="visual-pieces"
              className="form-control mb-3"
              value={pieces}
              required
              rows={5}
              onChange={(event) => setPieces(event.target.value)}
              placeholder="Informe entre 2 e 12 peças; sem foto corporal."
            />
            <label className="form-label" htmlFor="visual-preferences">
              Preferências práticas *
            </label>
            <input
              id="visual-preferences"
              className="form-control mb-3"
              value={preferences}
              required
              maxLength={500}
              onChange={(event) => setPreferences(event.target.value)}
            />
            <label className="form-label" htmlFor="visual-constraints">
              Restrições práticas
            </label>
            <input
              id="visual-constraints"
              className="form-control mb-3"
              value={constraints}
              maxLength={500}
              onChange={(event) => setConstraints(event.target.value)}
            />
            <label className="form-check mb-3">
              <input
                type="checkbox"
                className="form-check-input"
                checked={consent}
                onChange={(event) => setConsent(event.target.checked)}
                required
              />
              <span className="form-check-label">
                Confirmo entrada simulada e homologação interna, dentro da
                autorização de IA já registrada.
              </span>
            </label>
          </fieldset>
          <button
            className="btn btn-primary"
            disabled={working || !canPrepare || !consent}
          >
            {working
              ? "Registrando…"
              : draft
                ? "Reenviar o mesmo registro"
                : "Preparar minhas três opções"}
          </button>
        </form>
      )}
      {preparation && (
        <div className="card">
          <div className="card-body">
            <h2 className="h5">
              {preparation.status === "PDE_COMPLETED"
                ? "Três opções para a ocasião"
                : "Preparação da entrega"}
            </h2>
            {active.includes(preparation.status) && (
              <p role="status">
                Estamos preparando suas opções. Você pode retornar à mesma
                entrega.
              </p>
            )}
            {preparation.error && <p role="alert">{preparation.error}</p>}
            {preparation.imageBase64 && (
              <img
                src={"data:image/png;base64," + preparation.imageBase64}
                alt="Painel gerado para a ocasião e as peças informadas"
                style={{ width: "100%", height: "auto" }}
              />
            )}
            <button
              className="btn btn-outline-primary me-2 mt-3"
              disabled={working}
              onClick={() => draft && void refresh(draft)}
            >
              Consultar resultado preservado
            </button>
            {["PDE_COST_PENDING", "PDE_RAW_RECEIVED"].includes(
              preparation.status,
            ) && (
              <button
                className="btn btn-outline-primary me-2 mt-3"
                disabled={working}
                onClick={() => void reconcile()}
              >
                Reconciliar a mesma resposta
              </button>
            )}
            {deliveryUrl && (
              <a
                className="btn btn-primary mt-3"
                target="_blank"
                rel="noopener noreferrer"
                href={deliveryUrl}
              >
                Abrir minha entrega privada
              </a>
            )}
            <p className="mt-3 mb-0">
              A imagem será uma prova para a revisão da oferta e da comunicação.
              O preço, a continuidade paga e a margem continuam sujeitos aos
              pareceres do mesmo produto.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

/** Cria correlação e credencial locais sem reutilizar identidades de outra preparação. */
function randomKey() {
  return [...crypto.getRandomValues(new Uint8Array(32))]
    .map((v) => v.toString(16).padStart(2, "0"))
    .join("");
}

/** Explica falha de registro ou leitura sem incentivar regeneração. */
function message(cause: unknown) {
  if (axios.isAxiosError(cause)) {
    const data = cause.response?.data;
    return (
      data?.detail ||
      data?.reason ||
      data?.message ||
      "Não foi possível consultar a preparação. Reenvie o mesmo registro ou recupere a entrega preservada."
    );
  }
  return "Não foi possível consultar a preparação.";
}
