#!/usr/bin/env python3
"""Confirma confiança, nomes e validade do certificado servido nas superfícies Vega."""

import json
import socket
import ssl
import time


def check(host, port=443, context=None):
    """Consulta TLS sem desativar confiança ou identidade e exige quatorze dias de validade."""
    context = context or ssl.create_default_context()
    with socket.create_connection((host, port), timeout=15) as connection:
        with context.wrap_socket(connection, server_hostname=host) as secured:
            certificate = secured.getpeercert()
            expires = ssl.cert_time_to_seconds(certificate["notAfter"])
            if expires - time.time() < 14 * 86400:
                raise ValueError(f"{host}: certificado com menos de 14 dias de validade")
            return {"host": host, "expiresAt": certificate["notAfter"], "trusted": True}


if __name__ == "__main__":
    names = ["clubemusa.com.br", "www.clubemusa.com.br"] + [
        f"v{version}.clubemusa.com.br" for version in (1, 2, 5, 6, 7, 8, 9, 10)
    ]
    print(json.dumps([check(host) for host in names], indent=2))
