import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Edit3, PlusCircle, Save, Server, Trash2, X } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  PdeVpsServer,
  PdeVpsStatus,
  SavePdeVpsServerRequest,
  useDeletePdeVpsServer,
  usePdeVpsServers,
  useSavePdeVpsServer,
} from "../../api/pde/usePdeVpsServers";

const statusLabels: Record<PdeVpsStatus, string> = {
  PLANNED: "Planejada",
  ACTIVE: "Ativa",
  STAGING: "Staging",
  PAUSED: "Pausada",
  RETIRED: "Encerrada",
};

const statusBadges: Record<PdeVpsStatus, string> = {
  PLANNED: "text-bg-secondary",
  ACTIVE: "text-bg-success",
  STAGING: "text-bg-info",
  PAUSED: "text-bg-warning",
  RETIRED: "text-bg-dark",
};

const emptyForm = {
  name: "DokeHost PDE principal",
  provider: "DokeHost",
  ipAddress: "163.245.200.7",
  planName: "VPS Linux",
  region: "Brasil",
  vcpuCount: "",
  ramGb: "4",
  storageGb: "",
  monthlyCostBrl: "",
  productSlug: "metodo-musa-7-dias",
  environment: "production",
  domains: "v5.clubemusa.com.br, v6.clubemusa.com.br, v7.clubemusa.com.br",
  status: "PLANNED" as PdeVpsStatus,
  notes: "",
};

const brlFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

function formatBrl(value?: number | null) {
  return brlFormatter.format(value ?? 0);
}

function numberOrNull(value: string) {
  if (!value.trim()) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function serverToForm(server: PdeVpsServer) {
  return {
    name: server.name,
    provider: server.provider,
    ipAddress: server.ipAddress,
    planName: server.planName ?? "",
    region: server.region ?? "",
    vcpuCount: server.vcpuCount?.toString() ?? "",
    ramGb: server.ramGb?.toString() ?? "",
    storageGb: server.storageGb?.toString() ?? "",
    monthlyCostBrl: server.monthlyCostBrl?.toString() ?? "",
    productSlug: server.productSlug ?? "",
    environment: server.environment,
    domains: server.domains ?? "",
    status: server.status,
    notes: server.notes ?? "",
  };
}

export default function PdeVpsManagementPage() {
  const serversQuery = usePdeVpsServers();
  const saveServer = useSavePdeVpsServer();
  const deleteServer = useDeletePdeVpsServer();
  const summary = serversQuery.data;
  const servers = summary?.servers ?? [];
  const [editingId, setEditingId] = useState<number>();
  const [form, setForm] = useState(emptyForm);

  const costByProduct = useMemo(() => {
    const totals = new Map<string, number>();
    servers
      .filter(
        (server) => server.status === "ACTIVE" || server.status === "STAGING",
      )
      .forEach((server) => {
        const productSlug = server.productSlug || "Sem produto vinculado";
        totals.set(
          productSlug,
          (totals.get(productSlug) ?? 0) + server.monthlyCostBrl,
        );
      });
    return Array.from(totals.entries()).map(([productSlug, cost]) => ({
      productSlug,
      cost,
    }));
  }, [servers]);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload: SavePdeVpsServerRequest = {
      name: form.name,
      provider: form.provider,
      ipAddress: form.ipAddress,
      planName: form.planName,
      region: form.region,
      vcpuCount: numberOrNull(form.vcpuCount),
      ramGb: numberOrNull(form.ramGb),
      storageGb: numberOrNull(form.storageGb),
      monthlyCostBrl: Number(form.monthlyCostBrl || 0),
      productSlug: form.productSlug,
      environment: form.environment,
      domains: form.domains,
      status: form.status,
      notes: form.notes,
    };
    saveServer.mutate(
      { id: editingId, payload },
      {
        onSuccess: () => {
          setEditingId(undefined);
          setForm(emptyForm);
        },
      },
    );
  };

  const edit = (server: PdeVpsServer) => {
    setEditingId(server.id);
    setForm(serverToForm(server));
  };

  const cancelEdit = () => {
    setEditingId(undefined);
    setForm(emptyForm);
  };

  if (serversQuery.isLoading) {
    return <p className="text-muted">Carregando VPS dos PDEs...</p>;
  }

  if (serversQuery.isError || !summary) {
    return (
      <div className="alert alert-danger">
        Não foi possível carregar as VPS dos PDEs agora.
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Infra dos PDEs</PageTitle>
          <p className="text-muted mb-0">
            VPS, domínios e custo fixo mensal que impactam a margem dos produtos
            digitais.
          </p>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <section className="card h-100">
            <div className="card-body">
              <span className="text-muted small">Custo mensal ativo</span>
              <strong className="d-block fs-4">
                {formatBrl(summary.totalMonthlyCostBrl)}
              </strong>
            </div>
          </section>
        </div>
        <div className="col-md-4">
          <section className="card h-100">
            <div className="card-body">
              <span className="text-muted small">VPS ativas/staging</span>
              <strong className="d-block fs-4">{summary.activeServers}</strong>
            </div>
          </section>
        </div>
        <div className="col-md-4">
          <section className="card h-100">
            <div className="card-body">
              <span className="text-muted small">VPS cadastradas</span>
              <strong className="d-block fs-4">{summary.totalServers}</strong>
            </div>
          </section>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-xl-4">
          <form className="card" onSubmit={submit}>
            <div className="card-header d-flex align-items-center gap-2">
              {editingId ? <Edit3 size={18} /> : <PlusCircle size={18} />}
              <strong>{editingId ? "Editar VPS" : "Cadastrar VPS"}</strong>
            </div>
            <div className="card-body d-grid gap-3">
              <label className="form-label">
                Nome *
                <input
                  className="form-control"
                  required
                  value={form.name}
                  onChange={(event) =>
                    setForm({ ...form, name: event.target.value })
                  }
                />
              </label>
              <div className="row g-2">
                <label className="form-label col-md-6">
                  Provedor *
                  <select
                    className="form-select"
                    required
                    value={form.provider}
                    onChange={(event) =>
                      setForm({ ...form, provider: event.target.value })
                    }
                  >
                    <option>DokeHost</option>
                    <option>Locaweb</option>
                    <option>AWS</option>
                    <option>KingHost</option>
                    <option>Outro</option>
                  </select>
                </label>
                <label className="form-label col-md-6">
                  Status *
                  <select
                    className="form-select"
                    required
                    value={form.status}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        status: event.target.value as PdeVpsStatus,
                      })
                    }
                  >
                    {Object.entries(statusLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label className="form-label">
                IP ou host *
                <input
                  className="form-control"
                  required
                  value={form.ipAddress}
                  onChange={(event) =>
                    setForm({ ...form, ipAddress: event.target.value })
                  }
                />
              </label>
              <div className="row g-2">
                <label className="form-label col-md-6">
                  Plano
                  <input
                    className="form-control"
                    value={form.planName}
                    onChange={(event) =>
                      setForm({ ...form, planName: event.target.value })
                    }
                  />
                </label>
                <label className="form-label col-md-6">
                  Custo mensal *
                  <input
                    className="form-control"
                    min="0"
                    step="0.01"
                    type="number"
                    required
                    value={form.monthlyCostBrl}
                    onChange={(event) =>
                      setForm({ ...form, monthlyCostBrl: event.target.value })
                    }
                  />
                </label>
              </div>
              <div className="row g-2">
                <label className="form-label col-md-4">
                  vCPU
                  <input
                    className="form-control"
                    min="0"
                    type="number"
                    value={form.vcpuCount}
                    onChange={(event) =>
                      setForm({ ...form, vcpuCount: event.target.value })
                    }
                  />
                </label>
                <label className="form-label col-md-4">
                  RAM GB
                  <input
                    className="form-control"
                    min="0"
                    type="number"
                    value={form.ramGb}
                    onChange={(event) =>
                      setForm({ ...form, ramGb: event.target.value })
                    }
                  />
                </label>
                <label className="form-label col-md-4">
                  Disco GB
                  <input
                    className="form-control"
                    min="0"
                    type="number"
                    value={form.storageGb}
                    onChange={(event) =>
                      setForm({ ...form, storageGb: event.target.value })
                    }
                  />
                </label>
              </div>
              <div className="row g-2">
                <label className="form-label col-md-6">
                  Produto
                  <input
                    className="form-control"
                    value={form.productSlug}
                    onChange={(event) =>
                      setForm({ ...form, productSlug: event.target.value })
                    }
                  />
                </label>
                <label className="form-label col-md-6">
                  Ambiente *
                  <input
                    className="form-control"
                    required
                    value={form.environment}
                    onChange={(event) =>
                      setForm({ ...form, environment: event.target.value })
                    }
                  />
                </label>
              </div>
              <label className="form-label">
                Domínios
                <textarea
                  className="form-control"
                  rows={2}
                  value={form.domains}
                  onChange={(event) =>
                    setForm({ ...form, domains: event.target.value })
                  }
                />
              </label>
              <label className="form-label">
                Observações
                <textarea
                  className="form-control"
                  rows={3}
                  value={form.notes}
                  onChange={(event) =>
                    setForm({ ...form, notes: event.target.value })
                  }
                />
              </label>
            </div>
            <div className="card-footer d-flex gap-2">
              <button
                className="btn btn-primary d-inline-flex align-items-center gap-2"
                type="submit"
                disabled={saveServer.isPending}
              >
                <Save size={16} aria-hidden="true" />
                {saveServer.isPending ? "Salvando..." : "Salvar"}
              </button>
              {editingId ? (
                <button
                  className="btn btn-outline-secondary d-inline-flex align-items-center gap-2"
                  type="button"
                  onClick={cancelEdit}
                >
                  <X size={16} aria-hidden="true" />
                  Cancelar
                </button>
              ) : null}
            </div>
          </form>
        </div>

        <div className="col-xl-8">
          <section className="card mb-4">
            <div className="card-header">
              <strong>Custo fixo por produto</strong>
            </div>
            <div className="card-body">
              {costByProduct.length === 0 ? (
                <p className="text-muted mb-0">
                  Nenhum custo ativo ou staging vinculado a produto.
                </p>
              ) : (
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Produto</th>
                        <th>Custo mensal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {costByProduct.map((item) => (
                        <tr key={item.productSlug}>
                          <td>{item.productSlug}</td>
                          <td>
                            <strong>{formatBrl(item.cost)}</strong>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>

          <section className="card">
            <div className="card-header">
              <strong>VPS cadastradas</strong>
            </div>
            <div className="table-responsive">
              <table className="table align-middle mb-0">
                <thead>
                  <tr>
                    <th>VPS</th>
                    <th>Recursos</th>
                    <th>Custo</th>
                    <th>Produto</th>
                    <th>Status</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {servers.length === 0 ? (
                    <tr>
                      <td className="text-muted" colSpan={6}>
                        Nenhuma VPS cadastrada.
                      </td>
                    </tr>
                  ) : (
                    servers.map((server) => (
                      <tr key={server.id}>
                        <td>
                          <div className="d-flex align-items-start gap-2">
                            <Server size={18} className="text-primary mt-1" />
                            <div>
                              <strong>{server.name}</strong>
                              <div className="text-muted small">
                                {server.provider} · {server.ipAddress}
                              </div>
                              <div className="text-muted small">
                                {server.domains || "Sem domínio informado"}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="small">
                          {server.planName || "Plano não informado"}
                          <br />
                          <span className="text-muted">
                            {server.vcpuCount ?? "?"} vCPU ·{" "}
                            {server.ramGb ?? "?"} GB RAM ·{" "}
                            {server.storageGb ?? "?"} GB
                          </span>
                        </td>
                        <td>
                          <strong>{formatBrl(server.monthlyCostBrl)}</strong>
                          <div className="text-muted small">mensal</div>
                        </td>
                        <td className="small">
                          {server.productSlug || "Não vinculado"}
                          <br />
                          <span className="text-muted">
                            {server.environment}
                          </span>
                        </td>
                        <td>
                          <span
                            className={`badge ${statusBadges[server.status]}`}
                          >
                            {statusLabels[server.status]}
                          </span>
                        </td>
                        <td>
                          <div className="d-flex gap-2">
                            <button
                              className="btn btn-outline-secondary btn-sm"
                              type="button"
                              onClick={() => edit(server)}
                              aria-label={`Editar ${server.name}`}
                            >
                              <Edit3 size={16} aria-hidden="true" />
                            </button>
                            <button
                              className="btn btn-outline-danger btn-sm"
                              type="button"
                              disabled={deleteServer.isPending}
                              onClick={() => deleteServer.mutate(server.id)}
                              aria-label={`Remover ${server.name}`}
                            >
                              <Trash2 size={16} aria-hidden="true" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
