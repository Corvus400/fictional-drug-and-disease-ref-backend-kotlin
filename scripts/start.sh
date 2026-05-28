#!/bin/bash
# Description: Build and run the backend plus PostgreSQL with Apple Container.
# Usage: ./scripts/start.sh [--public]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

APP_CONTAINER_NAME="fictional-drugref-backend-app"
PG_CONTAINER_NAME="fictional-drugref-backend-postgres"
APP_PORT="${APP_PORT:-18080}"
ADMIN_PORT="${ADMIN_PORT:-19090}"
DB_NAME="drugref"
DB_USER="drugref"
DB_PASSWORD="drugref"
JWT_SECRET_VALUE="local-container-development-only-not-secret"
LOCAL_CMS_ORIGINS="http://127.0.0.1:5173,http://localhost:5173"
CORS_ALLOWED_ORIGINS_VALUE="${CORS_ALLOWED_ORIGINS:-$LOCAL_CMS_ORIGINS}"
ADMIN_CORS_ALLOWED_ORIGINS_VALUE="${ADMIN_CORS_ALLOWED_ORIGINS:-$LOCAL_CMS_ORIGINS}"
APP_MEMORY="1g"
APP_CPUS="2"
PG_MEMORY="1g"
PG_CPUS="2"
PG_IMAGE="postgres:17-alpine"
CMS_DIR="${CMS_DIR:-$(dirname "$PROJECT_DIR")/fictional-drug-and-disease-ref-cms}"
CMS_HOST="${CMS_HOST:-127.0.0.1}"
CMS_PORT="${CMS_PORT:-5173}"
CMS_API_TIMEOUT_MS="${CMS_API_TIMEOUT_MS:-10000}"
CMS_ADMIN_TOKEN_PATH="${CMS_ADMIN_TOKEN_PATH:-/v1/admin/token}"
CMS_PID_FILE="$PROJECT_DIR/.cms.pid"
CMS_LOG="$PROJECT_DIR/.cms.log"
CMS_LAUNCHD_LABEL="io.github.corvus400.fictionaldrugref.cms.dev"
TUNNEL_NAME="fictional-drugref-backend"
TUNNEL_HOSTNAME="${TUNNEL_HOSTNAME:-fictional-drugref.win}"
CLOUDFLARED_DIR="$PROJECT_DIR/cloudflared"
CLOUDFLARED_CONFIG="$CLOUDFLARED_DIR/config.yml"
TUNNEL_PID_FILE="$PROJECT_DIR/.cloudflared.pid"
TUNNEL_LOG="$PROJECT_DIR/.cloudflared.log"
TUNNEL_LAUNCHD_LABEL="io.github.corvus400.fictionaldrugref.backend.tunnel"
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

if [ -z "${CMS_ENABLED+x}" ]; then
    if [ "$PUBLISH" = "true" ]; then
        CMS_ENABLED="false"
    else
        CMS_ENABLED="true"
    fi
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

stop_existing_tunnel() {
    if command -v launchctl > /dev/null 2>&1; then
        launchctl remove "$TUNNEL_LAUNCHD_LABEL" 2>/dev/null || true
    fi

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

stop_existing_cms() {
    if command -v launchctl > /dev/null 2>&1; then
        launchctl remove "$CMS_LAUNCHD_LABEL" 2>/dev/null || true
    fi

    if [ ! -f "$CMS_PID_FILE" ]; then
        return 0
    fi

    local pid
    pid="$(cat "$CMS_PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "Stopping existing CMS dev server process ${pid}..."
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

launchd_pid() {
    local label="$1"
    launchctl list 2>/dev/null | awk -v label="$label" '$3 == label { print $1; exit }'
}

wait_for_launchd_pid() {
    local label="$1"
    local log_path="$2"
    local display_name="$3"
    local pid
    local i
    for i in $(seq 1 10); do
        pid="$(launchd_pid "$label")"
        case "$pid" in
            ""|"-") ;;
            *)
                printf '%s\n' "$pid"
                return 0
                ;;
        esac
        sleep 1
    done

    echo "ERROR: ${display_name} did not stay running. Log:" >&2
    tail -50 "$log_path" 2>/dev/null >&2 || true
    return 1
}

start_tunnel() {
    local cloudflared_bin
    cloudflared_bin="$(command -v cloudflared || true)"
    if [ -z "$cloudflared_bin" ]; then
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
    local pid
    if ! command -v launchctl > /dev/null 2>&1; then
        echo "ERROR: launchctl is required to keep Cloudflare Tunnel running after start.sh exits."
        exit 1
    fi
    launchctl submit -l "$TUNNEL_LAUNCHD_LABEL" -o "$TUNNEL_LOG" -e "$TUNNEL_LOG" -- \
        "$cloudflared_bin" tunnel --config "$CLOUDFLARED_CONFIG" run "$TUNNEL_NAME"
    pid="$(wait_for_launchd_pid "$TUNNEL_LAUNCHD_LABEL" "$TUNNEL_LOG" "Cloudflare Tunnel")"
    echo "$pid" > "$TUNNEL_PID_FILE"
    echo "OK: Cloudflare Tunnel is running as PID ${pid}"

    local public_url="https://${TUNNEL_HOSTNAME}/health/ready"
    wait_for_http_200 "$public_url" "public readiness"
    echo "OK: Public readiness passed at $public_url"
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

start_cms() {
    if [ "$CMS_ENABLED" != "true" ]; then
        return 0
    fi

    if [ ! -d "$CMS_DIR" ]; then
        echo "ERROR: CMS directory is missing: $CMS_DIR"
        echo "Set CMS_DIR to the sibling CMS checkout or run CMS_ENABLED=false ./scripts/start.sh."
        exit 1
    fi
    if [ ! -f "$CMS_DIR/package.json" ]; then
        echo "ERROR: CMS package.json is missing in $CMS_DIR."
        exit 1
    fi
    if [ ! -d "$CMS_DIR/node_modules" ]; then
        echo "ERROR: CMS dependencies are not installed in $CMS_DIR."
        echo "Run pnpm install in the CMS checkout, or run CMS_ENABLED=false ./scripts/start.sh."
        exit 1
    fi
    local pnpm_bin
    local cms_path
    pnpm_bin="$(command -v pnpm || true)"
    if [ -z "$pnpm_bin" ]; then
        echo "ERROR: pnpm is required to start the CMS dev server."
        exit 1
    fi
    cms_path="$PATH"

    stop_existing_cms
    echo ""
    echo "Starting CMS dev server..."
    local pid
    if ! command -v launchctl > /dev/null 2>&1; then
        echo "ERROR: launchctl is required to keep the CMS dev server running after start.sh exits."
        exit 1
    fi
    launchctl submit -l "$CMS_LAUNCHD_LABEL" -o "$CMS_LOG" -e "$CMS_LOG" -- \
        /bin/bash -c \
        'cd "$1" || exit 1
        export VITE_API_BASE_URL="http://127.0.0.1:${4}"
        export VITE_API_TIMEOUT_MS="$5"
        export VITE_ADMIN_TOKEN_PATH="$6"
        export PATH="$7"
        tail -f /dev/null | "$8" dev --host "$2" --port "$3" --strictPort' \
        _ "$CMS_DIR" "$CMS_HOST" "$CMS_PORT" "$ADMIN_PORT" "$CMS_API_TIMEOUT_MS" "$CMS_ADMIN_TOKEN_PATH" "$cms_path" "$pnpm_bin"
    pid="$(wait_for_launchd_pid "$CMS_LAUNCHD_LABEL" "$CMS_LOG" "CMS dev server")"
    echo "$pid" > "$CMS_PID_FILE"
    wait_for_http_200 "http://${CMS_HOST}:${CMS_PORT}/" "CMS dev server"
    echo "OK: CMS dev server is running as PID ${pid}"
}

if [ "$PUBLISH" = "true" ]; then
    stop_existing_tunnel
fi
if [ "$CMS_ENABLED" = "true" ]; then
    stop_existing_cms
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
echo "Step 1/6: Starting PostgreSQL..."
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
echo "Step 2/6: Building application image..."
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
ADMIN_HOST=0.0.0.0
ADMIN_PORT=${ADMIN_PORT}
ALLOW_CONTAINER_ADMIN_WILDCARD_BIND=true
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS_VALUE}
ADMIN_CORS_ALLOWED_ORIGINS=${ADMIN_CORS_ALLOWED_ORIGINS_VALUE}
EOF

echo ""
echo "Step 3/6: Starting application..."
container run -d --name "$APP_CONTAINER_NAME" \
    -p "127.0.0.1:${APP_PORT}:${APP_PORT}" \
    -p "127.0.0.1:${ADMIN_PORT}:${ADMIN_PORT}" \
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
echo "Step 4/6: Waiting for application readiness..."
wait_for_http_200 "http://${APP_IP}:${APP_PORT}/health/ready" "application readiness"
wait_for_http_200 "http://127.0.0.1:${APP_PORT}/health/ready" "published localhost readiness"
wait_for_http_200 "http://127.0.0.1:${ADMIN_PORT}/health/ready" "published admin localhost readiness"

echo ""
echo "Step 5/6: Verifying API data path..."
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
echo "Step 6/6: Starting CMS when enabled..."
start_cms

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
echo "  http://127.0.0.1:${ADMIN_PORT}/v1/admin/token"
if [ "$CMS_ENABLED" = "true" ]; then
    echo "  http://${CMS_HOST}:${CMS_PORT}/"
fi
if [ "$PUBLISH" = "true" ]; then
    echo "  https://${TUNNEL_HOSTNAME}/health/ready"
    echo "  https://${TUNNEL_HOSTNAME}/v1/drugs?page=1&page_size=5"
fi
echo ""
echo "Stop: ./scripts/stop.sh"
