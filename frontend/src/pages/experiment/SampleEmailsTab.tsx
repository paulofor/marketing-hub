import { useMemo, useState } from "react";
import WorkerRequestBanner from "./WorkerRequestBanner";
import { useSampleEmails } from "../../api/sampleEmail/useSampleEmails";
import { useRequestSampleEmails } from "../../api/sampleEmail/useRequestSampleEmails";
import type { SampleEmail } from "../../api/sampleEmail/types";

interface SampleEmailsTabProps {
  experimentId: string;
  requestedSampleEmails?: number | null;
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
}: SampleEmailsTabProps) {
  const { data: emails, isLoading, isError } = useSampleEmails(experimentId);
  const requestSampleEmails = useRequestSampleEmails(experimentId);
  const [copiedEmailId, setCopiedEmailId] = useState<number | null>(null);

  const existingCount = emails?.length ?? 0;
  const defaultQuantity = useMemo(() => {
    if (requestedSampleEmails && requestedSampleEmails > 0) {
      return requestedSampleEmails;
    }
    return Math.max(1, existingCount || 1);
  }, [existingCount, requestedSampleEmails]);

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
              {emails?.map((email) => (
                <article key={email.id} className="border rounded-3 p-3">
                  <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
                    <div>
                      <h6 className="mb-1">{email.subject}</h6>
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
                    <div className="d-flex flex-wrap gap-2">
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
              ))}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
