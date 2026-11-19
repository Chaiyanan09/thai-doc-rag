import type { Metadata } from "next";
import Link from "next/link";
// ฟอนต์ภาษาไทยแบบเก็บไฟล์ไว้ในโปรเจกต์ (ไม่ต้องโหลดจาก Google Fonts ตอน build)
import "@fontsource/ibm-plex-sans-thai/400.css";
import "@fontsource/ibm-plex-sans-thai/500.css";
import "@fontsource/ibm-plex-sans-thai/600.css";
import NavLinks from "@/components/NavLinks";
import StatusBar from "@/components/StatusBar";
import "./globals.css";

export const metadata: Metadata = {
  title: "ThaiDocRAG",
  description: "ถามตอบจากเอกสารภาษาไทย พร้อมอ้างอิงหน้า",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="th" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <header className="border-b border-zinc-200 bg-white">
          <div className="mx-auto flex w-full max-w-4xl flex-wrap items-center justify-between gap-3 px-6 py-3">
            <div className="flex items-center gap-4">
              <Link href="/" className="font-semibold tracking-tight">
                ThaiDocRAG
              </Link>
              <NavLinks />
            </div>
            <StatusBar />
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
