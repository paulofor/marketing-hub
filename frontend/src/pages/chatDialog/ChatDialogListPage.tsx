import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";

export default function ChatDialogListPage() {
  const { data, isLoading } = useChatDialogs();
  const dialogs = Array.isArray(data) ? data : [];
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
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

