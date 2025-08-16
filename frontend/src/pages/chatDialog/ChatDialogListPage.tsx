import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";
import { useUpdateChatDialog } from "../../api/chatDialog/useUpdateChatDialog";

export default function ChatDialogListPage() {
  const { data, isLoading } = useChatDialogs();
  const update = useUpdateChatDialog();
  const dialogs = Array.isArray(data)
    ? [...data].sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
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
