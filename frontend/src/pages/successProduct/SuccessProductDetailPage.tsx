import { useParams } from "react-router-dom";
import { useSuccessProduct } from "../../api/successProduct/useSuccessProduct";
import PageTitle from "../../components/PageTitle";

export default function SuccessProductDetailPage() {
  const { id } = useParams();
  const productId = Number(id);
  const { data, isLoading } = useSuccessProduct(productId);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  return (
    <div>
      <PageTitle>Produto de Sucesso {data.id}</PageTitle>
      <h3>Descrição</h3>
      <pre>{data.description}</pre>
      <h3>Nicho</h3>
      <p>{data.niche}</p>
      <h3>Avatar</h3>
      <p>{data.avatar}</p>
      <h3>Dor Explícita</h3>
      <pre>{data.explicitPain}</pre>
      <h3>Promessa</h3>
      <pre>{data.promise}</pre>
      <h3>Mecanismo Único</h3>
      <pre>{data.uniqueMechanism}</pre>
      <h3>Tripwire</h3>
      <pre>{data.tripwire}</pre>
      <h3>Reversão de Risco</h3>
      <pre>{data.riskReversal}</pre>
      <h3>Prova Social</h3>
      <pre>{data.socialProof}</pre>
      <h3>Monetização do Checkout</h3>
      <pre>{data.checkoutMonetization}</pre>
      <h3>Funil</h3>
      <pre>{data.funnel}</pre>
      <h3>Volume Criativo</h3>
      <pre>{data.creativeVolume}</pre>
      <h3>Storytelling</h3>
      <pre>{data.storytelling}</pre>
    </div>
  );
}
