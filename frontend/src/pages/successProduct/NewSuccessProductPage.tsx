import { useForm } from "react-hook-form";
import {
  useCreateSuccessProduct,
  CreateSuccessProduct,
} from "../../api/successProduct/useCreateSuccessProduct";
import PageTitle from "../../components/PageTitle";
import { SuccessProductPlatform } from "../../api/successProduct/useSuccessProducts";

export default function NewSuccessProductPage() {
  const create = useCreateSuccessProduct();
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<CreateSuccessProduct>({
    defaultValues: {
      description: "",
      platform: "COFRE" as SuccessProductPlatform,
      generateNicheHypothesis: false,
    },
  });

  const onSubmit = async (values: CreateSuccessProduct) => {
    try {
      await create.mutateAsync(values);
      reset({
        description: "",
        platform: "COFRE",
        generateNicheHypothesis: false,
      });
      alert("Produto de Sucesso salvo!");
    } catch (err) {
      alert("Erro ao salvar Produto de Sucesso");
    }
  };

  return (
    <div>
      <PageTitle>Novo Produto de Sucesso</PageTitle>
      <form noValidate>
        <textarea
          className="form-control mb-2"
          placeholder="Descrição"
          rows={5}
          {...register("description")}
        />
        <select className="form-select mb-2" {...register("platform")}>
          <option value="COFRE">Cofre</option>
          <option value="HOTMART">Hotmart</option>
          <option value="CLICKBANK">Clickbank</option>
        </select>
        <div className="form-check form-switch mb-3">
          <input
            className="form-check-input"
            type="checkbox"
            id="generateNicheHypothesis"
            {...register("generateNicheHypothesis")}
          />
          <label className="form-check-label" htmlFor="generateNicheHypothesis">
            Gerar Nicho e Hipótese
          </label>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          disabled={isSubmitting}
          onClick={handleSubmit(onSubmit, (errors) => {
            console.log("Validation errors", errors);
          })}
        >
          Salvar
        </button>
      </form>
    </div>
  );
}
