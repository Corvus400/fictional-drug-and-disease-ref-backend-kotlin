#!/bin/bash
# Description: Set up local tooling for the backend Apple Container workflow.
# Usage: ./scripts/setup.sh

set -euo pipefail

CONTAINER_VERSION="0.8.0"
CONTAINER_PKG_URL="https://github.com/apple/container/releases/download/${CONTAINER_VERSION}/container-installer-signed.pkg"
CONTAINER_PKG_PATH="/tmp/container-installer-${CONTAINER_VERSION}.pkg"
REQUIRED_JDK_VERSION="21.0.6-tem"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TUNNEL_NAME="fictional-drugref-backend"
TUNNEL_HOSTNAME="${TUNNEL_HOSTNAME:-fictional-drugref.win}"
CLOUDFLARED_DIR="$PROJECT_DIR/cloudflared"
CLOUDFLARED_CONFIG="$CLOUDFLARED_DIR/config.yml"
CLOUDFLARED_EXAMPLE="$CLOUDFLARED_DIR/config.yml.example"

echo "=== Backend environment setup ==="
echo ""

echo "Step 1/7: Checking macOS version..."
MACOS_MAJOR="$(sw_vers -productVersion | cut -d. -f1)"
echo "Detected macOS ${MACOS_MAJOR}"
if [ "$MACOS_MAJOR" -lt 26 ]; then
    echo "WARNING: Apple Container requires macOS 26 or later."
    echo "Use ./gradlew run for local JVM execution on older macOS versions."
fi

echo ""
echo "Step 2/7: Checking Homebrew..."
if command -v brew > /dev/null 2>&1; then
    echo "OK: Homebrew is installed"
else
    echo "WARNING: Homebrew is not installed. JDK 21 may still be available via another toolchain."
fi

echo ""
echo "Step 3/7: Checking JDK 21..."
if [ -z "${SDKMAN_DIR:-}" ]; then
    export SDKMAN_DIR="$HOME/.sdkman"
fi

resolve_java21() {
    local sdk_root="${SDKMAN_DIR:-$HOME/.sdkman}"
    local candidates_dir="$sdk_root/candidates/java"

    if [ -L "$candidates_dir/current" ]; then
        local current_target
        current_target="$(basename "$(readlink "$candidates_dir/current")")"
        if [[ "$current_target" =~ ^21([.\-]|$) ]]; then
            JDK21_PATH="$candidates_dir/$current_target"
            export JAVA_HOME="$JDK21_PATH"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    fi

    if [ -d "$candidates_dir" ]; then
        local sdk_jdk21=""
        local -a jdk21_dirs=()
        for dir in "$candidates_dir"/21*/; do
            [ -d "$dir" ] && jdk21_dirs+=("$(basename "$dir")")
        done
        if [ "${#jdk21_dirs[@]}" -gt 0 ]; then
            sdk_jdk21="$(printf '%s\n' "${jdk21_dirs[@]}" | sort -V | tail -1)"
        fi
        if [ -n "$sdk_jdk21" ]; then
            JDK21_PATH="$candidates_dir/$sdk_jdk21"
            export JAVA_HOME="$JDK21_PATH"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    fi

    if /usr/libexec/java_home -v 21 > /dev/null 2>&1; then
        JDK21_PATH="$(/usr/libexec/java_home -v 21)"
        export JAVA_HOME="$JDK21_PATH"
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi

    return 1
}

JDK21_PATH=""
if resolve_java21; then
    echo "OK: JDK 21 found at $JDK21_PATH"
else
    if [ ! -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
        echo "ERROR: JDK 21 was not found and SDKMAN is not installed."
        echo "Install SDKMAN from https://sdkman.io/install, then rerun ./scripts/setup.sh."
        exit 1
    fi

    echo "SDKMAN is installed but JDK 21 was not found."
    echo "Installing java $REQUIRED_JDK_VERSION..."
    echo Y | sdk install java "$REQUIRED_JDK_VERSION"
    set +u
    # shellcheck source=/dev/null
    source "$SDKMAN_DIR/bin/sdkman-init.sh"
    set -u
    if resolve_java21; then
        echo "OK: JDK 21 installed at $JDK21_PATH"
    else
        echo "ERROR: JDK 21 could not be resolved after installation."
        exit 1
    fi
fi

echo ""
echo "Step 4/7: Checking Apple Container..."
if command -v container > /dev/null 2>&1; then
    CONTAINER_INSTALLED_VERSION="$(container --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 || true)"
    echo "OK: Apple Container ${CONTAINER_INSTALLED_VERSION:-unknown} is installed"
else
    echo "Apple Container is not installed."
    if [ ! -f "$CONTAINER_PKG_PATH" ]; then
        echo "Downloading installer..."
        curl -L -o "$CONTAINER_PKG_PATH" "$CONTAINER_PKG_URL"
    fi
    echo "Opening the installer. Complete it, then press Enter here."
    open "$CONTAINER_PKG_PATH"
    read -r
    if ! command -v container > /dev/null 2>&1; then
        echo "ERROR: Apple Container was not found after installer completion."
        exit 1
    fi
fi

echo ""
echo "Step 5/7: Checking Rosetta 2..."
if [ "$(uname -m)" = "arm64" ]; then
    if arch -x86_64 /usr/bin/true 2>/dev/null; then
        echo "OK: Rosetta 2 is installed"
    else
        echo "Installing Rosetta 2..."
        if ! softwareupdate --install-rosetta --agree-to-license; then
            echo "ERROR: Rosetta 2 installation failed."
            exit 1
        fi
    fi
else
    echo "OK: Intel Mac detected; Rosetta 2 is not required"
fi

echo ""
echo "Step 6/7: Checking Apple Container system service..."
if container system status > /dev/null 2>&1; then
    echo "OK: Apple Container system service is running"
else
    echo "Starting Apple Container system service..."
    container system start
    if ! container system status > /dev/null 2>&1; then
        echo "ERROR: Apple Container system service did not start."
        exit 1
    fi
fi

echo ""
echo "Step 7/7: Checking Cloudflare Tunnel setup..."

install_cloudflared_if_missing() {
    if command -v cloudflared > /dev/null 2>&1; then
        echo "OK: cloudflared is installed"
        return 0
    fi

    if ! command -v brew > /dev/null 2>&1; then
        echo "WARNING: cloudflared is not installed and Homebrew is unavailable."
        echo "Install cloudflared manually, then rerun ./scripts/setup.sh before using ./scripts/start.sh --public."
        return 0
    fi

    echo "cloudflared is not installed."
    echo "Install it with Homebrew now? [y/N]"
    read -r install_answer
    if [[ "$install_answer" =~ ^[Yy]$ ]]; then
        brew install cloudflared
    else
        echo "Skipping cloudflared install. Public tunnel setup remains incomplete."
    fi
}

expand_home_path() {
    local raw_path="$1"
    case "$raw_path" in
        "~/"*) printf '%s\n' "$HOME/${raw_path#~/}" ;;
        *) printf '%s\n' "$raw_path" ;;
    esac
}

fix_cloudflared_permissions() {
    local path="$1"
    [ -n "$path" ] || return 0
    path="$(expand_home_path "$path")"
    if [ -e "$path" ]; then
        chmod 600 "$path"
        echo "OK: restricted permissions on $path"
    fi
}

cloudflared_tunnel_id() {
    cloudflared tunnel list --output json 2>/dev/null \
        | tr -d '\n' \
        | sed -n "s/.*\"id\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\"[^{}]*\"name\"[[:space:]]*:[[:space:]]*\"${TUNNEL_NAME}\".*/\\1/p" \
        | head -1
}

write_cloudflared_config() {
    local tunnel_id="$1"
    local hostname="$2"
    local credentials_file="$HOME/.cloudflared/${tunnel_id}.json"

    mkdir -p "$CLOUDFLARED_DIR"
    if [ ! -f "$CLOUDFLARED_EXAMPLE" ]; then
        echo "ERROR: Missing $CLOUDFLARED_EXAMPLE."
        exit 1
    fi

    sed \
        -e "s|<TUNNEL_UUID>|${tunnel_id}|g" \
        -e "s|<TUNNEL_NAME>|${TUNNEL_NAME}|g" \
        -e "s|<HOSTNAME>|${hostname}|g" \
        -e "s|<CREDENTIALS_FILE>|${credentials_file}|g" \
        "$CLOUDFLARED_EXAMPLE" > "$CLOUDFLARED_CONFIG"
    chmod 600 "$CLOUDFLARED_CONFIG"
    fix_cloudflared_permissions "$credentials_file"
    fix_cloudflared_permissions "$HOME/.cloudflared/cert.pem"
    echo "OK: wrote $CLOUDFLARED_CONFIG"
}

install_cloudflared_if_missing

if command -v cloudflared > /dev/null 2>&1; then
    if [ ! -f "$HOME/.cloudflared/cert.pem" ]; then
        echo "cloudflared is not authenticated."
        echo "A browser login will open. Complete it, then return here."
        cloudflared tunnel login
    else
        echo "OK: cloudflared certificate exists"
    fi
    fix_cloudflared_permissions "$HOME/.cloudflared/cert.pem"

    TUNNEL_ID="$(cloudflared_tunnel_id)"
    if [ -z "$TUNNEL_ID" ]; then
        echo "Creating Cloudflare Tunnel: $TUNNEL_NAME"
        cloudflared tunnel create "$TUNNEL_NAME"
        TUNNEL_ID="$(cloudflared_tunnel_id)"
    else
        echo "OK: Cloudflare Tunnel exists: $TUNNEL_NAME ($TUNNEL_ID)"
    fi

    if [ -z "$TUNNEL_ID" ]; then
        echo "ERROR: Could not resolve tunnel id for $TUNNEL_NAME."
        exit 1
    fi

    echo "Public hostname [$TUNNEL_HOSTNAME]:"
    read -r input_hostname
    if [ -n "$input_hostname" ]; then
        TUNNEL_HOSTNAME="$input_hostname"
    fi

    echo "Routing DNS for $TUNNEL_HOSTNAME..."
    if cloudflared tunnel route dns "$TUNNEL_NAME" "$TUNNEL_HOSTNAME"; then
        echo "OK: DNS route is configured"
    else
        echo "WARNING: DNS route command failed. If the CNAME already exists, continuing is usually safe."
    fi

    write_cloudflared_config "$TUNNEL_ID" "$TUNNEL_HOSTNAME"
else
    echo "Skipping Cloudflare Tunnel setup because cloudflared is unavailable."
fi

echo ""
echo "=== Setup complete ==="
echo "Start: ./scripts/start.sh"
echo "Public start: ./scripts/start.sh --public"
echo "Stop:  ./scripts/stop.sh"
