#!/bin/bash
# Description: Build and run the backend plus PostgreSQL with Apple Container.
# Usage: ./scripts/start.sh [--public]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

APP_CONTAINER_NAME="fictional-drugref-backend-app"
PG_CONTAINER_NAME="fictional-drugref-backend-postgres"
APP_PORT="${APP_PORT:-18080}"
DB_NAME="drugref"
DB_USER="drugref"
DB_PASSWORD="drugref"
JWT_SECRET_VALUE="local-container-development-only-not-secret"
APP_MEMORY="1g"
APP_CPUS="2"
PG_MEMORY="1g"
PG_CPUS="2"
PG_IMAGE="postgres:17-alpine"
TUNNEL_NAME="fictional-drugref-backend"
TUNNEL_HOSTNAME="${TUNNEL_HOSTNAME:-fictional-drugref.win}"
CLOUDFLARED_DIR="$PROJECT_DIR/cloudflared"
CLOUDFLARED_CONFIG="$CLOUDFLARED_DIR/config.yml"
TUNNEL_PID_FILE="$PROJECT_DIR/.cloudflared.pid"
TUNNEL_LOG="$PROJECT_DIR/.cloudflared.log"
PUBLISH="${PUBLISH:-false}"
case "$(uname -m)" in
    arm64) LOCAL_RUNTIME_PLATFORM="linux/arm64/v8" ;;
    *) LOCAL_RUNTIME_PLATFORM="linux/amd64" ;;
esac

cd "$PROJECT_DIR"

for arg in "$@"; do
    case "$arg" in
        --public) PUBLISH="true" ;;
        -h|--help)
            echo "Usage: ./scripts/start.sh [--public]"
            exit 0
            ;;
        *)
            echo "ERROR: Unknown argument: $arg"
            echo "Usage: ./scripts/start.sh [--public]"
            exit 1
            ;;
    esac
done

if [ "$PUBLISH" = "true" ]; then
    if ! command -v openssl > /dev/null 2>&1; then
        echo "ERROR: openssl is required for public mode secret generation."
        exit 1
    fi
    DB_PASSWORD="$(openssl rand -base64 32)"
    JWT_SECRET_VALUE="$(openssl rand -base64 32)"
fi

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

stop_existing_tunnel() {
    if [ ! -f "$TUNNEL_PID_FILE" ]; then
        return 0
    fi

    local pid
    pid="$(cat "$TUNNEL_PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "Stopping existing Cloudflare Tunnel process ${pid}..."
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

expand_home_path() {
    local raw_path="$1"
    case "$raw_path" in
        "~/"*) printf '%s\n' "$HOME/${raw_path#~/}" ;;
        *) printf '%s\n' "$raw_path" ;;
    esac
}

config_value() {
    local key="$1"
    awk -F: -v key="$key" '$1 ~ "^[[:space:]]*" key "[[:space:]]*$" { sub(/^[[:space:]]*/, "", $2); sub(/[[:space:]]*$/, "", $2); print $2; exit }' "$CLOUDFLARED_CONFIG"
}

ensure_private_file() {
    local path="$1"
    path="$(expand_home_path "$path")"
    if [ ! -f "$path" ]; then
        echo "ERROR: Required cloudflared file is missing: $path"
        echo "Run ./scripts/setup.sh first."
        exit 1
    fi
    chmod 600 "$path"
    local mode
    mode="$(stat -f "%Lp" "$path")"
    case "$mode" in
        600|400) ;;
        *)
            echo "ERROR: Could not restrict permissions for $path; current mode is $mode."
            exit 1
            ;;
    esac
}

start_tunnel() {
    if ! command -v cloudflared > /dev/null 2>&1; then
        echo "ERROR: cloudflared is not installed."
        echo "Run ./scripts/setup.sh first."
        exit 1
    fi
    if [ ! -f "$CLOUDFLARED_CONFIG" ]; then
        echo "ERROR: Missing $CLOUDFLARED_CONFIG."
        echo "Run ./scripts/setup.sh first."
        exit 1
    fi

    local tunnel_id
    local credentials_file
    tunnel_id="$(config_value "tunnel")"
    credentials_file="$(config_value "credentials-file")"
    if [ -z "$tunnel_id" ] || [ -z "$credentials_file" ]; then
        echo "ERROR: $CLOUDFLARED_CONFIG is missing tunnel or credentials-file."
        exit 1
    fi

    ensure_private_file "$credentials_file"
    ensure_private_file "$HOME/.cloudflared/cert.pem"

    echo ""
    echo "Starting Cloudflare Tunnel..."
    nohup cloudflared tunnel --config "$CLOUDFLARED_CONFIG" run "$TUNNEL_NAME" > "$TUNNEL_LOG" 2>&1 &
    echo $! > "$TUNNEL_PID_FILE"
    sleep 3
    local pid
    pid="$(cat "$TUNNEL_PID_FILE")"
    if ! kill -0 "$pid" 2>/dev/null; then
        echo "ERROR: Cloudflare Tunnel did not stay running. Log:"
        tail -50 "$TUNNEL_LOG" 2>/dev/null || true
        exit 1
    fi
    echo "OK: Cloudflare Tunnel is running as PID ${pid}"

    local public_url="https://${TUNNEL_HOSTNAME}/health/ready"
    local status
    status="$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 15 "$public_url" || true)"
    if [ "$status" = "200" ]; then
        echo "OK: Public readiness passed at $public_url"
    else
        echo "WARNING: Public readiness returned ${status:-no response} at $public_url."
        echo "Check $TUNNEL_LOG and Cloudflare DNS if the route was just created."
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
if [ "$PUBLISH" = "true" ]; then
    stop_existing_tunnel
fi

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
JWT_SECRET=${JWT_SECRET_VALUE}
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

if [ "$PUBLISH" = "true" ]; then
    start_tunnel
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
if [ "$PUBLISH" = "true" ]; then
    echo "  https://${TUNNEL_HOSTNAME}/health/ready"
    echo "  https://${TUNNEL_HOSTNAME}/v1/drugs?page=1&page_size=5"
fi
echo ""
echo "Stop: ./scripts/stop.sh"
