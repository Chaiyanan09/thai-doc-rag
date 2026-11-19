"use client";

import { Fragment } from "react";

type Props = {
  text: string;
  sourceCount: number;
  activeN: number | null;
  onCite: (n: number) => void;
};

// backend จัดเลขอ้างอิงเป็นรูปแบบ [S1] [S2] แล้ว วงเล็บอื่น เช่น [14] จะแสดงเป็นข้อความปกติ
const CITATION = /(\[S\d+\])/g;

/** แสดงคำตอบของ AI โดยเปลี่ยน [S1] [S2] เป็นปุ่มเล็กๆ ที่กดแล้วเปิดแหล่งอ้างอิงนั้น */
export default function AnswerText({ text, sourceCount, activeN, onCite }: Props) {
  const parts = text.split(CITATION);
  return (
    <p className="whitespace-pre-line leading-8 text-zinc-800">
      {parts.map((part, i) => {
        const n = i % 2 === 1 ? Number(part.slice(2, -1)) : 0;
        if (n < 1 || n > sourceCount) {
          return <Fragment key={i}>{part}</Fragment>;
        }
        return (
          <button
            key={i}
            type="button"
            onClick={() => onCite(n)}
            aria-label={`ดูแหล่งอ้างอิง ${n}`}
            className={`mx-0.5 inline-flex h-5 min-w-5 -translate-y-0.5 items-center justify-center rounded px-1 align-middle text-[11px] font-medium ${
              activeN === n ? "bg-zinc-900 text-white" : "bg-zinc-200 text-zinc-700 hover:bg-zinc-300"
            }`}
          >
            {n}
          </button>
        );
      })}
    </p>
  );
}
