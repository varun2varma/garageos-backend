"""Thin REST client for the Master Release Test.

Every call is appended to a shared, run-scoped transcript (see report.py)
so the final report can show exact request/response pairs for every step,
not just a pass/fail bit.
"""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from typing import Any


class ApiError(Exception):
    """A backend call returned an HTTP error status."""

    def __init__(self, method: str, path: str, status: int, body: Any):
        self.method = method
        self.path = path
        self.status = status
        self.body = body
        super().__init__(f"{method} {path} -> {status}: {str(body)[:300]}")


@dataclass
class CallRecord:
    method: str
    path: str
    status: int
    request_body: Any
    response_body: Any
    duration_ms: float
    actor: str | None = None


@dataclass
class MasterE2EClient:
    """One instance per test run. `base_url` points at the real running backend."""

    base_url: str = "http://localhost:8080/api/v1"
    transcript: list[CallRecord] = field(default_factory=list)

    def call(
        self,
        method: str,
        path: str,
        body: Any = None,
        token: str | None = None,
        actor: str | None = None,
        expect: tuple[int, ...] | None = None,
    ) -> Any:
        """Makes one HTTP call, records it, and returns the parsed JSON body.

        Raises ApiError if `expect` is given and the status isn't in it.
        When `expect` is None, any status is accepted and returned to the
        caller — used by negative/security tests that assert on the
        failure itself.
        """
        url = self.base_url + path
        data = None
        if body is not None:
            data = json.dumps(body).encode()
        elif method in ("POST", "PUT"):
            data = b""

        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        if token:
            req.add_header("Authorization", f"Bearer {token}")

        start = time.monotonic()
        try:
            with urllib.request.urlopen(req) as resp:
                raw_bytes = resp.read()
                status = resp.status
        except urllib.error.HTTPError as e:
            raw_bytes = e.read()
            status = e.code
        duration_ms = (time.monotonic() - start) * 1000

        try:
            raw = raw_bytes.decode()
        except UnicodeDecodeError:
            raw = f"<binary, {len(raw_bytes)} bytes>"

        try:
            parsed = json.loads(raw) if raw and not raw.startswith("<binary") else raw
        except json.JSONDecodeError:
            parsed = raw

        self.transcript.append(
            CallRecord(method, path, status, body, parsed, duration_ms, actor)
        )

        if expect is not None and status not in expect:
            raise ApiError(method, path, status, parsed)

        return parsed

    def data(self, body: Any) -> Any:
        """Unwraps the backend's {"data": ...} envelope when present."""
        if isinstance(body, dict) and "data" in body:
            return body["data"]
        return body

    def get(self, path: str, **kw) -> Any:
        return self.data(self.call("GET", path, **kw))

    def post(self, path: str, body: Any = None, **kw) -> Any:
        return self.data(self.call("POST", path, body, **kw))

    def put(self, path: str, body: Any = None, **kw) -> Any:
        return self.data(self.call("PUT", path, body, **kw))

    def multipart(
        self,
        path: str,
        fields: dict[str, str],
        file_field: str,
        filename: str,
        file_bytes: bytes,
        content_type: str,
        token: str | None = None,
        actor: str | None = None,
        expect: tuple[int, ...] | None = None,
    ) -> Any:
        boundary = "----MasterE2EBoundary7d8f3a"
        parts = []
        for k, v in fields.items():
            parts.append(
                f"--{boundary}\r\nContent-Disposition: form-data; name=\"{k}\"\r\n\r\n{v}\r\n"
            )
        body = "".join(parts).encode()
        body += (
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"{file_field}\"; "
            f"filename=\"{filename}\"\r\nContent-Type: {content_type}\r\n\r\n"
        ).encode() + file_bytes + f"\r\n--{boundary}--\r\n".encode()

        req = urllib.request.Request(self.base_url + path, data=body, method="POST")
        req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
        if token:
            req.add_header("Authorization", f"Bearer {token}")

        start = time.monotonic()
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read().decode()
                status = resp.status
        except urllib.error.HTTPError as e:
            raw = e.read().decode()
            status = e.code
        duration_ms = (time.monotonic() - start) * 1000

        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw

        self.transcript.append(
            CallRecord("POST(multipart)", path, status, {"fields": fields, "file": filename}, parsed, duration_ms, actor)
        )
        if expect is not None and status not in expect:
            raise ApiError("POST(multipart)", path, status, parsed)
        return self.data(parsed)
