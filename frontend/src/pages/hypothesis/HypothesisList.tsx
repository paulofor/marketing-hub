import { useState } from "react";
import { useAngles } from "../../api/angle/useAngles";
import { useHypotheses } from "../../api/hypothesis/useHypotheses";
import { useUpdateHypothesisStatus } from "../../api/hypothesis/useUpdateHypothesisStatus";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";
import NewHypothesisModal from "./NewHypothesisModal";
import Button from "../../components/ui/Button";

const statuses = ["ALL", "BACKLOG", "TESTING", "VALIDATED", "INVALIDATED"] as const;

export default function HypothesisList() {
  const [status, setStatus] = useState<string>("ALL");
  const [open, setOpen] = useState(false);
  const { data, isLoading } = useHypotheses(status);
  const update = useUpdateHypothesisStatus();
  const { data: angles } = useAngles();
  const angleMap = new Map<number, string>(
    Array.isArray(angles) ? angles.map((a) => [a.id, a.name]) : [],
  );
  const list = Array.isArray(data) ? data : [];

  const changeStatus = async (h: Hypothesis, s: string) => {
    if (s === h.status) return;
    await update.mutateAsync({ id: h.id, status: s });
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <div className="mb-3">
        <select
          className="form-select w-auto"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          {statuses.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div className="flex justify-end mb-3">
        <Button onClick={() => setOpen(true)}>Nova Hipótese</Button>
      </div>
      <NewHypothesisModal open={open} onOpenChange={setOpen} />
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Título</th>
              <th>Ângulo</th>
              <th>Oferta</th>
              <th>CPL</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {list.map((h) => (
              <tr key={h.id}>
                <td>{h.title}</td>
                <td>{angleMap.get(h.premiseAngleId ?? 0)}</td>
                <td>
                  {h.offerType === "TRIPWIRE"
                    ? `Tripwire R$ ${h.price ?? ""}`
                    : "Lead Magnet"}
                </td>
                <td>{h.kpiTargetCpl}</td>
                <td>
                  <select
                    className="form-select form-select-sm"
                    value={h.status}
                    onChange={(e) => changeStatus(h, e.target.value)}
                  >
                    {statuses
                      .filter((s) => s !== "ALL")
                      .map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                  </select>
                </td>
                <td>
                  <button className="btn btn-sm btn-outline-primary me-1">
                    Gerar Landing
                  </button>
                  <button className="btn btn-sm btn-outline-secondary me-1">
                    Criar Criativo
                  </button>
                  <button className="btn btn-sm btn-outline-danger">Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
