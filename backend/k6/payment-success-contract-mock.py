from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith('/payments/success'):
            body = {
                'success': True,
                'message': '결제 요청 성공',
                'data': {
                    'orderId': 18,
                    'orderKey': 'order_key_123',
                    'status': 'SUCCEEDED'
                },
                'timestamp': '2026-03-21T16:50:00'
            }
            encoded = json.dumps(body).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        return

if __name__ == '__main__':
    server = HTTPServer(('127.0.0.1', 18080), Handler)
    server.serve_forever()
