"use client";

import { useRef, useState } from "react";
import { api, type DocumentItem } from "@/lib/api";

type Props = { onUploaded: () => void };

export default function UploadCard({ onUploaded }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<DocumentItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function upload() {
    if (!file) return;
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const doc = await api.uploadDocument(file);
      setResult(doc);
      setFile(null);
      if (inputRef.current) inputRef.current.value = "";
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
      onUploaded();
    }
  }

  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-6">
      <h2 className="text-lg font-semibold">อัปโหลดเอกสาร</h2>
      <p className="mt-1 text-sm text-zinc-500">
        ไฟล์ PDF ไม่เกิน 30 MB ระบบจะอ่าน ตัดเป็น chunk และสร้าง embedding ก่อนเก็บลงฐานข้อมูล
      </p>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          disabled={busy}
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-zinc-600 file:mr-3 file:rounded-lg file:border-0 file:bg-zinc-100 file:px-4 file:py-2 file:text-sm file:font-medium file:text-zinc-800 hover:file:bg-zinc-200"
        />
        <button
          type="button"
          onClick={upload}
          disabled={!file || busy}
          className="shrink-0 rounded-lg bg-zinc-900 px-5 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:bg-zinc-300"
        >
          {busy ? "กำลังประมวลผล…" : "อัปโหลด"}
        </button>
      </div>

      {busy && (
        <p className="mt-3 text-sm text-zinc-500">
          ไฟล์ยาวอาจใช้เวลาหลายนาที โดยเฉพาะถ้า bge-m3 รันบน CPU
        </p>
      )}

      {result && (
        <div className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
          <p>
            <span className="font-medium">{result.filename}</span>: {result.pageCount} หน้า,{" "}
            {result.chunkCount} chunk
          </p>
          {result.emptyPages.length > 0 && (
            <p className="mt-1 text-amber-800">
              หน้า {result.emptyPages.join(", ")} แทบไม่มีข้อความ (อาจเป็นหน้าสแกน ต้องใช้ OCR)
            </p>
          )}
        </div>
      )}

      {error && <p className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-800">{error}</p>}
    </section>
  );
}
