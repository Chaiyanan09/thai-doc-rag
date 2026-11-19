# rag-frontend

หน้าเว็บของระบบ (Next.js 16 + React 19 + TypeScript + Tailwind 4) คุยกับ `rag-backend` อย่างเดียว ไม่เรียก Python ตรง

มีหน้าเดียวคือหน้าแชท (`/`) ใช้เหมือนแอป AI ทั่วไป:

- ปุ่ม **+** ในช่องพิมพ์: อัปโหลด PDF จากเครื่อง หรือเลือกเอกสารที่เคยอัปโหลดไว้แล้ว (ลบเอกสารออกจากระบบได้จากเมนูนี้)
- ลากไฟล์ PDF มาวางตรงไหนของหน้าก็ได้
- ไฟล์ที่แนบเป็นของแชทนั้น คำถามต่อๆ ไปจะค้นเฉพาะไฟล์ที่แนบ ถ้าไม่แนบอะไรเลยจะค้นจากทุกเอกสารในระบบ (บอกไว้ใต้ช่องพิมพ์ว่ากำลังค้นจากไหน)
- กด "เริ่มแชทใหม่" เพื่อล้างไฟล์ที่แนบ
- แนบไฟล์เดิมซ้ำไม่ต้องรออ่านใหม่ backend ตรวจ SHA-256 แล้วใช้เอกสารเดิม
- คำตอบมีเลขอ้างอิง กดเลขเพื่อดูข้อความต้นฉบับจากไฟล์และหน้านั้น

แถบด้านบนแสดงสถานะ Backend / Database / Model service

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
src/app/layout.tsx              แถบด้านบน + สถานะ
src/app/page.tsx                หน้าแชท
src/components/ChatPanel.tsx    รายการข้อความ ไฟล์ที่แนบในแชท อัปโหลด ลากวาง
src/components/Composer.tsx     ช่องพิมพ์ ปุ่ม + การ์ดไฟล์ที่รอส่ง ปุ่มส่ง
src/components/AttachMenu.tsx   เมนูของปุ่ม + (อัปโหลด / เลือก / ลบเอกสารในระบบ)
src/components/FileChip.tsx     การ์ดไฟล์ PDF เล็กๆ
src/components/AnswerText.tsx   เปลี่ยน [S1] [S2] ในคำตอบเป็นปุ่ม
src/components/SourceCard.tsx   แหล่งอ้างอิง 1 ชิ้น (ไฟล์ หน้า score ข้อความ)
src/components/StatusBar.tsx    เช็ก /api/health ทุก 15 วินาที
src/components/icons.tsx        ไอคอน SVG
src/lib/api.ts                  ฟังก์ชันเรียก backend ทั้งหมด และ type ของข้อมูล
```

ตอนนี้แต่ละคำถามแยกกัน ระบบยังไม่จำบทสนทนาก่อนหน้า (เช่น ถามต่อว่า "แล้วถ้าป่วยล่ะ" จะไม่รู้ว่าหมายถึงเรื่องอะไร)

ฟอนต์ภาษาไทยคือ IBM Plex Sans Thai จากแพ็กเกจ `@fontsource` (เก็บไฟล์ไว้ในโปรเจกต์ ไม่ต้องโหลดจาก Google Fonts)

