import { useState } from "react";
import { useCreateHypothesis } from "../../api/hypothesis/useCreateHypothesis";
import { useAngles } from "../../api/angle/useAngles";

export default function NewHypothesisModal({
  experimentId,
}: {
  experimentId: string;
}) {
  const [title, setTitle] = useState("");
  const [angle, setAngle] = useState("");
  const [offer, setOffer] = useState("LEAD");
  const [kpi, setKpi] = useState("0");
  const [status, setStatus] = useState("BACKLOG");
  const [submitted, setSubmitted] = useState(false);
  const create = useCreateHypothesis();
  const { data: angles } = useAngles();

  const submit = async () => {
    setSubmitted(true);
    if (!title.trim()) return;
    await create.mutateAsync({
      experimentId: Number(experimentId),
      title,
      premiseAngleId: angle ? Number(angle) : undefined,
      offerType: offer,
      kpiTargetCpl: Number(kpi),
    });
    setTitle("");
    setSubmitted(false);
  };

  return (
    <div>
      <input
        className={`form-control mb-2 ${submitted && !title.trim() ? "is-invalid" : ""}`}
        placeholder="Título"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      {submitted && !title.trim() && (
        <div className="invalid-feedback d-block">Título obrigatório</div>
      )}
      <select
        className="form-select mb-2"
        value={angle}
        onChange={(e) => setAngle(e.target.value)}
      >
        <option value="">Selecione Angle</option>
        {Array.isArray(angles) &&
          angles.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
      </select>
      <select
        className="form-select mb-2"
        value={status}
        onChange={(e) => setStatus(e.target.value)}
      >
        <option value="BACKLOG">Backlog</option>
        <option value="TESTING">Em Teste</option>
        <option value="VALIDATED">Validada</option>
        <option value="INVALIDATED">Invalidada</option>
      </select>
      <div className="form-check">
        <input
          className="form-check-input"
          type="radio"
          value="LEAD"
          checked={offer === "LEAD"}
          onChange={(e) => setOffer(e.target.value)}
          id="offerLead"
        />
        <label className="form-check-label" htmlFor="offerLead">
          Lead Magnet
        </label>
      </div>
      <div className="form-check mb-2">
        <input
          className="form-check-input"
          type="radio"
          value="TRIPWIRE"
          checked={offer === "TRIPWIRE"}
          onChange={(e) => setOffer(e.target.value)}
          id="offerTrip"
        />
        <label className="form-check-label" htmlFor="offerTrip">
          Tripwire
        </label>
      </div>
      {offer === "TRIPWIRE" && (
        <input
          className="form-control mb-2"
          placeholder="Preço"
          type="number"
          min="5"
        />
      )}
      <input
        className="form-control mb-2"
        placeholder="KPI CPL"
        value={kpi}
        type="number"
        onChange={(e) => setKpi(e.target.value)}
      />
      <button
        className="btn btn-primary"
        onClick={submit}
        disabled={!title.trim()}
      >
        Criar
      </button>
    </div>
  );
}
