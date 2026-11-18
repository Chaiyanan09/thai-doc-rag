"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type DocumentItem } from "@/lib/api";
import DocumentTable from "./DocumentTable";
import SearchPanel from "./SearchPanel";
import UploadCard from "./UploadCard";

/** หน้าทำงานของสัปดาห์ 1: อัปโหลด ดูรายการเอกสาร และทดสอบค้นหา */
export default function Workbench() {
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [listError, setListError] = useState<string | null>(null);

  const loadDocuments = useCallback(() => {
    api
      .listDocuments()
      .then((docs) => {
        setDocuments(docs);
        setListError(null);
      })
      .catch((e: Error) => setListError(e.message));
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  return (
    <div className="space-y-6">
      <UploadCard onUploaded={loadDocuments} />
      <DocumentTable documents={documents} error={listError} onChanged={loadDocuments} />
      <SearchPanel />
    </div>
  );
}
