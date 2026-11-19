// เรียก Spring Boot (rag-backend) ทั้งหมดผ่านไฟล์นี้
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export interface DocumentItem {
  id: string;
  filename: string;
  status: DocumentStatus;
  pageCount: number;
  chunkCount: number;
  emptyPages: number[];
  errorMessage: string | null;
  createdAt: string;
}

export interface ChatSource {
  n: number; // เลขอ้างอิง [n] ในคำตอบ
  chunkId: number;
  documentId: string;
  filename: string;
  page: number;
  content: string;
  score: number;
  cited: boolean; // คำตอบอ้างถึงชิ้นนี้หรือไม่
}

export interface ChatReply {
  question: string;
  answer: string;
  found: boolean; // false = เอกสารไม่มีคำตอบ
  sources: ChatSource[];
  model: string;
  retrievalMs: number;
  llmMs: number;
  citationFixes: number; // จำนวนประโยคที่ระบบตรวจแล้วแก้เลขอ้างอิงให้
}

export interface Health {
  status: string;
  database: string;
  modelService: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, { cache: "no-store", ...init });
  } catch {
    throw new Error(`ติดต่อ backend ไม่ได้ (${API_BASE_URL}) เปิด Spring Boot แล้วหรือยัง`);
  }
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { message?: string; error?: string } | null;
    throw new Error(body?.message ?? body?.error ?? `HTTP ${res.status}`);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export const api = {
  health: () => request<Health>("/api/health"),

  listDocuments: () => request<DocumentItem[]>("/api/documents"),

  uploadDocument: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<DocumentItem>("/api/documents", { method: "POST", body: form });
  },

  deleteDocument: (id: string) => request<void>(`/api/documents/${id}`, { method: "DELETE" }),

  // documentIds = ไฟล์ที่แนบในแชทนี้ ถ้าไม่ส่ง backend จะค้นจากทุกเอกสาร
  chat: (question: string, documentIds: string[] = []) =>
    request<ChatReply>("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(documentIds.length > 0 ? { question, documentIds } : { question }),
    }),
};
