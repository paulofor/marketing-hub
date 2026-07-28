import { type CSSProperties, useEffect, useRef } from 'react';
import Hls from 'hls.js';

type AdaptiveVideoPlayerProps = {
  src: string;
  poster?: string;
  className?: string;
  style?: CSSProperties;
  controls?: boolean;
  autoPlay?: boolean;
  muted?: boolean;
  loop?: boolean;
  playsInline?: boolean;
  preload?: 'none' | 'metadata' | 'auto';
  onPlaybackEvent?: (event: VideoPlaybackEvent) => void;
};

export type VideoPlaybackEvent = {
  type: 'play' | 'progress' | 'ended' | 'error';
  currentTime: number;
  duration: number;
  percent?: number;
};

function isHlsSource(src: string) {
  return src.includes('.m3u8');
}

function readPlaybackState(video: HTMLVideoElement, percent?: number): Omit<VideoPlaybackEvent, 'type'> {
  return {
    currentTime: Number.isFinite(video.currentTime) ? video.currentTime : 0,
    duration: Number.isFinite(video.duration) ? video.duration : 0,
    percent,
  };
}

export function AdaptiveVideoPlayer({
  src,
  poster,
  className,
  style,
  controls = false,
  autoPlay = false,
  muted = false,
  loop = false,
  playsInline = true,
  preload = 'metadata',
  onPlaybackEvent,
}: AdaptiveVideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const playbackEventRef = useRef(onPlaybackEvent);
  const progressMarksRef = useRef(new Set<number>());

  useEffect(() => {
    playbackEventRef.current = onPlaybackEvent;
  }, [onPlaybackEvent]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !src) {
      return undefined;
    }
    progressMarksRef.current.clear();

    if (!isHlsSource(src)) {
      playbackEventRef.current?.({ type: 'error', ...readPlaybackState(video) });
      return undefined;
    }

    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src;
      if (autoPlay) {
        video.play().catch(() => undefined);
      }
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
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        if (autoPlay) {
          video.play().catch(() => undefined);
        }
      });
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal) {
          playbackEventRef.current?.({ type: 'error', ...readPlaybackState(video) });
          hls.destroy();
          destroyed = true;
        }
      });
      return () => {
        if (!destroyed) {
          hls.destroy();
        }
      };
    }

    playbackEventRef.current?.({ type: 'error', ...readPlaybackState(video) });
    return undefined;
  }, [autoPlay, src]);

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
      onPlay={(event) => {
        onPlaybackEvent?.({ type: 'play', ...readPlaybackState(event.currentTarget) });
      }}
      onTimeUpdate={(event) => {
        const video = event.currentTarget;
        if (!Number.isFinite(video.duration) || video.duration <= 0) {
          return;
        }
        const percent = Math.min(100, Math.floor((video.currentTime / video.duration) * 100));
        [25, 50, 75, 95].forEach((mark) => {
          if (percent >= mark && !progressMarksRef.current.has(mark)) {
            progressMarksRef.current.add(mark);
            onPlaybackEvent?.({ type: 'progress', ...readPlaybackState(video, mark) });
          }
        });
      }}
      onEnded={(event) => {
        onPlaybackEvent?.({ type: 'ended', ...readPlaybackState(event.currentTarget, 100) });
      }}
      onError={(event) => {
        onPlaybackEvent?.({ type: 'error', ...readPlaybackState(event.currentTarget) });
      }}
    />
  );
}
