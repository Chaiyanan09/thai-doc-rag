"use client";

import { useEffect, useState } from "react";
import { api, type Health } from "@/lib/api";

type Item = { label: string; ok: boolean | null };

function Pill({ label, ok }: Item) {
  const color =
    ok === null ? "bg-zinc-300" : ok ? "bg-emerald-500" : "bg-rose-500";
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-zinc-200 bg-white px-2.5 py-0.5 text-xs text-zinc-600">
      <span className={`h-1.5 w-1.5 rounded-full ${color}`} />
      {label}
    </span>
  );
}

/** แสดงว่า backend, ฐานข้อมูล และ model service พร้อมหรือไม่ เช็กใหม่ทุก 15 วินาที */
export default function StatusBar() {
  const [health, setHealth] = useState<Health | null>(null);
  const [backendUp, setBackendUp] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    const check = () =>
      api
        .health()
        .then((h) => {
          if (cancelled) return;
          setHealth(h);
          setBackendUp(true);
        })
        .catch(() => {
          if (cancelled) return;
          setHealth(null);
          setBackendUp(false);
        });

    check();
    const timer = setInterval(check, 15_000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  const items: Item[] = [
    { label: "Backend", ok: backendUp },
    { label: "Database", ok: health ? health.database === "ok" : backendUp === false ? false : null },
    { label: "Model service", ok: health ? health.modelService === "ok" : backendUp === false ? false : null },
  ];

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <Pill key={item.label} {...item} />
      ))}
    </div>
  );
}
