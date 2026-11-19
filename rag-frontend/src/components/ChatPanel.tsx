"use client";

import { useEffect, useEffectEvent, useRef, useState } from "react";
import { api, type ChatReply, type DocumentItem } from "@/lib/api";
import AnswerText from "./AnswerText";
import Composer, { type Attachment } from "./Composer";
import FileChip from "./FileChip";
import SourceCard from "./SourceCard";

type Message =
  | { id: number; role: "user"; text: string; files: DocumentItem[] }
  | { id: number; role: "assistant"; reply: ChatReply }
  | { id: number; role: "error"; text: string };

const MAX_FILE_MB = 30; // ตรงกับ spring.servlet.multipart.max-file-size
const MAX_CHAT_FILES = 20; // ตรงกับ ChatService.MAX_DOCUMENTS

function isPdf(file: File) {
  return file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
}

/** รวมรายการเอกสารโดยไม่ซ้ำ id (ลำดับตามที่แนบ) */
function unique(docs: DocumentItem[]) {
  return [...new Map(docs.map((d) => [d.id, d])).values()];
}

/**
 * หน้าแชท: แนบ PDF ด้วยปุ่ม + (หรือลากไฟล์มาวาง) แล้วถามได้เลย
 *
 * ไฟล์ที่แนบเป็นของแชทนี้ คำถามต่อๆ ไปจะค้นเฉพาะไฟล์เหล่านั้น ถ้ายังไม่แนบอะไรเลยจะค้นจากทุกเอกสารในระบบ
 * กด "เริ่มแชทใหม่" เพื่อล้างไฟล์ที่แนบ (แต่ละคำถามยังแยกกัน AI ไม่จำบทสนทนาก่อนหน้า)
 */
export default function ChatPanel() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [chatDocs, setChatDocs] = useState<DocumentItem[]>([]); // ไฟล์ที่ส่งไปกับคำถามแล้วในแชทนี้
  const [pending, setPending] = useState<Attachment[]>([]); // ไฟล์ในช่องพิมพ์ รอส่งพร้อมคำถามถัดไป
  const [library, setLibrary] = useState<DocumentItem[] | null>(null); // เอกสารทั้งหมดในระบบ
  const [dragging, setDragging] = useState(false);
  const nextId = useRef(1);
  const nextKey = useRef(1);
  const bottomRef = useRef<HTMLDivElement>(null);

  const pendingDocs = pending.flatMap((a) => (a.status === "ready" ? [a.doc] : []));
  const scope = unique([...chatDocs, ...pendingDocs]);
  const scopeIds = new Set(scope.map((d) => d.id));
  const readyCount = library?.filter((d) => d.status === "READY").length;


  let scopeLabel = "";
  if (scope.length > 0) scopeLabel = `ค้นเฉพาะไฟล์ที่แนบในแชทนี้ (${scope.length} ไฟล์)`;
  else if (readyCount === 0) scopeLabel = "ยังไม่มีเอกสาร กด + เพื่อแนบ PDF";
  else if (readyCount !== undefined) scopeLabel = `ไม่ได้แนบไฟล์ ค้นจากเอกสารทั้งหมด (${readyCount} ไฟล์)`;

  function refreshLibrary() {
    api
      .listDocuments()
      .then(setLibrary)
      .catch(() => setLibrary((current) => current ?? []));
  }

  useEffect(() => {
    let cancelled = false;
    api
      .listDocuments()
      .then((docs) => !cancelled && setLibrary(docs))
      .catch(() => !cancelled && setLibrary([]));
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, busy]);

  function setAttachment(key: string, next: Attachment | null) {
    setPending((list) => {
      if (!list.some((a) => a.key === key)) return list; // ผู้ใช้กด × ไปแล้วระหว่างอัปโหลด
      if (next?.status === "ready" && list.some((a) => a.key !== key && a.status === "ready" && a.doc.id === next.doc.id)) {
        return list.filter((a) => a.key !== key); // ไฟล์เดียวกันแนบซ้ำ
      }
      return next ? list.map((a) => (a.key === key ? next : a)) : list.filter((a) => a.key !== key);
    });
  }

  async function upload(key: string, file: File) {
    try {
      const doc = await api.uploadDocument(file);
      if (doc.status !== "READY") throw new Error(doc.errorMessage ?? "อ่านไฟล์ไม่สำเร็จ");
      setAttachment(key, { key, filename: doc.filename, status: "ready", doc });
    } catch (e) {
      setAttachment(key, { key, filename: file.name, status: "error", error: (e as Error).message });
    } finally {
      refreshLibrary();
    }
  }

  function addFiles(files: File[]) {
    let room = MAX_CHAT_FILES - scope.length - pending.filter((a) => a.status === "uploading").length;
    const added: Attachment[] = [];
    for (const file of files) {
      const key = `f${nextKey.current++}`;
      const base = { key, filename: file.name };
      if (!isPdf(file)) {
        added.push({ ...base, status: "error", error: "รองรับเฉพาะไฟล์ PDF" });
      } else if (file.size > MAX_FILE_MB * 1024 * 1024) {
        added.push({ ...base, status: "error", error: `ไฟล์ใหญ่เกิน ${MAX_FILE_MB} MB` });
      } else if (room <= 0) {
        added.push({ ...base, status: "error", error: `แนบได้ไม่เกิน ${MAX_CHAT_FILES} ไฟล์ต่อแชท` });
      } else {
        room--;
        added.push({ ...base, status: "uploading" });
        upload(key, file);
      }
    }
    setPending((list) => [...list, ...added]);
  }

  function pickExisting(doc: DocumentItem) {
    if (scopeIds.has(doc.id)) return;
    const key = `f${nextKey.current++}`;
    setPending((list) => [...list, { key, filename: doc.filename, status: "ready", doc }]);
  }

  async function deleteDocument(doc: DocumentItem) {
    await api.deleteDocument(doc.id);
    setChatDocs((list) => list.filter((d) => d.id !== doc.id));
    setPending((list) => list.filter((a) => a.status !== "ready" || a.doc.id !== doc.id));
    setLibrary((list) => list?.filter((d) => d.id !== doc.id) ?? null);
  }

  // ลากไฟล์มาวางตรงไหนของหน้าก็ได้ (ถ้าไม่ดักไว้ เบราว์เซอร์จะเปิด PDF แทนหน้าเว็บ)
  const onDropFiles = useEffectEvent((files: File[]) => addFiles(files));
  useEffect(() => {
    let depth = 0;
    const hasFiles = (e: DragEvent) => e.dataTransfer?.types.includes("Files") ?? false;
    const enter = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      depth++;
      setDragging(true);
    };
    const over = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      if (e.dataTransfer) e.dataTransfer.dropEffect = "copy";
    };
    const leave = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      depth = Math.max(0, depth - 1);
      if (depth === 0) setDragging(false);
    };
    const drop = (e: DragEvent) => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      depth = 0;
      setDragging(false);
      onDropFiles(Array.from(e.dataTransfer?.files ?? []));
    };
    window.addEventListener("dragenter", enter);
    window.addEventListener("dragover", over);
    window.addEventListener("dragleave", leave);
    window.addEventListener("drop", drop);
    return () => {
      window.removeEventListener("dragenter", enter);
      window.removeEventListener("dragover", over);
      window.removeEventListener("dragleave", leave);
      window.removeEventListener("drop", drop);
    };
  }, []);

  async function send(text: string) {
    const question = text.trim();
    if (!question || busy || pending.some((a) => a.status === "uploading")) return;
    const files = unique(pendingDocs);
    const docs = scope;
    setChatDocs(docs);
    setPending([]);
    setMessages((m) => [...m, { id: nextId.current++, role: "user", text: question, files }]);
    setInput("");
    setBusy(true);
    try {
      const reply = await api.chat(question, docs.map((d) => d.id));
      setMessages((m) => [...m, { id: nextId.current++, role: "assistant", reply }]);
    } catch (e) {
      setMessages((m) => [...m, { id: nextId.current++, role: "error", text: (e as Error).message }]);
    } finally {
      setBusy(false);
    }
  }

  function newChat() {
    setMessages([]);
    setChatDocs([]);
    setPending([]);
  }

  const composer = (
    <Composer
      value={input}
      onChange={setInput}
      onSend={() => send(input)}
      busy={busy}
      pending={pending}
      onRemove={(key) => setAttachment(key, null)}
      onFiles={addFiles}
      library={library}
      attachedIds={scopeIds}
      onOpenMenu={refreshLibrary}
      onPick={pickExisting}
      onDelete={deleteDocument}
      scopeLabel={scopeLabel}
    />
  );

  return (
    <div className="flex flex-1 flex-col">
      {dragging && (
        <div className="pointer-events-none fixed inset-0 z-30 flex items-center justify-center bg-zinc-900/10 p-6 backdrop-blur-[1px]">
          <div className="rounded-3xl border-2 border-dashed border-zinc-400 bg-white/90 px-10 py-8 text-center">
            <p className="text-lg font-medium text-zinc-900">วางไฟล์ PDF เพื่อแนบในแชท</p>
            <p className="mt-1 text-sm text-zinc-500">ไม่เกิน {MAX_FILE_MB} MB ต่อไฟล์</p>
          </div>
        </div>
      )}

      <div className="flex-1 space-y-6 pb-6">
        {messages.length === 0 && (
          <div className="pt-10 text-center">
            <h1 className="text-2xl font-semibold tracking-tight">ถามอะไรเกี่ยวกับเอกสารก็ได้</h1>
            <p className="mt-2 text-zinc-500">
              <span className="block">กด + เพื่อแนบ PDF จากเครื่อง แล้วถามได้เลย</span>
              <span className="block">ระบบตอบจากเอกสารเท่านั้น พร้อมบอกว่ามาจากไฟล์ไหน หน้าไหน</span>
            </p>
          </div>
        )}

        {messages.map((message) => {
          if (message.role === "user") {
            return (
              <div key={message.id} className="flex flex-col items-end gap-2">
                {message.files.length > 0 && (
                  <div className="flex max-w-[85%] flex-wrap justify-end gap-2">
                    {message.files.map((f) => (
                      <FileChip key={f.id} filename={f.filename} detail={`${f.pageCount} หน้า`} />
                    ))}
                  </div>
                )}
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
            {scope.length > 0 ? "กำลังค้นในไฟล์ที่แนบและเรียบเรียงคำตอบ…" : "กำลังค้นเอกสารและเรียบเรียงคำตอบ…"}
          </p>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="sticky bottom-0 bg-background pb-6 pt-2">
        {(messages.length > 0 || chatDocs.length > 0) && (
          <div className="mb-2 flex justify-end">
            <button
              type="button"
              onClick={newChat}
              disabled={busy}
              className="text-xs text-zinc-400 hover:text-zinc-700 disabled:opacity-50"
            >
              เริ่มแชทใหม่
            </button>
          </div>
        )}
        {composer}
      </div>
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
          <p className="mt-1 text-sm text-zinc-500">ลองถามด้วยคำอื่น หรือกด + แนบเอกสารที่เกี่ยวข้อง</p>
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
        {reply.citationFixes > 0 && <span>ระบบแก้เลขอ้างอิง {reply.citationFixes} จุด</span>}
        <span>{reply.model}</span>
      </div>
    </div>
  );
}
