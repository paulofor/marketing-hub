import { FormEvent, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Pencil, Plus, Trash2 } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  type OprmOccupationCatalogItem,
  useCreateOprmOccupation,
  useDeleteOprmOccupation,
  useOprmOccupationCatalog,
  useUpdateOprmOccupation,
} from "../../api/oprm/useOprmOccupationCatalog";
import OprmModuleNavigation from "./OprmModuleNavigation";

interface CatalogFormState {
  occupationSeedRef: string;
  displayName: string;
  aliasesText: string;
  active: boolean;
}

const INITIAL_FORM: CatalogFormState = {
  occupationSeedRef: "",
  displayName: "",
  aliasesText: "",
  active: true,
};

function toRequestPayload(form: CatalogFormState) {
  return {
    occupationSeedRef: form.occupationSeedRef.trim(),
    displayName: form.displayName.trim(),
    aliases: form.aliasesText
      .split(",")
      .map((alias) => alias.trim())
      .filter((alias) => alias.length > 0),
    active: form.active,
  };
}

export default function OprmOccupationCatalogPage() {
  const queryClient = useQueryClient();
  const occupationCatalogQuery = useOprmOccupationCatalog();
  const createMutation = useCreateOprmOccupation();
  const updateMutation = useUpdateOprmOccupation();
  const deleteMutation = useDeleteOprmOccupation();

  const [formState, setFormState] = useState<CatalogFormState>(INITIAL_FORM);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const occupationCatalog = occupationCatalogQuery.data ?? [];
  const sortedCatalog = useMemo(
    () => [...occupationCatalog].sort((a, b) => a.displayName.localeCompare(b.displayName, "pt-BR")),
    [occupationCatalog],
  );

  function startEdit(item: OprmOccupationCatalogItem) {
    setEditingId(item.id);
    setFormState({
      occupationSeedRef: item.occupationSeedRef,
      displayName: item.displayName,
      aliasesText: item.aliases.join(", "),
      active: item.active,
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (editingId) {
      await updateMutation.mutateAsync({
        occupationId: editingId,
        request: toRequestPayload(formState),
      });
    } else {
      await createMutation.mutateAsync(toRequestPayload(formState));
    }

    await queryClient.invalidateQueries({ queryKey: ["oprm", "occupation-catalog"] });
    setEditingId(null);
    setFormState(INITIAL_FORM);
  }

  async function handleDelete(occupationId: string) {
    setDeletingId(occupationId);
    try {
      await deleteMutation.mutateAsync(occupationId);
      await queryClient.invalidateQueries({ queryKey: ["oprm", "occupation-catalog"] });
    } finally {
      setDeletingId(null);
    }
  }

  const saving = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Catálogo de Ocupações</PageTitle>
        <p className="text-secondary mb-0">
          Cadastre, altere e exclua ocupações que podem ser usadas no fluxo do OPRM.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-occupation-seed-ref">
                Referência da ocupação *
              </label>
              <input
                id="oprm-occupation-seed-ref"
                className="form-control"
                value={formState.occupationSeedRef}
                onChange={(event) =>
                  setFormState((current) => ({ ...current, occupationSeedRef: event.target.value }))
                }
                placeholder="Ex.: dentista"
                required
              />
            </div>
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-occupation-display-name">
                Nome de exibição *
              </label>
              <input
                id="oprm-occupation-display-name"
                className="form-control"
                value={formState.displayName}
                onChange={(event) =>
                  setFormState((current) => ({ ...current, displayName: event.target.value }))
                }
                placeholder="Ex.: Dentista"
                required
              />
            </div>
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-occupation-aliases">
                Aliases
              </label>
              <input
                id="oprm-occupation-aliases"
                className="form-control"
                value={formState.aliasesText}
                onChange={(event) =>
                  setFormState((current) => ({ ...current, aliasesText: event.target.value }))
                }
                placeholder="Ex.: odontologista, odonto"
              />
            </div>
            <div className="col-12 col-lg-3">
              <div className="form-check mt-2">
                <input
                  id="oprm-occupation-active"
                  className="form-check-input"
                  type="checkbox"
                  checked={formState.active}
                  onChange={(event) =>
                    setFormState((current) => ({ ...current, active: event.target.checked }))
                  }
                />
                <label className="form-check-label" htmlFor="oprm-occupation-active">
                  Ocupação ativa
                </label>
              </div>
            </div>
            <div className="col-12 col-lg-9 d-flex gap-2 justify-content-end">
              {editingId ? (
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => {
                    setEditingId(null);
                    setFormState(INITIAL_FORM);
                  }}
                  disabled={saving}
                >
                  Cancelar edição
                </button>
              ) : null}
              <button
                type="submit"
                className="btn btn-primary d-inline-flex align-items-center gap-2"
                disabled={saving || formState.occupationSeedRef.trim().length === 0 || formState.displayName.trim().length === 0}
              >
                {saving ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : <Plus size={14} aria-hidden="true" />}
                {editingId ? "Salvar alterações" : "Cadastrar ocupação"}
              </button>
            </div>
          </form>
        </div>
      </section>

      {occupationCatalogQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando catálogo de ocupações...</span>
          </div>
        </div>
      ) : null}

      {occupationCatalogQuery.isError ? (
        <div className="alert alert-danger d-flex align-items-start gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar o catálogo de ocupações.</strong>
            <p className="mb-0">Valide a disponibilidade do backend e tente novamente.</p>
          </div>
        </div>
      ) : null}

      {!occupationCatalogQuery.isLoading && !occupationCatalogQuery.isError ? (
        <section className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Referência</th>
                  <th scope="col">Nome</th>
                  <th scope="col">Aliases</th>
                  <th scope="col">Status</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                {sortedCatalog.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center py-4 text-secondary">
                      Nenhuma ocupação cadastrada.
                    </td>
                  </tr>
                ) : (
                  sortedCatalog.map((item) => {
                    const isDeleting = deletingId === item.id;
                    return (
                      <tr key={item.id}>
                        <td className="fw-semibold">{item.occupationSeedRef}</td>
                        <td>{item.displayName}</td>
                        <td>{item.aliases.length ? item.aliases.join(", ") : "—"}</td>
                        <td>
                          <span className={`badge ${item.active ? "text-bg-success" : "text-bg-secondary"}`}>
                            {item.active ? "Ativa" : "Inativa"}
                          </span>
                        </td>
                        <td>
                          <div className="d-flex flex-wrap gap-2">
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-2"
                              onClick={() => startEdit(item)}
                              disabled={saving || isDeleting}
                            >
                              <Pencil size={14} aria-hidden="true" />
                              Editar
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm d-inline-flex align-items-center gap-2"
                              onClick={() => handleDelete(item.id)}
                              disabled={saving || isDeleting}
                            >
                              {isDeleting ? (
                                <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                              ) : (
                                <Trash2 size={14} aria-hidden="true" />
                              )}
                              Excluir
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}
