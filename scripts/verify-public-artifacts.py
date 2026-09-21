#!/usr/bin/env python3
"""Comprova o destino DNS e os bytes públicos contra os artefatos versionados."""

import argparse
import hashlib
import json
from pathlib import Path
import socket
import time
import urllib.error
import urllib.parse
import urllib.request


def verify(base_url, expected_address, source_directory, filenames):
    """Rejeita host divergente, redirecionamento externo ou arquivo desatualizado."""
    base = urllib.parse.urlsplit(base_url)
    if base.scheme not in ("http", "https") or not base.hostname:
        raise ValueError("URL pública inválida")
    addresses = {entry[4][0] for entry in socket.getaddrinfo(base.hostname, base.port or 443)}
    if addresses != {expected_address}:
        raise ValueError(f"Destino DNS divergente: esperado={expected_address}; observado={sorted(addresses)}")
    root = Path(source_directory).resolve()
    evidence = []
    for filename in filenames:
        source = (root / filename).resolve()
        if not source.is_relative_to(root) or not source.is_file():
            raise ValueError(f"Artefato local inválido: {filename}")
        expected = hashlib.sha256(source.read_bytes()).hexdigest()
        url = urllib.parse.urljoin(base_url.rstrip("/") + "/", urllib.parse.quote(filename))
        request = urllib.request.Request(url + "?mh_test=1", headers={"Cache-Control": "no-cache"})
        with urllib.request.urlopen(request, timeout=20) as response:
            final = urllib.parse.urlsplit(response.geturl())
            if (final.scheme, final.netloc) != (base.scheme, base.netloc):
                raise ValueError(f"Artefato redirecionado para outra origem: {filename}")
            actual = hashlib.sha256(response.read(5 * 1024 * 1024 + 1)).hexdigest()
        if actual != expected:
            raise ValueError(f"Artefato público divergente: {filename}; esperado={expected}; observado={actual}")
        evidence.append({"url": url, "sha256": actual, "expectedAddress": expected_address})
    return evidence


def main():
    """Executa a prova com tentativas limitadas e diagnóstico sem conteúdo dos arquivos."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--expected-address", required=True)
    parser.add_argument("--source-directory", required=True)
    parser.add_argument("--attempts", type=int, default=3)
    parser.add_argument("--retry-delay", type=float, default=5)
    parser.add_argument("files", nargs="+")
    args = parser.parse_args()
    if args.attempts < 1 or args.retry_delay < 0:
        parser.error("Tentativas devem ser positivas e espera não negativa")
    for attempt in range(args.attempts):
        try:
            evidence = verify(args.base_url, args.expected_address, args.source_directory, args.files)
            print(json.dumps({"verified": True, "artifacts": evidence}, ensure_ascii=False))
            return
        except (OSError, ValueError, urllib.error.URLError) as error:
            if attempt + 1 == args.attempts:
                raise SystemExit(str(error)) from error
            time.sleep(args.retry_delay)


if __name__ == "__main__":
    main()
