# MCP Connectivity Test Report

Date: 2026-04-20 (UTC)
Endpoint: `https://mcpserverdigi.shop/mcp`

## Steps executed

1. Basic reachability with HTTP GET to `/mcp`.
2. JSON-RPC `initialize` via HTTP POST.
3. JSON-RPC `tools/list` via HTTP POST.
4. TLS/proxy verbose diagnostic with `curl -v`.
5. Checked local MCP client configured servers (`list_mcp_resources`).

## Raw request payloads

### initialize

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","clientInfo":{"name":"codex-cli","version":"1.0.0"},"capabilities":{}}}
```

### tools/list

```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

## Raw responses (abridged)

### GET /mcp

```http
HTTP/1.1 503 Service Unavailable
content-type: text/plain

upstream connect error or disconnect/reset before headers. reset reason: connection timeout
```

### POST /mcp (initialize)

```http
HTTP/1.1 503 Service Unavailable
content-type: text/plain

upstream connect error or disconnect/reset before headers. reset reason: connection timeout
```

### POST /mcp (tools/list)

```http
HTTP/1.1 503 Service Unavailable
content-type: text/plain

upstream connect error or disconnect/reset before headers. reset reason: connection timeout
```

### Curl verbose diagnostic excerpt

- Proxy tunnel to `mcpserverdigi.shop:443` established (`HTTP/1.1 200 OK`).
- TLS handshake succeeded and certificate validated.
- Upstream application returned `503 Service Unavailable` with timeout/reset reason.

## Conclusion

- MCP network path and TLS are reachable.
- MCP application endpoint did not complete handshake because upstream timed out before returning JSON-RPC response.
- No tools/resources could be listed.
- No SQL query could be executed.

## Suggested next actions

1. Confirm MCP server process behind `https://mcpserverdigi.shop/mcp` is running and healthy.
2. Verify reverse proxy route/timeout to upstream MCP service.
3. Confirm required auth mode (if any), then retry with proper `Authorization` header.
4. Once `initialize` returns JSON-RPC result, run `tools/list` and then call SQL read-only tool (e.g. `query` with `SELECT 1 AS ok;`).
