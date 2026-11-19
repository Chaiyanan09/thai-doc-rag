"""เทียบผล eval 2 รอบ: ตัวชี้วัดเปลี่ยนเท่าไร และข้อไหนแก้ได้ / พังลง

    py eval\\compare.py eval\\results\\20261119-2200_baseline.json eval\\results\\20261119-2215_tagged.json
"""

import json
import sys
from pathlib import Path

from run_eval import METRIC_LABELS


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if len(sys.argv) != 3:
        print(__doc__)
        return 1

    a, b = (json.loads(Path(p).read_text(encoding="utf-8")) for p in sys.argv[1:3])
    print(f"{'ตัวชี้วัด':<45} {a['label']:>12} {b['label']:>12} {'ต่าง':>8}")
    for key, label in METRIC_LABELS:
        va, vb = a["summary"].get(key), b["summary"].get(key)
        if va is None and vb is None:
            continue
        diff = f"{vb - va:+.1f}" if isinstance(va, (int, float)) and isinstance(vb, (int, float)) else ""
        print(f"{label:<45} {str(va):>12} {str(vb):>12} {diff:>8}")

    before = {r["id"]: r for r in a["rows"]}
    fixed, broke = [], []
    for r in b["rows"]:
        old = before.get(r["id"])
        if not old or "chat" not in old or "chat" not in r:
            continue
        if not old["chat"]["correct"] and r["chat"]["correct"]:
            fixed.append(r)
        elif old["chat"]["correct"] and not r["chat"]["correct"]:
            broke.append(r)

    print(f"\nแก้ได้ {len(fixed)} ข้อ:")
    for r in fixed:
        print(f"  {r['id']} {r['question']}")
    print(f"พังลง {len(broke)} ข้อ:")
    for r in broke:
        print(f"  {r['id']} {r['question']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
