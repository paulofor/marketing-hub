interface SubmissionSuccessCardProps {
  name?: string | null;
  email?: string | null;
  title?: string | null;
  message?: string | null;
  showGmailTip?: boolean;
}

export default function SubmissionSuccessCard({
  name,
  email,
  title,
  message,
  showGmailTip = true,
}: SubmissionSuccessCardProps) {
  const normalizedName = name?.trim() || "cliente";
  const normalizedEmail = email?.trim() ?? "";
  const finalTitle = title?.trim() || "Respostas enviadas!";
  const fallbackMessage = normalizedEmail
    ? `Obrigado, ${normalizedName}. Recebemos suas respostas e em breve entraremos em contato pelo e-mail ${normalizedEmail}.`
    : `Obrigado, ${normalizedName}. Recebemos suas respostas e em breve entraremos em contato.`;
  const finalMessage = message?.trim() || fallbackMessage;
  const isGmailAddress = showGmailTip && /@gmail\.com$/i.test(normalizedEmail);

  return (
    <div className="thank-you-card">
      <h2>{finalTitle}</h2>
      <p>{finalMessage}</p>
      {isGmailAddress ? (
        <p className="gmail-tip">
          Se você usa Gmail, confira também a pasta <strong>Todos os e-mails</strong> ou a aba de
          <strong> Promoções</strong>, pois nossa mensagem pode ser direcionada para lá.
        </p>
      ) : null}
      <p>Você pode fechar esta página com segurança.</p>
    </div>
  );
}
