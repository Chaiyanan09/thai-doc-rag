import StatusBar from "@/components/StatusBar";
import Workbench from "@/components/Workbench";

export default function Home() {
  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">
      <header className="mb-8 space-y-3">
        <p className="text-sm font-medium text-zinc-500">สัปดาห์ 1 · นำเข้าเอกสารและทดสอบค้นหา</p>
        <h1 className="text-3xl font-semibold tracking-tight">ThaiDocRAG</h1>
        <p className="text-zinc-600">ถามตอบจากเอกสารภาษาไทย พร้อมอ้างอิงหน้า</p>
        <StatusBar />
      </header>
      <Workbench />
    </main>
  );
}
