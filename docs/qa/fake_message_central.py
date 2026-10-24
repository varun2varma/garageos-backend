"""Local-only stand-in for the Message Central VerifyNow API, for browser QA of OTP flows. No SMS is ever sent.
Run it, then start the backend with OTP_ENABLED=true OTP_PROVIDER=SMS OTP_SMS_BASE_URL=http://localhost:9099
OTP_SMS_CUSTOMER_ID=qa OTP_SMS_PASSWORD=qa OTP_HASH_SECRET=<any local value>. The only accepted code is 123456."""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs


class Handler(BaseHTTPRequestHandler):
    def _send(self, obj):
        raw = json.dumps(obj).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        u = urlparse(self.path)
        q = parse_qs(u.query)
        if u.path.endswith("/authentication/token"):
            return self._send({"status": 200, "token": "fake-token"})
        if u.path.endswith("/validateOtp"):
            if q.get("code", [""])[0] == "123456":
                return self._send({"responseCode": 200, "data": {"verificationStatus": "VERIFICATION_COMPLETED"}})
            return self._send({"responseCode": 702, "data": {"responseCode": 702}})
        self.send_response(404); self.end_headers()

    def do_POST(self):
        if urlparse(self.path).path.endswith("/verification/v3/send"):
            return self._send({"responseCode": 200, "data": {"verificationId": "fake-verification"}})
        self.send_response(404); self.end_headers()

    def log_message(self, *a):
        pass


HTTPServer(("127.0.0.1", 9099), Handler).serve_forever()
