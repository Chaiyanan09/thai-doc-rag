import pymupdf

from app import config

LINES = [
    "ระเบียบมหาวิทยาลัย ว่าด้วยการศึกษาระดับปริญญาตรี",
    "ข้อ 1 นักศึกษาต้องลงทะเบียนเรียนไม่น้อยกว่าเก้าหน่วยกิต",
    "ในภาคการศึกษาปกติ และไม่เกินยี่สิบสองหน่วยกิต",
    "ข้อ 2 การลาพักการศึกษาต้องยื่นคำร้องภายในสองสัปดาห์แรก",
]


def make_pdf(font_path: str, pages: int = 2) -> bytes:
    doc = pymupdf.open()
    for _ in range(pages):
        page = doc.new_page()
        y = 72
        for line in LINES:
            page.insert_text((72, y), line, fontname="thai", fontfile=font_path, fontsize=12)
            y += 20
    return doc.tobytes()


def test_health(client):
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_ingest_thai_pdf(client, thai_font):
    pdf = make_pdf(thai_font)

    res = client.post("/ingest", files={"file": ("rules.pdf", pdf, "application/pdf")})

    assert res.status_code == 200, res.text
    body = res.json()
    assert body["page_count"] == 2
    assert body["empty_pages"] == []
    assert {c["page"] for c in body["chunks"]} == {1, 2}
    all_text = " ".join(c["text"] for c in body["chunks"])
    assert "ลงทะเบียนเรียน" in all_text
    assert "หน่วยกิต" in body["chunks"][0]["tokens"].split(" ")


def test_ingest_rejects_non_pdf(client):
    res = client.post("/ingest", files={"file": ("note.txt", b"hello", "text/plain")})
    assert res.status_code == 400


def test_embed_returns_one_vector_per_text(client):
    res = client.post("/embed", json={"texts": ["ลงทะเบียนกี่หน่วยกิต", "ลาพักการศึกษา"]})

    assert res.status_code == 200
    body = res.json()
    assert body["dim"] == 1024
    assert len(body["vectors"]) == 2
    assert len(body["vectors"][0]) == 1024


def test_embed_rejects_empty_text(client):
    res = client.post("/embed", json={"texts": ["  "]})
    assert res.status_code == 400


def test_api_key_is_checked_when_configured(client, monkeypatch):
    monkeypatch.setattr(config, "MODEL_API_KEY", "secret")

    assert client.post("/embed", json={"texts": ["ทดสอบ"]}).status_code == 401
    ok = client.post("/embed", json={"texts": ["ทดสอบ"]}, headers={"X-API-KEY": "secret"})
    assert ok.status_code == 200
