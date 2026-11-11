#!/usr/bin/env python3
"""Local preview of the built documentation (what nginx does in Docker), without Docker.

    python documentation/build.py                       # builds into ./site
    uvicorn --factory app.main:create_app --app-dir assistant --port 8001   # optional: the assistant
    python documentation/serve.py --port 8006 --assistant http://localhost:8001

Serves ./site and forwards /api/assistant/* to the assistant (streaming kept). Needs: starlette, uvicorn, httpx.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import httpx
import uvicorn
from starlette.applications import Starlette
from starlette.requests import Request
from starlette.responses import JSONResponse, StreamingResponse
from starlette.routing import Mount, Route
from starlette.staticfiles import StaticFiles


def create(site: Path, assistant: str) -> Starlette:
    client = httpx.AsyncClient(base_url=assistant, timeout=httpx.Timeout(180, connect=3))

    async def proxy(request: Request):
        url = request.url.path + (f"?{request.url.query}" if request.url.query else "")
        try:
            upstream = await client.send(
                client.build_request(request.method, url, content=await request.body(),
                                     headers={"content-type": request.headers.get("content-type", "application/json"),
                                              "x-real-ip": request.client.host if request.client else ""}),
                stream=True)
        except httpx.HTTPError:
            return JSONResponse({"status": 502, "code": "ASSISTANT_UNREACHABLE"}, status_code=502)
        return StreamingResponse(upstream.aiter_raw(), status_code=upstream.status_code,
                                 media_type=upstream.headers.get("content-type"), background=None)

    return Starlette(routes=[
        Route("/api/assistant/{rest:path}", proxy, methods=["GET", "POST"]),
        Mount("/", StaticFiles(directory=site, html=True)),
    ])


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--site", default=str(Path(__file__).resolve().parent.parent / "site"))
    parser.add_argument("--port", type=int, default=8006)
    parser.add_argument("--assistant", default="http://localhost:8001")
    args = parser.parse_args()
    uvicorn.run(create(Path(args.site), args.assistant), host="127.0.0.1", port=args.port)
