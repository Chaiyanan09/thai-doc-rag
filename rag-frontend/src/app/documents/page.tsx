import type { Metadata } from "next";
import Workbench from "@/components/Workbench";

export const metadata: Metadata = {
  title: "จัดการเอกสาร · ThaiDocRAG",
};

export default function DocumentsPage() {
  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">
      <header className="mb-8 space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">จัดการเอกสาร</h1>
        <p className="text-zinc-600">อัปโหลด PDF และทดสอบว่าระบบค้นเจอ chunk ที่ถูกต้องไหม</p>
      </header>
      <Workbench />
    </main>
  );
}
