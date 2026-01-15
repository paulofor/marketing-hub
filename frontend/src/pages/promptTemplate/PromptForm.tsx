import { FormEvent, useMemo, useState } from "react";
import { PROMPT_DOMAINS, PROMPT_VARIABLES } from "../../constants/prompts";

export interface PromptFormValues {
  name: string;
  domain: string;
  template: string;
  active: boolean;
}

interface PromptFormProps {
  initialValues?: Partial<PromptFormValues>;
  isSubmitting?: boolean;
  onSubmit: (values: PromptFormValues) => Promise<void> | void;
  defaultTemplates?: Record<string, string>;
}

export default function PromptForm({ initialValues, isSubmitting, onSubmit, defaultTemplates }: PromptFormProps) {
  const initialDomain = initialValues?.domain ?? PROMPT_DOMAINS[0]?.value ?? "";
  const [name, setName] = useState(initialValues?.name ?? "");
  const [domain, setDomain] = useState(initialDomain);
  const [template, setTemplate] = useState(initialValues?.template ?? defaultTemplates?.[initialDomain] ?? "");
  const [active, setActive] = useState(Boolean(initialValues?.active));

  const variables = useMemo(() => PROMPT_VARIABLES[domain] ?? [], [domain]);

  const handleDomainChange = (newDomain: string) => {
    setDomain(newDomain);
    if (!defaultTemplates) return;
    const nextTemplate = defaultTemplates[newDomain];
    const currentDefault = defaultTemplates[domain];
    const shouldReplaceTemplate =
      nextTemplate !== undefined && (template.trim().length === 0 || template === currentDefault);
    if (shouldReplaceTemplate) {
      setTemplate(nextTemplate);
    }
  };

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await onSubmit({
      name: name.trim(),
      domain,
      template,
      active,
    });
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="card-body d-flex flex-column gap-3">
        <div className="row g-3">
          <div className="col-md-6">
            <label htmlFor="prompt-name" className="form-label">
              Nome
            </label>
            <input
              id="prompt-name"
              className="form-control"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              disabled={isSubmitting}
              placeholder="Ex.: Prompt detalhamento v1"
            />
          </div>
          <div className="col-md-6">
            <label htmlFor="prompt-domain" className="form-label">
              Domínio / uso
            </label>
            <select
              id="prompt-domain"
              className="form-select"
              value={domain}
              onChange={(e) => handleDomainChange(e.target.value)}
              required
              disabled={isSubmitting}
            >
              {PROMPT_DOMAINS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <div className="form-text">Escolha onde este prompt será aplicado.</div>
          </div>
        </div>

        <div>
          <div className="form-check">
            <input
              id="prompt-active"
              className="form-check-input"
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              disabled={isSubmitting}
            />
            <label className="form-check-label" htmlFor="prompt-active">
              Tornar este prompt o ativo para o domínio selecionado
            </label>
          </div>
        </div>

        <div>
          <label htmlFor="prompt-template" className="form-label">
            Template (FreeMarker)
          </label>
          <textarea
            id="prompt-template"
            className="form-control"
            style={{ fontFamily: "monospace" }}
            rows={14}
            value={template}
            onChange={(e) => setTemplate(e.target.value)}
            required
            disabled={isSubmitting}
          />
          <div className="form-text">
            Use variáveis no formato <code>${"{variavel}"}</code> ou estruturas do FreeMarker para
            montar o texto dinamicamente.
          </div>
        </div>

        <div className="alert alert-light" role="note">
          <p className="mb-2 fw-semibold">Variáveis disponíveis para este domínio</p>
          {variables.length === 0 ? (
            <p className="mb-0">Nenhuma variável definida.</p>
          ) : (
            <ul className="mb-0">
              {variables.map((variable) => (
                <li key={variable}>
                  <code>{variable}</code>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="d-flex justify-content-end gap-2">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : null}
            <span className="ms-2">Salvar prompt</span>
          </button>
        </div>
      </div>
    </form>
  );
}
