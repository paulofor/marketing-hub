// Backend simulado e isolado para validar a imagem do conciliador sem credenciais externas.
import http from "node:http";
let dispatched = false;
let polls = 0;
http
  .createServer(async (request, response) => {
    response.setHeader("Content-Type", "application/json");
    if (request.url === "/health")
      return response.end(JSON.stringify({ dispatched, polls }));
    if (request.headers["x-process-worker-token"] !== "process-fixture-only") {
      response.statusCode = 401;
      return response.end("{}");
    }
    if (
      request.url.startsWith(
        "/api/internal/business-processes/automation/v1/stage-executions/pending?",
      )
    ) {
      polls++;
      return response.end(JSON.stringify(dispatched ? [] : [92001]));
    }
    if (
      request.url ===
        "/api/internal/business-processes/automation/v1/stage-executions/92001/reconcile" &&
      request.method === "POST"
    ) {
      dispatched = true;
      return response.end(
        JSON.stringify({ id: 92001, status: "WAITING_ACTIVITY" }),
      );
    }
    response.statusCode = 404;
    response.end("{}");
  })
  .listen(8000, "0.0.0.0");
