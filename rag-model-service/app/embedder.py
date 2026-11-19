"""แปลงข้อความเป็น vector ด้วย bge-m3 (ผ่าน sentence-transformers)."""

from functools import lru_cache
from typing import Protocol

from . import config


class Embedder(Protocol):
    model_name: str
    dim: int
    device: str

    def embed(self, texts: list[str]) -> list[list[float]]: ...


class SentenceTransformerEmbedder:
    def __init__(self, model_name: str, device: str | None, batch_size: int):
        # import ตรงนี้ เพราะ torch โหลดนาน และเทสไม่ต้องใช้
        from sentence_transformers import SentenceTransformer

        self.model_name = model_name
        self.batch_size = batch_size
        self.model = SentenceTransformer(model_name, device=device)
        self.dim = self.model.get_embedding_dimension()
        self.device = str(self.model.device)  # เช่น "cuda:0" หรือ "cpu"

    def embed(self, texts: list[str]) -> list[list[float]]:
        vectors = self.model.encode(
            texts,
            batch_size=self.batch_size,
            normalize_embeddings=True,  # ยาว 1 ทุกตัว ใช้ cosine distance ใน pgvector ได้ตรง
            convert_to_numpy=True,
            show_progress_bar=False,
        )
        return vectors.tolist()


@lru_cache(maxsize=1)
def get_embedder() -> Embedder:
    """สร้างครั้งเดียวแล้วใช้ซ้ำ (ครั้งแรกจะดาวน์โหลดโมเดลประมาณ 2.3 GB ไปไว้ที่ HF_HOME)."""
    return SentenceTransformerEmbedder(config.EMBED_MODEL, config.EMBED_DEVICE, config.EMBED_BATCH_SIZE)


def is_loaded() -> bool:
    return get_embedder.cache_info().currsize > 0
