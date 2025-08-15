import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useSuccessProduct } from "../../api/successProduct/useSuccessProduct";
import { useUpdateSuccessProduct } from "../../api/successProduct/useUpdateSuccessProduct";
import { SuccessProduct } from "../../api/successProduct/useSuccessProducts";

export default function EditSuccessProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const productId = Number(id);
  const { data, isLoading } = useSuccessProduct(productId);
  const update = useUpdateSuccessProduct();
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<SuccessProduct>();

  useEffect(() => {
    if (data) {
      reset(data);
    }
  }, [data, reset]);

  const onSubmit = async (values: SuccessProduct) => {
    await update.mutateAsync(values);
    navigate(-1);
  };

  if (isLoading || !data) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 600 }}>
      <PageTitle>Editar Produto de Sucesso</PageTitle>
      <form noValidate>
        <div className="form-check form-switch mb-2">
          <input
            className="form-check-input"
            type="checkbox"
            id="novo"
            {...register("novo")}
          />
          <label className="form-check-label" htmlFor="novo">
            Novo
          </label>
        </div>
        <input
          className="form-control mb-2"
          placeholder="Nome"
          {...register("name")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Descrição"
          rows={3}
          {...register("description")}
        />
        <input
          className="form-control mb-2"
          placeholder="Nicho"
          {...register("niche")}
        />
        <input
          className="form-control mb-2"
          placeholder="Avatar"
          {...register("avatar")}
        />
        <select className="form-select mb-2" {...register("platform")}>
          <option value="COFRE">Cofre</option>
          <option value="HOTMART">Hotmart</option>
          <option value="CLICKBANK">Clickbank</option>
        </select>
        <input
          className="form-control mb-2"
          placeholder="Tipo de Público"
          {...register("audienceType")}
        />
        <input
          className="form-control mb-2"
          placeholder="URL da Página de Vendas"
          {...register("salesPageUrl")}
        />
        <input
          className="form-control mb-2"
          placeholder="Instagram"
          {...register("instagramUrl")}
        />
        <input
          className="form-control mb-2"
          placeholder="Facebook"
          {...register("facebookUrl")}
        />
        <input
          className="form-control mb-2"
          placeholder="YouTube"
          {...register("youtubeUrl")}
        />
        <input
          type="number"
          className="form-control mb-2"
          placeholder="Instagram Account ID"
          {...register("instagramAccountId", { valueAsNumber: true })}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Dor Explícita"
          rows={2}
          {...register("explicitPain")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Promessa"
          rows={2}
          {...register("promise")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Mecanismo Único"
          rows={2}
          {...register("uniqueMechanism")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Tripwire"
          rows={2}
          {...register("tripwire")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Reversão de Risco"
          rows={2}
          {...register("riskReversal")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Prova Social"
          rows={2}
          {...register("socialProof")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Monetização do Checkout"
          rows={2}
          {...register("checkoutMonetization")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Funil de Vendas"
          rows={2}
          {...register("salesFunnel")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Volume Criativo"
          rows={2}
          {...register("creativeVolume")}
        />
        <textarea
          className="form-control mb-3"
          placeholder="Storytelling"
          rows={2}
          {...register("storytelling")}
        />
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
