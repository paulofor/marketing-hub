import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Loader2, Pencil, Plus, Search, Tags } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  ProductTypeDefinition,
  ProductTypeStatus,
  useProductTypes,
  useSaveProductType,
} from "../../api/productType/useProductTypes";

type ProductTypeForm = {
  id?: number;
  code: string;
  name: string;
  internalName: string;
  description: string;
  aliases: string;
  status: ProductTypeStatus;
};

const emptyForm: ProductTypeForm = {
  code: "",
  name: "",
  internalName: "",
  description: "",
  aliases: "",
  status: "PROPOSED",
};

function toForm(type: ProductTypeDefinition): ProductTypeForm {
  return {
    id: type.id,
    code: type.code,
    name: type.name,
    internalName: type.internalName ?? "",
    description: type.description ?? "",
    aliases: type.aliases.join("\n"),
    status: type.status,
  };
}

function statusLabel(status: ProductTypeStatus) {
  if (status === "ACTIVE") return "Em uso";
  if (status === "RETIRED") return "Aposentado";
  return "Em avaliação";
}

export default function ProductTypeCatalogPage() {
  const [query, setQuery] = useState("");
  const [form, setForm] = useState<ProductTypeForm>(emptyForm);
  const typesQuery = useProductTypes(true, query);
  const saveType = useSaveProductType();
  const types = useMemo(
    () => (Array.isArray(typesQuery.data) ? typesQuery.data : []),
    [typesQuery.data],
  );

  const submit = (event: FormEvent) => {
    event.preventDefault();
    saveType.mutate(
      {
        id: form.id,
        data: {
          code: form.code || undefined,
          name: form.name,
          internalName: form.internalName,
          description: form.description || undefined,
          aliases: form.aliases
            .split(/[,;\n]/)
            .map((alias) => alias.trim())
            .filter(Boolean),
          status: form.status,
        },
      },
      { onSuccess: () => setForm(emptyForm) },
    );
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Tipos de Produto</PageTitle>
          <p className="text-muted mb-0">
            Classifique por mecanismo de valor sem transformar nomes de trabalho
            em categorias duplicadas.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Voltar aos produtos
        </Link>
      </div>

      <div className="alert alert-info" role="note">
        Novas ideias podem nascer como <strong>Em avaliação</strong>. Ative o
        tipo somente quando a fronteira estiver clara; apelidos ajudam pessoas e
        agentes a encontrá-lo sem criar outra categoria. O nome interno usa um
        mineral único e permanece estável mesmo quando o tipo evolui.
      </div>

      <div className="row g-4">
        <div className="col-12 col-xl-4">
          <form className="card shadow-sm" onSubmit={submit}>
            <div className="card-body">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Tags size={20} aria-hidden="true" />
                <h2 className="h5 mb-0">
                  {form.id ? "Editar tipo" : "Cadastrar tipo"}
                </h2>
              </div>
              <label className="form-label" htmlFor="product-type-name">
                Nome canônico *
              </label>
              <input
                id="product-type-name"
                className="form-control mb-3"
                required
                maxLength={191}
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
              />

              <label
                className="form-label"
                htmlFor="product-type-internal-name"
              >
                Nome interno (mineral) *
              </label>
              <input
                id="product-type-internal-name"
                className="form-control mb-1"
                required
                maxLength={191}
                placeholder="Opala, Quartzo, Safira"
                value={form.internalName}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    internalName: event.target.value,
                  }))
                }
              />
              <p className="form-text mb-3">
                Identidade interna estável. Não aparece automaticamente na
                comunicação pública.
              </p>

              <label className="form-label" htmlFor="product-type-code">
                Código estável
              </label>
              <input
                id="product-type-code"
                className="form-control mb-1"
                maxLength={64}
                disabled={Boolean(
                  form.id &&
                  types.find((type) => type.id === form.id)?.productCount,
                )}
                placeholder="Gerado pelo nome se ficar vazio"
                value={form.code}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
              />
              <p className="form-text mb-3">
                Integrações usam o código; produtos vinculados impedem sua
                troca.
              </p>

              <label className="form-label" htmlFor="product-type-description">
                Quando usar{form.status === "ACTIVE" ? " *" : ""}
              </label>
              <textarea
                id="product-type-description"
                className="form-control mb-3"
                rows={4}
                maxLength={1000}
                required={form.status === "ACTIVE"}
                value={form.description}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
              />

              <label className="form-label" htmlFor="product-type-aliases">
                Apelidos internos
              </label>
              <textarea
                id="product-type-aliases"
                className="form-control mb-1"
                rows={4}
                placeholder="curso prático, experiência guiada"
                value={form.aliases}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    aliases: event.target.value,
                  }))
                }
              />
              <p className="form-text mb-3">
                Um por linha ou separados por vírgula. Não aparecem
                automaticamente na comunicação pública.
              </p>

              <label className="form-label" htmlFor="product-type-status">
                Estado *
              </label>
              <select
                id="product-type-status"
                className="form-select mb-3"
                required
                value={form.status}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    status: event.target.value as ProductTypeStatus,
                  }))
                }
              >
                <option value="PROPOSED">Em avaliação</option>
                <option value="ACTIVE">Em uso</option>
                <option value="RETIRED">Aposentado</option>
              </select>

              {saveType.isError && (
                <div className="alert alert-danger py-2" role="alert">
                  Não foi possível salvar. Verifique se nome canônico, nome
                  interno, código ou apelido já identificam outro tipo.
                </div>
              )}

              <div className="d-flex flex-wrap gap-2">
                <button
                  className="btn btn-primary"
                  type="submit"
                  disabled={
                    saveType.isPending ||
                    !form.internalName.trim() ||
                    (form.status === "ACTIVE" && !form.description.trim())
                  }
                >
                  {saveType.isPending ? (
                    <Loader2
                      className="spinning me-2"
                      size={16}
                      aria-hidden="true"
                    />
                  ) : (
                    <Plus className="me-2" size={16} aria-hidden="true" />
                  )}
                  {saveType.isPending
                    ? "Salvando..."
                    : form.id
                      ? "Salvar alterações"
                      : "Cadastrar tipo"}
                </button>
                {form.id && (
                  <button
                    className="btn btn-outline-secondary"
                    type="button"
                    disabled={saveType.isPending}
                    onClick={() => setForm(emptyForm)}
                  >
                    Cancelar
                  </button>
                )}
              </div>
            </div>
          </form>
        </div>

        <div className="col-12 col-xl-8">
          <div className="input-group mb-3">
            <span className="input-group-text" aria-hidden="true">
              <Search size={18} />
            </span>
            <input
              className="form-control"
              aria-label="Buscar tipo por nome, mineral, código ou apelido"
              placeholder="Nome, mineral, código ou apelido"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          {typesQuery.isLoading && <p>Carregando tipos...</p>}
          {typesQuery.isError && (
            <div className="alert alert-danger" role="alert">
              Não foi possível carregar o catálogo de tipos.
            </div>
          )}
          {!typesQuery.isLoading &&
            !typesQuery.isError &&
            types.length === 0 && (
              <p className="text-muted">Nenhum tipo corresponde à busca.</p>
            )}

          <div className="d-grid gap-3">
            {types.map((type) => (
              <article className="card shadow-sm" key={type.id}>
                <div className="card-body">
                  <div className="d-flex flex-wrap justify-content-between gap-3">
                    <div>
                      <span className="badge text-bg-light border mb-2">
                        {statusLabel(type.status)}
                      </span>
                      <h2 className="h5 mb-1">{type.name}</h2>
                      <span className="d-block text-muted small mb-1">
                        Nome interno: {type.internalName || "Pendente"}
                      </span>
                      <code>{type.code}</code>
                    </div>
                    <div className="text-end">
                      <strong>{type.productCount}</strong>
                      <span className="d-block text-muted small">
                        produto{type.productCount === 1 ? "" : "s"}
                      </span>
                    </div>
                  </div>
                  <p className="mt-3 mb-2">
                    {type.description || "Fronteira de uso ainda não descrita."}
                  </p>
                  <div
                    className="d-flex flex-wrap gap-2 mb-3"
                    aria-label={`Apelidos de ${type.name}`}
                  >
                    {type.aliases.length ? (
                      type.aliases.map((alias) => (
                        <span
                          className="badge text-bg-light border"
                          key={alias}
                        >
                          {alias}
                        </span>
                      ))
                    ) : (
                      <span className="text-muted small">Sem apelidos.</span>
                    )}
                  </div>
                  <button
                    className="btn btn-sm btn-outline-primary"
                    type="button"
                    onClick={() => setForm(toForm(type))}
                  >
                    <Pencil className="me-2" size={14} aria-hidden="true" />
                    Editar tipo
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
