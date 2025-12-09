import { useMemo, useState } from "react";
import WorkerRequestBanner from "./WorkerRequestBanner";
import { useSampleEmails } from "../../api/sampleEmail/useSampleEmails";
import { useRequestSampleEmails } from "../../api/sampleEmail/useRequestSampleEmails";
import { useSelectSampleEmail } from "../../api/sampleEmail/useSelectSampleEmail";
import type { SampleEmail } from "../../api/sampleEmail/types";

interface SampleEmailsTabProps {
  experimentId: string;
  requestedSampleEmails?: number | null;
  selectedSampleEmailId?: number | null;
  selectedSampleEmailSubject?: string | null;
  selectedSampleEmailUpdatedAt?: string | null;
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function renderBody(body?: string | null) {
  if (!body || !body.trim()) {
    return <p className="text-muted mb-0">Sem corpo gerado pelo worker.</p>;
  }
  const paragraphs = body.split(/\n{2,}/);
  return (
    <div className="d-flex flex-column gap-2">
      {paragraphs.map((paragraph, index) => {
        const lines = paragraph.split(/\n/);
        return (
          <p key={`paragraph-${index}`} className="mb-0">
            {lines.map((line, lineIndex) => (
              <span key={`line-${index}-${lineIndex}`}>
                {line}
                {lineIndex < lines.length - 1 ? <br /> : null}
              </span>
            ))}
          </p>
        );
      })}
    </div>
  );
}

function buildClipboardContent(email: SampleEmail) {
  const sections: string[] = [];
  sections.push(`Assunto: ${email.subject}`);
  if (email.previewText) {
    sections.push(`Prévia: ${email.previewText}`);
  }
  if (email.body) {
    sections.push(`Corpo:\n${email.body}`);
  }
  if (email.callToAction) {
    sections.push(`CTA sugerido: ${email.callToAction}`);
  }
  return sections.join("\n\n");
}

export default function SampleEmailsTab({
  experimentId,
  requestedSampleEmails,
  selectedSampleEmailId,
  selectedSampleEmailSubject,
  selectedSampleEmailUpdatedAt,
}: SampleEmailsTabProps) {
  const { data: emails, isLoading, isError } = useSampleEmails(experimentId);
  const requestSampleEmails = useRequestSampleEmails(experimentId);
  const selectSampleEmail = useSelectSampleEmail(experimentId);
  const [copiedEmailId, setCopiedEmailId] = useState<number | null>(null);

  const existingCount = emails?.length ?? 0;
  const defaultQuantity = useMemo(() => {
    if (requestedSampleEmails && requestedSampleEmails > 0) {
      return requestedSampleEmails;
    }
    return Math.max(1, existingCount || 1);
  }, [existingCount, requestedSampleEmails]);

  const selectedEmailFromList = useMemo(
    () => emails?.find((item) => item.selected) ?? null,
    [emails],
  );

  const currentSelectedId = selectedEmailFromList?.id ?? selectedSampleEmailId ?? null;
  const currentSelectedSubject =
    selectedEmailFromList?.subject ?? selectedSampleEmailSubject ?? null;
  const currentSelectedUpdatedAt =
    selectedEmailFromList?.updatedAt ?? selectedSampleEmailUpdatedAt ?? null;

  const handleCopy = async (email: SampleEmail) => {
    if (typeof navigator === "undefined" || !navigator.clipboard) {
      console.warn("Clipboard API indisponível para copiar e-mail de amostra");
      return;
    }
    try {
      await navigator.clipboard.writeText(buildClipboardContent(email));
      setCopiedEmailId(email.id);
      window.setTimeout(() => setCopiedEmailId(null), 2000);
    } catch (error) {
      console.error("Falha ao copiar e-mail de amostra", error);
    }
  };

  const handleCopyPrompt = async (prompt?: string | null) => {
    if (!prompt) return;
    if (typeof navigator === "undefined" || !navigator.clipboard) {
      console.warn("Clipboard API indisponível para copiar prompt");
      return;
    }
    try {
      await navigator.clipboard.writeText(prompt);
    } catch (error) {
      console.error("Falha ao copiar prompt do e-mail de amostra", error);
    }
  };

  const handleSelectEmail = async (emailId: number) => {
    try {
      await selectSampleEmail.mutateAsync(emailId);
    } catch (error) {
      console.error("Não foi possível registrar o e-mail selecionado", error);
    }
  };

  const handleClearSelection = async () => {
    try {
      await selectSampleEmail.mutateAsync(null);
    } catch (error) {
      console.error("Não foi possível remover a seleção do e-mail", error);
    }
  };

  const isMutatingSelection = selectSampleEmail.isPending;

  return (
    <div className="mt-3">
      <WorkerRequestBanner
        title="E-mails para envio de amostras"
        subtitle="Peça ao Worker IA textos prontos para compartilhar as imagens com marca d'água e orientar o lead sobre a compra do pacote completo."
        resourceName="e-mail"
        resourceNamePlural="e-mails"
        existingLabel="E-mails gerados"
        existingCount={existingCount}
        requestedCount={requestedSampleEmails}
        defaultQuantity={defaultQuantity}
        helperText="Informe quantos e-mails deseja gerar para acompanhar o envio das amostras."
        buttonLabel="Gerar e-mails de amostra"
        onRequest={(quantity) => requestSampleEmails.mutateAsync(quantity)}
        isRequesting={requestSampleEmails.isPending}
      />

      <section className="card mt-4">
        <div className="card-body d-flex flex-column flex-lg-row justify-content-between gap-3">
          <div>
            <h5 className="mb-1">E-mail selecionado para o experimento</h5>
            {currentSelectedId ? (
              <>
                <p className="mb-1 fw-semibold">{currentSelectedSubject}</p>
                {currentSelectedUpdatedAt ? (
                  <p className="text-muted small mb-0">
                    Atualizado em {formatDateTime(currentSelectedUpdatedAt)}
                  </p>
                ) : null}
              </>
            ) : (
              <p className="text-muted mb-0">
                Ainda não há um e-mail escolhido. Selecione abaixo qual modelo será utilizado ao
                enviar as imagens com marca d'água para o lead.
              </p>
            )}
          </div>
          <div className="d-flex gap-2 align-items-start">
            {currentSelectedId ? (
              <button
                type="button"
                className="btn btn-outline-danger"
                onClick={handleClearSelection}
                disabled={isMutatingSelection}
              >
                {isMutatingSelection ? "Atualizando..." : "Remover seleção"}
              </button>
            ) : null}
          </div>
        </div>
      </section>

      <section className="card mt-4">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <div>
              <h5 className="mb-1">E-mails gerados</h5>
              <p className="text-muted mb-0">
                Utilize os textos abaixo ao enviar o arquivo ZIP com as imagens contendo marca d'água.
              </p>
            </div>
            <span className="badge text-bg-secondary">{existingCount} e-mail(s)</span>
          </div>

          {isLoading ? <p className="mb-0">Carregando e-mails de amostra...</p> : null}
          {isError ? (
            <p className="text-danger mb-0">Não foi possível carregar os e-mails de amostra deste experimento.</p>
          ) : null}

          {!isLoading && !isError && existingCount === 0 ? (
            <p className="text-muted mb-0">
              Nenhum e-mail de amostra foi gerado ainda. Solicite uma nova leva para orientar o envio das imagens.
            </p>
          ) : null}

          {!isLoading && !isError && existingCount > 0 ? (
            <div className="d-flex flex-column gap-4">
              {emails?.map((email) => {
                const isSelected = email.selected || email.id === currentSelectedId;
                const articleClass = `border rounded-3 p-3 ${isSelected ? "border-success" : "border-light"}`;
                return (
                  <article key={email.id} className={articleClass}>
                    <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
                      <div className="d-flex flex-column gap-1">
                        <h6 className="mb-0">{email.subject}</h6>
                        {email.previewText ? (
                          <p className="text-muted mb-0">Prévia: {email.previewText}</p>
                        ) : null}
                      </div>
                      <div className="text-muted small text-md-end">
                        <div>Gerado em {formatDateTime(email.createdAt)}</div>
                        {email.model ? <div>Modelo: {email.model}</div> : null}
                      </div>
                    </div>

                    <hr />
                    <div className="d-flex flex-column gap-3">
                      <div>
                        <span className="fw-semibold d-block mb-1">Corpo do e-mail</span>
                        {renderBody(email.body)}
                      </div>
                      {email.callToAction ? (
                        <div>
                          <span className="fw-semibold d-block mb-1">CTA sugerido</span>
                          <p className="mb-0">{email.callToAction}</p>
                        </div>
                      ) : null}
                      <div className="d-flex flex-wrap gap-2 align-items-center">
                        {isSelected ? (
                          <span className="badge text-bg-success">Selecionado</span>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            onClick={() => handleSelectEmail(email.id)}
                            disabled={isMutatingSelection}
                          >
                            {isMutatingSelection ? "Atualizando..." : "Selecionar este e-mail"}
                          </button>
                        )}
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => handleCopy(email)}
                        >
                          {copiedEmailId === email.id ? "Copiado!" : "Copiar conteúdo"}
                        </button>
                        {email.prompt ? (
                          <button
                            type="button"
                            className="btn btn-link btn-sm"
                            onClick={() => handleCopyPrompt(email.prompt)}
                          >
                            Copiar prompt
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
