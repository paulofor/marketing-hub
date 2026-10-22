export const MUSA_V14_EXPERIENCE_VERSION =
  "musa-pde-entry-v14-primeiro-ajuste-aplicavel";

export function musaCommercialPath(version: string) {
  return `/experiences/${encodeURIComponent(version)}`;
}

export function resolveMusaCommercialRoute(pathname: string) {
  const prefix = musaCommercialPath(MUSA_V14_EXPERIENCE_VERSION);
  return pathname === prefix || pathname.startsWith(`${prefix}/`)
    ? MUSA_V14_EXPERIENCE_VERSION
    : "";
}
