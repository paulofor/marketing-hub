import { type CSSProperties, useEffect, useRef } from "react";
import Hls from "hls.js";

type AdaptiveVideoPlayerProps = {
  src: string;
  fallbackSrc?: string;
  poster?: string;
  className?: string;
  style?: CSSProperties;
  controls?: boolean;
  autoPlay?: boolean;
  muted?: boolean;
  loop?: boolean;
  playsInline?: boolean;
  preload?: "none" | "metadata" | "auto";
};

function isHlsSource(src: string) {
  return src.includes(".m3u8");
}

/** Reproduz video HLS adaptativo quando disponivel e usa MP4 como fallback. */
export function AdaptiveVideoPlayer({
  src,
  fallbackSrc,
  poster,
  className,
  style,
  controls = true,
  autoPlay = false,
  muted = false,
  loop = false,
  playsInline = true,
  preload = "metadata",
}: AdaptiveVideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !src) {
      return undefined;
    }

    if (!isHlsSource(src)) {
      video.src = src;
      return undefined;
    }

    if (video.canPlayType("application/vnd.apple.mpegurl")) {
      video.src = src;
      return undefined;
    }

    if (Hls.isSupported()) {
      let destroyed = false;
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
      });
      hls.loadSource(src);
      hls.attachMedia(video);
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal && fallbackSrc) {
          hls.destroy();
          destroyed = true;
          video.src = fallbackSrc;
        }
      });
      return () => {
        if (!destroyed) {
          hls.destroy();
        }
      };
    }

    if (fallbackSrc) {
      video.src = fallbackSrc;
    }
    return undefined;
  }, [fallbackSrc, src]);

  return (
    <video
      ref={videoRef}
      className={className}
      style={style}
      poster={poster}
      controls={controls}
      autoPlay={autoPlay}
      muted={muted}
      loop={loop}
      playsInline={playsInline}
      preload={preload}
    />
  );
}
