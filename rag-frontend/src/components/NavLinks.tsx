"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/", label: "แชท" },
  { href: "/documents", label: "จัดการเอกสาร" },
];

export default function NavLinks() {
  const pathname = usePathname();
  return (
    <nav className="flex gap-1 text-sm">
      {LINKS.map((link) => {
        const active = pathname === link.href;
        return (
          <Link
            key={link.href}
            href={link.href}
            className={`rounded-lg px-3 py-1.5 ${active ? "bg-zinc-100 font-medium text-zinc-900" : "text-zinc-500 hover:text-zinc-900"}`}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}
