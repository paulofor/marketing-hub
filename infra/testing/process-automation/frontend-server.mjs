import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { resolve, extname } from "node:path";
const root = resolve("frontend/dist");
const mime = {
  ".js": "text/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".json": "application/json",
};
createServer(async (req, res) => {
  try {
    if (req.url.startsWith("/api/") || req.url.startsWith("/fixture/")) {
      // O bundle usa a porta 80; o navegador de teste redireciona somente para este proxy local.
      const cors = {
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "GET,HEAD,OPTIONS,POST",
        "access-control-allow-headers": "content-type,x-process-worker-token",
      };
      if (req.method === "OPTIONS") {
        res.writeHead(204, cors);
        res.end();
        return;
      }
      let input = "";
      for await (const chunk of req) input += chunk;
      const response = await fetch("http://127.0.0.1:18092" + req.url, {
        method: req.method,
        headers: {
          "content-type": "application/json",
          ...(req.headers["x-process-worker-token"]
            ? {
                "X-Process-Worker-Token": req.headers["x-process-worker-token"],
              }
            : {}),
        },
        body: ["GET", "HEAD"].includes(req.method) ? undefined : input,
      });
      res.writeHead(response.status, {
        ...cors,
        "content-type":
          response.headers.get("content-type") || "application/json",
      });
      res.end(Buffer.from(await response.arrayBuffer()));
      return;
    }
    let path = resolve(
      root,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!path.startsWith(root + "/")) path = root + "/index.html";
    let data;
    try {
      data = await readFile(path);
    } catch {
      path = root + "/index.html";
      data = await readFile(path);
    }
    res.writeHead(200, {
      "content-type": mime[extname(path)] || "application/octet-stream",
    });
    res.end(data);
  } catch {
    res.writeHead(502);
    res.end("Local backend unavailable");
  }
}).listen(4173, "127.0.0.1");
