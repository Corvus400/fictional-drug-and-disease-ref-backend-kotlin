#!/bin/bash
# Description: Stop and delete backend Apple Container resources.
# Usage: ./scripts/stop.sh

set -euo pipefail

APP_CONTAINER_NAME="fictional-drugref-backend-app"
PG_CONTAINER_NAME="fictional-drugref-backend-postgres"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TUNNEL_PID_FILE="$PROJECT_DIR/.cloudflared.pid"

echo "=== Backend Apple Container stop ==="
echo ""

stop_tunnel() {
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

stop_tunnel

if ! command -v container > /dev/null 2>&1; then
    echo "ERROR: Apple Container is not installed."
    exit 1
fi

echo "Stopping and deleting containers..."
container stop "$APP_CONTAINER_NAME" 2>/dev/null || true
container delete "$APP_CONTAINER_NAME" 2>/dev/null || true
container stop "$PG_CONTAINER_NAME" 2>/dev/null || true
container delete "$PG_CONTAINER_NAME" 2>/dev/null || true

cleanup_zombie_runtime_processes() {
    local process_count
    process_count="$( (pgrep -f "container-runtime-linux" 2>/dev/null || true) | wc -l | tr -d ' ')"
    if [ "$process_count" -gt 1 ]; then
        echo "WARNING: Found ${process_count} container-runtime-linux processes; cleaning up."
        pkill -f "container-runtime-linux" 2>/dev/null || true
        sleep 2
        if pgrep -f "container-runtime-linux" > /dev/null 2>&1; then
            pkill -9 -f "container-runtime-linux" 2>/dev/null || true
        fi
    fi
}

cleanup_zombie_runtime_processes

echo ""
echo "=== Stop complete ==="
