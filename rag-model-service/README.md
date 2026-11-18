# rag-model-service

Python service เล็กๆ ที่ทำงาน 2 อย่างที่ Java ทำได้ไม่ดี: อ่าน PDF ภาษาไทยแล้วตัดเป็น chunk และแปลงข้อความเป็น vector ด้วย bge-m3
ผู้ใช้ไม่ได้เรียก service นี้ตรงๆ มีแค่ Spring Boot (`rag-backend`) ที่เรียก

| Endpoint | ทำอะไร |
|---|---|
| `GET /health` | เช็กว่า service ทำงาน และโหลดโมเดลแล้วหรือยัง |
| `POST /ingest` | รับไฟล์ PDF (form field `file`) คืน chunk พร้อมเลขหน้า และคำที่ตัดแล้ว (`tokens`) |
| `POST /embed` | รับ `{"texts": [...]}` (ไม่เกิน 64 ข้อความ) คืน vector 1024 มิติต่อข้อความ |

เปิด http://localhost:8001/docs เพื่อลองเรียกผ่านหน้าเว็บได้

## ติดตั้งครั้งแรก (PowerShell ใน VS Code)

ต้องมี Python 3.11–3.13 (แนะนำ 3.12)

```powershell
cd D:\ThaiDocRAG\rag-model-service
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements-dev.txt
copy .env.example .env
```

- ถ้า `Activate.ps1` ขึ้นว่าไม่อนุญาตให้รันสคริปต์ ให้รัน `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` ครั้งเดียว
- `pip install -r ...` บน Windows จะได้ torch แบบ CPU ถ้าจะใช้ GPU ดูหัวข้อ "ใช้ GPU" ด้านล่าง
- ใน VS Code กด `Ctrl+Shift+P` → `Python: Select Interpreter` → เลือก `.venv`

## ใช้ GPU (NVIDIA)

1. รัน `nvidia-smi` ดูค่า `CUDA Version` มุมขวาบน ถ้าเป็น 13.x ใช้ `cu130` ถ้าเป็น 12.x ใช้ `cu126`
2. เปลี่ยน torch เป็นรุ่น CUDA (ไฟล์ใหญ่ราว 2.5 GB จึงตั้งที่พักไฟล์ชั่วคราวไว้ไดร์ฟ D):
   ```powershell
   .\.venv\Scripts\Activate.ps1
   mkdir D:\pip-tmp -Force
   $env:TMP = "D:\pip-tmp"; $env:TEMP = "D:\pip-tmp"
   pip uninstall -y torch
   pip install torch --index-url https://download.pytorch.org/whl/cu126 --no-cache-dir
   python -c "import torch; print(torch.__version__, torch.cuda.is_available())"
   ```
   ต้องได้ `...+cu126 True`
3. เปิด uvicorn ใหม่ log จะขึ้น `device=cuda:0` และ `GET /health` จะมี `"device": "cuda:0"`

`EMBED_DEVICE` ใน `.env` ปล่อยว่างไว้ได้ ถ้ามี GPU จะใช้ GPU เอง

## รัน

```powershell
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --port 8001 --env-file .env --reload
```

ครั้งแรกจะดาวน์โหลด bge-m3 ประมาณ 2.3 GB ไปไว้ที่ `HF_HOME` ใน `.env` (ตั้งไว้ที่ไดร์ฟ D) รอจนขึ้น `Embedding model ready: dim=1024`

## เทส

```powershell
pytest
```

เทสใช้ embedder ปลอม จึงไม่ต้องโหลดโมเดลจริง และสร้าง PDF ภาษาไทยด้วยฟอนต์ Tahoma ในเครื่อง

## โครงสร้าง

```
app/
  main.py        endpoint ทั้งหมด
  pdf_reader.py  อ่านข้อความทีละหน้าด้วย PyMuPDF และต่อบรรทัดภาษาไทยไม่ให้มีช่องว่างเกิน
  chunker.py     ตัด chunk ตามย่อหน้า -> ช่องว่าง -> คำ (PyThaiNLP) ไม่ตัดกลางคำ และมี overlap
  embedder.py    โหลด bge-m3 ครั้งเดียวแล้วใช้ซ้ำ, vector ถูก normalize ให้ยาว 1
  config.py      ค่าตั้งค่าจาก .env
tests/
```

## เรื่องที่ควรรู้

- chunk หนึ่งอยู่ในหน้าเดียวเสมอ จะได้อ้างอิงเลขหน้าได้ตรง
- `empty_pages` คือหน้าที่แทบไม่มีข้อความ มักเป็นหน้าสแกน ต้องใช้ OCR (Typhoon OCR) ซึ่งจะเพิ่มทีหลัง
- PyThaiNLP มองคำประสมบางคำเป็นคำเดียว เช่น `ลงทะเบียนเรียน` จึงไม่มี token `ลงทะเบียน` แยก ต้องคิดเรื่องนี้ตอนทำ keyword search ในสัปดาห์ 3
