#!/usr/bin/env python3
import base64
import cgi
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


PNG_1X1 = (
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
)


class CharacterizerStub(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/internal/characterize":
            self.send_error(404)
            return

        form = cgi.FieldStorage(
            fp=self.rfile,
            headers=self.headers,
            environ={
                "REQUEST_METHOD": "POST",
                "CONTENT_TYPE": self.headers.get("Content-Type", ""),
            },
        )
        animal_name = form.getfirst("animal_name", "stub-animal")
        if "image" not in form:
            self.send_json(400, {"status": "error", "message": "image is required"})
            return

        self.send_json(
            200,
            {
                "status": "success",
                "provider": "local_stub",
                "fallback_from": None,
                "animal_name": animal_name,
                "content_type": "image/png",
                "image_base64": PNG_1X1,
                "elapsed_ms": 12,
            },
        )

    def log_message(self, fmt, *args):
        print("stub:", fmt % args)

    def send_json(self, status, body):
        payload = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8000), CharacterizerStub)
    print("local characterizer stub listening on http://localhost:8000", flush=True)
    server.serve_forever()
