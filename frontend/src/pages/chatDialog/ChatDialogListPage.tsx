import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";
import { useUpdateChatDialog } from "../../api/chatDialog/useUpdateChatDialog";

export default function ChatDialogListPage() {
  const { data, isLoading } = useChatDialogs();
  const update = useUpdateChatDialog();
  const parseDate = (value?: string) => {
    if (!value) return 0;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 0 : date.getTime();
  };
  const formatDate = (value?: string) => {
    const timestamp = parseDate(value);
    return timestamp ? new Date(timestamp).toLocaleDateString("pt-BR") : "";
  };
  const dialogs = Array.isArray(data)
    ? [...data].sort(
        (a, b) => parseDate(b.createdAt) - parseDate(a.createdAt),
      )
    : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Diálogos ChatGPT</PageTitle>
      <Link className="btn btn-primary mb-3" to="/chat-dialogs/new">
        Novo Diálogo
      </Link>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Descrição</th>
              <th>Tema</th>
              <th>Criado em</th>
              <th>URL</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {dialogs.map((d) => (
              <tr key={d.id}>
                <td>{d.id}</td>
                <td>{d.description}</td>
                <td>{d.theme}</td>
                <td>{formatDate(d.createdAt)}</td>
                <td>
                  <a href={d.url} target="_blank" rel="noopener noreferrer">
                    Abrir
                  </a>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => {
                      const description = prompt("Novo nome", d.description);
                      if (description) {
                        update.mutate({ id: d.id, description });
                      }
                    }}
                  >
                    Editar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
