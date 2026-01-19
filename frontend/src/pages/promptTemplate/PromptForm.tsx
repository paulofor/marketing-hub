import { FormEvent, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useValidatePromptTemplate } from "../../api/promptTemplate/useValidatePromptTemplate";
import { PromptTemplateValidationResponse } from "../../api/promptTemplate/types";
import type { PromptDomain } from "../../api/promptDomain/types";

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
  domains: PromptDomain[];
  isLoadingDomains?: boolean;
  autoSelectDomain?: boolean;
}

export default function PromptForm({
  initialValues,
  isSubmitting,
  onSubmit,
  defaultTemplates,
  domains,
  isLoadingDomains,
  autoSelectDomain = true,
}: PromptFormProps) {
  const initialDomain = initialValues?.domain ?? (autoSelectDomain ? domains[0]?.code ?? "" : "");
  const [name, setName] = useState(initialValues?.name ?? "");
  const [domain, setDomain] = useState(initialDomain);
  const [template, setTemplate] = useState(initialValues?.template ?? defaultTemplates?.[initialDomain] ?? "");
  const [active, setActive] = useState(Boolean(initialValues?.active));
  const [validationResult, setValidationResult] = useState<PromptTemplateValidationResponse | null>(null);
  const validatePrompt = useValidatePromptTemplate();

  useEffect(() => {
    setName(initialValues?.name ?? "");
  }, [initialValues?.name]);

  useEffect(() => {
    if (initialValues?.domain) {
      setDomain(initialValues.domain);
    } else if (autoSelectDomain && !domain && domains[0]) {
      setDomain(domains[0].code);
    }
  }, [initialValues?.domain, domains, domain, autoSelectDomain]);

  useEffect(() => {
    if (initialValues?.template !== undefined) {
      setTemplate(initialValues.template);
    } else if (defaultTemplates && domain && defaultTemplates[domain]) {
      setTemplate(defaultTemplates[domain]);
    }
  }, [initialValues?.template, domain, defaultTemplates]);

  useEffect(() => {
    setActive(Boolean(initialValues?.active));
  }, [initialValues?.active]);

  useEffect(() => {
    setValidationResult(null);
  }, [template, domain]);

  const displayedDomains = useMemo(() => {
    if (!domain) return domains;
    if (domains.some((item) => item.code === domain)) {
      return domains;
    }
    if (!domain) return domains;
    return [
      ...domains,
      {
        id: -1,
        code: domain,
        name: domain,
        description: "Domínio não encontrado (desativado)",
        objects: [],
        availableVariables: [],
      },
    ];
  }, [domains, domain]);

  const selectedDomain = displayedDomains.find((item) => item.code === domain);
  const variables = useMemo(() => selectedDomain?.availableVariables ?? [], [selectedDomain]);
  const objects = selectedDomain?.objects ?? [];
  const isBusy = Boolean(isSubmitting || validatePrompt.isPending);
  const hasDomain = Boolean(domain);

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

  const runValidation = async () => {
    if (!domain) {
      toast.error("Selecione um domínio antes de validar");
      return null;
    }
    try {
      const result = await validatePrompt.mutateAsync({ domain, template });
      setValidationResult(result);
      if (result.valid) {
        toast.success("Template validado com sucesso");
      }
      return result;
    } catch (error) {
      console.error(error);
      setValidationResult({
        valid: false,
        message: "Não foi possível validar o template no momento.",
        missingVariables: [],
        availableVariables: [],
      });
      toast.error("Falha ao validar o template");
      return null;
    }
  };

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const validation = await runValidation();
    if (!validation?.valid) {
      return;
    }
    await onSubmit({
      name: name.trim(),
      domain,
      template,
      active,
    });
  }

  if (isLoadingDomains) {
    return <p>Carregando domínios...</p>;
  }

  if (displayedDomains.length === 0) {
    return <p>Nenhum domínio disponível.</p>;
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="card-body d-flex flex-column gap-3">
        <div className="row g-3">
          <div className="col-md-6">
            <label htmlFor="prompt-name" className="form-label">
              Nome *
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
              Domínio / uso *
            </label>
            <select
              id="prompt-domain"
              className="form-select"
              value={domain}
              onChange={(e) => handleDomainChange(e.target.value)}
              required
              disabled={isSubmitting}
            >
              {!hasDomain ? (
                <option value="" disabled>
                  Selecione um domínio
                </option>
              ) : null}
              {displayedDomains.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
            </select>
            <div className="form-text">
              {objects.length > 0 ? (
                <span className="d-inline-flex flex-wrap gap-2">
                  Objetos habilitados:
                  {objects.map((object) => (
                    <span key={object.slug} className="badge text-bg-light">
                      {object.label}
                    </span>
                  ))}
                </span>
              ) : (
                <span>Este domínio não possui objetos configurados.</span>
              )}
            </div>
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
              disabled={isSubmitting || !hasDomain}
            />
            <label className="form-check-label" htmlFor="prompt-active">
              Tornar este prompt o ativo para o domínio selecionado
            </label>
          </div>
        </div>

        <div>
          <label htmlFor="prompt-template" className="form-label">
            Template (FreeMarker) *
          </label>
          <textarea
            id="prompt-template"
            className="form-control"
            style={{ fontFamily: "monospace" }}
            rows={14}
            value={template}
            onChange={(e) => setTemplate(e.target.value)}
            required
            disabled={isSubmitting || !hasDomain}
          />
          <div className="form-text">
            Use variáveis no formato <code>${"{variavel}"}</code> ou estruturas do FreeMarker para
            montar o texto dinamicamente.
          </div>
        </div>

        <div className="alert alert-light" role="note">
          <p className="mb-2 fw-semibold">Variáveis disponíveis para este domínio</p>
          {!hasDomain ? (
            <p className="mb-0">Selecione um domínio para liberar as variáveis e editar o template.</p>
          ) : variables.length === 0 ? (
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

        {validationResult ? (
          <div className={`alert ${validationResult.valid ? "alert-success" : "alert-danger"}`} role="status">
            <p className="mb-1 fw-semibold">{validationResult.message}</p>
            {!validationResult.valid && validationResult.missingVariables.length > 0 ? (
              <div>
                <p className="mb-1">Variáveis ausentes ou inválidas:</p>
                <ul className="mb-0">
                  {validationResult.missingVariables.map((variable) => (
                    <li key={variable}>
                      <code>{variable}</code>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </div>
        ) : null}

        {validationResult?.valid && validationResult.renderedPrompt ? (
          <div className="card border-0 bg-light">
            <div className="card-body">
              <h6 className="fw-semibold">Exemplo gerado</h6>
              <p className="text-muted">
                Texto renderizado com um registro real de cada objeto disponível para este domínio.
              </p>
              <pre className="mb-0" style={{ whiteSpace: "pre-wrap" }}>
                {validationResult.renderedPrompt}
              </pre>
            </div>
          </div>
        ) : null}

        <div className="d-flex justify-content-end gap-2">
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={runValidation}
            disabled={isBusy || !hasDomain}
          >
            {validatePrompt.isPending ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
            ) : null}
            <span className="ms-2">Validar sintaxe</span>
          </button>
          <button type="submit" className="btn btn-primary" disabled={isBusy || !hasDomain}>
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
