import { useEffect, useMemo, useState } from "react";
import { Sparkles, XCircle } from "lucide-react";
import "./CriativosTab.css";

interface WorkerRequestBannerProps {
  title: string;
  subtitle: string;
  resourceName: string;
  resourceNamePlural?: string;
  buttonLabel?: string;
  requestedCount?: number | null;
  existingCount?: number | null;
  existingLabel?: string;
  defaultQuantity?: number;
  helperText?: string;
  successMessage?: (quantity: number) => string;
  errorMessage?: string;
  onRequest: (quantity: number) => Promise<unknown>;
  isRequesting: boolean;
}

type LocalFeedback = {
  variant: "success" | "error";
  message: string;
};

const ICON_SIZE = 16;

export default function WorkerRequestBanner({
  title,
  subtitle,
  resourceName,
  resourceNamePlural,
  buttonLabel,
  requestedCount,
  existingCount,
  existingLabel = "Disponíveis",
  defaultQuantity = 1,
  helperText,
  successMessage,
  errorMessage,
  onRequest,
  isRequesting,
}: WorkerRequestBannerProps) {
  const plural = resourceNamePlural ?? `${resourceName}s`;
  const requested = requestedCount ?? 0;
  const existing = existingCount ?? 0;
  const [isDialogOpen, setDialogOpen] = useState(false);
  const [quantity, setQuantity] = useState(String(defaultQuantity));
  const [quantityError, setQuantityError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<LocalFeedback | null>(null);

  const effectiveDefaultQuantity = useMemo(() => {
    const safe = Number.isFinite(defaultQuantity) && (defaultQuantity ?? 0) > 0;
    return safe ? Number(defaultQuantity) : 1;
  }, [defaultQuantity]);

  useEffect(() => {
    if (!isDialogOpen) return;
    setQuantity(String(effectiveDefaultQuantity));
    setQuantityError(null);
  }, [effectiveDefaultQuantity, isDialogOpen]);

  const openDialog = () => {
    setDialogOpen(true);
  };

  const closeDialog = () => {
    setDialogOpen(false);
    setQuantityError(null);
  };

  const handleSubmit = async () => {
    const parsed = Number.parseInt(quantity, 10);
    if (!Number.isInteger(parsed) || parsed <= 0) {
      setQuantityError("Informe um número válido maior que zero.");
      return;
    }

    setQuantityError(null);
    try {
      await onRequest(parsed);
      closeDialog();
      setFeedback({
        variant: "success",
        message: successMessage
          ? successMessage(parsed)
          : `Solicitamos ${parsed} ${parsed === 1 ? resourceName : plural} ao Worker IA.`,
      });
    } catch {
      setQuantityError("Não foi possível enviar o pedido agora. Tente novamente.");
      setFeedback({
        variant: "error",
        message:
          errorMessage ?? `Não foi possível solicitar ${plural}. Tente novamente em instantes.`,
      });
    }
  };

  return (
    <>
      <div className="creative-toolbar">
        <div>
          <h2 className="h5 mb-1">{title}</h2>
          <p className="text-muted mb-2 small">{subtitle}</p>
          <div className="d-flex flex-wrap align-items-center gap-2 text-muted small">
            <span className="badge rounded-pill text-bg-primary">
              {existingLabel}: {existing}
            </span>
            <span className="badge rounded-pill text-bg-info">
              Solicitados: {requested}
            </span>
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary d-flex align-items-center gap-2"
            onClick={openDialog}
            disabled={isRequesting}
          >
            {isRequesting ? (
              <span className="spinner-border spinner-border-sm" role="status" />
            ) : (
              <Sparkles size={ICON_SIZE} />
            )}
            <span>{isRequesting ? "Solicitando..." : buttonLabel ?? `Gerar ${plural}`}</span>
          </button>
        </div>
      </div>

      {feedback ? (
        <div
          className={`alert ${
            feedback.variant === "success" ? "alert-success" : "alert-danger"
          } d-flex align-items-center justify-content-between`}
          role="alert"
        >
          <span>{feedback.message}</span>
          <button
            type="button"
            className="btn-close"
            aria-label="Fechar aviso"
            onClick={() => setFeedback(null)}
          />
        </div>
      ) : null}

      {isDialogOpen ? (
        <div className="modal d-block creative-request-modal" tabIndex={-1} role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{`Gerar ${plural}`}</h5>
                <button type="button" className="btn-close" onClick={closeDialog} aria-label="Fechar" />
              </div>
              <div className="modal-body">
                <div className="creative-request-body">
                  {helperText ? <p className="mb-0 text-muted">{helperText}</p> : null}
                  <div>
                    <label className="form-label" htmlFor="worker-request-quantity">
                      {`Quantidade de ${plural}`}
                    </label>
                    <input
                      id="worker-request-quantity"
                      type="number"
                      min={1}
                      className="form-control"
                      value={quantity}
                      onChange={(event) => setQuantity(event.target.value)}
                      disabled={isRequesting}
                    />
                  </div>
                  {quantityError ? (
                    <div className="creative-request-error" role="alert">
                      <XCircle size={18} />
                      <span>{quantityError}</span>
                    </div>
                  ) : null}
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-outline-secondary" onClick={closeDialog}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-primary d-flex align-items-center gap-2"
                  onClick={handleSubmit}
                  disabled={isRequesting}
                >
                  {isRequesting ? (
                    <span className="spinner-border spinner-border-sm" role="status" />
                  ) : (
                    <Sparkles size={ICON_SIZE} />
                  )}
                  <span>{isRequesting ? "Enviando..." : "Solicitar"}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
