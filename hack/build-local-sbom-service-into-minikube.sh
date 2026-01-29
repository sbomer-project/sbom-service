#!/bin/bash
set -e

SBOM_SERVICE_IMAGE="sbom-service:latest"
PROFILE="sbomer"
TAR_FILE="sbom-service.tar"

echo "--- 1. Building Maven project ---"
bash ./hack/build-with-schemas.sh prod

echo "--- 2. Building Container Image ---"
podman build --format docker -t "$SBOM_SERVICE_IMAGE" -f src/main/docker/Dockerfile.jvm .

echo "--- 3. Cleaning old image from Minikube ---"
# This ensures Minikube doesn't use a cached version of 'latest'
minikube -p "$PROFILE" image rm "$SBOM_SERVICE_IMAGE" || true

echo "--- 4. Exporting and Loading image ---"
if [ -f "$TAR_FILE" ]; then rm "$TAR_FILE"; fi
podman save -o "$TAR_FILE" "$SBOM_SERVICE_IMAGE"

echo "--- Loading sbom-service into Minikube ---"
# Force remove the old image from the cluster to clear the cache
minikube -p "$PROFILE" image rm "$SBOM_SERVICE_IMAGE" || true

# This sends the fresh file to Minikube
minikube -p "$PROFILE" image load "$TAR_FILE"

rm "$TAR_FILE"
echo "Done! Image '$SBOM_SERVICE_IMAGE' is fresh in cluster '$PROFILE'."
