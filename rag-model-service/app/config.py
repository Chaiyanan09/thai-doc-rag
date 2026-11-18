"""ค่าตั้งค่าทั้งหมดของ service อ่านจาก environment variable (หรือไฟล์ .env ตอนรันด้วย --env-file)."""

import os


def _int(name: str, default: int) -> int:
    value = os.getenv(name)
    return int(value) if value else default


def _bool(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or value == "":
        return default
    return value.strip().lower() in ("1", "true", "yes", "on")


# Embedding model (bge-m3 ให้ vector 1024 มิติ ต้องตรงกับคอลัมน์ vector(1024) ในฐานข้อมูล)
EMBED_MODEL = os.getenv("EMBED_MODEL", "BAAI/bge-m3")
# ว่างไว้ = ให้ sentence-transformers เลือกเอง (มี GPU ก็ใช้ cuda) หรือกำหนด "cpu" / "cuda"
EMBED_DEVICE = os.getenv("EMBED_DEVICE") or None
EMBED_BATCH_SIZE = _int("EMBED_BATCH_SIZE", 16)
# โหลดโมเดลตั้งแต่เปิด service (ช้าตอนเริ่ม แต่คำขอแรกไม่ต้องรอ)
PRELOAD_MODEL = _bool("PRELOAD_MODEL", True)

# การตัด chunk (นับเป็นจำนวนตัวอักษร)
CHUNK_MAX_CHARS = _int("CHUNK_MAX_CHARS", 800)
CHUNK_OVERLAP_CHARS = _int("CHUNK_OVERLAP_CHARS", 150)

# จำกัดขนาดไฟล์และจำนวนข้อความต่อคำขอ
MAX_UPLOAD_MB = _int("MAX_UPLOAD_MB", 30)
MAX_TEXTS_PER_EMBED = _int("MAX_TEXTS_PER_EMBED", 64)

# ถ้าตั้งค่าไว้ ทุกคำขอ (ยกเว้น /health) ต้องส่ง header X-API-KEY ให้ตรง
MODEL_API_KEY = os.getenv("MODEL_API_KEY", "")
