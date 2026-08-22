/// <reference types="vite/client" />

interface Window {
  __MUSA_RUNTIME_CONFIG__?: {
    VITE_MUSA_CHECKOUT_URL?: string;
    VITE_GOOGLE_CLIENT_ID?: string;
    VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE?: string;
    VITE_MUSA_HERO_VIDEO_URL?: string;
    VITE_MUSA_HERO_STREAM_URL?: string;
    VITE_PDE_PRODUCT_SLUG?: string;
  };
}

interface ImportMetaEnv {
  readonly VITE_MUSA_CHECKOUT_URL?: string;
  readonly VITE_GOOGLE_CLIENT_ID?: string;
  readonly VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE?: string;
  readonly VITE_MUSA_HERO_VIDEO_URL?: string;
  readonly VITE_MUSA_HERO_STREAM_URL?: string;
  readonly VITE_PDE_PRODUCT_SLUG?: string;
  readonly VITE_PDE_ENABLE_DEV_ACCESS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
