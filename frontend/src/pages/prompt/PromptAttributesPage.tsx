import { useForm } from "react-hook-form";
import { useParams } from "react-router-dom";
import { usePromptAttributes } from "../../api/prompt/usePromptAttributes";
import { useUpdatePromptAttribute } from "../../api/prompt/useUpdatePromptAttribute";
import { usePromptEntity } from "../../api/prompt/usePromptEntity";
import { useEntityAttributes } from "../../api/prompt/useEntityAttributes";
import { useCreatePromptAttribute } from "../../api/prompt/useCreatePromptAttribute";
import { useState } from "react";
import PageTitle from "../../components/PageTitle";

interface EditFormData {
  description: string;
}

interface CreateFormData {
  name: string;
}

export default function PromptAttributesPage() {
  const { entityId = "" } = useParams<{ entityId: string }>();
  const { data: entity } = usePromptEntity(entityId);
  const entityName = entity?.name ?? "";
  const { data: promptAttrs, isLoading } = usePromptAttributes(entityName);
  const { data: entityAttrs } = useEntityAttributes(entityName);
  const {
    register: registerEdit,
    handleSubmit: handleEdit,
    reset: resetEdit,
  } = useForm<EditFormData>();
  const {
    register: registerCreate,
    handleSubmit: handleCreate,
    reset: resetCreate,
  } = useForm<CreateFormData>();
  const update = useUpdatePromptAttribute(entityName);
  const create = useCreatePromptAttribute(entityName);
  const [editing, setEditing] = useState<string | null>(null);

  const onEdit = async (values: EditFormData) => {
    if (!editing) return;
    await update.mutateAsync({
      name: editing,
      description: values.description,
    });
    setEditing(null);
  };

  if (!entityName || isLoading) return <p>Carregando...</p>;

  const attributes = promptAttrs ?? [];
  const availableAttrs =
    entityAttrs?.filter((a) => !attributes.some((pa) => pa.name === a)) ?? [];

  const onCreate = async (values: CreateFormData) => {
    await create.mutateAsync(values);
    resetCreate();
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>{`Atributos de ${entity?.name || ""}`}</PageTitle>
      {availableAttrs.length > 0 && (
        <form onSubmit={handleCreate(onCreate)} noValidate className="mb-3">
          <select
            className="form-select mb-2"
            {...registerCreate("name", { required: true })}
          >
            <option value="">Selecione um atributo</option>
            {availableAttrs.map((a) => (
              <option key={a} value={a}>
                {a}
              </option>
            ))}
          </select>
          <div className="d-flex justify-content-end">
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleCreate(onCreate, (errors) => {
                console.log("Validation errors", errors);
              })}
            >
              Adicionar
            </button>
          </div>
        </form>
      )}
      <ul>
        {attributes.map((a) => (
          <li key={a.name} className="mb-3">
            <strong>{a.name}</strong>
            {editing === a.name ? (
              <form onSubmit={handleEdit(onEdit)} noValidate className="mt-2">
                <textarea
                  className="form-control mb-2"
                  {...registerEdit("description")}
                />
                <div className="d-flex justify-content-end">
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={handleEdit(onEdit, (errors) => {
                      console.log("Validation errors", errors);
                    })}
                  >
                    Salvar
                  </button>
                </div>
              </form>
            ) : (
              <>
                {a.description ? (
                  <div className="text-muted small">{a.description}</div>
                ) : (
                  <div className="text-muted small">Sem descrição</div>
                )}
                <button
                  type="button"
                  className="btn btn-link btn-sm"
                  onClick={() => {
                    setEditing(a.name);
                    resetEdit({ description: a.description || "" });
                  }}
                >
                  {a.description ? "Editar" : "Adicionar"}
                </button>
              </>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
