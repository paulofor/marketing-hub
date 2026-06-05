import { useEffect, useState, type ChangeEvent } from "react";

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
}: Props) {
  const [values, setValues] = useState<OpenAiModelFormValues>(initialValues);

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
      setValues((prev) => ({ ...prev, [field]: nextValue }));
    };

  const handleSubmit = () => onSubmit(values);

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
