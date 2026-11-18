from pythainlp.tokenize import word_tokenize

from app.chunker import chunk_pages, clean_text, empty_pages, split_text
from app.pdf_reader import PageText, _join

SENTENCE = "มหาวิทยาลัยกำหนดให้นักศึกษาลงทะเบียนเรียนไม่น้อยกว่าเก้าหน่วยกิตในภาคการศึกษาปกติ"


def test_join_thai_has_no_space_but_english_does():
    assert _join("นักศึกษาต้อง", "ลงทะเบียน") == "นักศึกษาต้องลงทะเบียน"
    assert _join("Students must", "register") == "Students must register"
    assert _join("วันละ", "100 บาท") == "วันละ 100 บาท"


def test_long_thai_text_is_split_on_word_boundaries():
    text = SENTENCE * 20  # ยาวมาก และไม่มีช่องว่างเลย
    words = set(word_tokenize(text, engine="newmm", keep_whitespace=False))

    chunks = split_text(text, max_chars=200, overlap=40)

    assert len(chunks) > 1
    for chunk in chunks:
        assert len(chunk) <= 200
        # ทุก chunk ต้องประกอบด้วยคำเต็มๆ ไม่มีคำที่ถูกตัดครึ่ง
        assert set(word_tokenize(chunk, engine="newmm", keep_whitespace=False)) <= words


def test_next_chunk_starts_with_end_of_previous_chunk():
    text = " ".join(f"ข้อ {i} {SENTENCE}" for i in range(10))
    chunks = split_text(text, max_chars=300, overlap=120)

    assert len(chunks) > 1
    for previous, current in zip(chunks, chunks[1:]):
        first_piece = current.split(" ")[0]
        assert first_piece in previous


def test_chunks_keep_page_numbers_and_skip_tiny_pages():
    pages = [
        PageText(page=1, text=SENTENCE * 5),
        PageText(page=2, text="12"),  # มีแค่เลขหน้า
        PageText(page=3, text=SENTENCE),
    ]
    chunks = chunk_pages(pages, max_chars=200, overlap=40)

    assert {c.page for c in chunks} == {1, 3}
    assert [c.index for c in chunks] == list(range(len(chunks)))
    assert all(c.tokens for c in chunks)
    assert empty_pages(pages) == [2]


def test_clean_text_collapses_spaces_and_blank_lines():
    assert clean_text("ข้อ  1  นักศึกษา\n\n\nข้อ 2") == "ข้อ 1 นักศึกษา\nข้อ 2"
