"use client";

import { useState, type FormEvent } from "react";
import { api, type SearchResult } from "@/lib/api";

/** สัปดาห์ 1: ทดสอบว่าค้นเจอ chunk ที่ถูกต้องไหม ก่อนจะส่งให้ LLM ตอบในสัปดาห์ 2 */
export default function SearchPanel() {
  const [query, setQuery] = useState("");
  const [limit, setLimit] = useState(5);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function search(e: FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setBusy(true);
    setError(null);
    try {
      setResult(await api.search(query, limit));
    } catch (err) {
      setResult(null);
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-6">
      <h2 className="text-lg font-semibold">ทดสอบค้นหา</h2>
      <p className="mt-1 text-sm text-zinc-500">
        พิมพ์คำถามภาษาไทย ระบบจะหา chunk ที่ความหมายใกล้ที่สุดด้วย vector search
      </p>

      <form onSubmit={search} className="mt-4 flex flex-col gap-3 sm:flex-row">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="เช่น ลาพักการศึกษาต้องทำอย่างไร"
          className="w-full rounded-lg border border-zinc-300 px-4 py-2 text-sm outline-none focus:border-zinc-900"
        />
        <select
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value))}
          className="rounded-lg border border-zinc-300 px-3 py-2 text-sm sm:w-32"
          aria-label="จำนวนผลลัพธ์"
        >
          {[3, 5, 10].map((n) => (
            <option key={n} value={n}>
              {n} ผลลัพธ์
            </option>
          ))}
        </select>
        <button
          type="submit"
          disabled={busy || !query.trim()}
          className="shrink-0 rounded-lg bg-zinc-900 px-5 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:bg-zinc-300"
        >
          {busy ? "กำลังค้น…" : "ค้นหา"}
        </button>
      </form>

      {error && <p className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-800">{error}</p>}

      {result && result.results.length === 0 && (
        <p className="mt-4 text-sm text-zinc-500">ไม่พบ chunk (ยังไม่มีเอกสารที่พร้อมใช้)</p>
      )}

      {result && result.results.length > 0 && (
        <ol className="mt-4 space-y-3">
          {result.results.map((hit, i) => (
            <li key={hit.chunkId} className="rounded-xl border border-zinc-200 p-4">
              <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
                <p className="font-medium text-zinc-900">
                  {i + 1}. {hit.filename} <span className="text-zinc-500">หน้า {hit.page}</span>
                </p>
                <p className="tabular-nums text-zinc-500">score {hit.score.toFixed(3)}</p>
              </div>
              <p className="mt-2 whitespace-pre-line text-sm leading-7 text-zinc-700">{hit.content}</p>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
