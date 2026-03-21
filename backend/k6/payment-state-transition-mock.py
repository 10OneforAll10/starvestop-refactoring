from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith('/payment-state/success'):
            body = {
                'success': True,
                'message': 'success-finalized',
                'data': {'status': 'SUCCEEDED', 'stockReleased': False}
            }
            self.respond(200, body)
        elif self.path.startswith('/payment-state/retryable-fail'):
            body = {
                'success': True,
                'message': 'retryable-failure-finalized',
                'data': {'status': 'FAILED_RETRYABLE', 'stockReleased': False}
            }
            self.respond(200, body)
        elif self.path.startswith('/payment-state/nonretryable-fail'):
            body = {
                'success': True,
                'message': 'nonretryable-failure-finalized',
                'data': {'status': 'FAILED_NON_RETRYABLE', 'stockReleased': True}
            }
            self.respond(200, body)
        else:
            self.send_response(404)
            self.end_headers()

    def respond(self, code, body):
        encoded = json.dumps(body).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format, *args):
        return

if __name__ == '__main__':
    server = HTTPServer(('127.0.0.1', 18081), Handler)
    server.serve_forever()
