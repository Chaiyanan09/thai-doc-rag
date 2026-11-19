"use client";

import { useEffect, useRef } from "react";
import type { ChatSource } from "@/lib/api";

type Props = {
  source: ChatSource;
  active: boolean;
  onToggle: () => void;
};

/** แหล่งอ้างอิง 1 ชิ้น: ชื่อไฟล์ หน้า และข้อความต้นฉบับ (กดเพื่อขยาย) */
export default function SourceCard({ source, active, onToggle }: Props) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (active) ref.current?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }, [active]);

  return (
    <div
      ref={ref}
      className={`rounded-xl border bg-white p-3 text-sm ${active ? "border-zinc-900" : "border-zinc-200"}`}
    >
      <button type="button" onClick={onToggle} className="flex w-full items-baseline justify-between gap-3 text-left">
        <span className="text-zinc-800">
          <span className="mr-2 inline-flex h-5 min-w-5 items-center justify-center rounded bg-zinc-100 px-1 text-[11px] font-medium text-zinc-700">
            {source.n}
          </span>
          {source.filename} <span className="text-zinc-500">· หน้า {source.page}</span>
        </span>
        <span className="shrink-0 text-xs tabular-nums text-zinc-400">score {source.score.toFixed(3)}</span>
      </button>
      <p className={`mt-2 whitespace-pre-line leading-7 text-zinc-600 ${active ? "" : "line-clamp-2"}`}>
        {source.content}
      </p>
    </div>
  );
}
