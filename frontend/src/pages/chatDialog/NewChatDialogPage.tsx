import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { useCreateChatDialog } from "../../api/chatDialog/useCreateChatDialog";
import PageTitle from "../../components/PageTitle";

interface FormData {
  url: string;
  description: string;
  theme: string;
}

export default function NewChatDialogPage() {
  const { register, handleSubmit, reset } = useForm<FormData>();
  const create = useCreateChatDialog();
  const navigate = useNavigate();

  const onSubmit = async (values: FormData) => {
    try {
      await create.mutateAsync(values);
      reset();
      navigate(-1);
    } catch {
      alert("Erro ao salvar diálogo");
    }
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Novo Diálogo</PageTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <input
          className="form-control mb-2"
          placeholder="URL"
          {...register("url")}
        />
        <input
          className="form-control mb-2"
          placeholder="Tema"
          {...register("theme")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Descrição"
          rows={3}
          {...register("description")}
        />
        <div className="d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-primary"
            disabled={create.isPending}
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log('Validation errors', errors);
            })}
          >
            Salvar
          </button>
        </div>
      </form>
    </div>
  );
}

