# rag-backend

Backend หลักของระบบ (Spring Boot 4.1 + Java 21) ทำหน้าที่รับไฟล์จากหน้าเว็บ เรียก Python model service ให้อ่านและแปลงเป็น vector แล้วเก็บลง PostgreSQL + pgvector จากนั้นค้นหาด้วย SQL

| Endpoint | ทำอะไร |
|---|---|
| `GET /api/health` | สถานะของ backend, ฐานข้อมูล และ model service |
| `POST /api/documents` | อัปโหลด PDF (form field `file`) แล้วรอจนประมวลผลเสร็จ |
| `GET /api/documents` | รายการเอกสารทั้งหมด (ใหม่สุดก่อน) |
| `GET /api/documents/{id}` | ข้อมูลเอกสาร 1 ไฟล์ |
| `DELETE /api/documents/{id}` | ลบเอกสาร (chunk ถูกลบตามด้วย ON DELETE CASCADE) |
| `GET /api/search?q=...&limit=5` | ค้น chunk ที่ความหมายใกล้คำถามที่สุด |

ทุก error ตอบเป็น `{"message": "..."}`

## เปิดใน IntelliJ

1. `File > Open` เลือกโฟลเดอร์ `D:\ThaiDocRAG\rag-backend` แล้วกด Trust Project รอ Maven โหลด dependency
2. `File > Project Structure > SDK` เลือก Java 21 (ตัวเดียวกับ ToothSeg)
3. คัดลอก `.env.example` เป็น `.env` แล้วใส่ `SPRING_DATASOURCE_PASSWORD` ให้ตรงกับ `DB_PASSWORD` ใน `D:\ThaiDocRAG\.env`
   Spring Boot อ่านไฟล์ `.env` เอง (`spring.config.import` ใน `application.properties`) ไม่ต้องตั้ง environment variable ใน IntelliJ

## รัน

ต้องเปิด 2 อย่างนี้ก่อน: ฐานข้อมูล (`docker compose up -d` ที่ `D:\ThaiDocRAG`) และ model service (พอร์ต 8001)

จากนั้นกด Run ที่ `RagBackendApplication` หรือใน terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

ครั้งแรก Flyway จะสร้างตาราง `documents` และ `chunks` ให้เอง (`db/migration/V1__init_schema.sql`)

ลองเรียก:

```powershell
curl.exe http://localhost:8081/api/health
curl.exe -F "file=@D:\path\to\file.pdf" http://localhost:8081/api/documents
```

การค้นด้วยภาษาไทยให้ลองจากหน้าเว็บ (`rag-frontend`) เพราะ PowerShell ส่งภาษาไทยใน URL เพี้ยนได้

## เทส

```powershell
.\mvnw.cmd test
```

ตอนนี้มีแค่ unit test ที่ไม่ต้องใช้ฐานข้อมูล

## โครงสร้าง

```
client/    ModelServiceClient เรียก Python ผ่าน RestClient (/ingest, /embed, /health)
document/  Document (JPA entity), DocumentService ขั้นตอนอัปโหลด, DocumentController
chunk/     ChunkRepository ใช้ JdbcTemplate/JdbcClient กับคอลัมน์ vector, PgVector แปลง float[] เป็นข้อความ
search/    SearchService ค้นด้วย vector, SearchController
web/       HealthController, ApiExceptionHandler
config/    CORS ให้หน้าเว็บพอร์ต 3001 เรียกได้
```

## ทำไมออกแบบแบบนี้

- `documents` ใช้ JPA เหมือน ToothSeg ส่วน `chunks` ใช้ SQL ตรงๆ เพราะต้องใช้ type `vector` และ operator `<=>` ของ pgvector
- `DocumentService.upload` ไม่ใส่ `@Transactional` ทั้งเมธอด เพราะการรอ Python อาจนานหลายนาที ไม่ควรถือ connection ฐานข้อมูลค้างไว้ จึงเปิด transaction แค่ตอนบันทึก chunk ทั้งหมดพร้อมเปลี่ยนสถานะเป็น READY
- ถ้ามีขั้นไหนล้ม เอกสารจะถูกบันทึกเป็น `FAILED` พร้อมข้อความ error ไม่หายเงียบ
- สัปดาห์ 2 จะเพิ่ม `spring-ai-starter-model-openai` (มี BOM ใน `pom.xml` แล้ว) เพื่อให้ LLM ตอบพร้อมอ้างอิงหน้า
