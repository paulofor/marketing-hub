import { request } from "node:http";

// Mantém o prazo ativo até consumir a resposta inteira, inclusive em sockets reutilizados.
export function httpProbe(
  address,
  { method = "GET", headers = {}, body, agent = false, timeoutMs = 10000 } = {},
) {
  return new Promise((resolve, reject) => {
    const label = `${method} ${new URL(address).pathname}`;
    let settled = false;
    const req = request(address, { method, headers, agent });
    const deadline = setTimeout(() => {
      const error = new Error(`Timeout HTTP após ${timeoutMs} ms: ${label}`);
      finish(error);
      req.destroy(error);
    }, timeoutMs);

    function finish(error, result) {
      if (settled) return;
      settled = true;
      clearTimeout(deadline);
      if (error) reject(error);
      else resolve(result);
    }

    req.on("error", (error) =>
      finish(new Error(`Falha HTTP: ${label}`, { cause: error })),
    );
    req.on("response", (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("error", (error) =>
        finish(
          new Error(`Resposta HTTP interrompida: ${label}`, { cause: error }),
        ),
      );
      response.on("aborted", () =>
        finish(new Error(`Resposta HTTP abortada: ${label}`)),
      );
      response.on("close", () => {
        if (!response.complete)
          finish(new Error(`Resposta HTTP incompleta: ${label}`));
      });
      response.on("end", () =>
        finish(null, {
          status: response.statusCode,
          headers: response.headers,
          body: Buffer.concat(chunks).toString(),
        }),
      );
    });
    req.end(body);
  });
}
