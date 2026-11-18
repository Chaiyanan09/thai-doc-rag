"use client";

import { useState } from "react";
import { api, type DocumentItem, type DocumentStatus } from "@/lib/api";

type Props = {
  documents: DocumentItem[];
  error: string | null;
  onChanged: () => void;
};

const STATUS: Record<DocumentStatus, { label: string; className: string }> = {
  READY: { label: "พร้อมใช้", className: "bg-emerald-50 text-emerald-800" },
  PROCESSING: { label: "กำลังประมวลผล", className: "bg-amber-50 text-amber-800" },
  FAILED: { label: "ล้มเหลว", className: "bg-rose-50 text-rose-800" },
};

const dateFormat = new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" });

export default function DocumentTable({ documents, error, onChanged }: Props) {
  const [deleting, setDeleting] = useState<string | null>(null);

  async function remove(doc: DocumentItem) {
    if (!confirm(`ลบ "${doc.filename}" และ chunk ทั้งหมดของไฟล์นี้?`)) return;
    setDeleting(doc.id);
    try {
      await api.deleteDocument(doc.id);
    } catch (e) {
      alert((e as Error).message);
    } finally {
      setDeleting(null);
      onChanged();
    }
  }

  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">เอกสารในระบบ</h2>
        <button type="button" onClick={onChanged} className="text-sm text-zinc-500 hover:text-zinc-900">
          รีเฟรช
        </button>
      </div>

      {error && <p className="mt-4 text-sm text-rose-700">{error}</p>}

      {!error && documents.length === 0 && (
        <p className="mt-4 text-sm text-zinc-500">ยังไม่มีเอกสาร ลองอัปโหลด PDF ด้านบน</p>
      )}

      {documents.length > 0 && (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-zinc-200 text-zinc-500">
              <tr>
                <th className="py-2 pr-4 font-medium">ไฟล์</th>
                <th className="py-2 pr-4 font-medium">สถานะ</th>
                <th className="py-2 pr-4 text-right font-medium">หน้า</th>
                <th className="py-2 pr-4 text-right font-medium">Chunk</th>
                <th className="py-2 pr-4 font-medium">อัปโหลดเมื่อ</th>
                <th className="py-2" />
              </tr>
            </thead>
            <tbody>
              {documents.map((doc) => (
                <tr key={doc.id} className="border-b border-zinc-100 align-top last:border-0">
                  <td className="py-3 pr-4">
                    <p className="font-medium text-zinc-900">{doc.filename}</p>
                    {doc.errorMessage && <p className="mt-1 text-xs text-rose-700">{doc.errorMessage}</p>}
                  </td>
                  <td className="py-3 pr-4">
                    <span className={`rounded-full px-2.5 py-1 text-xs ${STATUS[doc.status].className}`}>
                      {STATUS[doc.status].label}
                    </span>
                  </td>
                  <td className="py-3 pr-4 text-right tabular-nums">{doc.pageCount}</td>
                  <td className="py-3 pr-4 text-right tabular-nums">{doc.chunkCount}</td>
                  <td className="py-3 pr-4 whitespace-nowrap text-zinc-500">
                    {dateFormat.format(new Date(doc.createdAt))}
                  </td>
                  <td className="py-3 text-right">
                    <button
                      type="button"
                      onClick={() => remove(doc)}
                      disabled={deleting === doc.id}
                      className="text-sm text-zinc-400 hover:text-rose-700 disabled:opacity-50"
                    >
                      ลบ
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
