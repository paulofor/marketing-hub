import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";

const priceFormatter = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 5,
  maximumFractionDigits: 5,
});

function formatPrice(value?: number | null) {
  if (value === undefined || value === null) return "-";
  return priceFormatter.format(value);
}

export default function OpenAiModelListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useOpenAiModels();
  const models = Array.isArray(data) ? data : [];

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Modelos da OpenAI</PageTitle>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <p className="mb-0 text-body-secondary">
          Cadastre e acompanhe os preços por 1 milhão de tokens para cada
          modelo.
        </p>
        <Link className="btn btn-primary" to="/openai-models/new">
          Novo modelo
        </Link>
      </div>

      <div className="table-responsive">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Código</th>
              <th colSpan={3}>Standard (USD / 1M tokens)</th>
              <th colSpan={3}>Batch (USD / 1M tokens)</th>
              <th>Ações</th>
            </tr>
            <tr className="text-body-secondary small">
              <th></th>
              <th></th>
              <th>Input</th>
              <th>Input cacheado</th>
              <th>Output</th>
              <th>Input</th>
              <th>Input cacheado</th>
              <th>Output</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {models.map((model) => (
              <tr key={model.id}>
                <td>{model.name}</td>
                <td>{model.code}</td>
                <td>{formatPrice(model.priceInputStandard)}</td>
                <td>{formatPrice(model.priceInputCachedStandard)}</td>
                <td>{formatPrice(model.priceOutputStandard)}</td>
                <td>{formatPrice(model.priceInputBatch)}</td>
                <td>{formatPrice(model.priceInputCachedBatch)}</td>
                <td>{formatPrice(model.priceOutputBatch)}</td>
                <td>
                  <button
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => navigate(`/openai-models/${model.id}/edit`)}
                  >
                    Editar
                  </button>
                </td>
              </tr>
            ))}
            {models.length === 0 ? (
              <tr>
                <td colSpan={9} className="text-center text-body-secondary">
                  Nenhum modelo cadastrado ainda.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}
