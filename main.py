import logging
import time
import random
import os
import subprocess
import tempfile
import shutil
from pathlib import Path
from typing import Callable, Optional, List, Tuple

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from youtube_transcript_api import YouTubeTranscriptApi, TranscriptsDisabled, NoTranscriptFound
from pydantic import BaseModel

# ---------------------------------------
# Logging
# ---------------------------------------
logging.basicConfig(level=logging.DEBUG, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("yt-transcripts")

# ---------------------------------------
# FastAPI app
# ---------------------------------------
app = FastAPI(title="Transcript API", version="0.3.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # tighten for production
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

# ---------------------------------------
# Helpers
# ---------------------------------------

# Simple in-memory caches
# Success cache: {(videoId, lang, disableTranslate): (timestamp, data)}
CACHE_TTL_SECONDS = 60 * 30  # 30 minutes
_CACHE: dict[tuple[str, str, bool], tuple[float, list]] = {}

# Failure cache (circuit breaker): {(videoId, lang, disableTranslate): (timestamp, retry_after_seconds)}
FAIL_TTL_SECONDS_DEFAULT = 60 * 5  # 5 minutes cooldown
_FAIL_CACHE: dict[tuple[str, str, bool], tuple[float, int]] = {}

# Mutable env whitelist (server variables allowed to change at runtime)
ALLOWED_MUTABLE_ENV: set[str] = {
    "ENABLE_STT",           # "1"/"0"
    "STT_BACKEND",         # "openai"|"gemini"|"local"
    "GEMINI_MODEL",        # e.g., gemini-1.5-flash
    "GEMINI_SEGMENT_SEC",  # chunk seconds for Gemini splitting
}

def _require_admin(request: Request) -> None:
    token_header = (request.headers.get("X-Admin-Token", "") or "").strip()
    token_env = (os.getenv("ADMIN_TOKEN", "") or "").strip()
    if not token_env:
        # If no token configured, deny all mutation for safety
        raise HTTPException(status_code=403, detail="Admin token not configured on server")
    if token_header != token_env:
        raise HTTPException(status_code=403, detail="Forbidden: invalid admin token")


class EnvSetPayload(BaseModel):
    key: str
    value: str | None = None


@app.get("/admin/env")
def admin_env_get(request: Request, key: str):
    """
    Read current value of a whitelisted env var.
    Secured via X-Admin-Token header.
    """
    _require_admin(request)
    k = (key or "").strip().upper()
    if k not in ALLOWED_MUTABLE_ENV:
        raise HTTPException(status_code=400, detail="Key not allowed")
    return {"key": k, "value": os.getenv(k)}


@app.post("/admin/env")
def admin_env_set(request: Request, payload: EnvSetPayload):
    """
    Set or unset a whitelisted env var at runtime.
    - If value is None or empty => unset (delete) the variable.
    """
    _require_admin(request)
    k = (payload.key or "").strip().upper()
    if k not in ALLOWED_MUTABLE_ENV:
        raise HTTPException(status_code=400, detail="Key not allowed")
    v = None if (payload.value is None or str(payload.value).strip() == "") else str(payload.value)
    if v is None:
        os.environ.pop(k, None)
        log.info(f"[admin] UNSET {k}")
    else:
        os.environ[k] = v
        log.info(f"[admin] SET {k}='{v}'")
    return {"status": "ok", "key": k, "value": os.getenv(k)}

def cache_get(video_id: str, lang: str, disable_translate: bool) -> Optional[list]:
    key = (video_id, lang, bool(disable_translate))
    entry = _CACHE.get(key)
    if not entry:
        return None
    ts, data = entry
    if (time.time() - ts) > CACHE_TTL_SECONDS:
        _CACHE.pop(key, None)
        return None
    return data


@app.get("/health/stt")
def stt_health():
    """
    Lightweight readiness probe for STT pipeline.
    Returns flags for env keys, selected backend, model, and external tools.
    """
    enable_env = os.getenv("ENABLE_STT", "1").strip() != "0"
    backend = os.getenv("STT_BACKEND", "openai").strip().lower()
    gem_model = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
    has_google_key = bool(os.getenv("GOOGLE_API_KEY"))
    # Tooling
    tools = {
        "yt_dlp": _has_cmd("yt-dlp"),
        "ffmpeg": _has_cmd("ffmpeg"),
        "ffprobe": _has_cmd("ffprobe"),
    }
    # Module presence
    gem_pkg = False
    try:
        import google.generativeai  # type: ignore
        gem_pkg = True
    except Exception:
        gem_pkg = False

    return {
        "stt_enabled": enable_env,
        "backend": backend,
        "gemini": {
            "model": gem_model,
            "google_api_key_present": has_google_key,
            "package_available": gem_pkg,
        },
        "tools": tools,
        "status": "ready" if enable_env and (backend in ["gemini", "openai", "local"]) else "disabled",
    }

def cache_set(video_id: str, lang: str, disable_translate: bool, data: list) -> None:
    key = (video_id, lang, bool(disable_translate))
    _CACHE[key] = (time.time(), data)

def fail_cache_get(video_id: str, lang: str, disable_translate: bool) -> Optional[int]:
    key = (video_id, lang, bool(disable_translate))
    entry = _FAIL_CACHE.get(key)
    if not entry:
        return None
    ts, retry_sec = entry
    if (time.time() - ts) > retry_sec:
        _FAIL_CACHE.pop(key, None)
        return None
    # remaining time (rounded)
    remaining = max(1, int(retry_sec - (time.time() - ts)))
    return remaining

def fail_cache_set(video_id: str, lang: str, disable_translate: bool, retry_after_seconds: Optional[int] = None) -> None:
    key = (video_id, lang, bool(disable_translate))
    _FAIL_CACHE[key] = (time.time(), int(retry_after_seconds or FAIL_TTL_SECONDS_DEFAULT))

def _safe_float(val, default: float = 0.0) -> float:
    try:
        return float(val)
    except Exception:
        return default

def _normalize_durations(segments: List[dict]) -> List[dict]:
    """
    Ensure each segment has a positive duration. If duration is missing or <= 0,
    infer it from the next segment's start time. For the last segment, reuse the
    previous delta as a best-effort estimate.
    """
    if not segments:
        return segments
    normalized: List[dict] = []
    n = len(segments)
    for i, seg in enumerate(segments):
        start = _safe_float(seg.get("start", 0.0), 0.0)
        dur = seg.get("duration", 0.0)
        needs_fill = False
        try:
            needs_fill = float(dur) <= 0
        except Exception:
            needs_fill = True

        if needs_fill:
            if i < n - 1:
                next_start = _safe_float(segments[i + 1].get("start", start), start)
                filled = max(0.0, round(next_start - start, 3))
            else:
                # Last segment: approximate using previous delta if available
                if n >= 2:
                    prev_start = _safe_float(segments[i - 1].get("start", start), start)
                    filled = max(0.0, round(start - prev_start, 3))
                else:
                    filled = 0.0
            dur = filled

        new_seg = dict(seg)
        new_seg["start"] = start
        new_seg["duration"] = dur
        normalized.append(new_seg)
    return normalized

# Optional STT helpers (OpenAI Whisper, Gemini, Local)
# ---------------------------------------

def _has_cmd(cmd: str) -> bool:
    return shutil.which(cmd) is not None

def _download_audio_tmp(video_id: str) -> Optional[Path]:
    """
    Downloads audio-only for the given videoId using yt-dlp into a temp folder.
    Returns the Path to the downloaded file, or None on failure.
    """
    if not _has_cmd("yt-dlp"):
        log.debug("[stt] yt-dlp not found in PATH; skipping STT")
        return None
    tmpdir = Path(tempfile.mkdtemp(prefix="yt_stt_"))
    # Output template without extension; yt-dlp will pick suitable extension
    out_tmpl = str(tmpdir / "audio.%(ext)s")
    url = f"https://www.youtube.com/watch?v={video_id}"
    cmd = [
        "yt-dlp",
        "-f", "bestaudio/best",
        "--no-playlist",
        "-o", out_tmpl,
        url,
    ]
    try:
        log.debug(f"[stt] running: {' '.join(cmd)}")
        subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except Exception as e:
        log.debug(f"[stt] yt-dlp failed: {e}")
        shutil.rmtree(tmpdir, ignore_errors=True)
        return None
    # Find resulting file
    files = list(tmpdir.glob("audio.*"))
    if not files:
        shutil.rmtree(tmpdir, ignore_errors=True)
        return None
    return files[0]

def _whisper_transcribe_segments(audio_path: Path, lang: str) -> Optional[List[dict]]:
    """
    Uses OpenAI Whisper API to transcribe the audio file. Returns list of {text,start,duration}.
    Requires OPENAI_API_KEY. Falls back gracefully if package or key missing.
    """
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        log.debug("[stt] OPENAI_API_KEY not set; skipping STT")
        return None
    try:
        # Try new SDK style first
        from openai import OpenAI  # type: ignore
        client = OpenAI()
        # Request verbose JSON to get segments with timestamps if supported
        with open(audio_path, "rb") as f:
            try:
                resp = client.audio.transcriptions.create(
                    model="whisper-1",
                    file=f,
                    language=(lang if lang else "en"),
                    response_format="verbose_json",
                )
                segments = getattr(resp, "segments", None)
                if not segments:
                    # Fallback: no segments, return a single chunk without timestamps
                    text = getattr(resp, "text", "") or ""
                    if not text.strip():
                        return None
                    return [{"text": text, "start": 0.0, "duration": 0.0}]
                out = []
                for s in segments:
                    start = float(getattr(s, "start", 0.0))
                    end = float(getattr(s, "end", start))
                    out.append({
                        "text": getattr(s, "text", ""),
                        "start": start,
                        "duration": max(0.0, end - start),
                    })
                return out
            except Exception as e1:
                log.debug(f"[stt] OpenAI new SDK path failed: {e1}")
                return None
    except Exception:
        # Try legacy SDK import
        try:
            import openai  # type: ignore
            openai.api_key = api_key
            with open(audio_path, "rb") as f:
                try:
                    resp = openai.audio.transcriptions.create(
                        model="whisper-1",
                        file=f,
                        language=(lang if lang else "en"),
                        response_format="verbose_json",
                    )
                    segments = resp.get("segments") if isinstance(resp, dict) else getattr(resp, "segments", None)
                    if not segments:
                        text = (resp.get("text") if isinstance(resp, dict) else getattr(resp, "text", "")) or ""
                        if not text.strip():
                            return None
                        return [{"text": text, "start": 0.0, "duration": 0.0}]
                    out = []
                    for s in segments:
                        start = float(s.get("start", 0.0))
                        end = float(s.get("end", start))
                        out.append({
                            "text": s.get("text", ""),
                            "start": start,
                            "duration": max(0.0, end - start),
                        })
                    return out
                except Exception as e2:
                    log.debug(f"[stt] OpenAI legacy SDK path failed: {e2}")
                    return None
        except Exception as e0:
            log.debug(f"[stt] openai package not available: {e0}")
            return None

def _stt_fallback(video_id: str, lang: str) -> Optional[List[dict]]:
    """
    Full STT fallback pipeline: download audio via yt-dlp and transcribe via Whisper API.
    Cleans up temp files. Returns segments or None.
    """
    audio = _download_audio_tmp(video_id)
    if not audio:
        return None
    try:
        segs = _whisper_transcribe_segments(audio, lang)
        return _normalize_durations(segs)
    finally:
        try:
            shutil.rmtree(audio.parent, ignore_errors=True)
        except Exception:
            pass

# ------------------------------
# Gemini backend (chunked)
# ------------------------------

def _split_audio_ffmpeg(input_path: Path, segment_seconds: int = 300) -> List[Path]:
    """
    Split audio into ~segment_seconds chunks using ffmpeg. Returns list of chunk Paths.
    Requires ffmpeg in PATH. Falls back to single file if ffmpeg missing or fails.
    """
    if not _has_cmd("ffmpeg"):
        log.debug("[gemini] ffmpeg not found; using single-file transcription")
        return [input_path]
    out_dir = Path(tempfile.mkdtemp(prefix="yt_stt_seg_"))
    out_pattern = str(out_dir / "part_%03d.mp3")
    cmd = [
        "ffmpeg", "-y", "-i", str(input_path),
        "-vn", "-acodec", "libmp3lame", "-ar", "16000", "-ac", "1",
        "-f", "segment", "-segment_time", str(segment_seconds), out_pattern,
    ]
    try:
        log.debug(f"[gemini] splitting with ffmpeg: {' '.join(cmd)}")
        subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        parts = sorted(out_dir.glob("part_*.mp3"))
        return parts if parts else [input_path]
    except Exception as e:
        log.debug(f"[gemini] ffmpeg split failed: {e}")
        shutil.rmtree(out_dir, ignore_errors=True)
        return [input_path]

def _gemini_transcribe_files(paths: List[Path], lang: str, api_key_override: Optional[str] = None) -> Optional[List[dict]]:
    """
    Transcribe a list of audio files using Gemini API. Requires GOOGLE_API_KEY and google-generativeai.
    Produces a flat list of {text,start,duration} with approximate timestamps by concatenating chunk offsets.
    """
    api_key = (api_key_override or os.getenv("GOOGLE_API_KEY", "")).strip()
    if not api_key:
        log.debug("[gemini] GOOGLE_API_KEY not set; skipping Gemini STT")
        return None
    try:
        import google.generativeai as genai  # type: ignore
    except Exception as e:
        log.debug(f"[gemini] google-generativeai not available: {e}")
        return None
    try:
        genai.configure(api_key=api_key)
        model_name = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
        model = genai.GenerativeModel(model_name)
        all_segments: List[dict] = []
        offset = 0.0
        for p in paths:
            mime = "audio/mpeg" if p.suffix.lower() == ".mp3" else "application/octet-stream"
            try:
                # Upload file to Gemini's file API
                file = genai.upload_file(path=str(p), mime_type=mime)
                # Simple instruction to get raw transcript; model may return plain text
                prompt = f"Transcribe this audio to {lang or 'English'} text only. Return raw transcript without extra commentary."
                resp = model.generate_content([file, prompt])
                text = (resp.text or "").strip() if hasattr(resp, "text") else ""
                if not text:
                    # Some SDK versions return candidates
                    cand_text = ""
                    try:
                        if resp.candidates and resp.candidates[0].content.parts:
                            cand_text = "".join(getattr(pt, "text", "") for pt in resp.candidates[0].content.parts)
                    except Exception:
                        pass
                    text = cand_text.strip()
                if not text:
                    log.debug("[gemini] empty transcript text for chunk; skipping")
                    continue
                # We don't have token-level timestamps; emit one segment per chunk.
                seg = {"text": text, "start": offset, "duration": 0.0}
                all_segments.append(seg)
                # Update offset by rough duration using ffprobe if available, else chunk length guess
                dur = _probe_duration_seconds(p)
                offset += dur if dur else 0.0
            except Exception as e:
                log.debug(f"[gemini] chunk transcription failed: {e}")
                continue
        return _normalize_durations(all_segments) if all_segments else None
    except Exception as e:
        log.debug(f"[gemini] configure/generate failed: {e}")
        return None

def _probe_duration_seconds(path: Path) -> Optional[float]:
    if not _has_cmd("ffprobe"):
        return None
    cmd = [
        "ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=nokey=1:noprint_wrappers=1", str(path)
    ]
    try:
        out = subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True).stdout.strip()
        return float(out)
    except Exception:
        return None

def supports_listing() -> bool:
    """
    Checks if the installed youtube-transcript-api exposes list_transcripts.
    """
    try:
        import youtube_transcript_api as _m
        from youtube_transcript_api import YouTubeTranscriptApi as _Y
        has_list = hasattr(_Y, "list_transcripts")
        has_get = hasattr(_Y, "get_transcript")
        mod_file = getattr(_m, "__file__", "<none>")
        log.debug(f"[diag] yt module={mod_file} has_list_transcripts={has_list} has_get_transcript={has_get}")
        return has_list
    except Exception as e:
        log.debug(f"[diag] supports_listing import error: {e}")
        return False


def try_get_transcript_simple(video_id: str, preferred_langs: list[str]):
    """
    Fallback path for older versions without listing:
    Tries get_transcript with several language lists.
    """
    candidates = [preferred_langs, ["en"], ["en-US"], ["en-GB"]]
    for langs in candidates:
        try:
            data = YouTubeTranscriptApi.get_transcript(video_id, languages=langs)
            return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
        except Exception as e:
            log.debug(f"[simple] languages={langs} failed: {e}")
    return None


def fetch_with_retry(fetch_call: Callable[[], list], attempts: int = 5, base_delay: float = 1.2) -> Optional[list]:
    """
    Retries transcript_obj.fetch() to mitigate YouTube 429 / empty responses.
    Retries on:
      - HTTP 429 (Too Many Requests)
      - Empty/invalid XML ('no element found' or empty data)
    Adds small jitter to reduce thundering herd.
    """
    for i in range(attempts):
        try:
            data = fetch_call()
            if not data:
                raise ValueError("empty transcript data")
            return data
        except Exception as e:
            msg = str(e).lower()
            is_rate_limit = "429" in msg or "too many requests" in msg
            is_empty_xml = "no element found" in msg or "empty transcript" in msg or "empty" in msg
            if i < attempts - 1 and (is_rate_limit or is_empty_xml):
                delay = base_delay * (2 ** i) + random.uniform(0.0, 0.6)
                time.sleep(delay)
                continue
            raise


# ---------------------------------------
# Core fetch logic
# ---------------------------------------

def fetch_transcript_segments(video_id: str, lang: str = "en", disable_translate: bool = False):
    """
    Strategy (with throttling-safe retries):
    1) Manual transcript in [lang, en, en-US, en-GB] (retry)
    2) Translate manual to requested lang (retry) — SKIP if lang equals source, SKIP entirely if disable_translate
       GRACEFUL DEGRADE: if translation fails due to 429/empty, return original manual source transcript instead.
    3) Generated transcript in [lang, en, en-US, en-GB] (retry)
    4) If lang=auto → any (manual > generated) (retry)
    5) Last resort → first fetchable (retry)
    6) Direct fallback get_transcript(preferred_langs)
    For environments without listing support, fall back to get_transcript.
    """
    preferred_langs = [lang, "en", "en-US", "en-GB"]
    log.debug(f"[fetch] video_id={video_id} lang={lang} preferred={preferred_langs} disable_translate={disable_translate}")

    if not supports_listing():
        return try_get_transcript_simple(video_id, preferred_langs)

    try:
        list_obj = YouTubeTranscriptApi.list_transcripts(video_id)

        # Debug summary
        summary = []
        for tr in list_obj:
            summary.append({
                "language": tr.language,
                "language_code": tr.language_code,
                "generated": tr.is_generated,
                "translatable": tr.is_translatable
            })
        log.debug(f"[fetch] available_transcripts={summary}")

        # 1) Manual transcript in preferred languages (retry)
        for l in preferred_langs:
            try:
                tr = list_obj.find_manually_created_transcript([l])
                log.debug(f"[fetch] selected manual transcript lang={l}")
                data = fetch_with_retry(lambda: tr.fetch())
                return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
            except Exception as e:
                log.debug(f"[fetch] manual lang={l} not usable: {e}")

        # 2) Translate manual transcript to requested lang (retry) — skip if same lang or disabled
        if not disable_translate:
            for tr in list_obj:
                try:
                    if tr.is_translatable:
                        src_code = (tr.language_code or "").lower()
                        tgt_code = (lang or "en").lower()
                        if src_code == tgt_code:
                            continue
                        t = tr.translate(lang if lang else "en")
                        log.debug(f"[fetch] translate attempt from {tr.language_code} to lang={lang}")
                        try:
                            data = fetch_with_retry(lambda: t.fetch())
                            return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
                        except Exception as e2:
                            # Graceful degrade: return original manual transcript if available
                            log.debug(f"[fetch] translate failed ({e2}); degrading to original {tr.language_code}")
                            try:
                                data_src = fetch_with_retry(lambda: tr.fetch())
                                return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data_src]
                            except Exception as e3:
                                log.debug(f"[fetch] degrade fetch original {tr.language_code} failed: {e3}")
                except Exception as e:
                    log.debug(f"[fetch] translate setup failed for {tr.language_code}: {e}")

        # 3) Generated transcript in preferred languages (retry)
        for l in preferred_langs:
            try:
                tr = list_obj.find_generated_transcript([l])
                log.debug(f"[fetch] selected generated transcript lang={l}")
                data = fetch_with_retry(lambda: tr.fetch())
                return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
            except Exception as e:
                log.debug(f"[fetch] generated lang={l} not usable: {e}")

        # 4) AUTO mode: any available transcript (manual > generated) (retry)
        if lang.lower() == "auto":
            for tr in list_obj:
                try:
                    if not tr.is_generated:
                        log.debug(f"[fetch] AUTO picked manual {tr.language_code}")
                        data = fetch_with_retry(lambda: tr.fetch())
                        return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
                except Exception as e:
                    log.debug(f"[fetch] AUTO manual {tr.language_code} failed: {e}")
            for tr in list_obj:
                try:
                    if tr.is_generated:
                        log.debug(f"[fetch] AUTO picked generated {tr.language_code}")
                        data = fetch_with_retry(lambda: tr.fetch())
                        return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
                except Exception as e:
                    log.debug(f"[fetch] AUTO generated {tr.language_code} failed: {e}")

        # 5) Last resort: first fetchable transcript (any language) (retry)
        for tr in list_obj:
            try:
                log.debug(f"[fetch] last-resort try {tr.language_code} (generated={tr.is_generated})")
                data = fetch_with_retry(lambda: tr.fetch())
                return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
            except Exception as e:
                log.debug(f"[fetch] last-resort {tr.language_code} failed: {e}")

        # 6) Final direct fallback even when listing is available
        try:
            data = YouTubeTranscriptApi.get_transcript(video_id, languages=preferred_langs)
            return [{"text": s["text"], "start": s["start"], "duration": s["duration"]} for s in data]
        except Exception as e:
            log.debug(f"[fetch] direct get_transcript fallback failed: {e}")

    except (TranscriptsDisabled, NoTranscriptFound) as e:
        log.debug(f"[fetch] no transcripts available: {e}")
    except Exception as e:
        log.exception(f"[fetch] unexpected error: {e}")

    return None


# ---------------------------------------
# Routes
# ---------------------------------------

@app.get("/transcript")
def transcript(request: Request, videoId: str, lang: str = "en", disableTranslate: bool = False, sttFallback: bool = True, sttBackend: Optional[str] = None, forceSTT: bool = False):
    """
    Returns a list of segments: [{text, start, duration}, ...]
    If disableTranslate=true, translation is skipped and the best available manual/generated track is returned.
    """
    log.info(f"GET /transcript videoId={videoId} lang={lang} disableTranslate={disableTranslate} client={request.client.host}")
    # Optional Gemini API key override via header
    gem_header = request.headers.get("X-Gemini-Api-Key", "").strip()
    gem_override = gem_header if gem_header else None
    # 1) Cache check
    data = cache_get(videoId, lang, disableTranslate)
    if data:
        log.debug("[cache] hit")
        return data

    # 1b) Failure (circuit breaker) check
    remaining = fail_cache_get(videoId, lang, disableTranslate)
    if remaining:
        log.debug(f"[fail-cache] hit, remaining={remaining}s")
        headers = {"Retry-After": str(remaining)}
        raise HTTPException(status_code=429, detail="Upstream rate limited recently. Please retry later.", headers=headers)

    # 2) Optional: force STT path for testing or explicit bypass of YouTube
    if forceSTT:
        enable_env = os.getenv("ENABLE_STT", "1").strip() != "0"
        backend = (sttBackend or os.getenv("STT_BACKEND", "openai")).strip().lower()
        log.debug(f"[route] forceSTT=True, STT enabled={enable_env} backend='{backend}'")
        if not enable_env:
            raise HTTPException(status_code=400, detail="STT is disabled by ENABLE_STT=0")
        segs: Optional[List[dict]] = None
        if backend == "gemini":
            log.debug("[route] forceSTT -> Gemini (chunked)")
            audio = _download_audio_tmp(videoId)
            if audio:
                try:
                    parts = _split_audio_ffmpeg(audio, segment_seconds=int(os.getenv("GEMINI_SEGMENT_SEC", "300")))
                    segs = _gemini_transcribe_files(parts, lang, api_key_override=gem_override)
                finally:
                    try:
                        shutil.rmtree(audio.parent, ignore_errors=True)
                        if 'parts' in locals() and parts and parts[0].parent != audio.parent:
                            shutil.rmtree(parts[0].parent, ignore_errors=True)
                    except Exception:
                        pass
        elif backend == "openai":
            log.debug("[route] forceSTT -> Whisper API")
            segs = _stt_fallback(videoId, lang)
        elif backend == "local":
            log.debug("[route] forceSTT -> local backend not implemented yet")
        else:
            log.debug(f"[route] forceSTT -> unknown STT_BACKEND='{backend}'")
        if segs:
            cache_set(videoId, lang, disableTranslate, segs)
            return segs
        raise HTTPException(status_code=502, detail="forceSTT failed: no segments produced by backend")

    # 3) Upstream fetch (YouTube captions)
    try:
        data = fetch_transcript_segments(videoId, lang, disable_translate=disableTranslate)
    except Exception as e:
        # For upstream exceptions (often 429), try STT fallback before failing
        log.debug(f"[route] upstream exception: {e}")
        enable_env = os.getenv("ENABLE_STT", "1").strip() != "0"
        backend = (sttBackend or os.getenv("STT_BACKEND", "openai")).strip().lower()
        log.debug(f"[route] STT enabled={enable_env} backend='{backend}' (exception path)")
        if sttFallback and enable_env:
            segs: Optional[List[dict]] = None
            if backend == "gemini":
                log.debug("[route] attempting STT fallback via Gemini (chunked) [exception path]")
                audio = _download_audio_tmp(videoId)
                if audio:
                    try:
                        parts = _split_audio_ffmpeg(audio, segment_seconds=int(os.getenv("GEMINI_SEGMENT_SEC", "300")))
                        segs = _gemini_transcribe_files(parts, lang, api_key_override=gem_override)
                    finally:
                        try:
                            shutil.rmtree(audio.parent, ignore_errors=True)
                            if 'parts' in locals() and parts and parts[0].parent != audio.parent:
                                shutil.rmtree(parts[0].parent, ignore_errors=True)
                        except Exception:
                            pass
            elif backend == "openai":
                log.debug("[route] attempting STT fallback via Whisper API [exception path]")
                segs = _stt_fallback(videoId, lang)
            elif backend == "local":
                log.debug("[route] local STT backend not implemented yet [exception path]")
            else:
                log.debug(f"[route] unknown STT_BACKEND='{backend}', skipping [exception path]")
            if segs:
                cache_set(videoId, lang, disableTranslate, segs)
                return segs
        # If we reach here, STT was disabled or failed. Apply cooldown and return 429
        fail_cache_set(videoId, lang, disableTranslate, retry_after_seconds=120)
        headers = {"Retry-After": "120"}
        raise HTTPException(status_code=429, detail="Upstream error. Please retry later.", headers=headers)
    if not data:
        # 2b) Optional STT fallback (backend selection)
        enable_env = os.getenv("ENABLE_STT", "1").strip() != "0"
        backend = (sttBackend or os.getenv("STT_BACKEND", "openai")).strip().lower()
        log.debug(f"[route] STT enabled={enable_env} backend='{backend}' (no-data path)")
        if sttFallback and enable_env:
            segs: Optional[List[dict]] = None
            if backend == "gemini":
                log.debug("[route] attempting STT fallback via Gemini (chunked)")
                audio = _download_audio_tmp(videoId)
                if audio:
                    try:
                        parts = _split_audio_ffmpeg(audio, segment_seconds=int(os.getenv("GEMINI_SEGMENT_SEC", "300")))
                        segs = _gemini_transcribe_files(parts, lang, api_key_override=gem_override)
                    finally:
                        try:
                            # Clean both original and parts' parent if different
                            shutil.rmtree(audio.parent, ignore_errors=True)
                            if parts and parts[0].parent != audio.parent:
                                shutil.rmtree(parts[0].parent, ignore_errors=True)
                        except Exception:
                            pass
            elif backend == "openai":
                log.debug("[route] attempting STT fallback via Whisper API")
                segs = _stt_fallback(videoId, lang)
            elif backend == "local":
                log.debug("[route] local STT backend not implemented yet")
            else:
                log.debug(f"[route] unknown STT_BACKEND='{backend}', skipping")
            if segs:
                cache_set(videoId, lang, disableTranslate, segs)
                return segs
        # No data (likely 429 or no track available), add cooldown.
        fail_cache_set(videoId, lang, disableTranslate, retry_after_seconds=300)
        headers = {"Retry-After": "300"}
        raise HTTPException(
            status_code=429,
            detail="Transcript temporarily unavailable (throttled or no captions). STT fallback may be disabled or unavailable.",
            headers=headers
        )
    # 3) Cache store
    cache_set(videoId, lang, disableTranslate, data)
    return data


@app.get("/transcript/languages")
def transcript_languages(request: Request, videoId: str):
    """
    Lists available transcripts (language, code, generated, translatable).
    """
    log.info(f"GET /transcript/languages videoId={videoId} client={request.client.host}")
    if not supports_listing():
        return {"error": "Installed youtube-transcript-api does not support list_transcripts. "
                         "Use /transcript?videoId=...&lang=en or upgrade the package."}
    try:
        list_obj = YouTubeTranscriptApi.list_transcripts(videoId)
        langs = []
        for tr in list_obj:
            entry = {
                "language": tr.language,
                "language_code": tr.language_code,
                "generated": tr.is_generated,
                "translatable": tr.is_translatable
            }
            langs.append(entry)
            log.debug(f"[langs] {entry}")
        log.debug(f"[langs] total={len(langs)}")
        return langs
    except (TranscriptsDisabled, NoTranscriptFound) as e:
        log.debug(f"[langs] none available: {e}")
        return []
    except Exception as e:
        log.exception(f"[langs] unexpected error: {e}")
        return {"error": str(e)}
