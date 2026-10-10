import React, { useEffect, useState } from "react";

export type PrivateVideo = {
  assetUrl: string;
  thumbnailUrl?: string;
  vttUrl?: string;
  captions: string;
};

/** Exibe mídia já aprovada como ajuda opcional, sem bloquear a ação principal. */
export function PrivateCycleVideo({
  media,
  label,
  fallback,
}: {
  media?: PrivateVideo;
  label: string;
  fallback: string;
}) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [media?.assetUrl]);
  if (!media) return null;
  return (
    <details className="video-demo">
      <summary>Veja como funciona (opcional)</summary>
      {!failed ? (
        <video
          aria-label={label}
          controls
          playsInline
          preload="none"
          poster={media.thumbnailUrl}
          src={media.assetUrl}
          onError={() => setFailed(true)}
        >
          {media.vttUrl ? (
            <track
              kind="captions"
              srcLang="pt-BR"
              label="Português"
              src={media.vttUrl}
            />
          ) : null}
        </video>
      ) : (
        <p role="status">{fallback}</p>
      )}
      <p>Você pode começar sem assistir.</p>
      <details>
        <summary>Ler a explicação</summary>
        <p>{media.captions.replace(/\|/g, " ")}</p>
      </details>
    </details>
  );
}
