import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { useAiPromptSchemaTemplate } from "../../api/aiPromptSchemaTemplate/useAiPromptSchemaTemplate";
import { useUpdateAiPromptSchemaTemplate } from "../../api/aiPromptSchemaTemplate/useUpdateAiPromptSchemaTemplate";
import PageTitle from "../../components/PageTitle";

export default function EditAiPromptSchemaTemplatePage() {
  const { templateKey } = useParams();
  const resolvedTemplateKey = templateKey
    ? decodeURIComponent(templateKey)
    : "";
  const { data, isLoading } = useAiPromptSchemaTemplate(resolvedTemplateKey);
  const updateTemplate = useUpdateAiPromptSchemaTemplate(resolvedTemplateKey);
  const navigate = useNavigate();
  const [version, setVersion] = useState("");
  const [openAiModel, setOpenAiModel] = useState("");
  const [schemaName, setSchemaName] = useState("");
  const [promptMarkdownContent, setPromptMarkdownContent] = useState("");
  const [schemaJson, setSchemaJson] = useState("");
  const [active, setActive] = useState(false);

  useEffect(() => {
    if (!data) return;
    setVersion(data.version);
    setOpenAiModel(data.openAiModel);
    setSchemaName(data.schemaName);
    setPromptMarkdownContent(data.promptMarkdownContent);
    setSchemaJson(data.schemaJson);
    setActive(data.active);
  }, [data]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      JSON.parse(schemaJson);
    } catch {
      toast.error("Schema JSON inválido");
      return;
    }
    await updateTemplate.mutateAsync({
      version,
      openAiModel,
      schemaName,
      promptMarkdownContent,
      schemaJson,
      active,
    });
    toast.success("Template operacional atualizado");
    navigate("/ai-prompt-schema-templates");
  }

  if (!resolvedTemplateKey) return <p>Template não encontrado.</p>;
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Template não encontrado.</p>;

  return (
    <form onSubmit={handleSubmit} className="d-flex flex-column gap-3">
      <div className="d-flex align-items-center justify-content-between">
        <PageTitle>Editar prompt operacional</PageTitle>
        <button
          type="button"
          className="btn btn-outline-secondary"
          onClick={() => navigate("/ai-prompt-schema-templates")}
        >
          Voltar
        </button>
      </div>

      <div className="card">
        <div className="card-body d-flex flex-column gap-3">
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Pipeline</label>
              <input
                className="form-control"
                value={data.pipelineCode}
                disabled
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Etapa</label>
              <input className="form-control" value={data.stageCode} disabled />
            </div>
            <div className="col-md-4">
              <label className="form-label">Template key</label>
              <input
                className="form-control"
                value={data.templateKey}
                disabled
              />
            </div>
            <div className="col-md-4">
              <label htmlFor="ai-prompt-version" className="form-label">
                Versão *
              </label>
              <input
                id="ai-prompt-version"
                className="form-control"
                value={version}
                onChange={(event) => setVersion(event.target.value)}
                required
                disabled={updateTemplate.isPending}
              />
            </div>
            <div className="col-md-4">
              <label htmlFor="ai-prompt-model" className="form-label">
                Modelo OpenAI *
              </label>
              <input
                id="ai-prompt-model"
                className="form-control"
                value={openAiModel}
                onChange={(event) => setOpenAiModel(event.target.value)}
                required
                disabled={updateTemplate.isPending}
              />
            </div>
            <div className="col-md-4">
              <label htmlFor="ai-prompt-schema-name" className="form-label">
                Nome do schema *
              </label>
              <input
                id="ai-prompt-schema-name"
                className="form-control"
                value={schemaName}
                onChange={(event) => setSchemaName(event.target.value)}
                required
                disabled={updateTemplate.isPending}
              />
            </div>
          </div>

          <div className="form-check">
            <input
              id="ai-prompt-active"
              className="form-check-input"
              type="checkbox"
              checked={active}
              onChange={(event) => setActive(event.target.checked)}
              disabled={updateTemplate.isPending}
            />
            <label className="form-check-label" htmlFor="ai-prompt-active">
              Manter ativo para esta etapa
            </label>
          </div>

          <div>
            <label htmlFor="ai-prompt-content" className="form-label">
              Prompt Markdown *
            </label>
            <textarea
              id="ai-prompt-content"
              className="form-control"
              rows={18}
              style={{ fontFamily: "monospace" }}
              value={promptMarkdownContent}
              onChange={(event) => setPromptMarkdownContent(event.target.value)}
              required
              disabled={updateTemplate.isPending}
            />
          </div>

          <div>
            <label htmlFor="ai-prompt-schema-json" className="form-label">
              JSON schema *
            </label>
            <textarea
              id="ai-prompt-schema-json"
              className="form-control"
              rows={14}
              style={{ fontFamily: "monospace" }}
              value={schemaJson}
              onChange={(event) => setSchemaJson(event.target.value)}
              required
              disabled={updateTemplate.isPending}
            />
          </div>

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={() => navigate("/ai-prompt-schema-templates")}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={updateTemplate.isPending}
            >
              {updateTemplate.isPending ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                  Salvando
                </>
              ) : (
                "Salvar"
              )}
            </button>
          </div>
        </div>
      </div>
    </form>
  );
}
