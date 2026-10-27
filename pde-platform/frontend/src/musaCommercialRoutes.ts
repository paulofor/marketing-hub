export const MUSA_V14_EXPERIENCE_VERSION =
  "musa-pde-entry-v14-primeiro-ajuste-aplicavel";

export const MUSA_V15_EXPERIENCE_VERSION = "musa-pde-entry-v15-estilo-pessoal";

export const MUSA_V16_EXPERIENCE_VERSION =
  "musa-pde-entry-v16-modelagem-internacional";

export const MUSA_V17_EXPERIENCE_VERSION =
  "musa-pde-entry-v17-continuidade-clara";

export function musaCommercialPath(version: string) {
  return `/experiences/${encodeURIComponent(version)}`;
}

export function resolveMusaCommercialRoute(pathname: string) {
  return (
    [
      MUSA_V14_EXPERIENCE_VERSION,
      MUSA_V15_EXPERIENCE_VERSION,
      MUSA_V16_EXPERIENCE_VERSION,
      MUSA_V17_EXPERIENCE_VERSION,
    ].find((version) => {
      const prefix = musaCommercialPath(version);
      return pathname === prefix || pathname.startsWith(`${prefix}/`);
    }) || ""
  );
}
