#!/usr/bin/env bash

# Automated Retry Testing Script for sbom-service
# This script simulates a failure scenario and verifies automatic retry behavior

set -e

# Configuration
NAMESPACE="sbomer-test"
GATEWAY_URL="http://localhost:8080"
ERROR_CODE=99  # ERR_SYSTEM
ERROR_REASON="Automated test: System error"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl."
        exit 1
    fi
    
    # Check if jq is available
    if ! command -v jq &> /dev/null; then
        log_error "jq not found. Please install jq."
        exit 1
    fi
    
    # Check if curl is available
    if ! command -v curl &> /dev/null; then
        log_error "curl not found. Please install curl."
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace '$NAMESPACE' not found. Please deploy sbom-service first."
        exit 1
    fi
    
    # Check if gateway is accessible (just check if we get any response)
    if ! curl -s "$GATEWAY_URL/q/health" &> /dev/null; then
        log_error "Gateway not accessible at $GATEWAY_URL"
        log_warning "Make sure port-forward is running: kubectl port-forward svc/sbomer-release-gateway 8080:8080 -n $NAMESPACE"
        exit 1
    fi
    
    log_success "All prerequisites met"
}

# Get Kafka pod name
get_kafka_pod() {
    log_info "Finding Kafka pod..."
    KAFKA_POD=$(kubectl get pods -n "$NAMESPACE" -l app=kafka -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$KAFKA_POD" ]; then
        log_error "Kafka pod not found in namespace $NAMESPACE"
        exit 1
    fi
    
    log_success "Found Kafka pod: $KAFKA_POD"
}

# Send Kafka message
send_kafka_message() {
    local generation_id=$1
    local status=$2
    local result_code=$3
    local reason=$4
    local sbom=$5
    
    local message
    if [ "$status" = "COMPLETED" ]; then
        message="{\"status\":\"$status\",\"data\":{\"resultCode\":$result_code,\"sbom\":\"$sbom\"}}"
    else
        message="{\"status\":\"$status\",\"data\":{\"resultCode\":$result_code,\"reason\":\"$reason\"}}"
    fi
    
    log_info "Sending Kafka message: $status (code: $result_code)"
    
    kubectl exec -i "$KAFKA_POD" -n "$NAMESPACE" -- bash -c "
        echo '${generation_id}:${message}' | \
        /opt/kafka/bin/kafka-console-producer.sh \
          --bootstrap-server localhost:9092 \
          --topic generation.update \
          --property 'parse.key=true' \
          --property 'key.separator=:' 2>/dev/null
    "
}

# Trigger generation
trigger_generation() {
    log_info "Triggering new generation..."
    
    local response
    response=$(curl -s -X POST "$GATEWAY_URL/api/v1/generations" \
      -H "Content-Type: application/json" \
      -d '{
        "generationRequests": [{
          "target": {
            "type": "CONTAINER_IMAGE",
            "identifier": "quay.io/test/retry-test:v1"
          }
        }]
      }')
    
    REQUEST_ID=$(echo "$response" | jq -r '.id')
    
    if [ -z "$REQUEST_ID" ] || [ "$REQUEST_ID" = "null" ]; then
        log_error "Failed to create generation request"
        echo "Response: $response"
        exit 1
    fi
    
    log_success "Request created: $REQUEST_ID"
    
    # Wait a moment for generation to be created
    sleep 2
    
    # Get the generation ID from the request
    local generations_response
    generations_response=$(curl -s "$GATEWAY_URL/api/v1/requests/$REQUEST_ID/generations")
    
    GEN_ID=$(echo "$generations_response" | jq -r '.content[0].id')
    
    if [ -z "$GEN_ID" ] || [ "$GEN_ID" = "null" ]; then
        log_error "Failed to get generation ID from request"
        echo "Generations response: $generations_response"
        exit 1
    fi
    
    log_success "Generation created: $GEN_ID"
}

# Get run count
get_run_count() {
    local generation_id=$1
    curl -s "$GATEWAY_URL/api/v1/generations/$generation_id/runs" | jq 'length'
}

# Get generation status
get_generation_status() {
    local generation_id=$1
    curl -s "$GATEWAY_URL/api/v1/generations/$generation_id" | jq -r '.status'
}

# Get runs details
get_runs_details() {
    local generation_id=$1
    curl -s "$GATEWAY_URL/api/v1/generations/$generation_id/runs" | \
        jq -r '.[] | "  Run \(.attemptNumber): state=\(.state), result=\(.result // "null")"'
}

# Watch logs for retry message
watch_logs_for_retry() {
    local generation_id=$1
    local timeout=10
    
    log_info "Watching logs for retry trigger (timeout: ${timeout}s)..."
    
    # Get pod name
    local pod_name
    pod_name=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=sbomer-sbom-service -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$pod_name" ]; then
        log_warning "Could not find sbom-service pod to watch logs"
        return 1
    fi
    
    # Watch logs with timeout
    local found=false
    local end_time=$((SECONDS + timeout))
    
    while [ $SECONDS -lt $end_time ]; do
        if kubectl logs "$pod_name" -n "$NAMESPACE" --tail=50 2>/dev/null | \
           grep -q "Triggering immediate retry for generation $generation_id"; then
            found=true
            break
        fi
        sleep 1
    done
    
    if [ "$found" = true ]; then
        log_success "Retry trigger found in logs"
        # Show the actual log line
        kubectl logs "$pod_name" -n "$NAMESPACE" --tail=100 2>/dev/null | \
            grep "Triggering immediate retry for generation $generation_id" | tail -1
        return 0
    else
        log_warning "Retry trigger not found in logs within timeout"
        return 1
    fi
}

# Main test flow
main() {
    echo ""
    echo "=========================================="
    echo "  Automatic Retry Test for sbom-service"
    echo "=========================================="
    echo ""
    
    # Step 1: Prerequisites
    check_prerequisites
    echo ""
    
    # Step 2: Get Kafka pod
    get_kafka_pod
    echo ""
    
    # Step 3: Trigger generation
    trigger_generation
    echo ""
    
    # Step 4: Wait for initial run to be created
    log_info "Waiting for initial run to be created..."
    sleep 3
    
    initial_runs=$(get_run_count "$GEN_ID")
    log_info "Initial run count: $initial_runs"
    echo ""
    
    # Step 5: Send failure message
    log_info "Simulating failure with error code $ERROR_CODE..."
    send_kafka_message "$GEN_ID" "FAILED" "$ERROR_CODE" "$ERROR_REASON" ""
    echo ""
    
    # Step 6: Wait and check for retry
    log_info "Waiting for automatic retry to trigger..."
    sleep 5
    
    # Step 7: Watch logs
    watch_logs_for_retry "$GEN_ID"
    echo ""
    
    # Step 8: Verify retry run was created
    log_info "Verifying retry run was created..."
    retry_runs=$(get_run_count "$GEN_ID")
    
    if [ "$retry_runs" -gt "$initial_runs" ]; then
        log_success "Retry run created! Run count: $initial_runs → $retry_runs"
        echo ""
        log_info "Run details:"
        get_runs_details "$GEN_ID"
    else
        log_error "Retry run NOT created. Run count: $retry_runs"
        echo ""
        log_info "Current runs:"
        get_runs_details "$GEN_ID"
        echo ""
        log_error "TEST FAILED: Automatic retry did not trigger"
        exit 1
    fi
    echo ""
    
    # Step 9: Send success for retry
    log_info "Simulating success for retry attempt..."
    local sbom='<cyclonedx xmlns=\"http://cyclonedx.org/schema/bom/1.4\"><metadata><component type=\"container\"><name>test</name></component></metadata></cyclonedx>'
    send_kafka_message "$GEN_ID" "COMPLETED" "0" "" "$sbom"
    echo ""
    
    # Step 10: Wait and verify final state
    log_info "Waiting for generation to complete..."
    sleep 5
    
    final_status=$(get_generation_status "$GEN_ID")
    final_runs=$(get_run_count "$GEN_ID")
    
    echo ""
    log_info "Final Results:"
    echo "  Generation ID: $GEN_ID"
    echo "  Final Status: $final_status"
    echo "  Total Runs: $final_runs"
    echo ""
    log_info "Run details:"
    get_runs_details "$GEN_ID"
    echo ""
    
    # Step 11: Verify success criteria
    if [ "$final_status" = "COMPLETED" ] && [ "$final_runs" -eq 2 ]; then
        echo ""
        echo "=========================================="
        log_success "TEST PASSED ✓"
        echo "=========================================="
        echo ""
        echo "Summary:"
        echo "  ✓ Generation failed with ERR_SYSTEM"
        echo "  ✓ Automatic retry was triggered"
        echo "  ✓ Retry run was created (attemptNumber=2)"
        echo "  ✓ Retry succeeded"
        echo "  ✓ Final status is COMPLETED"
        echo "  ✓ Total runs: 2"
        echo ""
        exit 0
    else
        echo ""
        echo "=========================================="
        log_error "TEST FAILED ✗"
        echo "=========================================="
        echo ""
        echo "Expected:"
        echo "  - Final Status: COMPLETED"
        echo "  - Total Runs: 2"
        echo ""
        echo "Actual:"
        echo "  - Final Status: $final_status"
        echo "  - Total Runs: $final_runs"
        echo ""
        exit 1
    fi
}

# Run main function
main
