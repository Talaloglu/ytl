# syntax=docker/dockerfile:1
FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

# System deps: ffmpeg for audio processing, git (optional), and locales
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python deps
COPY requirements.txt /app/requirements.txt
RUN pip install --upgrade pip \
    && pip install -r /app/requirements.txt

# App code
COPY . /app

# Expose default port
EXPOSE 8000

# Start FastAPI with Uvicorn. Many PaaS set $PORT; default to 8000 locally
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}"]
