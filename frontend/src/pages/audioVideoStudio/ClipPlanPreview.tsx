import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import capabilityCatalog from "../../../../shared/video-capabilities/src/main/resources/video-capabilities/catalog.json";

export type ClipPlan = {
  capabilityRevision: string;
  finalSeconds: number;
  generatedSeconds: number;
  trimSeconds: number;
  clipsCostUsd: number | null;
  productionAllowed: boolean;
  blockers: string[];
  scenes: {
    order: number;
    modelId: string;
    requestedSeconds: number;
    usedSeconds: number;
    billedSeconds: number;
    maximumCostUsd: number | null;
    narrativeRole: string;
  }[];
};

export function ClipPlanPreview({
  providerName,
  finalSeconds,
  productionCostPlanJson,
  providerPlan,
}: {
  providerName: string;
  finalSeconds: number;
  productionCostPlanJson?: string | null;
  providerPlan?: string;
}) {
  const [resolution, setResolution] = useState("720p");
  const [audio, setAudio] = useState(false);
  const capabilities = capabilityCatalog.models.find(
    (model) => model.providerName === providerName,
  );
  const resolutions = capabilities?.resolutions ?? [];
  useEffect(() => {
    if (capabilities && !capabilities.resolutions.includes(resolution)) {
      setResolution(capabilities.resolutions[0]);
    }
  }, [providerName, resolution]);
  const preview = useMutation({
    mutationFn: async () =>
      (
        await axios.post<ClipPlan>("/api/sales-videos/studio/clip-plan", {
          providerName,
          finalSeconds,
          resolution,
          audio,
          imageReferences: 0,
          referenceVideoSeconds: 0,
        })
      ).data,
  });
  const key = `${providerName}:${finalSeconds}:${resolution}:${audio}`;
  const [previewKey, setPreviewKey] = useState("");
  const plan = previewKey === key ? preview.data : undefined;
  const costLabels: Record<string, string> = {
    AGENTS: "Agentes e revisões",
    REFERENCES: "Referências e imagens",
    VOICE: "Voz",
    MUSIC: "Trilha",
    POST_PRODUCTION: "Montagem e legendas",
    FEES: "Taxas",
    RETRIES: "Reserva para tentativas",
  };
  let remaining: Record<string, { maximumUsd?: number; evidence?: string }> =
    {};
  let costsValid = false;
  try {
    const stored = JSON.parse(productionCostPlanJson || "{}");
    remaining = stored.remainingCosts ?? {};
    costsValid =
      Boolean(providerPlan) &&
      stored.providerPlan === providerPlan &&
      stored.capabilityRevision === plan?.capabilityRevision &&
      stored.finalSeconds === finalSeconds &&
      Date.parse(stored.expiresAt) > Date.now() &&
      Array.isArray(stored.unresolvedAttemptIds) &&
      stored.unresolvedAttemptIds.length === 0 &&
      Object.keys(costLabels).every(
        (key) =>
          typeof remaining[key]?.maximumUsd === "number" &&
          remaining[key].maximumUsd! >= 0 &&
          Boolean(remaining[key].evidence),
      );
  } catch {
    /* Ausência de contrato íntegro mantém a previsão incompleta. */
  }
  const fullTotal =
    plan?.clipsCostUsd != null && costsValid
      ? Number(plan.clipsCostUsd) +
        Object.keys(costLabels).reduce(
          (sum, key) => sum + Number(remaining[key].maximumUsd),
          0,
        )
      : null;
  return (
    <article
      className="audio-video-studio-page__project-card"
      aria-label="Previsão de tomadas e custos"
    >
      <h3>Confira as tomadas e o custo antes de gerar</h3>
      <p>
        A duração final é diferente dos segundos cobrados por tomada. Esta
        consulta não gera vídeo nem reserva saldo.
      </p>
      <label>
        Resolução{" "}
        <select
          value={resolution}
          onChange={(e) => setResolution(e.target.value)}
        >
          {resolutions.map((r) => (
            <option key={r}>{r}</option>
          ))}
        </select>
      </label>{" "}
      <label>
        <input
          type="checkbox"
          checked={audio}
          onChange={(e) => setAudio(e.target.checked)}
        />{" "}
        Som gerado pelo próprio modelo
      </label>{" "}
      <button
        type="button"
        disabled={!finalSeconds || finalSeconds < 1 || preview.isPending}
        onClick={() => {
          setPreviewKey(key);
          preview.mutate();
        }}
      >
        Prever tomadas sem gerar
      </button>
      {preview.isError ? (
        <p role="alert">
          {axios.isAxiosError(preview.error)
            ? preview.error.response?.data?.message ||
              "Esta rota ainda não possui contrato de tomadas homologado. Consulte o catálogo."
            : "Não foi possível consultar o plano."}
        </p>
      ) : null}
      {plan ? (
        <>
          <p>
            <strong>Vídeo final: {plan.finalSeconds}s.</strong> Geração cobrada:{" "}
            {plan.generatedSeconds}s. Excedente a cortar na montagem:{" "}
            {plan.trimSeconds}s.
          </p>
          <div style={{ overflowX: "auto" }}>
            <table>
              <thead>
                <tr>
                  <th>Tomada</th>
                  <th>Modelo</th>
                  <th>Pedida / usada / cobrada</th>
                  <th>Custo previsto</th>
                  <th>Função narrativa</th>
                </tr>
              </thead>
              <tbody>
                {plan.scenes.map((scene) => (
                  <tr key={scene.order}>
                    <td>{scene.order}</td>
                    <td>{scene.modelId}</td>
                    <td>
                      {scene.requestedSeconds}s / {scene.usedSeconds}s /{" "}
                      {scene.billedSeconds}s
                    </td>
                    <td>
                      {scene.maximumCostUsd == null
                        ? "Desconhecido"
                        : `US$ ${Number(scene.maximumCostUsd).toFixed(2)}`}
                    </td>
                    <td>{scene.narrativeRole}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p>
            <strong>
              Clipes:{" "}
              {plan.clipsCostUsd == null
                ? "custo desconhecido"
                : `US$ ${Number(plan.clipsCostUsd).toFixed(2)}`}
              .
            </strong>{" "}
            Previsão parcial, ainda sem autorização de produção.
          </p>
          <p>Demais custos restantes da produção:</p>
          <ul>
            {Object.entries(costLabels).map(([key, label]) => (
              <li key={key}>
                {label}:{" "}
                {remaining[key]?.maximumUsd == null
                  ? "desconhecido — Apolo/Plutus devem obter fonte"
                  : `US$ ${Number(remaining[key].maximumUsd).toFixed(2)}`}
                .
              </li>
            ))}
          </ul>
          <p>
            <strong>
              Previsão integral restante:{" "}
              {fullTotal == null ? "incompleta" : `US$ ${fullTotal.toFixed(2)}`}
              .
            </strong>{" "}
            Custos já incorridos e outras reservas continuam no histórico
            financeiro do ciclo. O teto aprovado é um limite; não é gasto.
          </p>
          <ul>
            {plan.blockers.map((blocker) => (
              <li key={blocker}>{blocker}</li>
            ))}
          </ul>
          <details>
            <summary>Diagnóstico do catálogo</summary>
            <small>Contrato {plan.capabilityRevision}</small>
          </details>
        </>
      ) : null}
    </article>
  );
}
