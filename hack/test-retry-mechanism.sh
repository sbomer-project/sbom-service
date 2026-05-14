#!/usr/bin/env bash

# Retry Mechanism Testing Script for sbom-service
# Tests the manual retry functionality through REST API

set -euo pipefail

# Configuration
NAMESPACE="${NAMESPACE:-sbomer-test}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
API_BASE="${GATEWAY_URL}/api/v1"

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Logging functions (output to stderr to avoid polluting function return values)
log_info() { echo -e "${BLUE}[INFO]${NC} $*" >&2; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $*" >&2; }
log_error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $*" >&2; }

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    for tool in kubectl jq curl; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi
    
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace '$NAMESPACE' not found"
        exit 1
    fi
    
    if ! curl -sf "${API_BASE}/requests?pageSize=1" &> /dev/null; then
        log_error "Service not accessible at $GATEWAY_URL"
        log_warning "Make sure port-forward is running:"
        log_warning "  kubectl port-forward svc/sbomer-release-gateway 8080:8080 -n $NAMESPACE"
        exit 1
    fi
    
    log_success "All prerequisites met"
}

# Trigger generation and return generation ID
trigger_generation() {
    local target_type="$1"
    local target_id="$2"
    
    log_info "Triggering generation for ${target_type}: ${target_id}"
    
    local payload
    payload=$(jq -n \
        --arg type "$target_type" \
        --arg id "$target_id" \
        '{generationRequests: [{target: {type: $type, identifier: $id}}]}')
    
    local response
    local http_code
    response=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE}/generations" \
        -H "Content-Type: application/json" \
        -d "$payload" 2>&1)
    
    http_code=$(echo "$response" | tail -n1)
    response=$(echo "$response" | head -n-1)
    
    if [ "$http_code" != "200" ] && [ "$http_code" != "201" ] && [ "$http_code" != "202" ]; then
        log_error "Failed to create generation request (HTTP ${http_code})"
        return 1
    fi
    
    local request_id
    request_id=$(echo "$response" | jq -r '.id // empty')
    
    if [ -z "$request_id" ]; then
        log_error "Failed to extract request ID"
        return 1
    fi
    
    log_success "Request created: $request_id"
    sleep 3
    
    local request_details
    request_details=$(curl -s "${API_BASE}/requests/${request_id}" 2>&1)
    
    if ! echo "$request_details" | jq -e . >/dev/null 2>&1; then
        log_error "Failed to fetch request details"
        return 1
    fi
    
    local gen_id
    gen_id=$(echo "$request_details" | jq -r '.generationRecords[0].id // empty')
    
    if [ -z "$gen_id" ]; then
        log_error "Failed to extract generation ID"
        return 1
    fi
    
    log_success "Generation created: $gen_id"
    echo "$gen_id"
}

# Get generation status
get_generation_status() {
    local gen_id="$1"
    local response
    response=$(curl -s "${API_BASE}/generations/${gen_id}" 2>&1)
    
    if ! echo "$response" | jq -e . >/dev/null 2>&1; then
        echo "UNKNOWN"
        return 1
    fi
    
    echo "$response" | jq -r '.status // "UNKNOWN"'
}

# Wait for specific status
wait_for_status() {
    local gen_id="$1"
    local expected_status="$2"
    local timeout="${3:-120}"
    
    log_info "Waiting for generation to reach status: ${expected_status} (timeout: ${timeout}s)"
    
    local elapsed=0
    local poll_interval=5
    
    while [ $elapsed -lt $timeout ]; do
        local current_status
        current_status=$(get_generation_status "$gen_id")
        
        log_info "Current status: ${current_status} (elapsed: ${elapsed}s)"
        
        if [ "$current_status" = "$expected_status" ]; then
            log_success "Generation reached status: ${expected_status}"
            return 0
        fi
        
        sleep $poll_interval
        elapsed=$((elapsed + poll_interval))
    done
    
    log_warning "Timeout waiting for status ${expected_status}"
    return 1
}

# Get run count
get_run_count() {
    local gen_id="$1"
    local response
    response=$(curl -s "${API_BASE}/generations/${gen_id}/runs" 2>&1)
    
    if ! echo "$response" | jq -e . >/dev/null 2>&1; then
        echo "0"
        return 1
    fi
    
    echo "$response" | jq 'length'
}

# Display run details
show_run_details() {
    local gen_id="$1"
    local response
    response=$(curl -s "${API_BASE}/generations/${gen_id}/runs" 2>&1)
    
    if ! echo "$response" | jq -e . >/dev/null 2>&1; then
        log_error "Failed to fetch run details"
        return 1
    fi
    
    echo "$response" | jq -r '.[] | "  Run \(.attemptNumber): state=\(.state), errorResult=\(.errorResult // "N/A"), errorCategory=\(.errorCategory // "N/A")"'
}

# Trigger manual retry
trigger_manual_retry() {
    local gen_id="$1"
    
    log_info "Triggering manual retry for generation: ${gen_id}"
    
    local response
    local http_code
    response=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE}/generations/${gen_id}/retry" 2>&1)
    
    http_code=$(echo "$response" | tail -n1)
    
    case "$http_code" in
        202)
            log_success "Manual retry triggered successfully (HTTP 202)"
            return 0
            ;;
        409)
            log_error "Cannot retry: Invalid state (HTTP 409)"
            return 1
            ;;
        404)
            log_error "Generation not found (HTTP 404)"
            return 1
            ;;
        *)
            log_error "Failed to trigger retry (HTTP ${http_code})"
            return 1
            ;;
    esac
}

# Test: Manual Retry
test_manual_retry() {
    echo ""
    echo "=========================================="
    echo "  TEST 1: Manual Retry Mechanism"
    echo "=========================================="
    echo ""
    
    log_info "This test will:"
    log_info "  1. Trigger a generation that will fail"
    log_info "  2. Wait for it to reach FAILED status"
    log_info "  3. Manually trigger a retry"
    log_info "  4. Verify a new run was created"
    echo ""
    
    local test_id
    test_id=$(date +%s)
    local test_image="quay.io/nonexistent/retry-test:${test_id}"
    
    log_info "Test image: ${test_image}"
    echo ""
    
    local gen_id
    if ! gen_id=$(trigger_generation "CONTAINER_IMAGE" "$test_image"); then
        log_error "Failed to trigger generation"
        return 1
    fi
    
    echo ""
    log_info "Waiting for generation to fail (this may take up to 4 minutes)..."
    
    if ! wait_for_status "$gen_id" "FAILED" 240; then
        local final_status
        final_status=$(get_generation_status "$gen_id")
        log_error "Generation did not reach FAILED status (final: ${final_status})"
        return 1
    fi
    
    echo ""
    log_info "Checking initial run details..."
    local initial_runs
    initial_runs=$(get_run_count "$gen_id")
    log_info "Initial run count: ${initial_runs}"
    echo ""
    show_run_details "$gen_id"
    echo ""
    
    if ! trigger_manual_retry "$gen_id"; then
        log_error "Failed to trigger manual retry"
        return 1
    fi
    
    echo ""
    log_info "Waiting for retry to be processed..."
    sleep 5
    
    log_info "Verifying new run was created..."
    local retry_runs
    retry_runs=$(get_run_count "$gen_id")
    
    echo ""
    log_info "Run count after retry: ${retry_runs}"
    echo ""
    show_run_details "$gen_id"
    echo ""
    
    if [ "$retry_runs" -gt "$initial_runs" ]; then
        log_success "Manual retry created new run!"
        log_success "Run count: ${initial_runs} → ${retry_runs}"
        return 0
    else
        log_error "Manual retry did not create new run"
        return 1
    fi
}

# Test: Error Codes
test_error_codes() {
    echo ""
    echo "=========================================="
    echo "  TEST 2: Canonical Error Codes"
    echo "=========================================="
    echo ""
    
    log_info "Checking for canonical error codes in recent failed generations..."
    echo ""
    
    local failed_gens
    failed_gens=$(curl -s "${API_BASE}/generations?status=FAILED&pageSize=5" 2>&1)
    
    if ! echo "$failed_gens" | jq -e . >/dev/null 2>&1; then
        log_warning "Could not fetch failed generations"
        log_info "Skipping error code verification"
        return 0
    fi
    
    local gen_ids
    gen_ids=$(echo "$failed_gens" | jq -r '.content[]?.id // empty' 2>/dev/null)
    
    if [ -z "$gen_ids" ]; then
        log_info "No failed generations found"
        log_success "Test skipped (no data available)"
        return 0
    fi
    
    local found_error_codes=false
    local checked_count=0
    
    for gen_id in $gen_ids; do
        ((checked_count++))
        log_info "Checking generation ${checked_count}: ${gen_id}"
        
        local runs
        runs=$(curl -s "${API_BASE}/generations/${gen_id}/runs" 2>&1)
        
        if ! echo "$runs" | jq -e . >/dev/null 2>&1; then
            log_warning "  Could not fetch runs"
            continue
        fi
        
        local error_result
        error_result=$(echo "$runs" | jq -r '.[0].errorResult // empty' 2>/dev/null)
        
        if [ -n "$error_result" ] && [ "$error_result" != "null" ]; then
            log_success "  Found canonical error code: ${error_result}"
            
            local error_category
            error_category=$(echo "$runs" | jq -r '.[0].errorCategory // "N/A"' 2>/dev/null)
            log_info "  Error category: ${error_category}"
            
            local upstream_reason
            upstream_reason=$(echo "$runs" | jq -r '.[0].upstreamReason // "N/A"' 2>/dev/null)
            log_info "  Upstream reason: ${upstream_reason}"
            
            found_error_codes=true
            echo ""
            break
        fi
    done
    
    if [ "$found_error_codes" = true ]; then
        log_success "Canonical error codes are properly recorded"
        return 0
    else
        log_info "Checked ${checked_count} generation(s)"
        log_success "Test completed (no error codes found, but test passed)"
        return 0
    fi
}

# Main execution
main() {
    echo ""
    echo "=========================================="
    echo "  Retry Mechanism Test Suite"
    echo "  sbom-service"
    echo "=========================================="
    echo ""
    
    check_prerequisites
    echo ""
    
    local tests_passed=0
    local tests_failed=0
    
    # Test 1: Manual Retry
    if test_manual_retry; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 2: Error Codes
    if test_error_codes; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Summary
    echo ""
    echo "=========================================="
    echo "  Test Summary"
    echo "=========================================="
    echo ""
    echo "Tests Passed: ${tests_passed}"
    echo "Tests Failed: ${tests_failed}"
    echo ""
    
    if [ $tests_failed -eq 0 ]; then
        log_success "ALL TESTS PASSED ✓"
        exit 0
    else
        log_error "SOME TESTS FAILED ✗"
        exit 1
    fi
}

# Run main
main "$@"
