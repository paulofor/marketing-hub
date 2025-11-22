const VISITOR_COOKIE_NAME = "marketinghub_visitor_id";
const COOKIE_MAX_AGE_DAYS = 365;

function getCookieValue(name: string): string | undefined {
  const cookies = document.cookie.split(";").map((entry) => entry.trim());
  const target = cookies.find((cookie) => cookie.startsWith(`${name}=`));
  return target?.split("=")[1];
}

function generateVisitorId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function persistCookie(name: string, value: string, maxAgeDays: number) {
  const expires = new Date();
  expires.setDate(expires.getDate() + maxAgeDays);
  document.cookie = `${name}=${value}; expires=${expires.toUTCString()}; path=/; SameSite=Lax`;
}

export function ensureVisitorIdCookie(): string {
  const existing = getCookieValue(VISITOR_COOKIE_NAME);
  if (existing) {
    return existing;
  }
  const visitorId = generateVisitorId();
  persistCookie(VISITOR_COOKIE_NAME, visitorId, COOKIE_MAX_AGE_DAYS);
  return visitorId;
}
