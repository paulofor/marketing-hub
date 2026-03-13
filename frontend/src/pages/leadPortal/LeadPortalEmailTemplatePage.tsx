import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { toast } from "react-toastify";
import {
  type LeadPortalEmailTemplatePlaceholder,
  useLeadPortalEmailTemplate,
  useUpdateLeadPortalEmailTemplate,
} from "../../api/leadPortal/useLeadPortalEmailTemplate";

const FALLBACK_PLACEHOLDERS: LeadPortalEmailTemplatePlaceholder[] = [
  {
    key: "nome_cliente",
    token: "{{nome_cliente}}",
    label: "Nome do cliente",
    description: "Substituído automaticamente pelo nome informado pelo lead.",
  },
  {
    key: "link_pagamento",
    token: "{{link_pagamento}}",
    label: "Link do pagamento",
    description: "Checkout ativo no Mercado Pago para liberar as imagens originais.",
  },
  {
    key: "imagem_previa_1",
    token: "{{imagem_previa_1}}",
    label: "Prévia 1",
    description: "URL da primeira imagem com marca d'água.",
  },
  {
    key: "imagem_previa_2",
    token: "{{imagem_previa_2}}",
    label: "Prévia 2",
    description: "Segunda URL de imagem quando existir.",
  },
  {
    key: "imagem_previa_3",
    token: "{{imagem_previa_3}}",
    label: "Prévia 3",
    description: "Terceira URL de imagem quando o pacote possuir 3 arquivos.",
  },
];

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export default function LeadPortalEmailTemplatePage() {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const { data, isLoading, isError, error } = useLeadPortalEmailTemplate();
  const updateTemplate = useUpdateLeadPortalEmailTemplate();
  const [html, setHtml] = useState<string>("");
  const [isDirty, setIsDirty] = useState(false);

  const placeholders = useMemo(() => {
    if (data && data.placeholders && data.placeholders.length > 0) {
      return data.placeholders;
    }
    return FALLBACK_PLACEHOLDERS;
  }, [data]);

  useEffect(() => {
    if (!isDirty && data) {
      setHtml(data.html ?? "");
    }
  }, [data, isDirty]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const payload = html.trim().length > 0 ? html : null;
      await updateTemplate.mutateAsync(payload);
      toast.success("Template salvo com sucesso.");
      setIsDirty(false);
    } catch (err) {
      console.error("Falha ao salvar template", err);
      toast.error("Não foi possível salvar o template.");
    }
  };

  const handleReset = () => {
    setHtml(data?.html ?? "");
    setIsDirty(false);
  };

  const handleInsertToken = (token: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart ?? html.length;
    const end = textarea.selectionEnd ?? html.length;
    const newValue = html.slice(0, start) + token + html.slice(end);
    setHtml(newValue);
    setIsDirty(true);
    requestAnimationFrame(() => {
      textarea.selectionStart = textarea.selectionEnd = start + token.length;
      textarea.focus();
    });
  };

  if (isLoading) {
    return (
      <div className="container py-4">
        <p>Carregando template do e-mail...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="container py-4">
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar o template. {error instanceof Error ? error.message : null}
        </div>
      </div>
    );
  }

  return (
    <div className="container py-4">
      <div className="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3 mb-4">
        <div>
          <h1 className="h3 mb-1">Template do e-mail enviado ao lead</h1>
          <p className="text-muted mb-0">
            Personalize o HTML que acompanha as imagens com marca d'água e inclua o link de pagamento do Mercado Pago.
          </p>
        </div>
        <div className="text-lg-end text-muted">
          <small>
            Última atualização: <strong>{formatDate(data?.updatedAt)}</strong>
          </small>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-4">
          <div className="card h-100">
            <div className="card-body">
              <h2 className="h5">Variáveis disponíveis</h2>
              <p className="text-muted">
                Os tokens abaixo serão substituídos automaticamente quando o e-mail for gerado. Clique para inserir no editor.
              </p>
              <div className="d-flex flex-column gap-3">
                {placeholders.map((placeholder) => (
                  <div key={placeholder.key} className="border rounded p-3">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <strong>{placeholder.label}</strong>
                      <button
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => handleInsertToken(placeholder.token)}
                      >
                        Inserir
                      </button>
                    </div>
                    <code className="d-block mb-1">{placeholder.token}</code>
                    <p className="text-muted mb-0 small">{placeholder.description}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="col-lg-8">
          <form className="card h-100" onSubmit={handleSubmit}>
            <div className="card-body d-flex flex-column">
              <div className="mb-3">
                <label htmlFor="lead-portal-email-template" className="form-label fw-semibold">
                  HTML do e-mail
                </label>
                <textarea
                  id="lead-portal-email-template"
                  ref={textareaRef}
                  className="form-control font-monospace"
                  rows={18}
                  value={html}
                  onChange={(event) => {
                    setHtml(event.target.value);
                    setIsDirty(true);
                  }}
                  placeholder="<p>Olá {{nome_cliente}}, já preparamos as prévias...</p>"
                />
                <div className="form-text">
                  Inclua o texto completo do e-mail, imagens e botões. O sistema adiciona automaticamente o pixel de rastreamento e o rodapé com o ID do pacote.
                </div>
              </div>

              <div className="alert alert-info" role="alert">
                <p className="mb-1">
                  Recomendações rápidas:
                </p>
                <ul className="mb-0 ps-3">
                  <li>Use o token {"{{link_pagamento}}"} dentro de um botão ou link para apontar para o checkout do Mercado Pago.</li>
                  <li>Mostre até três imagens com {"<img src=\"{{imagem_previa_X}}\" />"} para destacar as prévias com marca d'água.</li>
                  <li>Comece o texto com uma saudação personalizada usando {"{{nome_cliente}}"}.</li>
                </ul>
              </div>

              <div className="mt-auto d-flex gap-2 justify-content-end">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={handleReset}
                  disabled={!isDirty || updateTemplate.isPending}
                >
                  Descartar alterações
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={!isDirty || updateTemplate.isPending}
                >
                  {updateTemplate.isPending ? "Salvando..." : "Salvar alterações"}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
