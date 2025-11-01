# Docker Build and Push Script

This script automates building and pushing Traccar Docker images to Docker Hub.

## Prerequisites

- Java (for building Traccar)
- Docker
- zip/unzip utilities
- Git (optional, for version management)

### On Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk docker.io zip unzip
```

### On macOS:
```bash
brew install openjdk@17 docker zip unzip
```

## Docker Hub Token

You need a Docker Hub access token to push images. To create one:

1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Give it a name (e.g., "traccar-build")
4. Copy the generated token

## Usage

### Method 1: Pass token as parameter
```bash
./docker-build-push.sh [VERSION] [DOCKER_HUB_TOKEN]
```

Example:
```bash
./docker-build-push.sh 6.9.0 dckr_pat_xxxxxxxxxxxxxxxxxxxxx
```

### Method 2: Use environment variable (recommended)
```bash
export DOCKER_HUB_TOKEN="dckr_pat_xxxxxxxxxxxxxxxxxxxxx"
./docker-build-push.sh 6.9.0
```

### Method 3: Auto-detect version
If you don't specify a version, it will be extracted from `build.gradle`:
```bash
export DOCKER_HUB_TOKEN="dckr_pat_xxxxxxxxxxxxxxxxxxxxx"
./docker-build-push.sh
```

## What the script does

1. Detects OS (Linux/macOS)
2. Extracts version from build.gradle (if not provided)
3. Builds Traccar using Gradle
4. Creates the installer package (traccar-other-VERSION.zip)
5. Logs in to Docker Hub
6. Builds Docker image using Ubuntu Dockerfile
7. Tags image with both version and 'latest'
8. Pushes both tags to Docker Hub
9. Logs out and cleans up

## Configuration

Current configuration (hardcoded in script):
- **Docker Hub Username**: `srkraut`
- **Docker Hub Repository**: `trackon`
- **Full Image Name**: `srkraut/trackon`

To change these, edit the following lines in `docker-build-push.sh`:
```bash
DOCKER_USERNAME="srkraut"
DOCKER_REPO="trackon"
```

## Building Web Interface

If you need to build the web interface separately:

```bash
cd traccar-web
npm install
npm run build
cd ..
```

Then run the docker build script.

## Troubleshooting

### Permission denied for Docker
If you get Docker permission errors on Linux:

```bash
# Add your user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Or the script will automatically use sudo
```

### Version not detected
If version detection fails, manually specify it:
```bash
./docker-build-push.sh 6.9.0 your_docker_token
```

### Web build not found
The script will warn if `traccar-web/build` doesn't exist. Build it first or continue anyway (the container will be missing the web UI).

## Example Complete Workflow

```bash
# 1. Checkout desired version
git checkout v6.9.0

# 2. Set your Docker Hub token
export DOCKER_HUB_TOKEN="dckr_pat_xxxxxxxxxxxxxxxxxxxxx"

# 3. Run the script
./docker-build-push.sh

# Script will:
# - Auto-detect version as 6.9.0
# - Build everything
# - Push to srkraut/trackon:6.9.0 and srkraut/trackon:latest
```

## Verify Published Image

After successful push:
```bash
# Pull the image
docker pull srkraut/trackon:6.9.0

# Run it
docker run -d -p 8082:8082 srkraut/trackon:6.9.0

# Access at http://localhost:8082
```

## Security Notes

- Never commit your Docker Hub token to git
- Use environment variables or pass as parameters
- The script automatically logs out after pushing
- Consider using Docker Hub organizations for team access
