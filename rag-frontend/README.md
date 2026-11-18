# rag-frontend

หน้าเว็บของระบบ (Next.js 16 + React 19 + TypeScript + Tailwind 4) คุยกับ `rag-backend` อย่างเดียว ไม่เรียก Python ตรง

ตอนนี้ (สัปดาห์ 1) มีหน้าเดียวที่ `/` สำหรับ:
- ดูสถานะ Backend / Database / Model service
- อัปโหลด PDF และดูรายการเอกสาร (ลบได้)
- ทดสอบค้นหา ดูว่าค้นเจอ chunk ไหน หน้าไหน และ score เท่าไร

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
src/app/page.tsx              หน้าแรก
src/components/Workbench.tsx  เก็บรายการเอกสาร และรีเฟรชหลังอัปโหลด/ลบ
src/components/UploadCard.tsx
src/components/DocumentTable.tsx
src/components/SearchPanel.tsx
src/components/StatusBar.tsx  เช็ก /api/health ทุก 15 วินาที
src/lib/api.ts                ฟังก์ชันเรียก backend ทั้งหมด และ type ของข้อมูล
```

ฟอนต์ภาษาไทยคือ IBM Plex Sans Thai จากแพ็กเกจ `@fontsource` (เก็บไฟล์ไว้ในโปรเจกต์ ไม่ต้องโหลดจาก Google Fonts)

สัปดาห์ 2 จะเพิ่มหน้าแชทที่ให้ LLM ตอบพร้อมอ้างอิงหน้า
