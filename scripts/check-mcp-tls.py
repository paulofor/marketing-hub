#!/usr/bin/env python3
"""Valida certificado público e consultas MCP sem relaxar TLS nem imprimir dados operacionais."""

import json
import socket
import ssl
import time
import urllib.request

DOMAINS = ("mcpserverdigi.shop", "www.mcpserverdigi.shop")


def certificate_health(domain, context):
    """Exige cadeia confiável, nome correto e ao menos duas semanas de validade."""
    with socket.create_connection((domain, 443), timeout=20) as connection:
        with context.wrap_socket(connection, server_hostname=domain) as secure:
            certificate = secure.getpeercert()
    expires = ssl.cert_time_to_seconds(certificate["notAfter"])
    days = (expires - time.time()) / 86400
    if days < 14:
        raise ValueError(f"Certificado de {domain} vence em menos de 14 dias")
    return {"domain": domain, "expires": certificate["notAfter"], "remainingDays": round(days, 1)}


def rpc(method, params, context):
    """Consulta JSON-RPC por HTTPS verificado e recusa falhas funcionais mesmo com HTTP 200."""
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode()
    request = urllib.request.Request("https://mcpserverdigi.shop/mcp", data=body,
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, context=context, timeout=90) as response:
        payload = json.load(response)
    if payload.get("id") != 1 or payload.get("error") or not isinstance(payload.get("result"), dict):
        raise ValueError("Resposta MCP inválida")
    result = payload["result"]
    if result.get("isError"):
        raise ValueError("MCP reportou erro funcional")
    return result


def main():
    """Confere os dois domínios, catálogo de tools e saúde do banco, sem mutações comerciais."""
    context = ssl.create_default_context()
    certificates = [certificate_health(domain, context) for domain in DOMAINS]
    tools = rpc("tools/list", {}, context).get("tools", [])
    names = {tool.get("name") for tool in tools}
    if not {"db_health", "db_query", "java_module_logs"}.issubset(names):
        raise ValueError("Tools canônicas ausentes")
    health = rpc("tools/call", {"name": "db_health", "arguments": {}}, context)
    data = health.get("structuredContent", {})
    if data.get("status") != "ok":
        raise ValueError("Banco não confirmou status ok")
    print(json.dumps({"tls": certificates, "tools": len(tools), "db_health": "ok"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
