#!/usr/bin/env bash

#
# Script to build and push Traccar Docker image to Docker Hub
# Usage: ./docker-build-push.sh [VERSION] [DOCKER_HUB_TOKEN]
#
# If VERSION is not provided, it will be extracted from build.gradle
# DOCKER_HUB_TOKEN can also be set via environment variable: DOCKER_HUB_TOKEN
#

set -e

# Color output functions
info() {
  echo -e "[\033[1;34mINFO\033[0m] $1"
}

ok() {
  echo -e "[\033[1;32m OK \033[0m] $1"
}

error() {
  echo -e "[\033[1;31mERROR\033[0m] $1"
}

warn() {
  echo -e "[\033[1;33mWARN\033[0m] $1"
}

# Configuration
DOCKER_USERNAME="srkraut"
DOCKER_REPO="trackon"
DOCKER_IMAGE="${DOCKER_USERNAME}/${DOCKER_REPO}"

# Change to script directory (traccar root)
cd "$(dirname "$0")"

# Get version from parameter or build.gradle
if [ -n "$1" ]; then
  VERSION="$1"
else
  info "Extracting version from build.gradle..."
  VERSION=$(grep -o 'Implementation-Version.*[0-9]\+\.[0-9]\+\.[0-9]\+' build.gradle | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+')
  if [ -z "$VERSION" ]; then
    error "Could not extract version from build.gradle"
    exit 1
  fi
  info "Detected version: $VERSION"
fi

# Get Docker Hub token
if [ -n "$2" ]; then
  DOCKER_TOKEN="$2"
elif [ -n "$DOCKER_HUB_TOKEN" ]; then
  DOCKER_TOKEN="$DOCKER_HUB_TOKEN"
else
  error "Docker Hub token not provided!"
  echo "Usage: $0 [VERSION] [DOCKER_HUB_TOKEN]"
  echo "Or set environment variable: export DOCKER_HUB_TOKEN=your_token"
  exit 1
fi

# Check if running on macOS or Linux
OS="$(uname -s)"
case "$OS" in
  Linux*)  PLATFORM="linux" ;;
  Darwin*) PLATFORM="macos" ;;
  *)       error "Unsupported OS: $OS"; exit 1 ;;
esac
info "Detected platform: $PLATFORM"

# Check prerequisites
info "Checking prerequisites..."
command -v java >/dev/null 2>&1 || { error "Java is not installed"; exit 1; }
command -v docker >/dev/null 2>&1 || { error "Docker is not installed"; exit 1; }
command -v zip >/dev/null 2>&1 || { error "zip is not installed"; exit 1; }
command -v unzip >/dev/null 2>&1 || { error "unzip is not installed"; exit 1; }

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
  error "gradlew not found. Are you in the traccar root directory?"
  exit 1
fi

ok "All prerequisites satisfied"

# Build traccar
info "Building Traccar $VERSION..."
./gradlew clean assemble
ok "Traccar build completed"

# Check if web build exists
if [ ! -d "traccar-web/build" ]; then
  warn "Web build not found at traccar-web/build/"
  warn "You may need to build the web interface separately"
  read -p "Continue anyway? (y/N) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
fi

# Create installer package
info "Creating installer package..."
cd setup
./package.sh "$VERSION" other
cd ..

# Move installer to installers directory
info "Moving installer to installers directory..."
mkdir -p installers
mv setup/traccar-other-${VERSION}.zip installers/
ok "Installer package created: installers/traccar-other-${VERSION}.zip"

# Check if installer exists
if [ ! -f "installers/traccar-other-${VERSION}.zip" ]; then
  error "Installer not found: installers/traccar-other-${VERSION}.zip"
  exit 1
fi

# Login to Docker Hub
info "Logging in to Docker Hub..."
echo "$DOCKER_TOKEN" | docker login -u "$DOCKER_USERNAME" --password-stdin
ok "Logged in to Docker Hub"

# Determine if sudo is needed for Docker
DOCKER_CMD="docker"
if [ "$PLATFORM" = "linux" ] && ! docker ps >/dev/null 2>&1; then
  warn "Docker requires sudo on this system"
  DOCKER_CMD="sudo docker"
  # Re-login with sudo if needed
  echo "$DOCKER_TOKEN" | sudo docker login -u "$DOCKER_USERNAME" --password-stdin
fi

# Build Docker image
info "Building Docker image ${DOCKER_IMAGE}:${VERSION}..."
$DOCKER_CMD build \
  -f docker/Dockerfile.ubuntu \
  --build-arg VERSION=${VERSION} \
  -t ${DOCKER_IMAGE}:${VERSION} \
  -t ${DOCKER_IMAGE}:latest \
  .
ok "Docker image built successfully"

# Push to Docker Hub
info "Pushing ${DOCKER_IMAGE}:${VERSION} to Docker Hub..."
$DOCKER_CMD push ${DOCKER_IMAGE}:${VERSION}
ok "Pushed ${DOCKER_IMAGE}:${VERSION}"

info "Pushing ${DOCKER_IMAGE}:latest to Docker Hub..."
$DOCKER_CMD push ${DOCKER_IMAGE}:latest
ok "Pushed ${DOCKER_IMAGE}:latest"

# Cleanup - logout
info "Logging out from Docker Hub..."
docker logout 2>/dev/null || true
if [ "$DOCKER_CMD" = "sudo docker" ]; then
  sudo docker logout 2>/dev/null || true
fi

ok "========================================="
ok "Build and push completed successfully!"
ok "========================================="
echo ""
info "Image details:"
echo "  - Repository: ${DOCKER_IMAGE}"
echo "  - Version tag: ${VERSION}"
echo "  - Latest tag: latest"
echo ""
info "Pull with:"
echo "  docker pull ${DOCKER_IMAGE}:${VERSION}"
echo "  docker pull ${DOCKER_IMAGE}:latest"
echo ""
info "Run with:"
echo "  docker run -d -p 8082:8082 ${DOCKER_IMAGE}:${VERSION}"
