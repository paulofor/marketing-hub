import io
import json
import struct
import urllib.error
import urllib.request
import zipfile
from datetime import datetime


PAYMENTS = "http://lead-portal-payments:8080"
MOCK = "http://127.0.0.1:8080"
PAYMENT_ID = "test-exp88-approved"
BRIEFING = {
    "paymentId": PAYMENT_ID,
    "buyerEmail": "teste+exp88@sandbox.local",
    "professionalName": "Studio Aurora Nails",
    "cityRegion": "Campinas",
    "whatsapp": "11999999999",
    "services": "Alongamento em gel e manutenção",
    "visualStyle": "Clean e elegante",
    "preferredColors": "Rosa e vinho",
    "weeklyGoal": "Preencher horários vagos",
    "notes": "mh_test=1 experimento 88",
}


def get_json(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def post_json(url: str, payload: dict) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=180) as response:
        return json.load(response)


def expect_failure(url: str) -> int:
    try:
        get_json(url)
    except urllib.error.HTTPError as error:
        return error.code
    raise AssertionError(f"A falha esperada foi aceita: {url}")


assert urllib.request.urlopen(PAYMENTS + "/health", timeout=10).read() == b"ok"
initial = get_json(
    PAYMENTS + "/api/v1/agenda-cheia/post-purchase?payment_id=" + PAYMENT_ID
)
assert initial["status"] == "AGUARDANDO_BRIEFING"
assert expect_failure(
    PAYMENTS + "/api/v1/agenda-cheia/post-purchase?payment_id=unknown-exp88"
) >= 400

delivered = post_json(
    PAYMENTS + "/api/v1/agenda-cheia/post-purchase/briefing", BRIEFING
)
assert delivered["status"] == "ENTREGUE"
assert delivered["submittedAt"]

emails = get_json(MOCK + "/evidence/emails")
assert emails["count"] == 2
assert all(email["to"] == "teste+exp88@sandbox.local" for email in emails["emails"])
assert all(
    email["externalReference"] == "agenda-cheia-nail-design"
    for email in emails["emails"]
)
final_email = emails["emails"][-1]
assert final_email["downloadUrl"]

download_url = final_email["downloadUrl"].replace(
    "http://localhost:18092", PAYMENTS
)
archive = urllib.request.urlopen(download_url, timeout=60).read()
with zipfile.ZipFile(io.BytesIO(archive)) as package:
    names = package.namelist()
    assert len(names) == 24
    assert sum(name.startswith("post-") for name in names) == 10
    assert sum(name.startswith("story-") for name in names) == 10
    for name in names:
        if not name.endswith(".png"):
            continue
        width, height = struct.unpack(">II", package.read(name)[16:24])
        expected = (1080, 1080) if name.startswith("post-") else (1080, 1920)
        assert (width, height) == expected
    assert {
        "legendas-prontas.txt",
        "mensagens-whatsapp.txt",
        "calendario-7-dias.txt",
        "LEIA-ME.txt",
    }.issubset(names)

duplicate = post_json(
    PAYMENTS + "/api/v1/agenda-cheia/post-purchase/briefing", BRIEFING
)
assert duplicate["id"] == delivered["id"]
assert duplicate["paymentId"] == delivered["paymentId"]
assert duplicate["status"] == delivered["status"]
first_timestamp = datetime.fromisoformat(delivered["submittedAt"].replace("Z", "+00:00"))
duplicate_timestamp = datetime.fromisoformat(duplicate["submittedAt"].replace("Z", "+00:00"))
assert abs((first_timestamp - duplicate_timestamp).total_seconds()) < 0.001
assert get_json(MOCK + "/evidence/emails")["count"] == 2
assert expect_failure(
    PAYMENTS
    + "/api/v1/agenda-cheia/post-purchase/deliveries/invalid-exp88-token/download"
) >= 400

print(
    json.dumps(
        {
            "status": "PASS",
            "paymentId": PAYMENT_ID,
            "deliveryStatus": delivered["status"],
            "emails": emails["count"],
            "zipBytes": len(archive),
            "files": len(names),
            "posts": 10,
            "stories": 10,
            "deduplicated": True,
            "negativeGates": 2,
        },
        ensure_ascii=False,
    )
)
