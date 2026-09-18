"""Minimal hand-rolled STOMP-over-WebSocket client for the dummy-GPS harness.

No JS STOMP library exists on the Python side worth adding as a dependency
for this one use — STOMP framing is a handful of lines of text protocol, so
this implements exactly what the Master Release Test needs against the
REAL, confirmed endpoints (see WebSocketConfig.java / LocationWebSocketController.java):

    STOMP endpoint:            ws://<host>:<port>/ws
    App destination prefix:    /app   (client SENDs to /app/location/update)
    Broker prefix:             /topic (client SUBSCRIBEs to /topic/trips/{tripId}/location)
    Auth:                      Authorization: Bearer <jwt> on the WebSocket
                                handshake HTTP request itself (there is no
                                STOMP CONNECT-frame header interceptor in
                                this backend — confirmed by reading the code,
                                not assumed).
"""
from __future__ import annotations

import json
import queue
import threading
import time
from dataclasses import dataclass, field

import websocket

NULL = "\x00"


def _frame(command: str, headers: dict[str, str], body: str = "") -> str:
    header_lines = "\n".join(f"{k}:{v}" for k, v in headers.items())
    return f"{command}\n{header_lines}\n\n{body}{NULL}"


def _parse_frame(raw: str) -> tuple[str, dict[str, str], str]:
    raw = raw.rstrip(NULL)
    parts = raw.split("\n\n", 1)
    head = parts[0]
    body = parts[1] if len(parts) > 1 else ""
    lines = head.split("\n")
    command = lines[0]
    headers = {}
    for line in lines[1:]:
        if ":" in line:
            k, v = line.split(":", 1)
            headers[k] = v
    return command, headers, body


@dataclass
class StompSession:
    ws_url: str
    token: str
    messages: "queue.Queue[dict]" = field(default_factory=queue.Queue)
    connected: bool = False
    _ws: websocket.WebSocket | None = None
    _thread: threading.Thread | None = None
    _stop: bool = False

    def connect(self, timeout: float = 5.0) -> None:
        self._ws = websocket.create_connection(
            self.ws_url,
            header=[f"Authorization: Bearer {self.token}"],
            timeout=timeout,
        )
        self._ws.send(_frame("CONNECT", {"accept-version": "1.2", "heart-beat": "0,0"}))
        command, headers, body = _parse_frame(self._ws.recv())
        if command != "CONNECTED":
            raise ConnectionError(f"STOMP CONNECT failed: {command} {headers} {body}")
        self.connected = True
        self._stop = False
        self._thread = threading.Thread(target=self._listen, daemon=True)
        self._thread.start()

    def _listen(self) -> None:
        while not self._stop:
            try:
                raw = self._ws.recv()
            except Exception:
                break
            if not raw:
                continue
            try:
                command, headers, body = _parse_frame(raw)
            except Exception:
                continue
            if command == "MESSAGE":
                try:
                    payload = json.loads(body) if body else {}
                except json.JSONDecodeError:
                    payload = {"_raw": body}
                self.messages.put({"destination": headers.get("destination"), "payload": payload, "receivedAt": time.time()})

    def subscribe(self, destination: str, sub_id: str = "sub-0") -> None:
        self._ws.send(_frame("SUBSCRIBE", {"id": sub_id, "destination": destination}))

    def send(self, destination: str, body: dict) -> None:
        payload = json.dumps(body)
        self._ws.send(_frame("SEND", {"destination": destination, "content-type": "application/json"}, payload))

    def drain(self, expected_count: int, timeout: float = 8.0) -> list[dict]:
        """Waits up to `timeout` seconds for at least `expected_count` MESSAGE
        frames, then returns everything received. Never raises on timeout —
        callers assert on the returned list's length themselves so a partial
        result is still visible in the report instead of a bare exception.
        """
        received = []
        deadline = time.time() + timeout
        while time.time() < deadline and len(received) < expected_count:
            remaining = max(0.1, deadline - time.time())
            try:
                received.append(self.messages.get(timeout=remaining))
            except queue.Empty:
                break
        return received

    def close(self) -> None:
        self._stop = True
        if self._ws:
            try:
                self._ws.close()
            except Exception:
                pass
        self.connected = False
