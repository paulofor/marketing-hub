import { FormEvent, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { apiBaseUrl } from "../config/api";
import type { FacebookAccount } from "../api/useFacebookAccounts";

/** Mantém a credencial somente durante o envio, fora de caches e históricos de mutações. */
export default function FacebookTokenUpdateDialog({
  account,
  onClose,
  onSaved,
}: {
  account: FacebookAccount;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const submitting = useRef(false);
  const [token, setToken] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const queryClient = useQueryClient();

  useEffect(() => {
    dialog.current?.showModal();
    dialog.current?.querySelector("input")?.focus();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const candidate = token.trim();
    if (submitting.current || !candidate) return;
    submitting.current = true;
    setPending(true);
    setError("");
    setToken("");
    try {
      // fetch evita reter o segredo em AxiosError ou nas variáveis do MutationCache.
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/facebook/${account.id}/token`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ accessToken: candidate }),
          cache: "no-store",
          redirect: "error",
        },
      );
      const result = await response.json().catch(() => null);
      if (!response.ok) {
        // Apenas mensagens conhecidas: nenhum texto arbitrário de proxy/Meta volta à tela.
        const safeMessages: Record<number, string> = {
          400: "Cole somente o token de usuário, sem URL, aspas ou prefixo Bearer.",
          404: "Conta Meta não encontrada. Atualize a página.",
          409: "Confira App ID, App Secret e conta de anúncios. Se a conta mudou durante a validação, atualize a página.",
          422: "A Meta não confirmou o token. Confira a validade, o aplicativo, o titular e as permissões ads_read e ads_management.",
          503: "A Meta está indisponível. A credencial anterior foi preservada; tente novamente.",
        };
        setError(
          safeMessages[response.status] ??
            "Não foi possível confirmar a atualização. Atualize a página e confira o estado antes de reenviar.",
        );
        return;
      }
      if (result?.accountId !== account.id || !result?.validatedAt) {
        setError(
          "A confirmação não pôde ser lida. Atualize a página e confira o estado antes de reenviar.",
        );
        return;
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
        queryClient.invalidateQueries({
          queryKey: ["facebook-configuration-status"],
        }),
      ]);
      onSaved(
        `Token validado e atualizado para ${account.name}. ${result.tokenRenewalEnabled ? "A renovação automática continua habilitada." : "A renovação automática permanece desativada na configuração da conta."}`,
      );
      onClose();
    } catch {
      setError(
        "A conexão foi interrompida. Atualize a página e confira o estado antes de reenviar o token.",
      );
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <dialog
      ref={dialog}
      aria-labelledby="meta-token-title"
      className="border rounded shadow p-4"
      style={{
        width: "min(94vw, 560px)",
        maxHeight: "90dvh",
        overflowY: "auto",
      }}
      onCancel={(event) => {
        if (pending) event.preventDefault();
        else onClose();
      }}
    >
      <form onSubmit={submit} autoComplete="off">
        <h2 id="meta-token-title" className="h5">
          Atualizar token da Meta
        </h2>
        <p className="mb-2">
          <strong>{account.name}</strong> · Conta de anúncios{" "}
          {account.adAccountId ?? "não cadastrada"}
        </p>
        <p className="small">
          No{" "}
          <a
            href="https://developers.facebook.com/tools/explorer/"
            target="_blank"
            rel="noopener noreferrer"
          >
            Graph API Explorer
          </a>
          , selecione o aplicativo{" "}
          <strong>{account.appId ?? "cadastrado na conta"}</strong>, gere um
          token de usuário com <strong>ads_read</strong> e{" "}
          <strong>ads_management</strong> e cole abaixo.
        </p>
        <label htmlFor="new-meta-token" className="form-label">
          Novo token de usuário
        </label>
        <input
          id="new-meta-token"
          type="password"
          className="form-control"
          value={token}
          onChange={(event) => setToken(event.target.value)}
          required
          minLength={8}
          maxLength={8192}
          autoComplete="new-password"
          autoCapitalize="none"
          spellCheck={false}
          autoFocus
          disabled={pending}
          aria-describedby="meta-token-help"
        />
        <p id="meta-token-help" className="form-text">
          A nova credencial só substitui a anterior após validação. Este envio
          não inicia campanhas nem altera orçamento.
        </p>
        {error && (
          <p role="alert" className="alert alert-danger">
            {error}
          </p>
        )}
        <div className="d-flex flex-wrap gap-2 mt-3">
          <button
            className="btn btn-primary"
            type="submit"
            disabled={pending || !token.trim()}
          >
            {pending ? "Validando na Meta…" : "Validar e salvar token"}
          </button>
          <button
            className="btn btn-outline-secondary"
            type="button"
            disabled={pending}
            onClick={onClose}
          >
            Cancelar
          </button>
        </div>
      </form>
    </dialog>
  );
}
