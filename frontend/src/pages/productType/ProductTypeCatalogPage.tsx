import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  CheckCircle2,
  CircleDashed,
  Code2,
  Loader2,
  Pencil,
  Plus,
  Search,
  Tags,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  ProductTypeDefinition,
  ProductTypeStatus,
  SaveProductTypeBlueprint,
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
  blueprint: SaveProductTypeBlueprint;
};

type BlueprintTextKey =
  | "customerJob"
  | "valueMechanism"
  | "experienceFlow"
  | "requiredInputs"
  | "expectedOutputs"
  | "memoryStrategy"
  | "integrationRequirements"
  | "safetyGuardrails"
  | "successMetrics";

const emptyBlueprint: SaveProductTypeBlueprint = {
  version: "",
  primaryChannel: "",
  customerJob: "",
  valueMechanism: "",
  experienceFlow: "",
  requiredInputs: "",
  expectedOutputs: "",
  memoryStrategy: "",
  integrationRequirements: "",
  safetyGuardrails: "",
  successMetrics: "",
  backendSdkModule: "",
  frontendSdkModule: "",
};

const emptyForm: ProductTypeForm = {
  code: "",
  name: "",
  internalName: "",
  description: "",
  aliases: "",
  status: "PROPOSED",
  blueprint: emptyBlueprint,
};

const blueprintTextFields: Array<{
  key: BlueprintTextKey;
  label: string;
  help: string;
}> = [
  {
    key: "customerJob",
    label: "Trabalho do cliente",
    help: "Dor, esforço e resultado que fazem o cliente procurar este tipo.",
  },
  {
    key: "valueMechanism",
    label: "Mecanismo de valor",
    help: "Como as entradas viram uma melhoria prática e percebida.",
  },
  {
    key: "experienceFlow",
    label: "Jornada base",
    help: "Sequência mínima desde a entrada até valor, continuidade e retorno.",
  },
  {
    key: "requiredInputs",
    label: "Entradas obrigatórias",
    help: "Dados, mídia, contexto e consentimentos necessários.",
  },
  {
    key: "expectedOutputs",
    label: "Saídas esperadas",
    help: "Entregas que materializam a promessa para o cliente.",
  },
  {
    key: "memoryStrategy",
    label: "Memória e segregação",
    help: "O que lembrar, por quanto tempo e como impedir mistura entre clientes.",
  },
  {
    key: "integrationRequirements",
    label: "Integrações obrigatórias",
    help: "Canais, backend, worker, armazenamento, autenticação e observabilidade.",
  },
  {
    key: "safetyGuardrails",
    label: "Segurança e bloqueios",
    help: "Condições que devem bloquear uma orientação ou ação insegura.",
  },
  {
    key: "successMetrics",
    label: "Métricas de sucesso",
    help: "Eventos de valor, recorrência, receita, custo, margem e sinais negativos.",
  },
];

function toForm(type: ProductTypeDefinition): ProductTypeForm {
  return {
    id: type.id,
    code: type.code,
    name: type.name,
    internalName: type.internalName ?? "",
    description: type.description ?? "",
    aliases: type.aliases.join("\n"),
    status: type.status,
    blueprint: {
      version: type.blueprint?.version ?? "",
      primaryChannel: type.blueprint?.primaryChannel ?? "",
      customerJob: type.blueprint?.customerJob ?? "",
      valueMechanism: type.blueprint?.valueMechanism ?? "",
      experienceFlow: type.blueprint?.experienceFlow ?? "",
      requiredInputs: type.blueprint?.requiredInputs ?? "",
      expectedOutputs: type.blueprint?.expectedOutputs ?? "",
      memoryStrategy: type.blueprint?.memoryStrategy ?? "",
      integrationRequirements: type.blueprint?.integrationRequirements ?? "",
      safetyGuardrails: type.blueprint?.safetyGuardrails ?? "",
      successMetrics: type.blueprint?.successMetrics ?? "",
      backendSdkModule: type.blueprint?.backendSdkModule ?? "",
      frontendSdkModule: type.blueprint?.frontendSdkModule ?? "",
    },
  };
}

function statusLabel(status: ProductTypeStatus) {
  if (status === "ACTIVE") return "Em uso";
  if (status === "RETIRED") return "Aposentado";
  return "Em avaliação";
}

function hasBlueprintContent(blueprint: SaveProductTypeBlueprint) {
  return Object.values(blueprint).some((value) => Boolean(value?.trim()));
}

function BlueprintSection({
  title,
  value,
}: {
  title: string;
  value?: string | null;
}) {
  return (
    <div className="col-12 col-lg-6">
      <div className="border rounded-3 p-3 h-100 bg-body-tertiary">
        <h4 className="h6 mb-2">{title}</h4>
        <p
          className="mb-0 small text-body-secondary"
          style={{ whiteSpace: "pre-line" }}
        >
          {value || "Ainda não definido."}
        </p>
      </div>
    </div>
  );
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
  const constructionReadyCount = types.filter(
    (type) => type.constructionReady,
  ).length;
  const activeCount = types.filter((type) => type.status === "ACTIVE").length;
  const blueprintStarted = hasBlueprintContent(form.blueprint);
  const blueprintRequired =
    form.status === "ACTIVE" && (!form.id || blueprintStarted);

  const updateBlueprint = (
    field: keyof SaveProductTypeBlueprint,
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      blueprint: { ...current.blueprint, [field]: value },
    }));
  };

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
          blueprint: blueprintStarted ? form.blueprint : null,
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
            Acompanhe a classificação, o mineral e a base necessária para
            construir cada PDE.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Voltar aos produtos
        </Link>
      </div>

      <div className="row g-3 mb-4" aria-label="Resumo do catálogo">
        <div className="col-12 col-sm-4">
          <div className="card h-100 shadow-sm">
            <div className="card-body">
              <span className="text-muted small">Tipos cadastrados</span>
              <strong className="d-block fs-3">{types.length}</strong>
            </div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="card h-100 shadow-sm">
            <div className="card-body">
              <span className="text-muted small">Em uso</span>
              <strong className="d-block fs-3">{activeCount}</strong>
            </div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="card h-100 shadow-sm">
            <div className="card-body">
              <span className="text-muted small">Bases prontas</span>
              <strong className="d-block fs-3">{constructionReadyCount}</strong>
            </div>
          </div>
        </div>
      </div>

      <div className="alert alert-info" role="note">
        Um canal só merece tipo próprio quando muda a experiência, a aquisição,
        as integrações, as evidências e o contrato de construção. O mineral é
        único e estável; a base detalhada evita que um novo produto comece
        apenas com um nome.
      </div>

      <div className="row g-4">
        <div className="col-12 col-xl-5">
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
                placeholder="Fluorita, Turmalina, Safira"
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
                rows={3}
                placeholder="consultor web, especialista mobile"
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
                automaticamente na oferta.
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

              <div className="border rounded-3 p-3 mb-3">
                <div className="d-flex align-items-center gap-2 mb-3">
                  <Code2 size={18} aria-hidden="true" />
                  <h3 className="h6 mb-0">Base para construção</h3>
                </div>
                <p className="small text-muted">
                  Preencha para transformar o tipo em um ponto de partida
                  reutilizável. Ao ativar uma base nova, os campos centrais são
                  obrigatórios.
                </p>

                <label
                  className="form-label"
                  htmlFor="product-type-blueprint-version"
                >
                  Versão da base{blueprintRequired ? " *" : ""}
                </label>
                <input
                  id="product-type-blueprint-version"
                  className="form-control mb-3"
                  maxLength={64}
                  required={blueprintRequired}
                  placeholder="consultant-pwa-v1"
                  value={form.blueprint.version}
                  onChange={(event) =>
                    updateBlueprint("version", event.target.value)
                  }
                />

                <label
                  className="form-label"
                  htmlFor="product-type-primary-channel"
                >
                  Canal principal{blueprintRequired ? " *" : ""}
                </label>
                <input
                  id="product-type-primary-channel"
                  className="form-control mb-3"
                  maxLength={64}
                  required={blueprintRequired}
                  list="product-type-channel-options"
                  placeholder="PWA ou WHATSAPP"
                  value={form.blueprint.primaryChannel}
                  onChange={(event) =>
                    updateBlueprint("primaryChannel", event.target.value)
                  }
                />
                <datalist id="product-type-channel-options">
                  <option value="PWA" />
                  <option value="WHATSAPP" />
                  <option value="WEB" />
                  <option value="MOBILE_APP" />
                  <option value="MULTICHANNEL" />
                </datalist>

                {blueprintTextFields.map((field) => (
                  <div key={field.key}>
                    <label
                      className="form-label"
                      htmlFor={`product-type-${field.key}`}
                    >
                      {field.label}
                      {blueprintRequired ? " *" : ""}
                    </label>
                    <textarea
                      id={`product-type-${field.key}`}
                      className="form-control mb-1"
                      rows={3}
                      maxLength={5000}
                      required={blueprintRequired}
                      value={form.blueprint[field.key]}
                      onChange={(event) =>
                        updateBlueprint(field.key, event.target.value)
                      }
                    />
                    <p className="form-text mb-3">{field.help}</p>
                  </div>
                ))}

                <label
                  className="form-label"
                  htmlFor="product-type-backend-sdk"
                >
                  SDK Java{blueprintRequired ? " *" : ""}
                </label>
                <input
                  id="product-type-backend-sdk"
                  className="form-control mb-1"
                  maxLength={255}
                  required={blueprintRequired}
                  placeholder="pde-platform/pde-harness-sdk"
                  value={form.blueprint.backendSdkModule}
                  onChange={(event) =>
                    updateBlueprint("backendSdkModule", event.target.value)
                  }
                />
                <p className="form-text mb-3">
                  Módulo que executa o harness do consultor.
                </p>

                <label
                  className="form-label"
                  htmlFor="product-type-frontend-sdk"
                >
                  SDK React
                  {blueprintRequired &&
                  form.blueprint.primaryChannel.trim().toUpperCase() === "PWA"
                    ? " *"
                    : ""}
                </label>
                <input
                  id="product-type-frontend-sdk"
                  className="form-control mb-1"
                  maxLength={255}
                  required={
                    blueprintRequired &&
                    form.blueprint.primaryChannel.trim().toUpperCase() === "PWA"
                  }
                  placeholder="pde-platform/frontend/src/consultant-sdk/v1"
                  value={form.blueprint.frontendSdkModule ?? ""}
                  onChange={(event) =>
                    updateBlueprint("frontendSdkModule", event.target.value)
                  }
                />
                <p className="form-text mb-0">
                  Obrigatório quando o cliente usa uma interface web própria.
                </p>
              </div>

              {saveType.isError && (
                <div className="alert alert-danger py-2" role="alert">
                  Não foi possível salvar. Verifique identidades duplicadas e se
                  a base ativa está completa.
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

        <div className="col-12 col-xl-7">
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
                      <div className="d-flex flex-wrap gap-2 mb-2">
                        <span className="badge text-bg-light border">
                          {statusLabel(type.status)}
                        </span>
                        <span
                          className={`badge ${
                            type.constructionReady
                              ? "text-bg-success"
                              : "text-bg-warning"
                          }`}
                        >
                          {type.constructionReady ? (
                            <CheckCircle2
                              className="me-1"
                              size={13}
                              aria-hidden="true"
                            />
                          ) : (
                            <CircleDashed
                              className="me-1"
                              size={13}
                              aria-hidden="true"
                            />
                          )}
                          {type.constructionReady
                            ? "Base pronta"
                            : "Base incompleta"}
                        </span>
                      </div>
                      <h2 className="h5 mb-1">{type.name}</h2>
                      <span className="d-block text-muted small mb-1">
                        Mineral: {type.internalName || "Pendente"}
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

                  <details className="border-top pt-3 mb-3">
                    <summary className="fw-semibold cursor-pointer">
                      Ver base de construção
                    </summary>
                    {!type.constructionReady &&
                      type.missingBlueprintFields.length > 0 && (
                        <div className="alert alert-warning py-2 mt-3 mb-3">
                          <strong>Faltam:</strong>{" "}
                          {type.missingBlueprintFields.join(", ")}.
                        </div>
                      )}
                    <div className="d-flex flex-wrap gap-2 mt-3 mb-3">
                      <span className="badge text-bg-primary">
                        Canal:{" "}
                        {type.blueprint?.primaryChannel || "Não definido"}
                      </span>
                      <span className="badge text-bg-light border">
                        Base: {type.blueprint?.version || "Não versionada"}
                      </span>
                    </div>
                    <div className="row g-3">
                      <BlueprintSection
                        title="Trabalho do cliente"
                        value={type.blueprint?.customerJob}
                      />
                      <BlueprintSection
                        title="Mecanismo de valor"
                        value={type.blueprint?.valueMechanism}
                      />
                      <BlueprintSection
                        title="Jornada base"
                        value={type.blueprint?.experienceFlow}
                      />
                      <BlueprintSection
                        title="Entradas obrigatórias"
                        value={type.blueprint?.requiredInputs}
                      />
                      <BlueprintSection
                        title="Saídas esperadas"
                        value={type.blueprint?.expectedOutputs}
                      />
                      <BlueprintSection
                        title="Memória e segregação"
                        value={type.blueprint?.memoryStrategy}
                      />
                      <BlueprintSection
                        title="Integrações obrigatórias"
                        value={type.blueprint?.integrationRequirements}
                      />
                      <BlueprintSection
                        title="Segurança e bloqueios"
                        value={type.blueprint?.safetyGuardrails}
                      />
                      <BlueprintSection
                        title="Métricas de sucesso"
                        value={type.blueprint?.successMetrics}
                      />
                      <BlueprintSection
                        title="SDKs básicos"
                        value={[
                          type.blueprint?.backendSdkModule
                            ? `Java: ${type.blueprint.backendSdkModule}`
                            : "Java: não definido",
                          type.blueprint?.frontendSdkModule
                            ? `React: ${type.blueprint.frontendSdkModule}`
                            : "React: não se aplica ou não definido",
                        ].join("\n")}
                      />
                    </div>
                  </details>

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
