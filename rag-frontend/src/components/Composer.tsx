"use client";

import { useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import type { DocumentItem } from "@/lib/api";
import AttachMenu from "./AttachMenu";
import FileChip from "./FileChip";
import { ArrowUpIcon, PlusIcon } from "./icons";

/** ไฟล์ที่แนบไว้ในช่องพิมพ์ รอส่งไปพร้อมคำถามถัดไป */
export type Attachment =
  | { key: string; filename: string; status: "uploading" }
  | { key: string; filename: string; status: "ready"; doc: DocumentItem }
  | { key: string; filename: string; status: "error"; error: string };

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  busy: boolean;
  pending: Attachment[];
  onRemove: (key: string) => void;
  onFiles: (files: File[]) => void;
  library: DocumentItem[] | null;
  attachedIds: Set<string>;
  onOpenMenu: () => void;
  onPick: (doc: DocumentItem) => void;
  onDelete: (doc: DocumentItem) => Promise<void>;
  scopeLabel: string;
};

/** ช่องพิมพ์คำถามแบบเดียวกับแอป AI ทั่วไป: ปุ่ม + แนบไฟล์ การ์ดไฟล์อยู่เหนือข้อความ ปุ่มส่งอยู่มุมขวา */
export default function Composer(props: Props) {
  const { value, onChange, onSend, busy, pending, onRemove, onFiles, scopeLabel } = props;
  const [menuOpen, setMenuOpen] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  const uploading = pending.some((a) => a.status === "uploading");
  const canSend = !busy && !uploading && value.trim().length > 0;

  function submit(e: FormEvent) {
    e.preventDefault();
    if (canSend) onSend();
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter = ส่ง, Shift+Enter = ขึ้นบรรทัดใหม่ (ไม่ส่งระหว่างกำลังพิมพ์ด้วย IME)
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      if (canSend) onSend();
    }
  }

  function toggleMenu() {
    if (!menuOpen) props.onOpenMenu();
    setMenuOpen(!menuOpen);
  }

  return (
    <form
      onSubmit={submit}
      className="rounded-2xl border border-zinc-300 bg-white shadow-sm focus-within:border-zinc-500"
    >
      {pending.length > 0 && (
        <div className="flex flex-wrap gap-2 px-3 pt-3">
          {pending.map((a) => (
            <FileChip
              key={a.key}
              filename={a.filename}
              status={a.status}
              detail={
                a.status === "uploading" ? "กำลังอ่านไฟล์…" : a.status === "error" ? a.error : `${a.doc.pageCount} หน้า`
              }
              onRemove={() => onRemove(a.key)}
            />
          ))}
        </div>
      )}

      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={onKeyDown}
        rows={1}
        placeholder={pending.length > 0 ? "ถามเกี่ยวกับไฟล์ที่แนบ…" : "พิมพ์คำถาม แล้วกด Enter"}
        className="block max-h-48 w-full resize-none bg-transparent px-4 pb-1 pt-3 outline-none field-sizing-content"
      />

      <div className="flex items-center gap-2 px-2 pb-2">
        <div className="relative">
          <button
            type="button"
            data-attach-toggle
            onClick={toggleMenu}
            aria-label="แนบไฟล์"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            title="แนบไฟล์ PDF"
            className={`flex h-8 w-8 items-center justify-center rounded-lg border text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 ${
              menuOpen ? "border-zinc-400 bg-zinc-50" : "border-zinc-200"
            }`}
          >
            <PlusIcon className={`h-4 w-4 transition-transform ${menuOpen ? "rotate-45" : ""}`} />
          </button>
          {menuOpen && (
            <AttachMenu
              library={props.library}
              attachedIds={props.attachedIds}
              onUpload={() => {
                setMenuOpen(false);
                fileInput.current?.click();
              }}
              onPick={(doc) => {
                setMenuOpen(false);
                props.onPick(doc);
              }}
              onDelete={props.onDelete}
              onClose={() => setMenuOpen(false)}
            />
          )}
          <input
            ref={fileInput}
            type="file"
            accept="application/pdf,.pdf"
            multiple
            hidden
            onChange={(e) => {
              onFiles(Array.from(e.target.files ?? []));
              e.target.value = ""; // เลือกไฟล์เดิมซ้ำได้
            }}
          />
        </div>

        <span className="min-w-0 flex-1 truncate text-xs text-zinc-400">{scopeLabel}</span>

        <button
          type="submit"
          disabled={!canSend}
          aria-label="ส่ง"
          title={uploading ? "รออ่านไฟล์ให้เสร็จก่อน" : "ส่ง (Enter)"}
          className="flex h-8 w-8 items-center justify-center rounded-lg bg-zinc-900 text-white disabled:cursor-not-allowed disabled:bg-zinc-200 disabled:text-zinc-400"
        >
          <ArrowUpIcon className="h-4 w-4" />
        </button>
      </div>
    </form>
  );
}
