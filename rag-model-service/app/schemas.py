from pydantic import BaseModel, Field


class ChunkOut(BaseModel):
    index: int
    page: int
    text: str
    tokens: str


class IngestResponse(BaseModel):
    filename: str
    page_count: int
    chunks: list[ChunkOut]
    empty_pages: list[int]


class EmbedRequest(BaseModel):
    texts: list[str] = Field(min_length=1)


class EmbedResponse(BaseModel):
    model: str
    dim: int
    vectors: list[list[float]]


class HealthResponse(BaseModel):
    status: str
    model: str
    model_loaded: bool
    device: str | None = None  # "cuda:0" = ใช้ GPU, "cpu" = ใช้ CPU
