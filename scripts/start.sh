#!/bin/bash
# Description: Build and run the backend plus PostgreSQL with Apple Container.
# Usage: ./scripts/start.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

APP_CONTAINER_NAME="fictional-drugref-backend-app"
PG_CONTAINER_NAME="fictional-drugref-backend-postgres"
APP_PORT="8080"
DB_NAME="drugref"
DB_USER="drugref"
DB_PASSWORD="drugref"
APP_MEMORY="1g"
APP_CPUS="2"
PG_MEMORY="1g"
PG_CPUS="2"
PG_IMAGE="postgres:17-alpine"
case "$(uname -m)" in
    arm64) LOCAL_RUNTIME_PLATFORM="linux/arm64/v8" ;;
    *) LOCAL_RUNTIME_PLATFORM="linux/amd64" ;;
esac

cd "$PROJECT_DIR"

GIT_SHA="$(git rev-parse --short HEAD 2>/dev/null || echo local)"
IMAGE_NAME="fictional-drug-and-disease-ref-backend-kotlin:${GIT_SHA}"
PG_ENV_FILE=""
APP_ENV_FILE=""

cleanup_env_files() {
    [ -n "$PG_ENV_FILE" ] && rm -f "$PG_ENV_FILE"
    [ -n "$APP_ENV_FILE" ] && rm -f "$APP_ENV_FILE"
}
trap cleanup_env_files EXIT

echo "=== Backend Apple Container start ==="
echo ""

if ! command -v container > /dev/null 2>&1; then
    echo "ERROR: Apple Container is not installed."
    echo "Run ./scripts/setup.sh first."
    exit 1
fi

if ! container system status > /dev/null 2>&1; then
    echo "Starting Apple Container system service..."
    container system start
fi

cleanup_duplicate_runtime_processes() {
    local process_count
    process_count="$( (pgrep -f "container-runtime-linux" 2>/dev/null || true) | wc -l | tr -d ' ')"
    if [ "$process_count" -gt 1 ]; then
        echo "WARNING: Found ${process_count} container-runtime-linux processes; cleaning up."
        pkill -f "container-runtime-linux" 2>/dev/null || true
        sleep 2
        if pgrep -f "container-runtime-linux" > /dev/null 2>&1; then
            pkill -9 -f "container-runtime-linux" 2>/dev/null || true
        fi
        container system start
    fi
}

container_ip() {
    local name="$1"
    container list | awk -v name="$name" '$0 ~ name { split($6, parts, "/"); print parts[1]; exit }'
}

wait_for_tcp() {
    local host="$1"
    local port="$2"
    local label="$3"
    local attempts=60
    local i
    for i in $(seq 1 "$attempts"); do
        if nc -z "$host" "$port" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done
    echo "ERROR: Timed out waiting for ${label} at ${host}:${port}."
    return 1
}

wait_for_http_200() {
    local url="$1"
    local label="$2"
    local attempts=90
    local status
    local i
    for i in $(seq 1 "$attempts"); do
        status="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 --max-time 5 "$url" || true)"
        if [ "$status" = "200" ]; then
            return 0
        fi
        sleep 1
    done
    echo "ERROR: Timed out waiting for ${label} at ${url}."
    container logs "$APP_CONTAINER_NAME" 2>/dev/null || true
    return 1
}

cleanup_duplicate_runtime_processes

echo "Cleaning up existing containers..."
container stop "$APP_CONTAINER_NAME" 2>/dev/null || true
container delete "$APP_CONTAINER_NAME" 2>/dev/null || true
container stop "$PG_CONTAINER_NAME" 2>/dev/null || true
container delete "$PG_CONTAINER_NAME" 2>/dev/null || true

PG_ENV_FILE="$(mktemp)"
chmod 600 "$PG_ENV_FILE"
cat > "$PG_ENV_FILE" <<EOF
POSTGRES_DB=${DB_NAME}
POSTGRES_USER=${DB_USER}
POSTGRES_PASSWORD=${DB_PASSWORD}
EOF

echo ""
echo "Step 1/5: Starting PostgreSQL..."
container run -d --name "$PG_CONTAINER_NAME" \
    --memory "$PG_MEMORY" --cpus "$PG_CPUS" \
    --env-file "$PG_ENV_FILE" \
    "$PG_IMAGE"

PG_IP=""
for _ in $(seq 1 30); do
    PG_IP="$(container_ip "$PG_CONTAINER_NAME")"
    [ -n "$PG_IP" ] && break
    sleep 1
done
if [ -z "$PG_IP" ]; then
    echo "ERROR: Could not resolve PostgreSQL container IP."
    container logs "$PG_CONTAINER_NAME" 2>/dev/null || true
    exit 1
fi
wait_for_tcp "$PG_IP" 5432 "PostgreSQL TCP readiness"
if container exec "$PG_CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
    echo "OK: PostgreSQL is ready at ${PG_IP}:5432"
else
    echo "ERROR: PostgreSQL TCP is open but pg_isready failed."
    container logs "$PG_CONTAINER_NAME" 2>/dev/null || true
    exit 1
fi

echo ""
echo "Step 2/5: Building application image..."
container build --build-arg "RUNTIME_PLATFORM=${LOCAL_RUNTIME_PLATFORM}" -t "$IMAGE_NAME" .

APP_ENV_FILE="$(mktemp)"
chmod 600 "$APP_ENV_FILE"
cat > "$APP_ENV_FILE" <<EOF
DB_URL=jdbc:postgresql://${PG_IP}:5432/${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
APP_ENV=production
JWT_SECRET=local-container-development-only-not-secret
PORT=${APP_PORT}
HOST=0.0.0.0
EOF

echo ""
echo "Step 3/5: Starting application..."
container run -d --name "$APP_CONTAINER_NAME" \
    -p "127.0.0.1:${APP_PORT}:${APP_PORT}" \
    --user 10001 --read-only --tmpfs /tmp \
    --memory "$APP_MEMORY" --cpus "$APP_CPUS" \
    --env-file "$APP_ENV_FILE" \
    "$IMAGE_NAME"

APP_IP=""
for _ in $(seq 1 30); do
    APP_IP="$(container_ip "$APP_CONTAINER_NAME")"
    [ -n "$APP_IP" ] && break
    sleep 1
done
if [ -z "$APP_IP" ]; then
    echo "ERROR: Could not resolve application container IP."
    container logs "$APP_CONTAINER_NAME" 2>/dev/null || true
    exit 1
fi

echo ""
echo "Step 4/5: Waiting for application readiness..."
wait_for_http_200 "http://${APP_IP}:${APP_PORT}/health/ready" "application readiness"

echo ""
echo "Step 5/5: Verifying API data path..."
if curl -s --connect-timeout 5 "http://${APP_IP}:${APP_PORT}/v1/drugs?page=1&page_size=5" | grep -q '"items"'; then
    echo "OK: /v1/drugs returns seeded data"
else
    echo "ERROR: /v1/drugs did not return expected data."
    container logs "$APP_CONTAINER_NAME" 2>/dev/null || true
    exit 1
fi

echo ""
echo "=== Backend started ==="
echo "App container: $APP_CONTAINER_NAME"
echo "PostgreSQL container: $PG_CONTAINER_NAME"
echo "App container IP: $APP_IP"
echo "PostgreSQL container IP: $PG_IP"
echo ""
echo "URLs:"
echo "  http://127.0.0.1:${APP_PORT}/health"
echo "  http://127.0.0.1:${APP_PORT}/health/ready"
echo "  http://127.0.0.1:${APP_PORT}/v1/drugs?page=1&page_size=5"
echo ""
echo "Stop: ./scripts/stop.sh"
