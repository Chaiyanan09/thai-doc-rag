"use client";

import { useEffect, useEffectEvent, useRef, useState } from "react";
import type { DocumentItem } from "@/lib/api";
import { CheckIcon, PaperclipIcon, TrashIcon } from "./icons";

type Props = {
  library: DocumentItem[] | null; // null = ยังโหลดไม่เสร็จหรือโหลดไม่ได้
  attachedIds: Set<string>;
  onUpload: () => void;
  onPick: (doc: DocumentItem) => void;
  onDelete: (doc: DocumentItem) => Promise<void>;
  onClose: () => void;
};

/**
 * เมนูของปุ่ม + : อัปโหลด PDF จากเครื่อง หรือเลือกเอกสารที่เคยอัปโหลดไว้แล้วมาใส่ในแชท
 * (ลบเอกสารออกจากระบบได้ตรงนี้ด้วย แทนหน้าจัดการเอกสารเดิม)
 */
export default function AttachMenu({ library, attachedIds, onUpload, onPick, onDelete, onClose }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const close = useEffectEvent(onClose);

  useEffect(() => {
    function onPointer(e: PointerEvent) {
      const target = e.target as Element | null;
      // ปุ่ม + มี data-attach-toggle ให้ปุ่มจัดการเปิด/ปิดเอง ไม่งั้นกดแล้วเมนูปิดแล้วเปิดใหม่ทันที
      if (ref.current?.contains(target) || target?.closest("[data-attach-toggle]")) return;
      close();
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") close();
    }
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  async function remove(doc: DocumentItem) {
    if (!window.confirm(`ลบ "${doc.filename}" ออกจากระบบ?\nข้อมูลที่ค้นได้จากไฟล์นี้จะหายไปด้วย`)) return;
    setDeleting(doc.id);
    setError(null);
    try {
      await onDelete(doc);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setDeleting(null);
    }
  }

  return (
    <div
      ref={ref}
      role="menu"
      className="absolute bottom-full left-0 z-20 mb-2 w-[min(22rem,calc(100vw-3rem))] overflow-hidden rounded-2xl border border-zinc-200 bg-white text-sm shadow-lg"
    >
      <button
        type="button"
        role="menuitem"
        onClick={onUpload}
        className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-zinc-50"
      >
        <PaperclipIcon className="h-4 w-4 text-zinc-500" />
        <span>
          <span className="block text-zinc-900">อัปโหลด PDF จากเครื่อง</span>
          <span className="block text-xs text-zinc-500">หรือลากไฟล์มาวางในหน้าแชท · ไม่เกิน 30 MB</span>
        </span>
      </button>

      <div className="border-t border-zinc-100 px-4 pb-1 pt-3 text-xs font-medium text-zinc-500">เอกสารในระบบ</div>
      <div className="max-h-64 overflow-y-auto pb-2">
        {library === null && <p className="px-4 py-2 text-xs text-zinc-400">กำลังโหลด…</p>}
        {library?.length === 0 && <p className="px-4 py-2 text-xs text-zinc-400">ยังไม่มีเอกสาร</p>}
        {library?.map((doc) => {
          const attached = attachedIds.has(doc.id);
          const ready = doc.status === "READY";
          return (
            <div key={doc.id} className="group flex items-center gap-1 pr-2 hover:bg-zinc-50">
              <button
                type="button"
                role="menuitem"
                disabled={!ready || attached}
                onClick={() => onPick(doc)}
                title={ready ? (attached ? "อยู่ในแชทนี้แล้ว" : "ใช้ไฟล์นี้ในแชท") : (doc.errorMessage ?? undefined)}
                className="flex min-w-0 flex-1 items-center gap-3 px-4 py-2 text-left disabled:cursor-default"
              >
                <span className="flex h-4 w-4 shrink-0 items-center justify-center text-emerald-600">
                  {attached && <CheckIcon className="h-4 w-4" />}
                </span>
                <span className="min-w-0">
                  <span className={`block truncate ${ready ? "text-zinc-800" : "text-zinc-400"}`}>{doc.filename}</span>
                  <span className={`block text-xs ${doc.status === "FAILED" ? "text-rose-600" : "text-zinc-400"}`}>
                    {doc.status === "READY"
                      ? `${doc.pageCount} หน้า${attached ? " · อยู่ในแชทนี้แล้ว" : ""}`
                      : doc.status === "PROCESSING"
                        ? "กำลังอ่านไฟล์…"
                        : "อ่านไฟล์ไม่สำเร็จ"}
                  </span>
                </span>
              </button>
              <button
                type="button"
                onClick={() => remove(doc)}
                disabled={deleting !== null}
                aria-label={`ลบ ${doc.filename}`}
                title="ลบออกจากระบบ"
                className="rounded-lg p-1.5 text-zinc-300 hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50 group-hover:text-zinc-400"
              >
                <TrashIcon className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
      {error && <p className="border-t border-rose-100 bg-rose-50 px-4 py-2 text-xs text-rose-700">{error}</p>}
    </div>
  );
}
