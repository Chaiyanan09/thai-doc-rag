"""Model service: งานที่ต้องใช้ Python (อ่าน PDF, ตัดคำไทย, สร้าง embedding).

Spring Boot เป็นตัวเรียก service นี้ ผู้ใช้ไม่ได้เรียกตรง
    POST /ingest  รับไฟล์ PDF -> คืน chunk ทีละหน้า
    POST /embed   รับข้อความ -> คืน vector 1024 มิติ
    GET  /health  เช็กว่า service พร้อมหรือยัง
"""

import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, File, Header, HTTPException, UploadFile

from . import config
from .chunker import chunk_pages, empty_pages
from .embedder import Embedder, get_embedder, is_loaded
from .pdf_reader import PdfReadError, read_pdf
from .schemas import ChunkOut, EmbedRequest, EmbedResponse, HealthResponse, IngestResponse

log = logging.getLogger("uvicorn.error")


@asynccontextmanager
async def lifespan(_: FastAPI):
    if config.PRELOAD_MODEL:
        log.info("Loading embedding model %s (first run downloads it) ...", config.EMBED_MODEL)
        embedder = get_embedder()
        log.info("Embedding model ready: dim=%s", embedder.dim)
    yield


app = FastAPI(title="Thai RAG model service", version="0.1.0", lifespan=lifespan)


def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    if config.MODEL_API_KEY and x_api_key != config.MODEL_API_KEY:
        raise HTTPException(status_code=401, detail="API key ไม่ถูกต้อง")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model=config.EMBED_MODEL, model_loaded=is_loaded())


# ใช้ def ธรรมดา (ไม่ใช่ async def) เพราะงานกิน CPU, FastAPI จะรันใน thread pool ให้เอง
@app.post("/ingest", response_model=IngestResponse, dependencies=[Depends(require_api_key)])
def ingest(file: UploadFile = File(...)) -> IngestResponse:
    limit = config.MAX_UPLOAD_MB * 1024 * 1024
    data = file.file.read(limit + 1)
    if len(data) > limit:
        raise HTTPException(status_code=413, detail=f"ไฟล์ใหญ่เกิน {config.MAX_UPLOAD_MB} MB")
    if b"%PDF" not in data[:1024]:
        raise HTTPException(status_code=400, detail="ไฟล์นี้ไม่ใช่ PDF")

    try:
        pages = read_pdf(data)
    except PdfReadError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    chunks = chunk_pages(pages, config.CHUNK_MAX_CHARS, config.CHUNK_OVERLAP_CHARS)
    log.info("ingest %s: %d pages, %d chunks", file.filename, len(pages), len(chunks))
    return IngestResponse(
        filename=file.filename or "document.pdf",
        page_count=len(pages),
        chunks=[ChunkOut(index=c.index, page=c.page, text=c.text, tokens=c.tokens) for c in chunks],
        empty_pages=empty_pages(pages),
    )


@app.post("/embed", response_model=EmbedResponse, dependencies=[Depends(require_api_key)])
def embed(req: EmbedRequest, embedder: Embedder = Depends(get_embedder)) -> EmbedResponse:
    if len(req.texts) > config.MAX_TEXTS_PER_EMBED:
        raise HTTPException(status_code=400, detail=f"ส่งได้ไม่เกิน {config.MAX_TEXTS_PER_EMBED} ข้อความต่อครั้ง")
    texts = [t.strip() for t in req.texts]
    if any(not t for t in texts):
        raise HTTPException(status_code=400, detail="มีข้อความว่าง")
    return EmbedResponse(model=embedder.model_name, dim=embedder.dim, vectors=embedder.embed(texts))
