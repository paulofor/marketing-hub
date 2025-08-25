import { useForm } from "react-hook-form";
import { useParams } from "react-router-dom";
import { usePromptAttributes } from "../../api/prompt/usePromptAttributes";
import { useCreatePromptAttribute } from "../../api/prompt/useCreatePromptAttribute";
import { useEntityAttributes } from "../../api/prompt/useEntityAttributes";
import { useUpdatePromptAttribute } from "../../api/prompt/useUpdatePromptAttribute";
import { useState } from "react";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
}

interface EditFormData {
  description: string;
}

export default function PromptAttributesPage() {
  const { entityName = "" } = useParams<{ entityName: string }>();
  const { data, isLoading } = usePromptAttributes(entityName);
  const create = useCreatePromptAttribute(entityName);
  const { data: entityAttrs } = useEntityAttributes(entityName);
  const { register, handleSubmit, reset } = useForm<FormData>();
  const { register: registerEdit, handleSubmit: handleEdit, reset: resetEdit } =
    useForm<EditFormData>();
  const update = useUpdatePromptAttribute(entityName);
  const [editing, setEditing] = useState<string | null>(null);

  const onSubmit = async (values: FormData) => {
    try {
      await create.mutateAsync({ name: values.name });
      reset({ name: "" });
    } catch {
      alert("Erro ao salvar atributo");
    }
  };

  const onEdit = async (values: EditFormData) => {
    if (!editing) return;
    await update.mutateAsync({ name: editing, description: values.description });
    setEditing(null);
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>{`Atributos de ${entityName}`}</PageTitle>
      <ul>
        {Array.isArray(data) &&
          data.map((a) => (
            <li key={a.name} className="mb-3">
              <strong>{a.name}</strong>
              {editing === a.name ? (
                <form
                  onSubmit={handleEdit(onEdit)}
                  noValidate
                  className="mt-2"
                >
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
                  {a.description && (
                    <div className="text-muted small">{a.description}</div>
                  )}
                  <button
                    type="button"
                    className="btn btn-link btn-sm"
                    onClick={() => {
                      setEditing(a.name);
                      resetEdit({ description: a.description || "" });
                    }}
                  >
                    Editar
                  </button>
                </>
              )}
            </li>
          ))}
      </ul>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3">
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <select id="name" className="form-control mb-2" {...register("name")}> 
          <option value="">Selecione o atributo</option>
          {entityAttrs?.map((attr) => (
            <option key={attr} value={attr}>
              {attr}
            </option>
          ))}
        </select>
        <div className="mt-3 d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-primary"
            disabled={create.isPending}
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log("Validation errors", errors);
            })}
          >
            Salvar
          </button>
        </div>
      </form>
    </div>
  );
}
