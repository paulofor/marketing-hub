import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  cycleApi,
  cycleError,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";

type Alignment = {
  available: boolean;
  reason: string;
  cycleRevision: number;
  bigIdeaRevision: number;
  currentOffer: string;
  proposedOffer: string | null;
  proposedPriceBrl: number | null;
};

/** Apresenta a reconciliação autorizada pelo backend, sem redefinir oferta ou orçamento na tela. */
export default function CyclePlanningAlignment({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const client = useQueryClient();
  const [requestKey] = useState(createCycleRequestKey);
  const endpoint = `${cycleApi}/products/${cycle.productId}/${cycle.id}/planning-alignment`;
  const preview = useQuery({
    queryKey: ["cycle-planning-alignment", cycle.id, cycle.revision],
    enabled: cycle.status === "OPEN" && cycle.stage === "PLANNING",
    queryFn: async () => (await axios.get<Alignment>(endpoint)).data,
  });
  const mutation = useMutation({
    mutationFn: async () =>
      (
        await axios.post<LearningCycle>(endpoint, {
          requestKey,
          expectedRevision: preview.data!.cycleRevision,
          expectedBigIdeaRevision: preview.data!.bigIdeaRevision,
          operatorName: "Operador administrativo · alinhamento pela tela",
          reason:
            "Alinhar a entrada privada à definição e direção já registradas do produto, preservando histórico, custos e tetos.",
        })
      ).data,
    onSuccess: async (result) => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["learning-cycles"] }),
        client.invalidateQueries({ queryKey: ["cycle-planning-alignment"] }),
        client.invalidateQueries({
          queryKey: ["products", cycle.productId, "business-processes"],
        }),
      ]);
      onUpdated(result);
    },
  });
  if (cycle.status !== "OPEN" || cycle.stage !== "PLANNING") return null;
  if (preview.error) return <p role="alert">{cycleError(preview.error)}</p>;
  if (!preview.data?.available) return null;
  return (
    <section
      className="card card-body mb-3"
      aria-label="Alinhar oferta do planejamento"
    >
      <h3 className="h5">Conferir oferta antes de retomar Atena</h3>
      <p>{preview.data.reason}</p>
      <details>
        <summary>Oferta recebida neste ciclo</summary>
        <p>{preview.data.currentOffer}</p>
      </details>
      <p>
        <strong>
          Definição registrada · Big Idea v{preview.data.bigIdeaRevision}:
        </strong>{" "}
        {preview.data.proposedOffer}
      </p>
      <p>
        Custos, teto acumulado e pareceres anteriores ficam preservados. Depois
        do alinhamento, retome o Processo 2 para Atena analisar a entrada
        corrigida e encaminhar a economia a Plutus.
      </p>
      <button
        type="button"
        className="btn btn-outline-primary"
        disabled={mutation.isPending}
        onClick={() => mutation.mutate()}
      >
        {mutation.isPending ? (
          <>
            <span
              className="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            />
            Alinhando…
          </>
        ) : (
          "Alinhar à definição registrada"
        )}
      </button>
      {mutation.error ? (
        <p role="alert" className="text-danger mt-2">
          {cycleError(mutation.error)}
        </p>
      ) : null}
    </section>
  );
}
