import type { Metadata } from "next";
// ฟอนต์ภาษาไทยแบบเก็บไฟล์ไว้ในโปรเจกต์ (ไม่ต้องโหลดจาก Google Fonts ตอน build)
import "@fontsource/ibm-plex-sans-thai/400.css";
import "@fontsource/ibm-plex-sans-thai/500.css";
import "@fontsource/ibm-plex-sans-thai/600.css";
import "./globals.css";

export const metadata: Metadata = {
  title: "ThaiDocRAG",
  description: "ถามตอบจากเอกสารภาษาไทย พร้อมอ้างอิงหน้า",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="th" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
