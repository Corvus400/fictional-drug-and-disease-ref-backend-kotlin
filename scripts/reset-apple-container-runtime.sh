#!/bin/bash
# Description: Force reset stuck Apple Container runtime processes.
# Usage: ./scripts/reset-apple-container-runtime.sh --force

set -euo pipefail

if [ "${1:-}" != "--force" ]; then
    echo "ERROR: This command kills Apple Container runtime processes for every running container."
    echo "Use only when the Apple Container runtime is stuck and named container stop/delete cannot recover it."
    echo "Usage: ./scripts/reset-apple-container-runtime.sh --force"
    exit 2
fi

echo "WARNING: Killing all container-runtime-linux processes."
pkill -f "container-runtime-linux" 2>/dev/null || true
sleep 2
if pgrep -f "container-runtime-linux" > /dev/null 2>&1; then
    pkill -9 -f "container-runtime-linux" 2>/dev/null || true
fi

container system start
echo "Apple Container runtime reset complete."
