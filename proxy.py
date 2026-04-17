import http.server
import urllib.request
import ssl
import sys

RAILWAY_IP_OVERRIDE = '151.101.2.15'
RAILWAY_DOMAIN = 'fabulous-gratitude-production-9d95.up.railway.app'

class DNSBypassProxyHandler(http.server.BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-KIWI-Client-Type, X-KIWI-Device-ID')
        self.end_headers()

    def handle_request(self):
        url = f'https://{RAILWAY_IP_OVERRIDE}{self.path}'
        req = urllib.request.Request(url, method=self.command)
        req.add_header('Host', RAILWAY_DOMAIN)
        
        for key, val in self.headers.items():
            if key.lower() not in ('host', 'origin', 'referer', 'accept-encoding'):
                req.add_header(key, val)
                
        length = int(self.headers.get('Content-Length', 0))
        if length > 0:
            req.data = self.rfile.read(length)
            
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        
        try:
            r = urllib.request.urlopen(req, context=ctx)
            self.send_response(r.status)
            for k, v in r.headers.items():
                if k.lower() not in ('access-control-allow-origin', 'transfer-encoding'):
                    self.send_header(k, v)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(r.read())
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(e.read())
        except Exception as e:
            self.send_response(500)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(str(e).encode())

    def do_GET(self): self.handle_request()
    def do_POST(self): self.handle_request()
    def do_PUT(self): self.handle_request()
    def do_DELETE(self): self.handle_request()

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8081
    server = http.server.HTTPServer(('127.0.0.1', port), DNSBypassProxyHandler)
    server.serve_forever()
