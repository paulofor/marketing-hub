import { FormEvent, useState } from "react";
import { toast } from "react-toastify";
import { useAgents } from "../../api/agent/useAgents";
import {
  useCreateSystemImprovement,
  useSystemImprovements,
} from "../../api/systemImprovement/useSystemImprovements";
import PageTitle from "../../components/PageTitle";

const initialForm = {
  agentKey: "",
  title: "",
  description: "",
  taskReference: "",
};

export default function SystemImprovementsPage() {
  const agents = useAgents();
  const improvements = useSystemImprovements();
  const create = useCreateSystemImprovement();
  const [form, setForm] = useState(initialForm);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    await create.mutateAsync({
      ...form,
      taskReference: form.taskReference.trim() || undefined,
    });
    setForm(initialForm);
    toast.success("Melhoria cadastrada.");
  };

  return (
    <div>
      <PageTitle>Melhorias do Sistema</PageTitle>
      <p className="text-body-secondary">
        Backlog de melhorias percebidas pelos agentes durante tarefas reais.
      </p>

      <section className="card mb-4">
        <div className="card-body">
          <h2 className="h5">Cadastrar melhoria</h2>
          <form className="row g-3" onSubmit={submit}>
            <div className="col-md-4">
              <label className="form-label" htmlFor="improvement-agent">
                Agente solicitante
              </label>
              <select
                id="improvement-agent"
                className="form-select"
                required
                value={form.agentKey}
                onChange={(event) =>
                  setForm({ ...form, agentKey: event.target.value })
                }
              >
                <option value="">Selecione</option>
                {(agents.data ?? []).map((agent) => (
                  <option key={agent.id} value={agent.agentKey}>
                    {agent.nickname} — {agent.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-8">
              <label className="form-label" htmlFor="improvement-title">
                Título
              </label>
              <input
                id="improvement-title"
                className="form-control"
                required
                maxLength={160}
                value={form.title}
                onChange={(event) =>
                  setForm({ ...form, title: event.target.value })
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="improvement-description">
                Melhoria sugerida
              </label>
              <textarea
                id="improvement-description"
                className="form-control"
                required
                rows={4}
                maxLength={10000}
                value={form.description}
                onChange={(event) =>
                  setForm({ ...form, description: event.target.value })
                }
              />
            </div>
            <div className="col-md-8">
              <label className="form-label" htmlFor="improvement-task">
                Referência da tarefa (opcional)
              </label>
              <input
                id="improvement-task"
                className="form-control"
                maxLength={200}
                placeholder="Ex.: experimento 88, job ou execução"
                value={form.taskReference}
                onChange={(event) =>
                  setForm({ ...form, taskReference: event.target.value })
                }
              />
            </div>
            <div className="col-md-4 d-flex align-items-end">
              <button
                className="btn btn-primary w-100"
                disabled={create.isPending}
              >
                {create.isPending ? "Salvando..." : "Cadastrar melhoria"}
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card">
        <div className="table-responsive">
          <table className="table align-middle mb-0">
            <thead>
              <tr>
                <th>Data</th>
                <th>Agente</th>
                <th>Melhoria</th>
                <th>Tarefa</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {(improvements.data ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{new Date(item.requestedAt).toLocaleString("pt-BR")}</td>
                  <td>{item.agentNickname}</td>
                  <td>
                    <div className="fw-semibold">{item.title}</div>
                    <div className="small text-body-secondary">
                      {item.description}
                    </div>
                  </td>
                  <td>{item.taskReference || "—"}</td>
                  <td>
                    <span className="badge text-bg-light">{item.status}</span>
                  </td>
                </tr>
              ))}
              {!improvements.isLoading && improvements.data?.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="text-center text-body-secondary py-4"
                  >
                    Nenhuma melhoria cadastrada.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
