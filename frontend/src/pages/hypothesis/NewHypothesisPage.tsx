import { useState } from "react";
import { useParams } from "react-router-dom";
import { useAngles } from "../../api/angle/useAngles";
import { useCreateHypothesis } from "../../api/hypothesis/useCreateHypothesis";
import PageTitle from "../../components/PageTitle";

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const create = useCreateHypothesis();
  const { data: angles } = useAngles();
  const [form, setForm] = useState({
    title: "",
    angleId: "",
    offerType: "LEAD",
    price: "",
    kpiTargetCpl: "",
  });

  const submit = async () => {
    if (!nicheId) return;
    await create.mutateAsync({
      marketNicheId: Number(nicheId),
      title: form.title,
      premiseAngleId: form.angleId ? Number(form.angleId) : undefined,
      offerType: form.offerType,
      kpiTargetCpl: Number(form.kpiTargetCpl),
      ...(form.offerType === "TRIPWIRE" && form.price
        ? { price: Number(form.price) }
        : {}),
    });
    setForm({ title: "", angleId: "", offerType: "LEAD", price: "", kpiTargetCpl: "" });
  };

  return (
    <div>
      <PageTitle>Nova Hipótese</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Título"
        value={form.title}
        onChange={(e) => setForm({ ...form, title: e.target.value })}
      />
      <select
        className="form-select mb-2"
        value={form.angleId}
        onChange={(e) => setForm({ ...form, angleId: e.target.value })}
      >
        <option value="">Selecione Ângulo</option>
        {Array.isArray(angles) &&
          angles.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
      </select>
      <div className="mb-2">
        <div className="form-check">
          <input
            className="form-check-input"
            type="radio"
            id="offer-lead"
            value="LEAD"
            checked={form.offerType === "LEAD"}
            onChange={(e) => setForm({ ...form, offerType: e.target.value })}
          />
          <label className="form-check-label" htmlFor="offer-lead">
            Lead Magnet
          </label>
        </div>
        <div className="form-check">
          <input
            className="form-check-input"
            type="radio"
            id="offer-trip"
            value="TRIPWIRE"
            checked={form.offerType === "TRIPWIRE"}
            onChange={(e) => setForm({ ...form, offerType: e.target.value })}
          />
          <label className="form-check-label" htmlFor="offer-trip">
            Tripwire
          </label>
        </div>
      </div>
      {form.offerType === "TRIPWIRE" && (
        <input
          type="number"
          className="form-control mb-2"
          placeholder="Preço"
          value={form.price}
          onChange={(e) => setForm({ ...form, price: e.target.value })}
        />
      )}
      <input
        type="number"
        step="0.01"
        className="form-control mb-2"
        placeholder="KPI alvo (CPL em R$)"
        value={form.kpiTargetCpl}
        onChange={(e) => setForm({ ...form, kpiTargetCpl: e.target.value })}
      />
      <button className="btn btn-primary" onClick={submit} disabled={create.isPending}>
        {create.isPending ? (
          <>
            <span className="spinner-border spinner-border-sm me-2" role="status" />
            Processando...
          </>
        ) : (
          "Salvar"
        )}
      </button>
    </div>
  );
}
