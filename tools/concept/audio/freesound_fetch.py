#!/usr/bin/env python3
"""Fetch the original files of the Freesound sounds listed in CREDITS.md (Freesound API, OAuth2).

The public HQ previews that import_sfx.py uses are lossy; the production pass rebuilds the
recorded effects from the originals. Downloading an original needs an OAuth2 access token of a
Freesound user, so this script runs in three steps:

  python3 tools/concept/audio/freesound_fetch.py              prints the authorisation link
  python3 tools/concept/audio/freesound_fetch.py --code CODE  exchanges the code shown after
                                                              approving for an access token
  python3 tools/concept/audio/freesound_fetch.py --download   downloads every sound in CREDITS.md
                                                              that is not cached yet

Credentials and tokens live in ~/.config/terran-vanguard/freesound.json (client_id and
client_secret written by the user; the tokens are added by --code), never in the repository.
Originals are cached in ~/.cache/terran-vanguard/freesound/ as <id>_<name>.<type>; only the
processed files made from them are committed. Throttling (HTTP 429) is waited out; any other
failed request stops the script.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://freesound.org/apiv2"
CREDENTIALS = Path.home() / ".config" / "terran-vanguard" / "freesound.json"
CACHE = Path.home() / ".cache" / "terran-vanguard" / "freesound"
CREDITS = Path(__file__).resolve().parents[3] / "CREDITS.md"
SOUND_URL = re.compile(r"freesound\.org/people/[^/\s]+/sounds/(\d+)")
# Freesound throttles OAuth2 requests at 30 a minute; two requests per sound (info and download).
REQUEST_GAP = 2.1
THROTTLE_RETRIES = 5


def load_credentials():
    try:
        return json.loads(CREDENTIALS.read_text())
    except FileNotFoundError:
        sys.exit(f"missing {CREDENTIALS}: create it with your client_id and client_secret")


def save_credentials(credentials):
    CREDENTIALS.write_text(json.dumps(credentials, indent=2))
    os.chmod(CREDENTIALS, 0o600)


def post_token(credentials, grant):
    data = urllib.parse.urlencode({
        "client_id": credentials["client_id"],
        "client_secret": credentials["client_secret"],
        **grant,
    }).encode()
    with urllib.request.urlopen(f"{API}/oauth2/access_token/", data) as response:
        token = json.load(response)
    credentials["access_token"] = token["access_token"]
    credentials["refresh_token"] = token["refresh_token"]
    credentials["expires_at"] = time.time() + token["expires_in"] - 60
    save_credentials(credentials)


def access_token(credentials):
    if "refresh_token" not in credentials:
        sys.exit("no token yet: run without arguments, approve the link, then pass --code CODE")
    if time.time() >= credentials["expires_at"]:
        post_token(credentials, {"grant_type": "refresh_token", "refresh_token": credentials["refresh_token"]})
    return credentials["access_token"]


def get(url, token):
    """GET with the token; waits out Freesound's throttling (HTTP 429) instead of failing."""
    request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    for attempt in range(THROTTLE_RETRIES):
        try:
            return urllib.request.urlopen(request)
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == THROTTLE_RETRIES - 1:
                raise
            wait = int(error.headers.get("Retry-After") or 60)
            print(f"throttled, waiting {wait} s")
            time.sleep(wait)


def sound_ids():
    return sorted({int(match) for match in SOUND_URL.findall(CREDITS.read_text())})


def download(credentials):
    CACHE.mkdir(parents=True, exist_ok=True)
    ids = sound_ids()
    cached = {int(path.name.split("_", 1)[0]) for path in CACHE.iterdir() if path.name[0].isdigit()}
    missing = [sound for sound in ids if sound not in cached]
    print(f"{len(ids)} sounds in CREDITS.md, {len(ids) - len(missing)} cached, {len(missing)} to fetch")
    for count, sound in enumerate(missing, 1):
        token = access_token(credentials)
        with get(f"{API}/sounds/{sound}/?fields=id,name,type", token) as response:
            info = json.load(response)
        time.sleep(REQUEST_GAP)
        stem = re.sub(r"[^A-Za-z0-9._-]+", "-", Path(info["name"]).stem).strip("-")[:60]
        target = CACHE / f"{sound}_{stem}.{info['type']}"
        partial = target.with_suffix(target.suffix + ".part")
        with get(f"{API}/sounds/{sound}/download/", token) as response:
            partial.write_bytes(response.read())
        partial.rename(target)
        print(f"[{count}/{len(missing)}] {target.name} ({target.stat().st_size // 1024} KiB)")
        time.sleep(REQUEST_GAP)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--code", help="the authorisation code shown after approving the link")
    action.add_argument("--download", action="store_true", help="download the originals")
    args = parser.parse_args()
    credentials = load_credentials()
    try:
        if args.code:
            post_token(credentials, {"grant_type": "authorization_code", "code": args.code})
            print(f"access token stored in {CREDENTIALS}")
        elif args.download:
            download(credentials)
        else:
            query = urllib.parse.urlencode({"client_id": credentials["client_id"], "response_type": "code"})
            print(f"open and approve: {API}/oauth2/authorize/?{query}")
    except urllib.error.HTTPError as error:
        sys.exit(f"{error.url}: HTTP {error.code} {error.reason}: {error.read().decode(errors='replace')[:300]}")
    except urllib.error.URLError as error:
        sys.exit(f"request failed: {error.reason}")


if __name__ == "__main__":
    main()
