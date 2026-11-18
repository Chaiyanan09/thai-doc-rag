"""ตัดข้อความแต่ละหน้าเป็น chunk โดยไม่ตัดกลางคำภาษาไทย.

ลำดับการแบ่ง: ย่อหน้า -> ช่องว่าง -> คำ (PyThaiNLP newmm)
แล้วค่อยรวมชิ้นเล็กๆ กลับเป็น chunk ที่ยาวไม่เกิน max_chars
chunk หนึ่งอยู่ในหน้าเดียวเสมอ เพื่อให้อ้างอิงเลขหน้าได้ตรง
"""

import re
from dataclasses import dataclass

from pythainlp.tokenize import word_tokenize
from pythainlp.util import normalize

from .pdf_reader import PageText

_SPACES = re.compile(r"[ \t ​]+")
_MIN_CHUNK_CHARS = 10  # สั้นกว่านี้ถือว่าเป็นเศษ เช่น เลขหน้าเดี่ยวๆ


@dataclass
class Chunk:
    index: int
    page: int
    text: str
    tokens: str  # คำที่ตัดแล้วคั่นด้วยช่องว่าง ใช้กับการค้นหาแบบ keyword (สัปดาห์ 3)


def clean_text(text: str) -> str:
    text = normalize(text)  # จัดลำดับสระ/วรรณยุกต์ที่เพี้ยน และลบอักขระล่องหน
    lines = [_SPACES.sub(" ", line).strip() for line in text.split("\n")]
    return "\n".join(line for line in lines if line)


def _units(text: str, max_chars: int) -> list[tuple[str, str]]:
    """แตกข้อความเป็นชิ้น (ตัวคั่นที่อยู่หน้าชิ้น, ชิ้น) โดยแต่ละชิ้นยาวไม่เกิน max_chars."""
    units: list[tuple[str, str]] = []
    for para in text.split("\n"):
        para = para.strip()
        if not para:
            continue
        para_sep = "\n" if units else ""
        if len(para) <= max_chars:
            units.append((para_sep, para))
            continue
        for i, phrase in enumerate(para.split(" ")):
            if not phrase:
                continue
            sep = para_sep if i == 0 else " "
            if len(phrase) <= max_chars:
                units.append((sep, phrase))
                continue
            # ข้อความไทยยาวที่ไม่มีช่องว่างเลย: ตัดเป็นคำ
            words = word_tokenize(phrase, engine="newmm", keep_whitespace=False)
            for j, word in enumerate(words):
                word_sep = sep if j == 0 else ""
                while len(word) > max_chars:  # คำยาวผิดปกติ (เช่น URL) ตัดตรงๆ
                    units.append((word_sep, word[:max_chars]))
                    word, word_sep = word[max_chars:], ""
                if word:
                    units.append((word_sep, word))
    return units


def _join(units: list[tuple[str, str]]) -> str:
    return "".join((sep if i else "") + piece for i, (sep, piece) in enumerate(units))


def split_text(text: str, max_chars: int, overlap: int) -> list[str]:
    """รวมชิ้นเป็น chunk ยาวไม่เกิน max_chars และให้ chunk ถัดไปเริ่มด้วยท้าย chunk ก่อนหน้าประมาณ overlap ตัวอักษร."""
    chunks: list[str] = []
    current: list[tuple[str, str]] = []
    for sep, piece in _units(text, max_chars):
        candidate = current + [(sep, piece)]
        if current and len(_join(candidate)) > max_chars:
            chunks.append(_join(current))
            tail: list[tuple[str, str]] = []
            for unit in reversed(current):
                if len(_join([unit] + tail)) > overlap:
                    break
                tail.insert(0, unit)
            current = tail
            if current and len(_join(current + [(sep, piece)])) > max_chars:
                current = []
        current.append((sep, piece))
    if current:
        chunks.append(_join(current))
    return chunks


def to_tokens(text: str) -> str:
    words = word_tokenize(text.lower(), engine="newmm", keep_whitespace=False)
    return " ".join(w for w in words if w.strip())


def chunk_pages(pages: list[PageText], max_chars: int, overlap: int) -> list[Chunk]:
    result: list[Chunk] = []
    for page in pages:
        for text in split_text(clean_text(page.text), max_chars, overlap):
            if len(text.replace(" ", "")) < _MIN_CHUNK_CHARS:
                continue
            result.append(Chunk(index=len(result), page=page.page, text=text, tokens=to_tokens(text)))
    return result


def empty_pages(pages: list[PageText]) -> list[int]:
    """หน้าที่แทบไม่มีข้อความ มักเป็นหน้าสแกน ต้องใช้ OCR."""
    return [p.page for p in pages if len(clean_text(p.text).replace(" ", "")) < 20]
