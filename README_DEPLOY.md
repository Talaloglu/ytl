# yt-transcript-api — Cloud Deployment Guide

This guide helps you deploy the transcript service so your Android device can fetch transcripts without your PC.

## Features
- FastAPI app exposing `/transcript` and `/health/stt`
- Gemini STT fallback (chunked, with duration normalization)
- Optional client-provided key via `X-Gemini-Api-Key`

## Prerequisites
- A cloud platform (Railway, Render, Fly.io, or a VPS)
- A Google API Key with access to Gemini (`GOOGLE_API_KEY`)

## Environment Variables
Set these in your hosting provider:
- `ENABLE_STT=1` — enable STT fallback
- `STT_BACKEND=gemini` — choose backend (gemini|openai)
- `GOOGLE_API_KEY=<your gemini key>` — default server key
- `GEMINI_MODEL=gemini-1.5-flash` — (optional) model name
- `GEMINI_SEGMENT_SEC=300` — (optional) chunk length seconds
- `LOGLEVEL=INFO` — (recommended)

## Health Check
- `GET /health/stt` → verifies STT readiness (packages, tools, api key presence)

---

## Option A: Deploy on Railway (Docker)
1. Create a new project → Deploy from Git/Repo or upload this folder.
2. Railway auto-detects Dockerfile.
3. Configure env vars listed above.
4. Deploy → wait for “running”.
5. Visit `https://<your-railway-domain>/health/stt` to verify readiness.

## Option B: Deploy on Render (Docker)
1. New Web Service → Select repo/folder with this `Dockerfile`.
2. Set env vars as above.
3. Render exposes the app on port `$PORT` automatically (Dockerfile uses it).
4. Verify: `https://<your-render-domain>/health/stt`.

## Option C: Any VPS (Docker)
```bash
# From yt-transcript-api/
docker build -t yt-transcript-api .

docker run -d \
  -p 8000:8000 \
  -e ENABLE_STT=1 \
  -e STT_BACKEND=gemini \
  -e GOOGLE_API_KEY=YOUR_KEY \
  -e LOGLEVEL=INFO \
  --name yt-transcript-api \
  yt-transcript-api
```
- Add a reverse proxy (Caddy/Nginx) for HTTPS if public.

## Android App Configuration
- In app Settings, set Base URL to your deployed endpoint:
  - `https://your-domain/transcript`
- Optional in-app controls (already wired):
  - Force STT toggle (testing)
  - Gemini API Key override (sent as `X-Gemini-Api-Key`)

## Included Files
- `Dockerfile` — Python 3.11 slim, installs ffmpeg, installs Python deps, runs Uvicorn
- `Procfile` — `web: uvicorn main:app --host 0.0.0.0 --port $PORT`
- `requirements.txt` — FastAPI, Uvicorn, yt-dlp, google-generativeai, youtube-transcript-api
- `main.py` — FastAPI app with STT fallback and health endpoint

## Troubleshooting
- 429/No Captions: STT fallback should trigger (check logs).
- Durations 0: Duration normalization patches values before returning.
- Health endpoint failing:
  - Ensure env vars are set and `google-generativeai` is installed
  - Ensure `ffmpeg`, `yt-dlp` are available (Dockerfile installs ffmpeg; `yt-dlp` is in requirements)

## Security Notes
- Prefer server-side `GOOGLE_API_KEY`. Use client override only when necessary.
- Rotate keys if exposed; run with `LOGLEVEL=INFO` to minimize sensitive logs.
