/// <reference types="vite/client" />

interface Window {
  __MUSA_RUNTIME_CONFIG__?: {
    VITE_MUSA_CHECKOUT_URL?: string;
    VITE_GOOGLE_CLIENT_ID?: string;
    VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE?: string;
    VITE_MUSA_HERO_VIDEO_URL?: string;
    VITE_MUSA_HERO_STREAM_URL?: string;
  };
}
