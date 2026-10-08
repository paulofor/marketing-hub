import { useState, useEffect, useRef } from "react";
import axios from "axios";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";
import { cycleError } from "../../api/learningCycle/useLearningCycles";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";

/** Abre a implementação com acesso privado, sem segredo na URL e sem iniciar inferência paga. */
export default function CyclePrivateKitLaunch({
  cycle,
}: {
  cycle: LearningCycle;
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const pending = useRef<{
    window: Window;
    origin: string;
    secret: string;
    ready: boolean;
  } | null>(null);
  useEffect(() => {
    const receive = (event: MessageEvent) => {
      const p = pending.current;
      if (
        !p ||
        event.source !== p.window ||
        event.origin !== p.origin ||
        event.data?.type !== "PDE_PRIVATE_KIT_READY_V1"
      )
        return;
      p.ready = true;
      if (p.secret) {
        p.window.postMessage(
          { type: "PDE_PRIVATE_KIT_ACCESS_V1", sessionToken: p.secret },
          p.origin,
        );
        pending.current = null;
      }
    };
    window.addEventListener("message", receive);
    return () => window.removeEventListener("message", receive);
  }, []);
  const impl = cycle.valueFlow?.implementation;
  if (!impl?.available || !impl.prototypeUrl) return null;
  async function launch() {
    if (!impl?.prototypeUrl) return;
    setError("");
    setBusy(true);
    const page = window.open(impl.prototypeUrl, "_blank");
    if (!page) {
      setError(
        "O navegador bloqueou a abertura. Permita a nova aba para esta página e tente novamente.",
      );
      setBusy(false);
      return;
    }
    pending.current = {
      window: page,
      origin: new URL(impl.prototypeUrl).origin,
      secret: "",
      ready: false,
    };
    try {
      const { data } = await axios.post<{ sessionToken: string }>(
        "/api/pde/kit/private/v1/admin/sessions",
        {
          requestKey: createCycleRequestKey(),
          productId: cycle.productId,
          cycleId: cycle.id,
          prototypeVersion: cycle.productVersion,
          scenarioCode: "ADHERENT",
          deviceProfile: "DESKTOP_1440",
        },
      );
      const p = pending.current;
      if (p) {
        p.secret = data.sessionToken;
        if (p.ready) {
          page.postMessage(
            { type: "PDE_PRIVATE_KIT_ACCESS_V1", sessionToken: p.secret },
            p.origin,
          );
          pending.current = null;
        }
      }
    } catch (ex) {
      page.close();
      pending.current = null;
      setError(cycleError(ex));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="mt-2">
      <button
        className="btn btn-primary"
        type="button"
        disabled={busy}
        onClick={() => void launch()}
      >
        {busy
          ? "Preparando acesso privado…"
          : "Abrir versão privada e primeira aplicação"}
      </button>
      <p className="small mb-0">
        Usa dados fictícios, preserva os arquivos e não cria compra, campanha ou
        chamada paga.
      </p>
      {error && (
        <p role="alert" className="alert alert-danger">
          {error}
        </p>
      )}
    </div>
  );
}
