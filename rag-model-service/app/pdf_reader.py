"""อ่านข้อความจาก PDF ทีละหน้าด้วย PyMuPDF แล้วต่อบรรทัดกลับเป็นย่อหน้า."""

import re
from dataclasses import dataclass
from functools import reduce

import pymupdf

_THAI = re.compile(r"[฀-๿]")


@dataclass
class PageText:
    page: int  # เลขหน้าเริ่มที่ 1
    text: str


@dataclass
class _Block:
    x0: float
    y0: float
    x1: float
    y1: float
    text: str
    line_count: int

    @property
    def line_height(self) -> float:
        return max((self.y1 - self.y0) / self.line_count, 1.0)


class PdfReadError(ValueError):
    pass


def _join(left: str, right: str) -> str:
    """ต่อข้อความ 2 ท่อน: ถ้ารอยต่อเป็นตัวอักษรไทยทั้งสองฝั่งให้ต่อติดกัน (ภาษาไทยไม่เว้นวรรคระหว่างคำ)
    ไม่อย่างนั้นคั่นด้วยช่องว่าง"""
    if left and right and _THAI.match(left[-1]) and _THAI.match(right[0]):
        return left + right
    return f"{left} {right}" if left else right


def _blocks(page: pymupdf.Page) -> list[_Block]:
    """กลุ่มข้อความในหน้า เรียงจากบนลงล่าง

    ใช้โหมด "blocks" เพราะโหมด "dict" ของ PyMuPDF ทำสระบน/ล่างตัวสุดท้ายของบรรทัดหายในบาง PDF
    (เช่น "ปกติ" เหลือ "ปกต")
    """
    result: list[_Block] = []
    for x0, y0, x1, y1, text, _, block_type in page.get_text("blocks", sort=True):
        if block_type != 0:  # 1 = รูปภาพ
            continue
        lines = [line.strip() for line in text.split("\n") if line.strip()]
        if lines:
            result.append(_Block(x0, y0, x1, y1, reduce(_join, lines), len(lines)))
    return result


def _paragraphs(blocks: list[_Block]) -> list[str]:
    """ต่อบรรทัดที่ถูกตัดเพราะความกว้างหน้ากระดาษกลับเป็นย่อหน้าเดียว

    PDF หลายแบบ (เช่น ที่ export จาก Chrome) เก็บ 1 บรรทัดเป็น 1 block
    จึงต่อ block บรรทัดเดียวเข้ากับ block ถัดไปเมื่อ
    - บรรทัดก่อนหน้ายาวเกิน 70% ของความกว้าง (ถ้าสั้นแปลว่าจบย่อหน้าแล้ว)
    - บรรทัดถัดไปเริ่มที่ขอบซ้าย (ถ้าย่อหน้าเข้าไปแปลว่าขึ้นย่อหน้าใหม่)
    - ไม่มีบรรทัดว่างคั่น (ถ้าห่างเกินหนึ่งบรรทัดแปลว่าเว้นย่อหน้า)
    block ที่อยู่บรรทัดเดียวกัน (เช่น ช่องในตาราง) ต่อกันด้วยช่องว่าง
    """
    if not blocks:
        return []
    left = min(b.x0 for b in blocks)
    width = max(max(b.x1 for b in blocks) - left, 1.0)

    paragraphs: list[str] = []
    current = blocks[0].text
    prev = blocks[0]
    for block in blocks[1:]:
        same_row = abs(block.y0 - prev.y0) < prev.line_height * 0.5 and block.x0 >= prev.x1 - 1
        wrapped = (
            prev.line_count == 1
            and prev.x1 >= left + width * 0.7
            and block.x0 - left < block.line_height * 0.5
            and block.y0 - prev.y1 < prev.line_height
        )
        if same_row:
            current = f"{current} {block.text}"
        elif wrapped:
            current = _join(current, block.text)
        else:
            paragraphs.append(current)
            current = block.text
        prev = block
    paragraphs.append(current)
    return paragraphs


def read_pdf(data: bytes) -> list[PageText]:
    try:
        doc = pymupdf.open(stream=data, filetype="pdf")
    except Exception as exc:  # ไฟล์เสียหรือไม่ใช่ PDF
        raise PdfReadError("เปิดไฟล์ PDF ไม่ได้") from exc

    with doc:
        if doc.needs_pass:
            raise PdfReadError("PDF มีรหัสผ่าน กรุณาปลดรหัสก่อนอัปโหลด")

        pages: list[PageText] = []
        for number, page in enumerate(doc, start=1):
            text = "\n".join(_paragraphs(_blocks(page)))
            pages.append(PageText(page=number, text=text))
        return pages
