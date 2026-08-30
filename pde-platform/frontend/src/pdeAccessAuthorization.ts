export const PDE_ACCESS_TOKEN_HEADER = "X-PDE-Access-Token";

type AccessHeaderOptions = {
  json?: boolean;
};

/** Monta a autorização da área paga sem transportar o bearer na URL. */
export function pdeAccessHeaders(
  accessToken: string,
  options: AccessHeaderOptions = {},
): Record<string, string> {
  return {
    [PDE_ACCESS_TOKEN_HEADER]: accessToken,
    ...(options.json ? { "Content-Type": "application/json" } : {}),
  };
}
