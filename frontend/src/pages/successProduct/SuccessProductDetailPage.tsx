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
      <div className="card">
        <div className="card-body">
          <dl className="row mb-0">
            <dt className="col-sm-3">Descrição</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.description}
            </dd>
            <dt className="col-sm-3">Nicho</dt>
            <dd className="col-sm-9">{data.niche}</dd>
            <dt className="col-sm-3">Avatar</dt>
            <dd className="col-sm-9">{data.avatar}</dd>
            <dt className="col-sm-3">Dor Explícita</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.explicitPain}
            </dd>
            <dt className="col-sm-3">Promessa</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.promise}
            </dd>
            <dt className="col-sm-3">Mecanismo Único</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.uniqueMechanism}
            </dd>
            <dt className="col-sm-3">Tripwire</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.tripwire}
            </dd>
            <dt className="col-sm-3">Reversão de Risco</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.riskReversal}
            </dd>
            <dt className="col-sm-3">Prova Social</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.socialProof}
            </dd>
            <dt className="col-sm-3">Monetização do Checkout</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.checkoutMonetization}
            </dd>
            <dt className="col-sm-3">Funil</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.funnel}
            </dd>
            <dt className="col-sm-3">Volume Criativo</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.creativeVolume}
            </dd>
            <dt className="col-sm-3">Storytelling</dt>
            <dd className="col-sm-9" style={{ whiteSpace: "pre-wrap" }}>
              {data.storytelling}
            </dd>
          </dl>
        </div>
      </div>
    </div>
  );
}
