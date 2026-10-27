import { useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useState } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import {
  cycleError,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

interface Plan {
  revision: number;
  architectureTaskId: number;
  architectureSha256: string;
  boundProfileCode: string | null;
  installedProfiles: string[];
}

/** Vincula a capacidade de teste instalada ao desenho aprovado; não registra teste ou aceite comercial. */
export default function CycleKitImplementationBinding({
  cycle,
}: {
  cycle: LearningCycle;
}) {
  const client = useQueryClient();
  const [profileCode, setProfileCode] = useState("beauty-v1");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const root = "/api/pde/kit/private/v1/admin/implementations";
  const plan = useQuery({
    queryKey: [
      "kit-implementation-plan",
      cycle.productId,
      cycle.id,
      cycle.revision,
    ],
    queryFn: async () =>
      (
        await axios.get<Plan>(root, {
          params: { productId: cycle.productId, cycleId: cycle.id },
          timeout: 15000,
        })
      ).data,
    retry: false,
  });
  async function bind() {
    if (!plan.data) return;
    setBusy(true);
    setError("");
    try {
      await axios.post(
        root,
        {
          requestKey: createCycleRequestKey(),
          expectedRevision: plan.data.revision,
          productId: cycle.productId,
          cycleId: cycle.id,
          architectureTaskId: plan.data.architectureTaskId,
          architectureSha256: plan.data.architectureSha256,
          profileCode,
          operatorName: "Operador do Marketing Hub",
        },
        { timeout: 30000 },
      );
      await client.invalidateQueries({ queryKey: ["learning-cycles"] });
      await plan.refetch();
    } catch (ex) {
      setError(cycleError(ex));
    } finally {
      setBusy(false);
    }
  }
  if (
    !plan.data ||
    plan.data.boundProfileCode ||
    !plan.data.installedProfiles.includes("beauty-v1")
  )
    return null;
  return (
    <div className="mt-2">
      <p>
        O desenho está pronto. A capacidade instalada permite preparar e testar
        os kits de cabelo, barbearia, maquiagem, sobrancelhas/cílios, estética
        não clínica e unhas.
      </p>
      <label className="form-label d-block">
        Material da demonstração *
        <select
          className="form-select"
          value={profileCode}
          onChange={(event) => setProfileCode(event.target.value)}
          disabled={busy}
        >
          <option value="beauty-v1">
            Ilustrações de teste — versão anterior
          </option>
          {plan.data.installedProfiles.includes("beauty-v2") && (
            <option value="beauty-v2">
              Referências fotográficas licenciadas — nova versão
            </option>
          )}
        </select>
      </label>
      <button
        className="btn btn-primary"
        type="button"
        disabled={busy}
        onClick={() => void bind()}
      >
        {busy && (
          <span
            className="spinner-border spinner-border-sm me-2"
            aria-hidden="true"
          />
        )}
        {busy ? "Preparando…" : "Preparar implementação por especialidade"}
      </button>
      <p className="small">
        Vincula o compositor ao desenho desta versão. Os arquivos ainda precisam
        ser produzidos e testados; fotografias de referência e ilustrações de
        teste não aprovam a qualidade comercial. Sem chamada paga, compra ou
        anúncio.
      </p>
      {error && (
        <p role="alert" className="alert alert-danger">
          {error}
        </p>
      )}
    </div>
  );
}
