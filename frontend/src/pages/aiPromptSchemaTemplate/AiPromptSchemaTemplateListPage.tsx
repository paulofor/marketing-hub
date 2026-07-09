import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useActivateAiPromptSchemaTemplate } from "../../api/aiPromptSchemaTemplate/useActivateAiPromptSchemaTemplate";
import { useAiPromptSchemaTemplates } from "../../api/aiPromptSchemaTemplate/useAiPromptSchemaTemplates";
import PageTitle from "../../components/PageTitle";

const DEFAULT_PIPELINE = "gera-sales-page-v1";

export default function AiPromptSchemaTemplateListPage() {
  const [pipelineCode, setPipelineCode] = useState(DEFAULT_PIPELINE);
  const [stageCode, setStageCode] = useState("");
  const { data, isLoading } = useAiPromptSchemaTemplates(
    pipelineCode || undefined,
    stageCode || undefined,
  );
  const templates = data ?? [];
  const navigate = useNavigate();
  const activateTemplate = useActivateAiPromptSchemaTemplate();
  const stageOptions = useMemo(() => {
    return Array.from(
      new Set(templates.map((template) => template.stageCode)),
    ).sort();
  }, [templates]);

  async function handleActivate(templateKey: string) {
    await activateTemplate.mutateAsync(templateKey);
    toast.success("Template ativado");
  }

  return (
    <div className="d-flex flex-column gap-3">
      <PageTitle>Prompts operacionais de IA</PageTitle>

      <div className="row g-3 align-items-end">
        <div className="col-md-4">
          <label htmlFor="ai-prompt-pipeline" className="form-label">
            Pipeline
          </label>
          <input
            id="ai-prompt-pipeline"
            className="form-control"
            value={pipelineCode}
            onChange={(event) => setPipelineCode(event.target.value)}
            placeholder="gera-sales-page-v1"
          />
        </div>
        <div className="col-md-4">
          <label htmlFor="ai-prompt-stage" className="form-label">
            Etapa
          </label>
          <input
            id="ai-prompt-stage"
            className="form-control"
            value={stageCode}
            onChange={(event) => setStageCode(event.target.value)}
            list="ai-prompt-stage-options"
            placeholder="Todas"
          />
          <datalist id="ai-prompt-stage-options">
            {stageOptions.map((stage) => (
              <option key={stage} value={stage} />
            ))}
          </datalist>
        </div>
      </div>

      {isLoading ? (
        <p>Carregando...</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Etapa</th>
                <th>Versão</th>
                <th>Modelo</th>
                <th>Schema</th>
                <th>Status</th>
                <th>Atualizado</th>
                <th className="text-end">Ações</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr
                  key={template.templateKey}
                  className={template.active ? "table-success" : ""}
                >
                  <td>
                    <div className="fw-semibold">{template.stageCode}</div>
                    <small className="text-body-secondary">
                      {template.templateKey}
                    </small>
                  </td>
                  <td>{template.version}</td>
                  <td>{template.openAiModel}</td>
                  <td>{template.schemaName}</td>
                  <td>
                    {template.active ? (
                      <span className="badge text-bg-success">Ativo</span>
                    ) : (
                      <span className="badge text-bg-secondary">Inativo</span>
                    )}
                  </td>
                  <td>
                    {template.updatedAt
                      ? new Date(template.updatedAt).toLocaleString()
                      : "-"}
                  </td>
                  <td className="text-end">
                    <div className="d-flex justify-content-end gap-2">
                      {!template.active ? (
                        <button
                          type="button"
                          className="btn btn-outline-success btn-sm"
                          onClick={() => handleActivate(template.templateKey)}
                          disabled={activateTemplate.isPending}
                        >
                          {activateTemplate.isPending ? (
                            <span
                              className="spinner-border spinner-border-sm"
                              aria-hidden="true"
                            />
                          ) : (
                            "Ativar"
                          )}
                        </button>
                      ) : null}
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm"
                        onClick={() =>
                          navigate(
                            `/ai-prompt-schema-templates/${encodeURIComponent(template.templateKey)}/edit`,
                          )
                        }
                      >
                        Editar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {templates.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center text-body-secondary">
                    Nenhum template operacional encontrado.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
