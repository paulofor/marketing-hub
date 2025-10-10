import { useEffect, useMemo, useState } from "react";
import { Sparkles, XCircle } from "lucide-react";
import { useCreatives } from "../../api/creative/useCreatives";
import { useRequestCreatives } from "../../api/experiment/useRequestCreatives";
import "./CriativosTab.css";

interface CreativeLibraryBannerProps {
  experimentId: string;
  requestedCreatives?: number | null;
  onRequestSuccess?: (quantity: number) => void;
  onRequestError?: () => void;
}

type LocalFeedback = {
  variant: "success" | "error";
  message: string;
};

const ICON_SIZE = 16;

export default function CreativeLibraryBanner({
  experimentId,
  requestedCreatives,
  onRequestSuccess,
  onRequestError,
}: CreativeLibraryBannerProps) {
  const requestedCount = requestedCreatives ?? 0;
  const defaultQuantity = useMemo(
    () => Math.max(1, requestedCreatives ?? 1),
    [requestedCreatives],
  );
  const { data } = useCreatives(experimentId);
  const creatives = Array.isArray(data) ? data : [];
  const totalCreatives = creatives.length;
  const requestCreatives = useRequestCreatives(experimentId);
  const [isDialogOpen, setDialogOpen] = useState(false);
  const [quantity, setQuantity] = useState(String(defaultQuantity));
  const [quantityError, setQuantityError] = useState<string | null>(null);
  const [localFeedback, setLocalFeedback] = useState<LocalFeedback | null>(null);

  useEffect(() => {
    if (!isDialogOpen) return;
    setQuantity(String(defaultQuantity));
    setQuantityError(null);
  }, [defaultQuantity, isDialogOpen]);

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
      await requestCreatives.mutateAsync(parsed);
      closeDialog();
      if (onRequestSuccess) {
        onRequestSuccess(parsed);
      } else {
        setLocalFeedback({
          variant: "success",
          message: `Solicitamos ${parsed} ${parsed === 1 ? "criativo" : "criativos"} ao Worker IA.`,
        });
      }
    } catch {
      setQuantityError("Não foi possível enviar o pedido agora. Tente novamente.");
      if (onRequestError) {
        onRequestError();
      } else {
        setLocalFeedback({
          variant: "error",
          message: "Não foi possível solicitar os criativos. Tente novamente em instantes.",
        });
      }
    }
  };

  return (
    <>
      <div className="creative-toolbar">
        <div>
          <h2 className="h5 mb-1">Biblioteca de criativos</h2>
          <div className="d-flex flex-wrap align-items-center gap-2 text-muted small">
            <span className="badge rounded-pill text-bg-primary">
              {totalCreatives} {totalCreatives === 1 ? "item" : "itens"}
            </span>
            <span className="badge rounded-pill text-bg-info">
              Solicitados: {requestedCount}
            </span>
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary d-flex align-items-center gap-2"
            onClick={openDialog}
            disabled={requestCreatives.isPending}
          >
            {requestCreatives.isPending ? (
              <span className="spinner-border spinner-border-sm" role="status" />
            ) : (
              <Sparkles size={ICON_SIZE} />
            )}
            <span>{requestCreatives.isPending ? "Solicitando..." : "Gerar criativos"}</span>
          </button>
        </div>
      </div>

      {localFeedback ? (
        <div
          className={`alert ${
            localFeedback.variant === "success" ? "alert-success" : "alert-danger"
          } d-flex align-items-center justify-content-between`}
          role="alert"
        >
          <span>{localFeedback.message}</span>
          <button
            type="button"
            className="btn-close"
            aria-label="Fechar aviso"
            onClick={() => setLocalFeedback(null)}
          />
        </div>
      ) : null}

      {isDialogOpen ? (
        <div className="modal d-block creative-request-modal" tabIndex={-1} role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Gerar novos criativos</h5>
                <button type="button" className="btn-close" onClick={closeDialog} aria-label="Fechar" />
              </div>
              <div className="modal-body">
                <div className="creative-request-body">
                  <p className="mb-0 text-muted">
                    Informe quantos novos criativos deseja solicitar para este experimento.
                  </p>
                  <div>
                    <label className="form-label" htmlFor="creative-request-quantity">
                      Quantidade de criativos
                    </label>
                    <input
                      id="creative-request-quantity"
                      type="number"
                      min={1}
                      className="form-control"
                      value={quantity}
                      onChange={(event) => setQuantity(event.target.value)}
                      disabled={requestCreatives.isPending}
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
                  disabled={requestCreatives.isPending}
                >
                  {requestCreatives.isPending ? (
                    <span className="spinner-border spinner-border-sm" role="status" />
                  ) : (
                    <Sparkles size={ICON_SIZE} />
                  )}
                  <span>{requestCreatives.isPending ? "Enviando..." : "Solicitar"}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
