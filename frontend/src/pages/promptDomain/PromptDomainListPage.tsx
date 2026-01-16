import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { usePromptDomains } from "../../api/promptDomain/usePromptDomains";
import { useDeletePromptDomain } from "../../api/promptDomain/useDeletePromptDomain";

export default function PromptDomainListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = usePromptDomains();
  const deleteDomain = useDeletePromptDomain();
  const domains = data ?? [];

  const handleDelete = async (id: number) => {
    const confirmed = window.confirm("Tem certeza que deseja remover este domínio?");
    if (!confirmed) return;
    try {
      await deleteDomain.mutateAsync(id);
      toast.success("Domínio removido com sucesso");
    } catch (error) {
      console.error(error);
      toast.error("Não foi possível remover o domínio");
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <div className="d-flex align-items-center justify-content-between">
        <PageTitle>Domínios de prompt</PageTitle>
        <Link to="/prompt-domains/new" className="btn btn-primary">
          Novo domínio
        </Link>
      </div>

      {isLoading ? (
        <p>Carregando...</p>
      ) : domains.length === 0 ? (
        <div className="alert alert-light" role="status">
          <p className="mb-2">Nenhum domínio configurado.</p>
          <Link to="/prompt-domains/new" className="btn btn-sm btn-primary">
            Configurar primeiro domínio
          </Link>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Código</th>
                <th>Objetos</th>
                <th>Atualizado em</th>
                <th className="text-end">Ações</th>
              </tr>
            </thead>
            <tbody>
              {domains.map((domain) => (
                <tr key={domain.id}>
                  <td>{domain.name}</td>
                  <td>
                    <code>{domain.code}</code>
                  </td>
                  <td>
                    {domain.objects.length === 0 ? (
                      <span className="text-body-secondary">Nenhum</span>
                    ) : (
                      <div className="d-flex flex-wrap gap-2">
                        {domain.objects.map((object) => (
                          <span key={object.slug} className="badge text-bg-light">
                            {object.label}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>
                  <td>{domain.updatedAt ? new Date(domain.updatedAt).toLocaleString() : "-"}</td>
                  <td className="text-end d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => navigate(`/prompt-domains/${domain.id}/edit`)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleDelete(domain.id)}
                      disabled={deleteDomain.isPending}
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
