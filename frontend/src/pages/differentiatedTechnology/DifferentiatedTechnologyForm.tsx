import { FormEvent, useEffect, useState } from "react";

export type DifferentiatedTechnologyFormState = {
  name: string;
  description?: string;
  promptText?: string;
};

interface Props {
  initialValues: DifferentiatedTechnologyFormState;
  onSubmit: (values: DifferentiatedTechnologyFormState) => void;
  isSubmitting?: boolean;
  submitLabel?: string;
}

export default function DifferentiatedTechnologyForm({
  initialValues,
  onSubmit,
  isSubmitting,
  submitLabel = "Salvar",
}: Props) {
  const [form, setForm] = useState<DifferentiatedTechnologyFormState>(initialValues);

  useEffect(() => {
    setForm(initialValues);
  }, [initialValues]);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit(form);
  };

  return (
    <form className="row g-3" onSubmit={handleSubmit}>
      <div className="col-12 col-md-6">
        <label className="form-label">Nome</label>
        <input
          className="form-control"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          required
        />
      </div>
      <div className="col-12">
        <label className="form-label">Descrição</label>
        <textarea
          className="form-control"
          value={form.description || ""}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          rows={3}
        />
      </div>
      <div className="col-12">
        <label className="form-label">Texto para prompt</label>
        <textarea
          className="form-control"
          value={form.promptText || ""}
          onChange={(e) => setForm({ ...form, promptText: e.target.value })}
          rows={6}
          placeholder="Conteúdo pronto para ser usado em prompts"
        />
      </div>
      <div className="col-12 d-flex gap-2">
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting || !form.name}
        >
          {isSubmitting ? "Salvando..." : submitLabel}
        </button>
      </div>
    </form>
  );
}
