# rag-backend

Backend หลักของระบบ (Spring Boot 4.1 + Java 21 + Spring AI 2.0) ทำหน้าที่รับไฟล์จากหน้าเว็บ เรียก Python model service ให้อ่านและแปลงเป็น vector แล้วเก็บลง PostgreSQL + pgvector ค้นหาด้วย SQL และให้ LLM (Typhoon) ตอบคำถามพร้อมอ้างอิงหน้า

| Endpoint | ทำอะไร |
|---|---|
| `GET /api/health` | สถานะของ backend, ฐานข้อมูล และ model service |
| `POST /api/documents` | อัปโหลด PDF (form field `file`) แล้วรอจนประมวลผลเสร็จ |
| `GET /api/documents` | รายการเอกสารทั้งหมด (ใหม่สุดก่อน) |
| `GET /api/documents/{id}` | ข้อมูลเอกสาร 1 ไฟล์ |
| `DELETE /api/documents/{id}` | ลบเอกสาร (chunk ถูกลบตามด้วย ON DELETE CASCADE) |
| `GET /api/search?q=...&limit=5` | ค้น chunk ที่ความหมายใกล้คำถามที่สุด |
| `POST /api/chat` | body `{"question": "..."}` ได้คำตอบที่มีรหัสอ้างอิง `[S1]` พร้อม chunk ที่ใช้ (`sources`) และ `found=false` ถ้าเอกสารไม่มีคำตอบ |

ทุก error ตอบเป็น `{"message": "..."}`

## เปิดใน IntelliJ

1. `File > Open` เลือกโฟลเดอร์ `D:\ThaiDocRAG\rag-backend` แล้วกด Trust Project รอ Maven โหลด dependency
2. `File > Project Structure > SDK` เลือก Java 21 (ตัวเดียวกับ ToothSeg)
3. คัดลอก `.env.example` เป็น `.env` แล้วใส่
   - `SPRING_DATASOURCE_PASSWORD` ให้ตรงกับ `DB_PASSWORD` ใน `D:\ThaiDocRAG\.env`
   - `TYPHOON_API_KEY` จาก https://playground.opentyphoon.ai (ห้าม commit)

   Spring Boot อ่านไฟล์ `.env` เอง (`spring.config.import` ใน `application.properties`) ไม่ต้องตั้ง environment variable ใน IntelliJ

## รัน

ต้องเปิด 2 อย่างนี้ก่อน: ฐานข้อมูล (`docker compose up -d` ที่ `D:\ThaiDocRAG`) และ model service (พอร์ต 8001)

จากนั้นกด Run ที่ `RagBackendApplication` หรือใน terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway จะสร้างตารางให้เอง: `documents`, `chunks` (`V1__init_schema.sql`) และ `chat_logs` (`V2__chat_logs.sql`)

ลองเรียก:

```powershell
curl.exe http://localhost:8081/api/health
curl.exe -F "file=@D:\path\to\file.pdf" http://localhost:8081/api/documents
```

การถามหรือค้นด้วยภาษาไทยให้ลองจากหน้าเว็บ (`rag-frontend`) เพราะ PowerShell ส่งภาษาไทยเพี้ยนได้

ดูคำถามที่เคยถามทั้งหมด:

```sql
SELECT created_at, question, found, cited_chunk_ids, llm_ms, left(answer, 80)
FROM chat_logs ORDER BY created_at DESC LIMIT 20;
```

## เทส

```powershell
.\mvnw.cmd test
```

ตอนนี้มีแค่ unit test ที่ไม่ต้องใช้ฐานข้อมูลและไม่เรียก LLM จริง

## โครงสร้าง

```
client/    ModelServiceClient เรียก Python ผ่าน RestClient (/ingest, /embed, /health)
document/  Document (JPA entity), DocumentService ขั้นตอนอัปโหลด, DocumentController
chunk/     ChunkRepository ใช้ JdbcTemplate/JdbcClient กับคอลัมน์ vector, PgVector แปลง float[] เป็นข้อความ
search/    SearchService ค้นด้วย vector, SearchController
chat/      ChatService (RAG), PromptBuilder กติกาและข้อความที่ส่งให้ LLM, CitationParser อ่านเลขอ้างอิง, ChatLogRepository
web/       HealthController, ApiExceptionHandler
config/    CORS ให้หน้าเว็บพอร์ต 3001 เรียกได้
```

## ทำไมออกแบบแบบนี้

- `documents` ใช้ JPA เหมือน ToothSeg ส่วน `chunks` ใช้ SQL ตรงๆ เพราะต้องใช้ type `vector` และ operator `<=>` ของ pgvector
- `DocumentService.upload` ไม่ใส่ `@Transactional` ทั้งเมธอด เพราะการรอ Python อาจนานหลายนาที ไม่ควรถือ connection ฐานข้อมูลค้างไว้ จึงเปิด transaction แค่ตอนบันทึก chunk ทั้งหมดพร้อมเปลี่ยนสถานะเป็น READY
- ถ้ามีขั้นไหนล้ม เอกสารจะถูกบันทึกเป็น `FAILED` พร้อมข้อความ error ไม่หายเงียบ
- Typhoon มี API แบบเดียวกับ OpenAI จึงใช้ `spring-ai-starter-model-openai` แล้วเปลี่ยนแค่ `base-url` กับ `model` ใน `application.properties` จะเปลี่ยนไปใช้ LLM เจ้าอื่นก็แก้แค่ 3 ค่านี้
- กันการแต่งคำตอบ 3 ชั้น
  1. ตัด chunk ที่คะแนนต่ำกว่า `app.chat.min-score` ถ้าไม่เหลือเลยตอบ "ไม่พบข้อมูลนี้ในเอกสาร" โดยไม่เรียก LLM
  2. system prompt สั่งให้ตอบจากข้อมูลอ้างอิงเท่านั้น และบอกประโยคที่ต้องตอบเมื่อหาไม่เจอ
  3. บอก LLM ว่าข้อความในเอกสารเป็นข้อมูล ไม่ใช่คำสั่ง (กัน prompt injection จากไฟล์ที่อัปโหลด)
- chunk ที่ส่งให้ LLM ติดรหัส `[S1]`, `[S2]`, ... แทนเลขเปล่า เพราะระเบียบมีเลข "ข้อ 14" อยู่แล้ว ถ้าใช้ `[1]` โมเดลอาจเขียน `[14]` โดยหมายถึงข้อ 14 จากนั้น `CitationParser` แปลงรหัสกลับเป็น chunk ที่ส่งไป หน้าเว็บจึงแสดงชื่อไฟล์และหน้าได้ถูก
- ทุกคำถามถูกบันทึกใน `chat_logs` (ส่ง chunk ไหน อ้างถึงชิ้นไหน ใช้เวลาเท่าไร) ไว้วัดผลในสัปดาห์ 3
