"""รันชุดคำถามกับ backend ที่เปิดอยู่ แล้ววัดผลการค้นและการตอบ

ใช้แค่ไลบรารีมาตรฐานของ Python ไม่ต้องติดตั้งอะไรเพิ่ม

    py eval\\run_eval.py                       (ชื่อรอบตั้งเองจากค่าที่ backend ใช้ เช่น tagged-k5-v2)
    py eval\\run_eval.py --label try --only q11,q12
    py eval\\run_eval.py --label search-only --no-chat

ผลลัพธ์อยู่ใน eval/results/<วันเวลา>_<label>.json และ .md
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
QUESTIONS = HERE / "questions.json"
RESULTS = HERE / "results"


# ---------- เรียก API ----------

def call(api: str, path: str, body: dict | None = None, timeout: int = 120):
    url = api.rstrip("/") + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    req = urllib.request.Request(url, data=data, headers=headers, method="POST" if data else "GET")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as res:
            return json.loads(res.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code} {path}: {detail[:300]}") from None


# ---------- ให้คะแนน ----------

def normalize(text: str) -> str:
    return text.lower().replace(",", "").replace(" ", "")


def facts_ok(answer: str, must_include: list[str]) -> bool:
    """คำตอบต้องมีทุกรายการ แต่ละรายการมีคำที่ใช้แทนกันได้คั่นด้วย |"""
    a = normalize(answer)
    return all(any(normalize(alt) in a for alt in item.split("|")) for item in must_include)


def search_rank(results: list[dict], pages: list[int]) -> int | None:
    """อันดับแรก (เริ่มที่ 1) ที่ผลการค้นมาจากหน้าที่ถูก"""
    for i, hit in enumerate(results, start=1):
        if hit["page"] in pages:
            return i
    return None


def score_chat(reply: dict, q: dict) -> dict:
    cited = [s for s in reply["sources"] if s["cited"]]
    cited_pages = [s["page"] for s in cited]
    cited_correct = sum(1 for p in cited_pages if p in q["pages"])
    answerable = bool(q["pages"])
    result = {
        "found": reply["found"],
        "answer": reply["answer"],
        "cited_pages": cited_pages,
        "cited_correct": cited_correct,
        "sent_pages": [s["page"] for s in reply["sources"]],
        "retrieval_ms": reply["retrievalMs"],
        "llm_ms": reply["llmMs"],
        "citation_fixes": reply.get("citationFixes", 0),
    }
    if answerable:
        result["facts_ok"] = facts_ok(reply["answer"], q["must_include"])
        result["citation_hit"] = cited_correct > 0
        result["correct"] = reply["found"] and result["facts_ok"] and result["citation_hit"]
    else:
        result["correct"] = not reply["found"]
    return result


# ---------- สรุป ----------

def pct(n: int, d: int) -> float | None:
    return round(100 * n / d, 1) if d else None


def summarize(rows: list[dict]) -> dict:
    answerable = [r for r in rows if r["pages"]]
    unanswerable = [r for r in rows if not r["pages"]]
    s = {"questions": len(rows), "answerable": len(answerable), "unanswerable": len(unanswerable)}

    ranks = [r["search_rank"] for r in answerable if "search_rank" in r]
    if ranks:
        s["hit@1"] = pct(sum(1 for x in ranks if x == 1), len(ranks))
        s["hit@3"] = pct(sum(1 for x in ranks if x and x <= 3), len(ranks))
        s["hit@5"] = pct(sum(1 for x in ranks if x and x <= 5), len(ranks))
        s["mrr"] = round(sum(1 / x for x in ranks if x) / len(ranks), 3)

    chats = [r for r in rows if r.get("chat") and "error" not in r["chat"]]
    if chats:
        ans = [r["chat"] for r in chats if r["pages"]]
        una = [r["chat"] for r in chats if not r["pages"]]
        cited_total = sum(len(c["cited_pages"]) for c in ans)
        s["correct"] = pct(sum(1 for r in chats if r["chat"]["correct"]), len(chats))
        s["answered"] = pct(sum(1 for c in ans if c["found"]), len(ans))
        s["facts_ok"] = pct(sum(1 for c in ans if c["facts_ok"]), len(ans))
        s["citation_hit"] = pct(sum(1 for c in ans if c["citation_hit"]), len(ans))
        s["citation_precision"] = pct(sum(c["cited_correct"] for c in ans), cited_total)
        s["refused_correctly"] = pct(sum(1 for c in una if not c["found"]), len(una))
        s["avg_llm_ms"] = round(sum(r["chat"]["llm_ms"] for r in chats) / len(chats))
        s["citation_fixed_answers"] = sum(1 for r in chats if r["chat"].get("citation_fixes", 0) > 0)
        s["errors"] = sum(1 for r in rows if r.get("chat") and "error" in r["chat"])

        by_type = {}
        for r in chats:
            t = by_type.setdefault(r["type"], [0, 0])
            t[0] += r["chat"]["correct"]
            t[1] += 1
        s["correct_by_type"] = {k: f"{v[0]}/{v[1]}" for k, v in sorted(by_type.items())}
    return s


METRIC_LABELS = [
    ("hit@1", "ค้นเจอหน้าที่ถูกเป็นอันดับ 1 (%)"),
    ("hit@3", "ค้นเจอหน้าที่ถูกใน 3 อันดับแรก (%)"),
    ("hit@5", "ค้นเจอหน้าที่ถูกใน 5 อันดับแรก (%)"),
    ("mrr", "MRR (1 = อันดับ 1 ทุกข้อ)"),
    ("correct", "ตอบถูกทั้งหมด (%)"),
    ("answered", "ข้อที่มีคำตอบ: ตอบ ไม่ปฏิเสธ (%)"),
    ("facts_ok", "ข้อที่มีคำตอบ: มีข้อมูลสำคัญครบ (%)"),
    ("citation_hit", "ข้อที่มีคำตอบ: อ้างอิงถูกหน้าอย่างน้อย 1 (%)"),
    ("citation_precision", "อ้างอิงทั้งหมดที่ถูกหน้า (%)"),
    ("refused_correctly", "ข้อที่ไม่มีคำตอบ: ตอบว่าไม่พบ (%)"),
    ("avg_llm_ms", "เวลา LLM เฉลี่ย (ms)"),
    ("citation_fixed_answers", "คำตอบที่ระบบแก้เลขอ้างอิงให้ (ข้อ)"),
    ("errors", "เรียก API ไม่สำเร็จ (ข้อ)"),
]


def to_markdown(run: dict) -> str:
    s = run["summary"]
    lines = [f"# Eval: {run['label']}", "", f"- เวลา: {run['started_at']}", f"- ค่าตั้งค่า: `{json.dumps(run['settings'], ensure_ascii=False)}`", ""]
    lines += ["| ตัวชี้วัด | ค่า |", "|---|---|"]
    lines += [f"| {label} | {s[key]} |" for key, label in METRIC_LABELS if key in s]
    if "correct_by_type" in s:
        lines += ["", "ถูกแยกตามประเภท: " + ", ".join(f"{k} {v}" for k, v in s["correct_by_type"].items())]

    def yes_no(value) -> str:
        return "✓" if value is True else "✗" if value is False else "-"

    lines += ["", "| ข้อ | ประเภท | คำถาม | หน้าที่ถูก | ค้นเจออันดับ | ส่งให้ AI (หน้า) | อ้างอิง (หน้า) | ข้อมูลครบ | ผล |", "|---|---|---|---|---|---|---|---|---|"]
    for r in run["rows"]:
        c = r.get("chat", {})
        result = "" if not c else ("error" if "error" in c else yes_no(c["correct"]))
        rank = r.get("search_rank", "-") if r["pages"] else "-"
        lines.append(
            f"| {r['id']} | {r['type']} | {r['question']} | {r['pages'] or '-'} | {rank if rank is not None else 'ไม่เจอ'} "
            f"| {c.get('sent_pages', '-')} | {c.get('cited_pages', '-')} | {yes_no(c.get('facts_ok'))} | {result} |"
        )

    wrong = [r for r in run["rows"] if r.get("chat") and not r["chat"].get("correct")]
    if wrong:
        lines += ["", "## ข้อที่ผิด", ""]
        for r in wrong:
            c = r["chat"]
            lines += [f"**{r['id']} {r['question']}** (หน้าที่ถูก {r['pages'] or 'ไม่มี'})", "",
                      f"> {c.get('answer', c.get('error'))}", ""]
    return "\n".join(lines) + "\n"


# ---------- main ----------

def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="วัดผล ThaiDocRAG ด้วยชุดคำถาม")
    parser.add_argument("--label", help="ชื่อรอบนี้ (ไม่ใส่ = ตั้งจากค่าที่ backend ใช้ เช่น tagged-k5-v2)")
    parser.add_argument("--api", default="http://localhost:8081")
    parser.add_argument("--only", help="รันเฉพาะบางข้อ เช่น q11,q12")
    parser.add_argument("--no-chat", action="store_true", help="วัดเฉพาะการค้น (เร็ว ไม่เรียก LLM)")
    parser.add_argument("--delay", type=float, default=0.5, help="เว้นระหว่างคำถาม (วินาที) กันเกิน rate limit")
    args = parser.parse_args()

    questions = json.loads(QUESTIONS.read_text(encoding="utf-8"))["questions"]
    if args.only:
        wanted = set(args.only.split(","))
        questions = [q for q in questions if q["id"] in wanted]

    health = call(args.api, "/api/health")
    if health.get("database") != "ok" or health.get("modelService") != "ok":
        print(f"backend ยังไม่พร้อม: {health}")
        return 1
    try:
        settings = call(args.api, "/api/chat/settings")
    except RuntimeError:
        settings = {}
    if not args.label:
        args.label = f"{settings.get('sourceFormat', 'unknown')}-k{settings.get('topK', '?')}-{settings.get('promptVersion', 'v1')}"
        if settings.get("verifyCitations"):
            args.label += "-cv"
    print(f"รอบ: {args.label}  ค่าตั้งค่า: {settings}\n")

    started = datetime.now()
    rows = []
    for q in questions:
        row = {k: q[k] for k in ("id", "type", "question", "pages")}
        hits = call(args.api, "/api/search?" + urllib.parse.urlencode({"q": q["question"], "limit": 5}))["results"]
        if q["pages"]:
            row["search_rank"] = search_rank(hits, q["pages"])

        if not args.no_chat:
            try:
                row["chat"] = score_chat(call(args.api, "/api/chat", {"question": q["question"]}), q)
            except RuntimeError as e:
                row["chat"] = {"error": str(e), "correct": False}
            time.sleep(args.delay)

        c = row.get("chat", {})
        status = "" if not c else ("ERROR" if "error" in c else ("✓" if c["correct"] else "✗"))
        print(f"{q['id']} {q['type']:<12} search@{row.get('search_rank', '-')}  cited {c.get('cited_pages', '-')}  {status}  {q['question']}")
        rows.append(row)

    run = {"label": args.label, "started_at": started.isoformat(timespec="seconds"), "settings": settings,
           "summary": summarize(rows), "rows": rows}

    RESULTS.mkdir(exist_ok=True)
    stem = f"{started:%Y%m%d-%H%M}_{args.label}"
    (RESULTS / f"{stem}.json").write_text(json.dumps(run, ensure_ascii=False, indent=2), encoding="utf-8")
    (RESULTS / f"{stem}.md").write_text(to_markdown(run), encoding="utf-8")

    print()
    for key, label in METRIC_LABELS:
        if key in run["summary"]:
            print(f"{label:<45} {run['summary'][key]}")
    if "correct_by_type" in run["summary"]:
        print("ถูกแยกตามประเภท:", run["summary"]["correct_by_type"])
    print(f"\nบันทึกผลที่ eval/results/{stem}.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
