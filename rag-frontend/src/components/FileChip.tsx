import { Spinner, XIcon } from "./icons";

type Props = {
  filename: string;
  detail?: string; // บรรทัดเล็กใต้ชื่อไฟล์ เช่น "7 หน้า" หรือข้อความ error
  status?: "ready" | "uploading" | "error";
  onRemove?: () => void;
};

/** การ์ดไฟล์ PDF ขนาดเล็ก ใช้ทั้งในช่องพิมพ์ (ก่อนส่ง) และในข้อความของผู้ใช้ (ส่งแล้ว) */
export default function FileChip({ filename, detail, status = "ready", onRemove }: Props) {
  const error = status === "error";
  const ext = filename.includes(".") ? filename.split(".").pop()!.slice(0, 4).toUpperCase() : "FILE";
  return (
    <div
      title={error && detail ? `${filename}\n${detail}` : filename}
      className={`group relative flex max-w-64 items-center gap-2.5 rounded-xl border py-1.5 pl-1.5 pr-3 text-left ${
        error ? "border-rose-200 bg-rose-50" : "border-zinc-200 bg-white"
      }`}
    >
      <span
        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-[10px] font-semibold ${
          status === "uploading" ? "bg-zinc-100 text-zinc-500" : error ? "bg-rose-100 text-rose-700" : "bg-rose-600 text-white"
        }`}
      >
        {status === "uploading" ? <Spinner /> : ext}
      </span>
      <span className="min-w-0">
        <span className="block truncate text-sm text-zinc-800">{filename}</span>
        {detail && (
          <span className={`block truncate text-xs ${error ? "text-rose-700" : "text-zinc-500"}`}>{detail}</span>
        )}
      </span>
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          aria-label={`เอา ${filename} ออก`}
          className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full border border-zinc-200 bg-white text-zinc-500 shadow-sm hover:text-zinc-900"
        >
          <XIcon className="h-3 w-3" />
        </button>
      )}
    </div>
  );
}
