import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./alcyone.css";

type Preparation = {
  jobId: string;
  productName: string;
  status: string;
  imageBase64?: string;
  error?: string;
  input: {
    occasion: string;
    pieces: string[];
    preferences: string;
    constraints: string;
  };
  estimatedCostUsd?: number;
  providerCalls: number | null;
  productVersion: string;
};

/** Recupera a própria entrega privada pelo backend PDE; leitura nunca enfileira geração. */
function VisualPreparation() {
  const jobId =
    new URLSearchParams(window.location.search).get("preparation") || "";
  const [token] = useState(() => {
    const key = "visual-delivery:" + jobId;
    const fragment = new URLSearchParams(window.location.hash.slice(1));
    const access =
      fragment.get("access") || window.sessionStorage.getItem(key) || "";
    if (access) window.sessionStorage.setItem(key, access);
    if (fragment.has("access"))
      window.history.replaceState(
        null,
        "",
        window.location.pathname + window.location.search,
      );
    return access;
  });
  const [result, setResult] = useState<Preparation | null>(null);
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (!/^pde-visual-v1-[a-f0-9-]{36}$/.test(jobId) || !token) {
      setError("Abra a entrega pelo acesso privado da sua preparação.");
      return;
    }
    let cancelled = false;
    let timer: number;
    const load = async () => {
      setWorking(true);
      try {
        const response = await fetch(
          "/api/pde/visual-personalization/v1/preparations/" + jobId,
          { headers: { "X-PDE-Visual-Session": token } },
        );
        const data = await response.json();
        if (!response.ok)
          throw new Error(
            data.detail ||
              data.message ||
              "A entrega não pode ser consultada agora.",
          );
        if (cancelled) return;
        setResult(data);
        setError("");
        if (
          ["PDE_QUEUED", "PDE_RUNNING", "PDE_RAW_RECEIVED"].includes(
            data.status,
          )
        )
          timer = window.setTimeout(() => {
            void load();
          }, 3000);
      } catch (cause) {
        if (!cancelled)
          setError(
            cause instanceof Error
              ? cause.message
              : "Não foi possível recuperar a entrega.",
          );
      } finally {
        if (!cancelled) setWorking(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [jobId, token, revision]);
  /** Atualiza aplicação e custo da mesma resposta, sem pedir geração ao worker. */
  async function reconcile() {
    setWorking(true);
    try {
      const response = await fetch(
        "/api/pde/visual-personalization/v1/preparations/" +
          jobId +
          "/reconcile",
        { method: "POST", headers: { "X-PDE-Visual-Session": token } },
      );
      const data = await response.json();
      if (!response.ok)
        throw new Error(
          data.detail ||
            data.message ||
            "Não foi possível conciliar esta resposta.",
        );
      setResult(data);
      setError("");
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Não foi possível conciliar esta resposta.",
      );
    } finally {
      setWorking(false);
    }
  }
  return (
    <main className="alcyone-shell">
      <p className="alcyone-eyebrow">Homologação privada · entrada simulada</p>
      <h1>{result?.productName || "Sua entrega visual"}</h1>
      <p>
        Três opções para ajudar a escolher o que vestir na ocasião, usando as
        peças informadas.
      </p>
      {error && <p role="alert">{error}</p>}
      {result && (
        <section aria-label="Minha entrega">
          <h2>{result.input.occasion}</h2>
          <p>{result.input.preferences}</p>
          {result.imageBase64 ? (
            <>
              <img
                src={"data:image/png;base64," + result.imageBase64}
                alt="Três combinações geradas para esta entrada"
                style={{ width: "100%", height: "auto", display: "block" }}
              />
              <p>
                Confira a combinação com suas peças reais antes de usar. A
                imagem é uma referência, sem garantia de satisfação.
              </p>
            </>
          ) : (
            <p role="status">
              {result.error ||
                "Estamos preparando suas opções. Sua entrada está preservada."}
            </p>
          )}
          <details>
            <summary>Dados da homologação</summary>
            <p>
              Versão de referência: {result.productVersion}. Chamadas de IA
              nesta tentativa:{" "}
              {result.providerCalls ?? "aguardando confirmação"}. Custo{" "}
              {result.estimatedCostUsd == null
                ? "não informado"
                : "estimado em US$ " +
                  Number(result.estimatedCostUsd).toFixed(6)}
              .
            </p>
            <p>
              Sem compra, publicação ou mídia. Não é prova de venda ou
              contribuição.
            </p>
            {["PDE_RAW_RECEIVED", "PDE_COST_PENDING"].includes(
              result.status,
            ) && (
              <button disabled={working} onClick={() => void reconcile()}>
                Reconciliar a mesma resposta
              </button>
            )}
          </details>
        </section>
      )}
      <button
        disabled={working}
        onClick={() => setRevision((value) => value + 1)}
      >
        {working ? "Consultando…" : "Recuperar minha entrega"}
      </button>
      <p>
        A oferta paga e sua continuidade ainda serão revisadas. Esta homologação
        não cobra nem abre checkout.
      </p>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <VisualPreparation />
  </React.StrictMode>,
);
