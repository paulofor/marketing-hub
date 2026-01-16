import { FormEvent, useEffect, useMemo, useState } from "react";
import type { PromptDomainObject } from "../../api/promptDomain/types";

export interface PromptDomainFormValues {
  code?: string;
  name: string;
  description?: string;
  objects: string[];
}

interface PromptDomainFormProps {
  initialValues?: PromptDomainFormValues;
  objects?: PromptDomainObject[];
  isLoadingObjects?: boolean;
  isSubmitting?: boolean;
  disableCode?: boolean;
  onSubmit: (values: PromptDomainFormValues) => Promise<void> | void;
}

export default function PromptDomainForm({
  initialValues,
  objects = [],
  isLoadingObjects,
  isSubmitting,
  disableCode,
  onSubmit,
}: PromptDomainFormProps) {
  const [code, setCode] = useState(initialValues?.code ?? "");
  const [name, setName] = useState(initialValues?.name ?? "");
  const [description, setDescription] = useState(initialValues?.description ?? "");
  const [selectedObjects, setSelectedObjects] = useState<string[]>(initialValues?.objects ?? []);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setCode(initialValues?.code ?? "");
    setName(initialValues?.name ?? "");
    setDescription(initialValues?.description ?? "");
    setSelectedObjects(initialValues?.objects ?? []);
  }, [initialValues?.code, initialValues?.name, initialValues?.description, initialValues?.objects]);

  const mergedObjects = useMemo(() => {
    const map = new Map(objects.map((item) => [item.slug, item]));
    selectedObjects.forEach((slug) => {
      if (!map.has(slug)) {
        map.set(slug, {
          type: slug.toUpperCase(),
          slug,
          label: slug,
          contextKey: slug,
        });
      }
    });
    return Array.from(map.values());
  }, [objects, selectedObjects]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (selectedObjects.length === 0) {
      setError("Selecione ao menos um objeto para o domínio.");
      return;
    }
    setError(null);
    await onSubmit({
      code: code?.trim(),
      name: name.trim(),
      description: description?.trim(),
      objects: selectedObjects,
    });
  };

  const toggleObject = (slug: string) => {
    setSelectedObjects((prev) =>
      prev.includes(slug) ? prev.filter((item) => item !== slug) : [...prev, slug],
    );
  };

  const normalizeCode = (value: string) =>
    value
      .replace(/[^\w-]/g, "")
      .replace(/-/g, "_")
      .toUpperCase();

  const isDisabled = Boolean(isSubmitting);

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <div className="card-body d-flex flex-column gap-3">
        <div className="row g-3">
          <div className="col-md-6">
            <label htmlFor="prompt-domain-code" className="form-label">
              Código *
            </label>
            <input
              id="prompt-domain-code"
              className="form-control"
              type="text"
              placeholder="EX.: NICHE_HYPOTHESIS"
              value={code}
              onChange={(event) => setCode(normalizeCode(event.target.value))}
              disabled={disableCode || isDisabled}
              required
            />
            <div className="form-text">Use letras maiúsculas, números ou _.</div>
          </div>
          <div className="col-md-6">
            <label htmlFor="prompt-domain-name" className="form-label">
              Nome *
            </label>
            <input
              id="prompt-domain-name"
              className="form-control"
              type="text"
              placeholder="Ex.: Geração de hipóteses"
              value={name}
              onChange={(event) => setName(event.target.value)}
              disabled={isDisabled}
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="prompt-domain-description" className="form-label">
            Descrição
          </label>
          <textarea
            id="prompt-domain-description"
            className="form-control"
            rows={3}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            disabled={isDisabled}
          />
        </div>

        <div>
          <p className="form-label mb-1">Objetos disponíveis *</p>
          {isLoadingObjects ? (
            <p className="text-body-secondary">Carregando objetos...</p>
          ) : mergedObjects.length === 0 ? (
            <p className="text-body-secondary">Nenhum objeto disponível.</p>
          ) : (
            <div className="d-flex flex-wrap gap-3">
              {mergedObjects.map((object) => (
                <label key={object.slug} className="form-check">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    checked={selectedObjects.includes(object.slug)}
                    onChange={() => toggleObject(object.slug)}
                    disabled={isDisabled}
                  />
                  <span className="ms-2">{object.label}</span>
                </label>
              ))}
            </div>
          )}
          {error ? <p className="text-danger small mt-2">{error}</p> : null}
        </div>

        <div className="d-flex justify-content-end">
          <button type="submit" className="btn btn-primary" disabled={isDisabled}>
            {isSubmitting ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : null}
            <span className="ms-2">Salvar domínio</span>
          </button>
        </div>
      </div>
    </form>
  );
}
