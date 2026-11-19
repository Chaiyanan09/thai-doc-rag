"use client";

import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { api, type ChatReply } from "@/lib/api";
import AnswerText from "./AnswerText";
import SourceCard from "./SourceCard";

type Message =
  | { id: number; role: "user"; text: string }
  | { id: number; role: "assistant"; reply: ChatReply }
  | { id: number; role: "error"; text: string };

const EXAMPLES = [
  "ลาพักการศึกษาต้องทำอย่างไร",
  "เกรดเท่าไหร่ถึงโดนรีไทร์",
  "ค่าเทอมเท่าไหร่",
  "มหาวิทยาลัยมีหอพักให้ไหม",
];

/** หน้าแชท: ส่งคำถามไป /api/chat แล้วแสดงคำตอบพร้อมแหล่งอ้างอิง (ตอนนี้แต่ละคำถามแยกกัน ไม่จำบทสนทนาก่อนหน้า) */
export default function ChatPanel() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const nextId = useRef(1);
  const bottomRef = useRef<HTMLDivElement>(null);

  const asked = new Set(messages.flatMap((m) => (m.role === "user" ? [m.text] : [])));
  const suggestions = EXAMPLES.filter((q) => !asked.has(q));

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, busy]);

  async function send(text: string) {
    const question = text.trim();
    if (!question || busy) return;
    const userId = nextId.current++;
    setMessages((m) => [...m, { id: userId, role: "user", text: question }]);
    setInput("");
    setBusy(true);
    try {
      const reply = await api.chat(question);
      const id = nextId.current++;
      setMessages((m) => [...m, { id, role: "assistant", reply }]);
    } catch (e) {
      const id = nextId.current++;
      setMessages((m) => [...m, { id, role: "error", text: (e as Error).message }]);
    } finally {
      setBusy(false);
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    send(input);
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter = ส่ง, Shift+Enter = ขึ้นบรรทัดใหม่ (ไม่ส่งระหว่างกำลังพิมพ์ด้วย IME)
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      send(input);
    }
  }

  return (
    <div className="flex flex-1 flex-col">
      <div className="flex-1 space-y-6 pb-6">
        {messages.length === 0 && (
          <div className="pt-10 text-center">
            <h1 className="text-2xl font-semibold tracking-tight">ถามอะไรเกี่ยวกับเอกสารก็ได้</h1>
            <p className="mt-2 text-zinc-500">ระบบตอบจากเอกสารที่อัปโหลดเท่านั้น พร้อมบอกว่ามาจากไฟล์ไหน หน้าไหน</p>
            <div className="mt-8 flex flex-wrap justify-center gap-2">
              {EXAMPLES.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => send(q)}
                  className="rounded-full border border-zinc-200 bg-white px-4 py-2 text-sm text-zinc-700 hover:border-zinc-400"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message) => {
          if (message.role === "user") {
            return (
              <div key={message.id} className="flex justify-end">
                <p className="max-w-[85%] whitespace-pre-line rounded-2xl bg-zinc-900 px-4 py-2.5 text-white">
                  {message.text}
                </p>
              </div>
            );
          }
          if (message.role === "error") {
            return (
              <p key={message.id} className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-800">
                {message.text}
              </p>
            );
          }
          return <AssistantMessage key={message.id} reply={message.reply} />;
        })}

        {busy && (
          <p className="animate-pulse rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-500">
            กำลังค้นเอกสารและเรียบเรียงคำตอบ…
          </p>
        )}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={onSubmit} className="sticky bottom-0 bg-background pb-6 pt-2">
        {messages.length > 0 && (
          <div className="mb-2 flex flex-wrap items-center gap-2">
            {suggestions.map((q) => (
              <button
                key={q}
                type="button"
                onClick={() => send(q)}
                disabled={busy}
                className="rounded-full border border-zinc-200 bg-white px-3 py-1 text-xs text-zinc-600 hover:border-zinc-400 disabled:opacity-50"
              >
                {q}
              </button>
            ))}
            <button
              type="button"
              onClick={() => setMessages([])}
              disabled={busy}
              className="ml-auto text-xs text-zinc-400 hover:text-zinc-700 disabled:opacity-50"
            >
              เริ่มแชทใหม่
            </button>
          </div>
        )}
        <div className="flex items-end gap-2 rounded-2xl border border-zinc-300 bg-white p-2 focus-within:border-zinc-900">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            rows={1}
            placeholder="พิมพ์คำถาม แล้วกด Enter"
            className="max-h-40 flex-1 resize-none bg-transparent px-2 py-1.5 outline-none field-sizing-content"
          />
          <button
            type="submit"
            disabled={busy || !input.trim()}
            className="rounded-xl bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:bg-zinc-300"
          >
            ส่ง
          </button>
        </div>
      </form>
    </div>
  );
}

function AssistantMessage({ reply }: { reply: ChatReply }) {
  const [active, setActive] = useState<number | null>(null);
  const [showAll, setShowAll] = useState(false);

  const cited = reply.sources.filter((s) => s.cited);
  // ถ้า AI ตอบโดยไม่ใส่เลขอ้างอิง ให้แสดงทุกชิ้นที่ส่งไป
  const visible = showAll || cited.length === 0 ? reply.sources : cited;

  function openSource(n: number) {
    if (!cited.some((s) => s.n === n)) setShowAll(true);
    setActive(n);
  }

  if (!reply.found) {
    return (
      <div>
        <div className="rounded-2xl bg-zinc-100 px-4 py-3 text-zinc-600">
          <p>{reply.answer}</p>
          <p className="mt-1 text-sm text-zinc-500">ลองถามด้วยคำอื่น หรือตรวจว่าอัปโหลดเอกสารที่เกี่ยวข้องแล้ว</p>
        </div>
        {/* ให้เห็นว่าระบบค้นเจออะไร จะได้รู้ว่าพลาดที่การค้นหรือที่ AI */}
        {reply.sources.length > 0 && (
          <>
            <button
              type="button"
              onClick={() => setShowAll(!showAll)}
              className="mt-2 text-xs text-zinc-400 hover:text-zinc-700"
            >
              {showAll ? "ซ่อนข้อมูลที่ค้นเจอ" : `ดู ${reply.sources.length} ชิ้นที่ระบบค้นเจอ แต่ AI ตัดสินว่าไม่ตอบคำถามนี้`}
            </button>
            {showAll && (
              <div className="mt-2 space-y-2">
                {reply.sources.map((source) => (
                  <SourceCard
                    key={source.n}
                    source={source}
                    active={active === source.n}
                    onToggle={() => setActive(active === source.n ? null : source.n)}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    );
  }

  return (
    <div>
      <div className="rounded-2xl border border-zinc-200 bg-white px-4 py-3">
        <AnswerText text={reply.answer} sourceCount={reply.sources.length} activeN={active} onCite={openSource} />
      </div>

      <p className="mt-3 mb-2 text-xs text-zinc-500">
        {cited.length === 0
          ? "AI ไม่ได้ระบุแหล่งอ้างอิง นี่คือข้อมูลทั้งหมดที่ส่งให้อ่าน"
          : showAll
            ? `ข้อมูลทั้ง ${reply.sources.length} ชิ้นที่ส่งให้ AI อ่าน`
            : "แหล่งอ้างอิง"}
      </p>
      <div className="space-y-2">
        {visible.map((source) => (
          <SourceCard
            key={source.n}
            source={source}
            active={active === source.n}
            onToggle={() => setActive(active === source.n ? null : source.n)}
          />
        ))}
      </div>

      <div className="mt-2 flex flex-wrap gap-x-3 text-xs text-zinc-400">
        {cited.length > 0 && reply.sources.length > cited.length && (
          <button type="button" onClick={() => setShowAll(!showAll)} className="hover:text-zinc-700">
            {showAll ? "แสดงเฉพาะที่อ้างถึง" : `ดูทั้ง ${reply.sources.length} ชิ้นที่ส่งให้ AI อ่าน`}
          </button>
        )}
        <span>ค้นหา {reply.retrievalMs} ms</span>
        <span>ตอบ {(reply.llmMs / 1000).toFixed(1)} วินาที</span>
        <span>{reply.model}</span>
      </div>
    </div>
  );
}
