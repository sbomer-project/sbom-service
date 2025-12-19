#!/bin/bash

# Exit immediately if a command fails
set -e

# Variables for minikube profile and sbom-service
SBOM_SERVICE_IMAGE="sbom-service:latest"
PROFILE="sbomer"
TAR_FILE="sbom-service.tar"

echo "--- Building and inserting sbom-service image into Minikube registry ---"

bash ./hack/build-with-schemas.sh

podman build --format docker -t "$SBOM_SERVICE_IMAGE" -f src/main/docker/Dockerfile.jvm .

echo "--- Exporting sbom-service image to archive ---"
if [ -f "$TAR_FILE" ]; then
    rm "$TAR_FILE"
fi
podman save -o "$TAR_FILE" "$SBOM_SERVICE_IMAGE"

echo "--- Loading sbom-service into Minikube ---"
# This sends the file to Minikube
minikube -p "$PROFILE" image load "$TAR_FILE"

echo "--- Cleanup ---"
rm "$TAR_FILE"

echo "Done! Image '$SBOM_SERVICE_IMAGE' is ready in cluster '$PROFILE'."