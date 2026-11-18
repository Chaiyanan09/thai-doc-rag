import hashlib
import os
from pathlib import Path

import pytest

# ต้องตั้งก่อน import app เพื่อไม่ให้โหลดโมเดลจริงตอนเทส
os.environ["PRELOAD_MODEL"] = "false"
os.environ["MODEL_API_KEY"] = ""

from fastapi.testclient import TestClient  # noqa: E402

from app.embedder import get_embedder  # noqa: E402
from app.main import app  # noqa: E402

THAI_FONT_CANDIDATES = [
    "C:/Windows/Fonts/tahoma.ttf",
    "C:/Windows/Fonts/LeelawUI.ttf",
    "/usr/share/fonts/opentype/tlwg/Loma.otf",
    "/usr/share/fonts/truetype/tlwg/Loma.ttf",
    "/usr/share/fonts/truetype/freefont/FreeSerif.ttf",
]


class FakeEmbedder:
    """แทนโมเดลจริงในเทส: คืน vector 1024 มิติที่เหมือนเดิมทุกครั้งสำหรับข้อความเดียวกัน."""

    model_name = "fake-bge-m3"
    dim = 1024
    device = "cpu"

    def embed(self, texts: list[str]) -> list[list[float]]:
        out = []
        for text in texts:
            seed = hashlib.sha256(text.encode("utf-8")).digest()
            out.append([seed[i % len(seed)] / 255.0 for i in range(self.dim)])
        return out


@pytest.fixture
def client():
    app.dependency_overrides[get_embedder] = FakeEmbedder
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture
def thai_font() -> str:
    for path in THAI_FONT_CANDIDATES:
        if Path(path).exists():
            return path
    pytest.skip("ไม่พบฟอนต์ภาษาไทยในเครื่อง")
