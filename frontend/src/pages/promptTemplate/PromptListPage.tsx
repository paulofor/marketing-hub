import { Link, useNavigate } from "react-router-dom";
import { useState, useMemo } from "react";
import PageTitle from "../../components/PageTitle";
import { usePrompts } from "../../api/promptTemplate/usePrompts";
import { usePromptDomains } from "../../api/promptDomain/usePromptDomains";
import { useActivatePrompt } from "../../api/promptTemplate/useActivatePrompt";

export default function PromptListPage() {
  const [domain, setDomain] = useState("");
  const { data: domainData, isLoading: isLoadingDomains } = usePromptDomains();
  const domainOptions = domainData ?? [];
  const domainLabels = useMemo(() => new Map(domainOptions.map((item) => [item.code, item.name])), [domainOptions]);
  const { data, isLoading } = usePrompts(domain || undefined);
  const prompts = Array.isArray(data) ? data : [];
  const navigate = useNavigate();
  const activatePrompt = useActivatePrompt();

  const handleActivate = async (id: number) => {
    await activatePrompt.mutateAsync(id);
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="d-flex align-items-center justify-content-between">
        <PageTitle>Prompts</PageTitle>
        <Link className="btn btn-primary" to="/prompts/new">
          Novo prompt
        </Link>
      </div>

      <div className="d-flex align-items-end gap-3">
        <div style={{ maxWidth: 360 }}>
          <label htmlFor="prompt-domain-filter" className="form-label mb-1">
            Filtrar por domínio
          </label>
          <select
            id="prompt-domain-filter"
            className="form-select"
            value={domain}
            onChange={(event) => setDomain(event.target.value)}
            disabled={isLoadingDomains}
          >
            <option value="">Todos</option>
            {domainOptions.map((item) => (
              <option key={item.code} value={item.code}>
                {item.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading ? (
        <p>Carregando...</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Domínio</th>
                <th>Status</th>
                <th>Atualizado</th>
                <th className="text-end">Ações</th>
              </tr>
            </thead>
            <tbody>
              {prompts.map((prompt) => (
                <tr key={prompt.id} className={prompt.active ? "table-success" : ""}>
                  <td>{prompt.name}</td>
                  <td>{domainLabels.get(prompt.domain) ?? prompt.domain}</td>
                  <td>
                    {prompt.active ? (
                      <span className="badge text-bg-success">Ativo</span>
                    ) : (
                      <span className="badge text-bg-secondary">Inativo</span>
                    )}
                  </td>
                  <td>{prompt.updatedAt ? new Date(prompt.updatedAt).toLocaleString() : "-"}</td>
                  <td className="text-end d-flex justify-content-end gap-2">
                    {!prompt.active ? (
                      <button
                        type="button"
                        className="btn btn-outline-success btn-sm"
                        onClick={() => handleActivate(prompt.id)}
                        disabled={activatePrompt.isPending}
                      >
                        Ativar
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => navigate(`/prompts/${prompt.id}/edit`)}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
              {prompts.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center text-body-secondary">
                    Nenhum prompt encontrado.
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
