import { Fragment } from "react";
import { useParams, Link } from "react-router-dom";
import { useSuccessProduct } from "../../api/successProduct/useSuccessProduct";
import PageTitle from "../../components/PageTitle";

export default function SuccessProductDetailPage() {
  const { id } = useParams();
  const productId = Number(id);
  const { data, isLoading } = useSuccessProduct(productId);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const nextId = data.id + 1;

  const rows = [
    { label: "Descrição", value: data.description, pre: true },
    { label: "Nicho", value: data.niche },
    { label: "Avatar", value: data.avatar },
    { label: "Página de Vendas", value: data.salesPageUrl },
    { label: "Instagram", value: data.instagramUrl },
    { label: "Facebook", value: data.facebookUrl },
    { label: "YouTube", value: data.youtubeUrl },
    { label: "Dor Explícita", value: data.explicitPain, pre: true },
    { label: "Promessa", value: data.promise, pre: true },
    { label: "Mecanismo Único", value: data.uniqueMechanism, pre: true },
    { label: "Tripwire", value: data.tripwire, pre: true },
    { label: "Reversão de Risco", value: data.riskReversal, pre: true },
    { label: "Prova Social", value: data.socialProof, pre: true },
    {
      label: "Monetização do Checkout",
      value: data.checkoutMonetization,
      pre: true,
    },
    { label: "Funil", value: data.funnel, pre: true },
    { label: "Volume Criativo", value: data.creativeVolume, pre: true },
    { label: "Storytelling", value: data.storytelling, pre: true },
  ];

  return (
    <div>
      <PageTitle>{data.name || `Produto de Sucesso ${data.id}`}</PageTitle>
      <div className="card">
        <div className="card-body p-0">
          <dl className="row mb-0">
            {rows.map((r, idx) => (
              <Fragment key={r.label}>
                <dt
                  className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                >
                  {r.label}
                </dt>
                <dd
                  className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                  style={r.pre ? { whiteSpace: "pre-wrap" } : undefined}
                >
                  {r.value}
                </dd>
              </Fragment>
            ))}
          </dl>
        </div>
      </div>
      <Link className="btn btn-primary mt-3" to={`/success-products/${nextId}`}>
        Próximo Produto
      </Link>
    </div>
  );
}
