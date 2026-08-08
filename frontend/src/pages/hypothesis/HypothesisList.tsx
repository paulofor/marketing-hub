import { useState } from "react";
import { Link } from "react-router-dom";
import { useAngles } from "../../api/angle/useAngles";
import { useHypotheses } from "../../api/hypothesis/useHypotheses";
import { useNiches } from "../../api/niche/useNiches";
import { useUpdateHypothesisStatus } from "../../api/hypothesis/useUpdateHypothesisStatus";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";

const statuses = [
  "ALL",
  "BACKLOG",
  "TESTING",
  "VALIDATED",
  "INVALIDATED",
] as const;

export default function HypothesisList() {
  const [status, setStatus] = useState<string>("ALL");
  const [page, setPage] = useState(0);
  const { data, isLoading } = useHypotheses(status, page);
  const update = useUpdateHypothesisStatus();
  const { data: angles } = useAngles();
  const { data: niches } = useNiches();
  const angleMap = new Map<number, string>(
    Array.isArray(angles) ? angles.map((a) => [a.id, a.name]) : [],
  );
  const list = data?.items ?? [];

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
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          {statuses.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Título</th>
              <th>Ângulo</th>
              <th>Nicho</th>
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
                <td>{niches?.find((n) => n.id === h.marketNicheId)?.name}</td>
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
                  <Link
                    className="btn btn-sm btn-outline-dark me-1"
                    to={`/niches/${h.marketNicheId}/hypotheses/${h.id}`}
                  >
                    Abrir
                  </Link>
                  <button className="btn btn-sm btn-outline-primary me-1">
                    Gerar Landing
                  </button>
                  <button className="btn btn-sm btn-outline-secondary me-1">
                    Criar Criativo
                  </button>
                  <button className="btn btn-sm btn-outline-danger">
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="d-flex justify-content-between align-items-center mt-3">
        <span className="text-muted small">
          {data?.totalElements ?? 0} hipóteses · página {page + 1} de{" "}
          {Math.max(data?.totalPages ?? 1, 1)}
        </span>
        <div className="btn-group" aria-label="Paginação de hipóteses">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Anterior
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={page + 1 >= (data?.totalPages ?? 0)}
            onClick={() => setPage((current) => current + 1)}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}
