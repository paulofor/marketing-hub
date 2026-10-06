import { createServer } from "node:http";
const role = process.argv[2];
if (!["principal", "pde"].includes(role))
  throw new Error("Destino de teste inválido.");
createServer((request, response) => {
  response.setHeader("Content-Type", "application/json");
  response.end(
    JSON.stringify({
      role,
      path: request.url,
      sessionHeaderPresent: Boolean(request.headers["x-mira-session"]),
    }),
  );
}).listen(8080, "0.0.0.0");
