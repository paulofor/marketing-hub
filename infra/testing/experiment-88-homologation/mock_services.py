import hashlib
import json
import os
import smtplib
import struct
import threading
import zlib
from email.message import EmailMessage
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


APPROVED_ROOT = Path("/test/approved")
EMAILS = []
LOCK = threading.Lock()


def png_bytes(variant: int) -> bytes:
    width = 1024
    height = 1024
    row = bytearray()
    for x in range(width):
        block = (x // 4) % 2
        row.extend(
            (
                (variant * 23 + block * 120 + x) % 256,
                (variant * 37 + block * 90 + x * 2) % 256,
                (variant * 41 + block * 150 + x * 3) % 256,
            )
        )
    raw = (b"\x00" + bytes(row)) * height

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 0))
        + chunk(b"IEND", b"")
    )


def prepare_approved_library() -> None:
    APPROVED_ROOT.mkdir(parents=True, exist_ok=True)
    manifest = ["# file\tsha256\tmodel\tscore\tdecision\thas_text"]
    for variant in range(10):
        name = f"photo-{variant + 1:02d}.png"
        content = png_bytes(variant)
        (APPROVED_ROOT / name).write_bytes(content)
        digest = hashlib.sha256(content).hexdigest()
        manifest.append(f"{name}\t{digest}\ttest-double\t10\tAPPROVED\tfalse")
    (APPROVED_ROOT / "approved-manifest.tsv").write_text(
        "\n".join(manifest) + "\n", encoding="utf-8"
    )


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path == "/health":
            self.respond(200, {"status": "UP"})
            return
        if self.path == "/v1/payments/test-exp88-approved":
            self.respond(
                200,
                {
                    "id": "test-exp88-approved",
                    "status": "approved",
                    "transaction_amount": 0.67,
                    "currency_id": "BRL",
                    "description": "Agenda Cheia Nail Design - Compra teste",
                    "external_reference": "agenda-cheia-nail-design",
                    "date_approved": "2026-08-20T12:00:00Z",
                    "payer": {"email": "teste+exp88@sandbox.local"},
                    "metadata": {"mh_test": "1", "experiment_id": "88"},
                },
            )
            return
        if self.path == "/evidence/emails":
            with LOCK:
                self.respond(200, {"count": len(EMAILS), "emails": list(EMAILS)})
            return
        self.respond(404, {"error": "not_found"})

    def do_POST(self) -> None:
        payload = json.loads(self.read_request_body() or b"{}")
        if self.path == "/api/v1/product-deliveries/send":
            with LOCK:
                EMAILS.append(payload)
            self.send_sandbox_email(payload)
            self.respond(200, {"requestId": f"email-{len(EMAILS)}", "status": "SENT", "message": "ok"})
            return
        if self.path == "/checkout/preferences":
            self.respond(
                200,
                {
                    "id": "pref-exp88-test",
                    "initPoint": "http://mock-services:8080/checkout/test-exp88-approved",
                },
            )
            return
        self.respond(404, {"error": "not_found"})

    def read_request_body(self) -> bytes:
        if self.headers.get("Transfer-Encoding", "").lower() != "chunked":
            return self.rfile.read(int(self.headers.get("Content-Length", "0")))
        chunks = bytearray()
        while True:
            size_line = self.rfile.readline().strip().split(b";", 1)[0]
            size = int(size_line, 16)
            if size == 0:
                self.rfile.readline()
                break
            chunks.extend(self.rfile.read(size))
            self.rfile.read(2)
        return bytes(chunks)

    def send_sandbox_email(self, payload: dict) -> None:
        if os.environ.get("SMTP_ENABLED", "false").lower() != "true":
            return
        message = EmailMessage()
        message["From"] = "homologacao@marketinghub.local"
        message["To"] = payload.get("to", "teste+exp88@sandbox.local")
        message["Subject"] = payload.get("subject", "Agenda Cheia - homologacao")
        message.set_content(json.dumps(payload, ensure_ascii=False, indent=2))
        with smtplib.SMTP(
            os.environ.get("SMTP_HOST", "sandbox-mail"),
            int(os.environ.get("SMTP_PORT", "1025")),
            timeout=5,
        ) as smtp:
            smtp.send_message(message)

    def respond(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args) -> None:
        return


prepare_approved_library()
ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
