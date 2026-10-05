import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import readline from "node:readline";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

// Exercita o processo MCP e seu backend HTTP, sem modelo, credencial ou dados reais.
for (const execution of ["510", "920"]) {
  test(`leituras anotadas recuperam contexto e memória da execução ${execution}`, async () => {
    const requests = [];
    let sourceUnavailable = false;
    const backend = http.createServer((req, res) => {
      requests.push({ method: req.method, url: req.url });
      res.setHeader("Content-Type", "application/json");
      if (sourceUnavailable) {
        res.statusCode = 503;
        res.end(
          JSON.stringify({
            message: "Fonte sintética temporariamente indisponível",
          }),
        );
        return;
      }
      res.end(
        JSON.stringify({
          id: execution,
          financialSnapshot: {
            commercialContext: {
              nextAction: "Autorização sintética somente para IA.",
            },
          },
          memories: [],
        }),
      );
    });
    await new Promise((r) => backend.listen(0, "127.0.0.1", r));
    const child = spawn(
      process.execPath,
      [
        fileURLToPath(
          new URL(
            "../../main/resources/mcp/financial-agent.mjs",
            import.meta.url,
          ),
        ),
      ],
      {
        env: {
          ...process.env,
          MCP_BACKEND_URL: `http://127.0.0.1:${backend.address().port}`,
          MCP_EXECUTION_ID: execution,
        },
        stdio: ["pipe", "pipe", "pipe"],
      },
    );
    const pending = new Map();
    let id = 0;
    child.stderr.resume();
    const lines = readline.createInterface({ input: child.stdout });
    lines.on("line", (line) => {
      const msg = JSON.parse(line);
      const p = pending.get(msg.id);
      if (p) {
        pending.delete(msg.id);
        msg.error
          ? p.reject(new Error(msg.error.message))
          : p.resolve(msg.result);
      }
    });
    const call = (method, params) =>
      new Promise((resolve, reject) => {
        const key = ++id;
        pending.set(key, { resolve, reject });
        child.stdin.write(
          JSON.stringify({ jsonrpc: "2.0", id: key, method, params }) + "\n",
        );
      });
    try {
      assert.equal((await call("initialize", {})).serverInfo.version, "1.2.0");
      const { tools } = await call("tools/list", {});
      for (const name of [
        "consultar_execucao_financeira",
        "recuperar_memoria_especializada",
      ]) {
        const tool = tools.find((t) => t.name === name);
        assert.deepEqual(tool.annotations, {
          readOnlyHint: true,
          destructiveHint: false,
          openWorldHint: false,
        });
        const result = await call("tools/call", {
          name,
          arguments:
            name === "recuperar_memoria_especializada"
              ? {
                  scopeType: "COMMERCIAL_PLAN",
                  scopeId: String(Number(execution) + 1),
                }
              : {},
        });
        assert.equal(JSON.parse(result.content[0].text).id, execution);
      }
      assert.equal(
        tools.find((t) => t.name === "registrar_aprendizado_candidato")
          .annotations.readOnlyHint,
        false,
      );
      await assert.rejects(
        call("tools/call", {
          name: "executar_shell",
          arguments: { command: "id" },
        }),
        /não permitida/,
      );
      assert.equal(requests.length, 2);
      assert(requests.every((r) => r.method === "GET"));
      assert.equal(
        requests[0].url,
        `/api/financial-agent/v1/internal/executions/${execution}`,
      );
      assert(requests[1].url.includes("/agents/financial-agent?"));
      assert(
        requests[1].url.includes("scopeId=" + String(Number(execution) + 1)),
      );
      // Uma falha de integração não pode parecer uma consulta bem-sucedida sem fonte.
      sourceUnavailable = true;
      await assert.rejects(
        call("tools/call", {
          name: "consultar_execucao_financeira",
          arguments: {},
        }),
        /HTTP 503/,
      );
      assert.equal(requests.length, 3);
      assert(requests.every((r) => r.method === "GET"));
    } finally {
      child.kill();
      lines.close();
      await new Promise((r) => backend.close(r));
    }
  });
}
