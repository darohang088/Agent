#!/usr/bin/env python3
"""Probe the production TLS endpoint without credentials or certificate bypasses."""
import argparse
import http.client
from pathlib import Path
import re
import ssl
import sys
from urllib.parse import urlsplit

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--url', help='Override the literal production BuildConfig API URL')
args = parser.parse_args()
if args.url is None:
    gradle = (Path(__file__).resolve().parents[1] / 'app/build.gradle.kts').read_text()
    block = re.search(r'create\("prod"\)\s*\{(.*?)\n        \}', gradle, re.S)
    match = re.search(r'https://[^\\"\s]+', block.group(1)) if block else None
    if not match:
        parser.error('No literal production HTTPS URL found; supply --url')
    args.url = match.group()
url = urlsplit(args.url)
if url.scheme != 'https' or not url.hostname or url.username or url.password or url.query or url.fragment:
    parser.error('Use an HTTPS base URL without credentials, query, or fragment')
connection = http.client.HTTPSConnection(url.hostname, url.port or 443,
                                       timeout=15, context=ssl.create_default_context())
try:
    connection.connect()  # Verifies certificate chain and hostname/IP identity.
    protocol = connection.sock.version()
    connection.request('HEAD', url.path or '/')
    response = connection.getresponse()
    print(f'PASS: verified {protocol}; HTTPS status {response.status}')
    print('TLS only: this does not validate authentication or API response contracts.')
except (OSError, ssl.SSLError, http.client.HTTPException) as error:
    print(f'FAIL: production HTTPS unavailable ({type(error).__name__}: {error})', file=sys.stderr)
    sys.exit(1)
finally:
    connection.close()
