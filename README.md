# ThaiDocRAG

ระบบถามตอบจากเอกสารภาษาไทย (RAG) ที่ตอบพร้อมอ้างอิงหน้า

```
Next.js (3001) ──> Spring Boot (8081) ──> Python FastAPI (8001)   อ่าน PDF, ตัดคำไทย, bge-m3
                         │
                         └──> PostgreSQL 18 + pgvector (5433, Docker)
```

| โฟลเดอร์ | เปิดด้วย | ภาษา / เครื่องมือ | พอร์ต |
|---|---|---|---|
| `rag-frontend` | VS Code | Next.js 16, TypeScript, Tailwind | 3001 |
| `rag-backend` | IntelliJ | Spring Boot 4.1, Java 21, JPA, Flyway | 8081 |
| `rag-model-service` | VS Code | Python, FastAPI, PyMuPDF, PyThaiNLP, bge-m3 | 8001 |
| `docker-compose.yml` | - | PostgreSQL 18 + pgvector | 5433 |

ทุกพอร์ตไม่ชนกับ ToothSeg (3000 / 8080 / 8000 / 5432) จึงเปิดพร้อมกันได้

## ตั้งค่าครั้งแรก

ทำตามลำดับ รายละเอียดอยู่ใน README ของแต่ละโฟลเดอร์

1. **ฐานข้อมูล** (PowerShell ที่ `D:\ThaiDocRAG`, เปิด Docker Desktop ก่อน)
   ```powershell
   copy .env.example .env      # แล้วแก้ DB_PASSWORD
   docker compose up -d
   docker compose ps           # รอจน STATUS เป็น healthy
   ```
2. **Model service**: ทำตาม `rag-model-service/README.md` (สร้าง venv, `pip install`, คัดลอก `.env`)
3. **Backend**: เปิด `rag-backend` ใน IntelliJ, คัดลอก `.env.example` เป็น `.env` แล้วใส่รหัสผ่านเดียวกับข้อ 1 และ `TYPHOON_API_KEY` (สมัครฟรีที่ https://playground.opentyphoon.ai)
4. **Frontend**: `npm install` และคัดลอก `.env.example` เป็น `.env.local`

## รันทุกวัน

1. `docker compose up -d` (ถ้า container ยังไม่รัน)
2. model service: `uvicorn app.main:app --port 8001 --env-file .env --reload`
3. backend: กด Run ใน IntelliJ
4. frontend: `npm run dev` แล้วเปิด http://localhost:3001

## ลองใช้

1. แถบสถานะบนหน้าเว็บต้องเป็นสีเขียวครบ 3 ตัว
2. อัปโหลด `sample-docs/sample-regulation-th.pdf` (หรือ PDF ภาษาไทยที่เป็นข้อความ ไม่ใช่สแกน)
3. ไปที่หน้าแชท ถามคำถามจาก `sample-docs/test-questions.md` คำตอบควรมีเลขอ้างอิงที่ชี้ไปหน้าที่ถูก และข้อ "หอพัก" ต้องตอบว่าไม่พบ

## ดูข้อมูลในฐานข้อมูล

```powershell
docker exec -it thairag-db psql -U rag_app -d thairag
```

```sql
SELECT filename, status, page_count, chunk_count FROM documents;
SELECT page_number, left(content, 80) FROM chunks ORDER BY id LIMIT 5;
```

หรือใน IntelliJ Database tool: host `localhost`, port `5433`, database `thairag`, user `rag_app`

## ถ้าไม่อยากใช้ Docker

ติดตั้ง pgvector เข้า PostgreSQL 18 ที่มีในเครื่อง ต้องคอมไพล์ด้วย Visual Studio (C++ Build Tools) ตามขั้นตอน Windows ใน https://github.com/pgvector/pgvector
จากนั้นสร้าง role `rag_app` และ database `thairag` แล้วเปลี่ยน `SPRING_DATASOURCE_URL` เป็นพอร์ต 5432

## แผน

- สัปดาห์ 1: นำเข้าเอกสาร + ค้นหาด้วย vector ✅
- สัปดาห์ 2: หน้าแชท + Spring AI ให้ LLM (Typhoon) ตอบพร้อมอ้างอิงหน้า
- สัปดาห์ 3: ชุดคำถามทดสอบ 50 ข้อ + hybrid search (vector + keyword)
- สัปดาห์ 4: ล็อกอิน (Spring Security + JWT), Docker Compose ครบทุก service, deploy
