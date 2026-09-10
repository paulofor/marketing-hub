import { createServer } from "node:http";

const server = createServer(async (request, response) => {
  if (request.url === "/api/pde/access/materials/authorize") {
    response.writeHead(
      request.headers["x-pde-access-token"] === "local-evidence" ? 204 : 403,
    );
    response.end();
    return;
  }
  let body = "";
  for await (const chunk of request) body += chunk;
  response.writeHead(200, { "content-type": "application/json" });
  response.end(
    JSON.stringify({
      generation: process.env.GENERATION,
      url: request.url,
      method: request.method,
      body,
    }),
  );
}).listen(8096, "0.0.0.0");

// Mantém o IP antigo ocupado sem a API, reproduzindo conexão recusada após recriação do backend.
process.on("SIGUSR2", () => {
  server.close();
  server.closeAllConnections();
  setInterval(() => {}, 1000);
});
