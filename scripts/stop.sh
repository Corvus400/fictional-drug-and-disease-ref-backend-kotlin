#!/bin/bash
# Description: Stop and delete backend Apple Container resources.
# Usage: ./scripts/stop.sh

set -euo pipefail

APP_CONTAINER_NAME="fictional-drugref-backend-app"
PG_CONTAINER_NAME="fictional-drugref-backend-postgres"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TUNNEL_PID_FILE="$PROJECT_DIR/.cloudflared.pid"
CMS_PID_FILE="$PROJECT_DIR/.cms.pid"
TUNNEL_LAUNCHD_LABEL="io.github.corvus400.fictionaldrugref.backend.tunnel"
CMS_LAUNCHD_LABEL="io.github.corvus400.fictionaldrugref.cms.dev"

echo "=== Backend Apple Container stop ==="
echo ""

stop_tunnel() {
    if command -v launchctl > /dev/null 2>&1; then
        launchctl remove "$TUNNEL_LAUNCHD_LABEL" 2>/dev/null || true
    fi

    if [ ! -f "$TUNNEL_PID_FILE" ]; then
        return 0
    fi

    local pid
    pid="$(cat "$TUNNEL_PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "Stopping Cloudflare Tunnel process ${pid}..."
        kill "$pid"
        for _ in $(seq 1 10); do
            if ! kill -0 "$pid" 2>/dev/null; then
                break
            fi
            sleep 1
        done
        if kill -0 "$pid" 2>/dev/null; then
            echo "WARNING: Cloudflare Tunnel process ${pid} did not stop after SIGTERM."
            kill -9 "$pid" 2>/dev/null || true
        fi
    fi
    rm -f "$TUNNEL_PID_FILE"
}

stop_cms() {
    if command -v launchctl > /dev/null 2>&1; then
        launchctl remove "$CMS_LAUNCHD_LABEL" 2>/dev/null || true
    fi

    if [ ! -f "$CMS_PID_FILE" ]; then
        return 0
    fi

    local pid
    pid="$(cat "$CMS_PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "Stopping CMS dev server process ${pid}..."
        kill "$pid"
        for _ in $(seq 1 10); do
            if ! kill -0 "$pid" 2>/dev/null; then
                break
            fi
            sleep 1
        done
        if kill -0 "$pid" 2>/dev/null; then
            echo "WARNING: CMS dev server process ${pid} did not stop after SIGTERM."
            kill -9 "$pid" 2>/dev/null || true
        fi
    fi
    rm -f "$CMS_PID_FILE"
}

stop_tunnel
stop_cms

if ! command -v container > /dev/null 2>&1; then
    echo "ERROR: Apple Container is not installed."
    exit 1
fi

echo "Stopping and deleting containers..."
container stop "$APP_CONTAINER_NAME" 2>/dev/null || true
container delete "$APP_CONTAINER_NAME" 2>/dev/null || true
container stop "$PG_CONTAINER_NAME" 2>/dev/null || true
container delete "$PG_CONTAINER_NAME" 2>/dev/null || true

echo ""
echo "=== Stop complete ==="
