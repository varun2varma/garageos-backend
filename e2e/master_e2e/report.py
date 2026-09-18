"""Master Release Test reporting: JSON (machine), HTML (human), and a
Markdown summary. Every step recorded via StepRecorder.step(...) becomes
one row in all three; the API transcript from MasterE2EClient is embedded
so a FAIL can be traced back to the exact request/response that caused it.
"""
from __future__ import annotations

import datetime
import html
import json
import os
from dataclasses import dataclass, field
from typing import Any

from .client import MasterE2EClient

STATUS_PASS = "PASS"
STATUS_FAIL = "FAIL"
STATUS_BLOCKED = "BLOCKED"
STATUS_NOT_AUTOMATED = "NOT_YET_AUTOMATED"


@dataclass
class StepResult:
    phase: str
    name: str
    actor: str
    status: str
    detail: str = ""
    classification: str = ""  # CODE_BUG | TEST_BUG | ENVIRONMENT_ISSUE | PRODUCT_DECISION_REQUIRED | NOT_IMPLEMENTED | EXTERNAL_SERVICE_ISSUE
    duration_ms: float = 0.0


@dataclass
class StepRecorder:
    run_id: str
    client: MasterE2EClient
    steps: list[StepResult] = field(default_factory=list)
    started_at: datetime.datetime = field(default_factory=datetime.datetime.now)

    def step(self, phase: str, name: str, actor: str, fn, classify_fail: str = "CODE_BUG"):
        """Runs fn(); records PASS/FAIL based on whether it raises."""
        import time as _time
        start = _time.monotonic()
        try:
            fn()
            status, detail, cls = STATUS_PASS, "", ""
        except Exception as e:  # noqa: BLE001 — we want every failure shape recorded, not just AssertionError
            status, detail, cls = STATUS_FAIL, f"{type(e).__name__}: {e}", classify_fail
        duration = (_time.monotonic() - start) * 1000
        result = StepResult(phase, name, actor, status, detail, cls, duration)
        self.steps.append(result)
        tag = {"PASS": "OK", "FAIL": "FAIL"}[status]
        print(f"[{tag}] {phase} :: {name} ({actor}) — {detail[:200]}")
        return result

    def blocked(self, phase: str, name: str, actor: str, reason: str, classification: str = "PRODUCT_DECISION_REQUIRED"):
        result = StepResult(phase, name, actor, STATUS_BLOCKED, reason, classification)
        self.steps.append(result)
        print(f"[BLOCKED] {phase} :: {name} ({actor}) — {reason}")
        return result

    def not_automated(self, phase: str, name: str, actor: str, reason: str):
        result = StepResult(phase, name, actor, STATUS_NOT_AUTOMATED, reason)
        self.steps.append(result)
        print(f"[NOT_YET_AUTOMATED] {phase} :: {name} ({actor}) — {reason}")
        return result

    # ---- report generation ----

    def summary_counts(self) -> dict[str, int]:
        counts = {STATUS_PASS: 0, STATUS_FAIL: 0, STATUS_BLOCKED: 0, STATUS_NOT_AUTOMATED: 0}
        for s in self.steps:
            counts[s.status] = counts.get(s.status, 0) + 1
        return counts

    def write(self, out_dir: str) -> dict[str, str]:
        os.makedirs(out_dir, exist_ok=True)
        json_path = os.path.join(out_dir, f"{self.run_id}.json")
        html_path = os.path.join(out_dir, f"{self.run_id}.html")
        md_path = os.path.join(out_dir, f"{self.run_id}.md")

        payload = {
            "runId": self.run_id,
            "startedAt": self.started_at.isoformat(),
            "finishedAt": datetime.datetime.now().isoformat(),
            "summary": self.summary_counts(),
            "steps": [
                {
                    "phase": s.phase, "name": s.name, "actor": s.actor, "status": s.status,
                    "detail": s.detail, "classification": s.classification, "durationMs": round(s.duration_ms, 1),
                }
                for s in self.steps
            ],
            "apiTranscript": [
                {
                    "method": c.method, "path": c.path, "status": c.status, "actor": c.actor,
                    "durationMs": round(c.duration_ms, 1),
                    "requestBody": c.request_body, "responseBody": c.response_body,
                }
                for c in self.client.transcript
            ],
        }
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, default=str)

        with open(md_path, "w", encoding="utf-8") as f:
            f.write(self._render_markdown())

        with open(html_path, "w", encoding="utf-8") as f:
            f.write(self._render_html())

        return {"json": json_path, "html": html_path, "markdown": md_path}

    def _render_markdown(self) -> str:
        counts = self.summary_counts()
        lines = [
            f"# GarageST Master Release Test — {self.run_id}",
            "",
            f"Started: {self.started_at.isoformat()}  ",
            f"Finished: {datetime.datetime.now().isoformat()}",
            "",
            f"**PASS: {counts[STATUS_PASS]} | FAIL: {counts[STATUS_FAIL]} | "
            f"BLOCKED: {counts[STATUS_BLOCKED]} | NOT_YET_AUTOMATED: {counts[STATUS_NOT_AUTOMATED]}**",
            "",
            "| Phase | Step | Actor | Status | Detail |",
            "|---|---|---|---|---|",
        ]
        for s in self.steps:
            detail = s.detail.replace("|", "\\|").replace("\n", " ")[:200]
            lines.append(f"| {s.phase} | {s.name} | {s.actor} | {s.status} | {detail} |")
        return "\n".join(lines) + "\n"

    def _render_html(self) -> str:
        counts = self.summary_counts()
        rows = []
        color = {"PASS": "#1a7f37", "FAIL": "#cf222e", "BLOCKED": "#9a6700", "NOT_YET_AUTOMATED": "#57606a"}
        for s in self.steps:
            rows.append(
                f"<tr><td>{html.escape(s.phase)}</td><td>{html.escape(s.name)}</td>"
                f"<td>{html.escape(s.actor)}</td>"
                f"<td style='color:{color.get(s.status, '#000')};font-weight:600'>{s.status}</td>"
                f"<td>{html.escape(s.classification)}</td>"
                f"<td><code>{html.escape(s.detail[:500])}</code></td>"
                f"<td>{s.duration_ms:.0f}ms</td></tr>"
            )
        return f"""<!DOCTYPE html><html><head><meta charset="utf-8">
<title>GarageST Master Release Test — {self.run_id}</title>
<style>
body{{font-family:system-ui,sans-serif;margin:2rem;background:#f6f8fa}}
table{{border-collapse:collapse;width:100%;background:#fff}}
td,th{{border:1px solid #d0d7de;padding:6px 10px;text-align:left;font-size:13px}}
th{{background:#eaeef2}}
h1{{font-size:20px}} .summary{{font-size:16px;margin:1rem 0}}
</style></head><body>
<h1>GarageST Master Release Test — {self.run_id}</h1>
<div class="summary">
PASS: <b style="color:#1a7f37">{counts[STATUS_PASS]}</b> &nbsp;
FAIL: <b style="color:#cf222e">{counts[STATUS_FAIL]}</b> &nbsp;
BLOCKED: <b style="color:#9a6700">{counts[STATUS_BLOCKED]}</b> &nbsp;
NOT_YET_AUTOMATED: <b style="color:#57606a">{counts[STATUS_NOT_AUTOMATED]}</b>
</div>
<table><tr><th>Phase</th><th>Step</th><th>Actor</th><th>Status</th><th>Classification</th><th>Detail</th><th>Duration</th></tr>
{''.join(rows)}
</table>
</body></html>"""
