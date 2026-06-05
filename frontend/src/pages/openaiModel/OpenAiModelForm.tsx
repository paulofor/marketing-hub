import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import type { OpenAiModelCatalogPrice } from "../../api/openAiModel/useOpenAiModelCatalog";

export type OpenAiModelFormValues = {
  name: string;
  code: string;
  priceInputStandard: string;
  priceInputCachedStandard: string;
  priceOutputStandard: string;
  priceInputBatch: string;
  priceInputCachedBatch: string;
  priceOutputBatch: string;
  acceptsImageInput: boolean;
};

type OpenAiModelPriceField =
  | "priceInputStandard"
  | "priceInputCachedStandard"
  | "priceOutputStandard"
  | "priceInputBatch"
  | "priceInputCachedBatch"
  | "priceOutputBatch";

interface Props {
  initialValues?: OpenAiModelFormValues;
  onSubmit: (values: OpenAiModelFormValues) => void;
  isSubmitting?: boolean;
  submitLabel?: string;
  nameOnly?: boolean;
  officialModelCodes?: string[];
  officialModelPrices?: Record<string, OpenAiModelCatalogPrice>;
  isLoadingOfficialModels?: boolean;
}

const DEFAULT_VALUES: OpenAiModelFormValues = {
  name: "",
  code: "",
  priceInputStandard: "",
  priceInputCachedStandard: "",
  priceOutputStandard: "",
  priceInputBatch: "",
  priceInputCachedBatch: "",
  priceOutputBatch: "",
  acceptsImageInput: false,
};

export default function OpenAiModelForm({
  initialValues = DEFAULT_VALUES,
  onSubmit,
  isSubmitting = false,
  submitLabel = "Salvar",
  nameOnly = false,
  officialModelCodes = [],
  officialModelPrices = {},
  isLoadingOfficialModels = false,
}: Props) {
  const [values, setValues] = useState<OpenAiModelFormValues>(initialValues);
  const [showSelectionWarning, setShowSelectionWarning] = useState(false);

  useEffect(() => {
    setValues(initialValues);
  }, [initialValues]);

  const handleChange =
    (field: keyof OpenAiModelFormValues) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const nextValue =
        event.target.type === "checkbox"
          ? event.target.checked
          : event.target.value;
      setShowSelectionWarning(false);
      setValues((prev) => ({ ...prev, [field]: nextValue }));
    };

  const normalizedQuery = values.name.trim().toLowerCase();
  const matchingOfficialModels = useMemo(() => {
    if (!nameOnly || !normalizedQuery) return [];
    return officialModelCodes
      .filter((modelCode) => modelCode.toLowerCase().includes(normalizedQuery))
      .sort((left, right) => left.localeCompare(right));
  }, [nameOnly, normalizedQuery, officialModelCodes]);
  const hasExactOfficialModel = matchingOfficialModels.some(
    (modelCode) => modelCode.toLowerCase() === normalizedQuery,
  );
  const mustChooseOfficialModel =
    nameOnly && matchingOfficialModels.length > 0 && !hasExactOfficialModel;

  const formatPrice = (value?: number) => {
    if (value === undefined || value === null) return "—";
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
      maximumFractionDigits: 5,
    }).format(value);
  };

  const renderOfficialModelPricing = (modelCode: string, selected: boolean) => {
    const textClass = selected ? "text-white-50" : "text-body-secondary";
    const pricing = officialModelPrices[modelCode];
    if (!pricing) {
      return (
        <span className={`d-block small ${textClass} mt-1`}>
          Preços oficiais não encontrados para esse modelo na página de preços.
        </span>
      );
    }
    return (
      <span className={`d-flex flex-wrap gap-2 small ${textClass} mt-1`}>
        <span>Standard input: {formatPrice(pricing.priceInputStandard)}</span>
        <span>Standard output: {formatPrice(pricing.priceOutputStandard)}</span>
        <span>Batch input: {formatPrice(pricing.priceInputBatch)}</span>
        <span>Batch output: {formatPrice(pricing.priceOutputBatch)}</span>
      </span>
    );
  };

  const selectOfficialModel = (modelCode: string) => {
    setShowSelectionWarning(false);
    setValues((prev) => ({ ...prev, name: modelCode }));
  };

  const handleSubmit = () => {
    if (mustChooseOfficialModel) {
      setShowSelectionWarning(true);
      return;
    }
    onSubmit(values);
  };

  const submitDisabled = isSubmitting || !values.name.trim();

  const renderPriceField = (
    field: OpenAiModelPriceField,
    label: string,
    helper?: string,
  ) => (
    <div className="col-md-6">
      <label className="form-label fw-semibold" htmlFor={field}>
        {label}
      </label>
      <input
        id={field}
        type="number"
        step="0.00001"
        className="form-control"
        value={values[field]}
        onChange={handleChange(field)}
        min={0}
        placeholder="0.00000"
      />
      {helper ? <small className="text-body-secondary">{helper}</small> : null}
    </div>
  );

  return (
    <div className="card">
      <div className="card-body">
        <div className="row g-3">
          <div className="col-md-6">
            <label className="form-label fw-semibold" htmlFor="name">
              Nome do modelo <span className="text-danger">*</span>
            </label>
            <input
              id="name"
              className="form-control"
              value={values.name}
              onChange={handleChange("name")}
              placeholder="ex: GPT-4o mini"
            />
          </div>
          {!nameOnly ? (
            <div className="col-md-6">
              <label className="form-label fw-semibold" htmlFor="code">
                Código do modelo
              </label>
              <input
                id="code"
                className="form-control"
                value={values.code}
                onChange={handleChange("code")}
                placeholder="ex: gpt-4o-mini"
              />
            </div>
          ) : null}
          {nameOnly && values.name.trim() ? (
            <div className="col-12">
              <div className="border rounded p-3 bg-light">
                <div className="d-flex align-items-center justify-content-between gap-2 flex-wrap mb-2">
                  <strong>Modelos oficiais encontrados</strong>
                  {isLoadingOfficialModels ? (
                    <span className="text-body-secondary small">
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        aria-hidden="true"
                      />
                      Consultando OpenAI...
                    </span>
                  ) : null}
                </div>
                {matchingOfficialModels.length > 0 ? (
                  <div className="d-flex flex-column gap-2">
                    {matchingOfficialModels.map((modelCode) => (
                      <button
                        key={modelCode}
                        type="button"
                        className={`btn text-start ${
                          modelCode.toLowerCase() === normalizedQuery
                            ? "btn-primary"
                            : "btn-outline-primary"
                        }`}
                        onClick={() => selectOfficialModel(modelCode)}
                      >
                        <span className="d-flex align-items-center gap-2 flex-wrap">
                          <code>{modelCode}</code>
                          <span className="small">
                            {modelCode.toLowerCase() === normalizedQuery
                              ? "selecionado"
                              : "selecionar"}
                          </span>
                        </span>
                        {renderOfficialModelPricing(
                          modelCode,
                          modelCode.toLowerCase() === normalizedQuery,
                        )}
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="text-body-secondary mb-0">
                    Nenhum modelo oficial encontrado para esse texto. Verifique
                    o nome ou código antes de salvar.
                  </p>
                )}
                {showSelectionWarning ? (
                  <div className="alert alert-warning mt-3 mb-0">
                    Escolha um modelo oficial da lista antes de inserir no
                    catálogo administrativo.
                  </div>
                ) : null}
              </div>
            </div>
          ) : null}
          {!nameOnly ? (
            <div className="col-12">
              <div className="form-check form-switch">
                <input
                  id="acceptsImageInput"
                  className="form-check-input"
                  type="checkbox"
                  checked={values.acceptsImageInput}
                  onChange={handleChange("acceptsImageInput")}
                />
                <label
                  className="form-check-label fw-semibold"
                  htmlFor="acceptsImageInput"
                >
                  Aceita imagem + prompt
                </label>
              </div>
              <small className="text-body-secondary">
                Marque quando o modelo puder receber imagens como entrada, por
                exemplo para Quality Review visual.
              </small>
            </div>
          ) : (
            <div className="col-12">
              <div className="alert alert-info mb-0">
                Informe somente o nome/código do modelo. Código canônico, preços
                por 1 milhão de tokens e capacidade serão preenchidos pelo
                backend consultando as fontes oficiais da OpenAI.
              </div>
            </div>
          )}
        </div>

        {!nameOnly ? (
          <div className="row g-3 mt-3">
            <div className="col-12">
              <p className="fw-semibold mb-1">
                Preços - modo standard (por 1 milhão de tokens)
              </p>
              <p className="text-body-secondary small mb-0">
                Inclui preços de entrada, entrada com cache e saída.
              </p>
            </div>
            {renderPriceField(
              "priceInputStandard",
              "Preço de input",
              "Tokens de entrada sem cache (USD)",
            )}
            {renderPriceField(
              "priceInputCachedStandard",
              "Preço de input (cacheado)",
              "Tokens de entrada com cache (USD)",
            )}
            {renderPriceField(
              "priceOutputStandard",
              "Preço de output",
              "Tokens de saída (USD)",
            )}
          </div>
        ) : null}

        {!nameOnly ? (
          <div className="row g-3 mt-3">
            <div className="col-12">
              <p className="fw-semibold mb-1">
                Preços - modo batch (por 1 milhão de tokens)
              </p>
              <p className="text-body-secondary small mb-0">
                Valores aplicados às operações em lote.
              </p>
            </div>
            {renderPriceField(
              "priceInputBatch",
              "Preço de input (batch)",
              "Tokens de entrada sem cache (USD)",
            )}
            {renderPriceField(
              "priceInputCachedBatch",
              "Preço de input (cacheado, batch)",
              "Tokens de entrada com cache (USD)",
            )}
            {renderPriceField(
              "priceOutputBatch",
              "Preço de output (batch)",
              "Tokens de saída (USD)",
            )}
          </div>
        ) : null}

        <div className="mt-4">
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={submitDisabled}
          >
            {isSubmitting ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                />
                Salvando...
              </>
            ) : (
              submitLabel
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
