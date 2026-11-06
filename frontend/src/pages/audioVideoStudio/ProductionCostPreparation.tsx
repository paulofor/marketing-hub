import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { VideoProject } from "../../api/salesVideo/types";
import {
  type VideoProductionCycle,
  videoProductionError,
} from "../../api/salesVideo/useVideoProductionCycles";
import catalog from "../../../../shared/video-capabilities/src/main/resources/video-capabilities/catalog.json";

const labels = {
  AGENTS: "Agentes e revisões",
  REFERENCES: "Imagens e referências",
  VOICE: "Voz",
  MUSIC: "Trilha",
  POST_PRODUCTION: "Montagem e legendas",
  FEES: "Taxas",
  RETRIES: "Tentativas",
};
type Item = { maximumUsd: string; evidence: string };

/** Permite preparar evidências pelo Estúdio e deixa ao servidor a reavaliação do mesmo ciclo. */
export function ProductionCostPreparation({
  project,
  cycle,
}: {
  project: VideoProject;
  cycle?: VideoProductionCycle;
}) {
  const client = useQueryClient();
  const [items, setItems] = useState<Record<string, Item>>({});
  const [unresolved, setUnresolved] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  useEffect(() => {
    let saved: any = {};
    try {
      saved = JSON.parse(project.productionCostPlanJson || "{}");
    } catch {
      /* Ausência permanece explícita. */
    }
    const sameCycle = saved.cycleId === cycle?.id;
    setItems(
      Object.fromEntries(
        Object.keys(labels).map((key) => [
          key,
          {
            maximumUsd:
              sameCycle && saved.remainingCosts?.[key]?.maximumUsd != null
                ? String(saved.remainingCosts[key].maximumUsd)
                : "",
            evidence: sameCycle
              ? saved.remainingCosts?.[key]?.evidence || ""
              : "",
          },
        ]),
      ),
    );
    setUnresolved(
      sameCycle ? (saved.unresolvedAttemptIds || []).join("\n") : "",
    );
    setExpiresAt(sameCycle ? saved.expiresAt?.slice(0, 16) || "" : "");
  }, [project.id, project.productionCostPlanJson, cycle?.id]);
  const save = useMutation({
    mutationFn: async () =>
      (
        await axios.put<VideoProductionCycle>(
          `/api/sales-videos/projects/${project.id}/autonomy/v1/cycles/${cycle!.id}/production-cost-plan`,
          {
            cycleId: cycle!.id,
            productId: project.productId,
            experimentId: project.experimentId,
            providerPlan: project.providerPlan,
            finalSeconds: project.targetDurationSeconds,
            capabilityRevision: catalog.revision,
            expiresAt: new Date(`${expiresAt}:00Z`).toISOString(),
            remainingCosts: Object.fromEntries(
              Object.keys(labels).map((key) => [
                key,
                {
                  maximumUsd: items[key]?.maximumUsd?.trim()
                    ? Number(items[key].maximumUsd)
                    : null,
                  evidence: items[key]?.evidence || "",
                },
              ]),
            ),
            unresolvedAttemptIds: unresolved
              .split("\n")
              .map((s) => s.trim())
              .filter(Boolean),
          },
        )
      ).data,
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ["video-production-cycles", project.id],
      });
      client.invalidateQueries({
        queryKey: ["sales-video-project", project.id],
      });
    },
  });
  if (
    cycle?.status !== "PROVIDER_PREFLIGHT_BLOCKED" ||
    cycle.providerPreflight?.failureCode !== "PROVIDER_FULL_COST_PLAN_REQUIRED"
  )
    return null;
  return (
    <form
      aria-label="Preparação integral de custos"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <h3>Completar a previsão deste ciclo</h3>
      <p>
        Apolo prepara os máximos em US$ e suas fontes; Plutus revisa antes de
        gerar. O teto aprovado não é alterado. Custo desconhecido deve
        permanecer pendente.
      </p>
      {Object.entries(labels).map(([key, label]) => (
        <fieldset key={key}>
          <legend>{label}</legend>
          <label>
            Máximo restante em US$ *
            <input
              required
              type="number"
              min="0"
              step="0.0001"
              value={items[key]?.maximumUsd || ""}
              onChange={(e) =>
                setItems((current) => ({
                  ...current,
                  [key]: { ...current[key], maximumUsd: e.target.value },
                }))
              }
            />
          </label>
          <label>
            Fonte ou evidência *
            <input
              required
              value={items[key]?.evidence || ""}
              onChange={(e) =>
                setItems((current) => ({
                  ...current,
                  [key]: { ...current[key], evidence: e.target.value },
                }))
              }
            />
          </label>
        </fieldset>
      ))}
      <label>
        Validade da previsão (UTC) *
        <input
          required
          type="datetime-local"
          value={expiresAt}
          onChange={(e) => setExpiresAt(e.target.value)}
        />
      </label>
      <label>
        Tentativas com custo ou resultado incerto (uma por linha)
        <textarea
          value={unresolved}
          onChange={(e) => setUnresolved(e.target.value)}
        />
      </label>
      <button type="submit" disabled={save.isPending}>
        {save.isPending
          ? "Registrando e reavaliando…"
          : "Registrar evidências e reavaliar o ciclo"}
      </button>
      {save.isError ? (
        <p role="alert">
          {videoProductionError(
            save.error,
            "Não foi possível reavaliar a previsão.",
          )}
        </p>
      ) : null}
      {save.data ? (
        <p role="status">
          Ciclo #{save.data.id}: {save.data.status}. Teto preservado: US${" "}
          {save.data.budgetLimitUsd.toFixed(2)}.
        </p>
      ) : null}
    </form>
  );
}
