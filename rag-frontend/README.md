# rag-frontend

หน้าเว็บของระบบ (Next.js 16 + React 19 + TypeScript + Tailwind 4) คุยกับ `rag-backend` อย่างเดียว ไม่เรียก Python ตรง

มี 2 หน้า:
- `/` **แชท**: ถามคำถาม ได้คำตอบที่มีเลขอ้างอิง กดเลขเพื่อดูข้อความต้นฉบับจากไฟล์และหน้านั้น
- `/documents` **จัดการเอกสาร**: อัปโหลด PDF ดูรายการ ลบ และทดสอบค้นหาแบบดู chunk ตรงๆ

แถบด้านบนแสดงสถานะ Backend / Database / Model service ทุกหน้า

## ติดตั้งครั้งแรก (VS Code terminal)

```powershell
cd D:\ThaiDocRAG\rag-frontend
npm install
copy .env.example .env.local
```

## รัน

```powershell
npm run dev
```

เปิด http://localhost:3001 (ใช้พอร์ต 3001 เพื่อไม่ชนกับ ToothSeg ที่ใช้ 3000)

ต้องเปิด backend ที่พอร์ต 8081 ก่อน ถ้าแถบสถานะเป็นสีแดงแปลว่ายังติดต่อไม่ได้

## โครงสร้าง

```
src/app/layout.tsx              แถบเมนูด้านบน + สถานะ
src/app/page.tsx                หน้าแชท
src/app/documents/page.tsx      หน้าจัดการเอกสาร
src/components/ChatPanel.tsx    รายการข้อความ ช่องพิมพ์ ตัวอย่างคำถาม
src/components/AnswerText.tsx   เปลี่ยน [1] [2] ในคำตอบเป็นปุ่ม
src/components/SourceCard.tsx   แหล่งอ้างอิง 1 ชิ้น (ไฟล์ หน้า score ข้อความ)
src/components/Workbench.tsx    หน้าจัดการเอกสาร (UploadCard, DocumentTable, SearchPanel)
src/components/StatusBar.tsx    เช็ก /api/health ทุก 15 วินาที
src/lib/api.ts                  ฟังก์ชันเรียก backend ทั้งหมด และ type ของข้อมูล
```

ตอนนี้แต่ละคำถามแยกกัน ระบบยังไม่จำบทสนทนาก่อนหน้า (เช่น ถามต่อว่า "แล้วถ้าป่วยล่ะ" จะไม่รู้ว่าหมายถึงเรื่องอะไร)

ฟอนต์ภาษาไทยคือ IBM Plex Sans Thai จากแพ็กเกจ `@fontsource` (เก็บไฟล์ไว้ในโปรเจกต์ ไม่ต้องโหลดจาก Google Fonts)

