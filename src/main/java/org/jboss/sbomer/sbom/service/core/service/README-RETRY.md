# Retry Mechanism Architecture

## Overview

The retry mechanism provides automatic and manual retry capabilities for failed SBOM generations and enhancements. It uses a dual-code error system with canonical error codes for stable, service-owned retry logic.

## Error Code System

### Dual-Code Architecture

The system uses two types of error codes:

#### 1. Legacy Codes (GenerationResult, EnhancementResult)
- **Source**: Defined by external workers (generators, enhancers)
- **Received**: Via Kafka events from external systems
- **Purpose**: Backward compatibility and external system integration
- **Examples**: `ERR_OOM`, `ERR_SYSTEM`, `ERR_ENHANCEMENT`, `ERR_CONFIG_INVALID`
- **Stability**: May change when external workers are updated

#### 2. Canonical Codes (ErrorResult)
- **Source**: Service-owned, defined in `ErrorResult` enum
- **Purpose**: Internal logic, retry decisions, stable classification
- **Metadata**: Includes category, retryability, ownership, severity
- **Examples**: `EXTERNAL_RESOURCE_EXHAUSTED`, `GENERATOR_EXECUTION_FAILED`, `INVALID_REQUEST`
- **Stability**: Stable across external worker changes

### Error Flow

```
1. External worker fails with legacy code (e.g., ERR_OOM)
   ↓
2. Service receives failure via Kafka event
   ↓
3. ErrorMapper translates to canonical code (EXTERNAL_RESOURCE_EXHAUSTED)
   ↓
4. Retry decision based on canonical code's isRetryable() property
   ↓
5. Both codes stored in database for diagnostics and traceability
```

### Error Mapping Examples

| Legacy Code | Canonical Code | Retryable | Rationale |
|------------|----------------|-----------|-----------|
| `ERR_OOM` | `EXTERNAL_RESOURCE_EXHAUSTED` | ✅ Yes | Transient resource issue |
| `ERR_SYSTEM` | `EXTERNAL_SYSTEM_ERROR` | ✅ Yes | Infrastructure failure |
| `ERR_CONFIG_INVALID` | `EXTERNAL_BAD_CONFIGURATION` | ❌ No | Permanent config error |
| `ERR_INDEX_INVALID` | `INVALID_TARGET` | ❌ No | Bad input data |
| `ERR_GENERAL` | `GENERATOR_EXECUTION_FAILED` | ✅ Yes | Generic execution failure |

## Retry Decision Logic

### Automatic Retry

Triggered immediately when a failure occurs if ALL conditions are met:

1. **Global retry enabled**: `sbomer.retry.enabled=true`
2. **Error is retryable**: `ErrorResult.isRetryable() == true`
3. **Attempts not exhausted**: `current_attempts < max_attempts_for_error_type`

**Flow diagram:**
```
Failure event received
        ↓
Map legacy code to canonical ErrorResult
        ↓
Retry enabled globally?
   ├─ No  → stop
   └─ Yes
        ↓
Canonical error present and retryable?
   ├─ No  → stop
   └─ Yes
        ↓
Attempts < configured max?
   ├─ No  → stop
   └─ Yes
        ↓
Create retry run + mark entity PENDING_RETRY
        ↓
Schedule retry event to Kafka
```

**Process details:**
```
1. Failure event received (e.g., generation.update with status=FAILED)
   ↓
2. Map legacy code → canonical code via ErrorMapper
   ↓
3. Check if canonical code is retryable
   ↓
4. Count existing run attempts for this generation/enhancement
   ↓
5. If attempts < max_attempts: trigger retry
   ↓
6. Create new run record, update status to PENDING_RETRY
   ↓
7. Schedule new generation/enhancement event to Kafka
```

### Manual Retry

Available via Admin API regardless of automatic retry status:

**Endpoints:**
- `POST /api/v1/generations/{id}/retry` - Retry a failed generation
- `POST /api/v1/enhancements/{id}/retry` - Retry a failed enhancement

**Validation:**
- Entity must be in `FAILED` state
- Cannot retry if already `PENDING_RETRY`
- Creates new run record and schedules retry event

**Use Cases:**
- Override automatic retry limits
- Retry after fixing external issues
- Manual intervention for edge cases

## Configuration

### Canonical Code Configuration (Recommended)

```properties
# Global toggle
sbomer.retry.enabled=true

# Per-error-type max attempts
sbomer.retry.error.external-resource-exhausted.max-attempts=3
sbomer.retry.error.external-system-error.max-attempts=5
sbomer.retry.error.external-timeout.max-attempts=3
sbomer.retry.error.generator-execution-failed.max-attempts=3
sbomer.retry.error.enhancer-execution-failed.max-attempts=3
sbomer.retry.error.database-error.max-attempts=3
sbomer.retry.error.dependency-unavailable.max-attempts=5
```

### Retryable Error Types

| Canonical Code | Max Attempts (Default) | Use Case |
|---------------|------------------------|----------|
| `EXTERNAL_RESOURCE_EXHAUSTED` | 3 | OOM, disk space exhausted |
| `EXTERNAL_SYSTEM_ERROR` | 5 | Infrastructure failures |
| `EXTERNAL_TIMEOUT` | 3 | Worker timeout |
| `GENERATOR_EXECUTION_FAILED` | 3 | Generator execution errors |
| `ENHANCER_EXECUTION_FAILED` | 3 | Enhancer execution errors |
| `DATABASE_ERROR` | 3 | Transient DB issues |
| `DEPENDENCY_UNAVAILABLE` | 5 | External service unavailable |
| `SCHEMA_REGISTRY_ERROR` | 3 | Apicurio Registry issues |
| `GENERATION_SCHEDULING_ERROR` | 3 | Retry scheduling of generation dispatch failures |
| `ENHANCEMENT_SCHEDULING_ERROR` | 3 | Retry scheduling of enhancement dispatch failures |
| `RETRY_EXECUTION_ERROR` | 2 | Failure while performing retry orchestration |
| `TRANSACTION_ERROR` | 3 | Transient transaction rollback or commit failures |

### Non-Retryable Error Types

| Canonical Code | Reason |
|---------------|--------|
| `INVALID_REQUEST` | Bad input from client |
| `INVALID_TARGET` | Malformed target identifier |
| `INVALID_RECIPE` | Invalid recipe configuration |
| `CONFIG_MISSING` | Missing required configuration |
| `CONFIG_INVALID` | Invalid service configuration |
| `EXTERNAL_BAD_CONFIGURATION` | Worker configuration error |
| `ENTITY_NOT_FOUND` | Referenced entity doesn't exist |
| `INVALID_STATE_TRANSITION` | Invalid state change attempt |

## State Transitions

### Generation States

```
Initial Request
    ↓
PENDING ──────────→ GENERATING ──────→ COMPLETED ✓
                        │
                        ↓ (failure)
                    FAILED
                        │
                        ↓ (retry triggered)
                PENDING_RETRY ──────→ GENERATING (retry attempt)
```

### Enhancement States

```
Previous Enhancement Complete
    ↓
PENDING ──────────→ ENHANCING ──────→ COMPLETED ✓
                        │
                        ↓ (failure)
                    FAILED
                        │
                        ↓ (retry triggered)
                PENDING_RETRY ──────→ ENHANCING (retry attempt)
```

### Run Records

Each generation/enhancement has multiple run records tracking individual attempts:

```
Generation/Enhancement
    │
    ├── Run #1 (attempt_number=1, state=FAILED)
    ├── Run #2 (attempt_number=2, state=FAILED)
    └── Run #3 (attempt_number=3, state=COMPLETED)
```

## Transparency

Retries are **transparent to external workers**:

- Workers receive normal `generation.created` or `enhancement.created` events
- No indication that this is a retry attempt
- `PENDING_RETRY` status is internal only (not visible to workers)
- Workers process retries identically to initial attempts

**Why?** This design keeps workers simple and stateless. They don't need retry logic or awareness of previous attempts.

## Key Components

### AutomaticRetryService

**Responsibility**: Evaluates failures and triggers immediate retries

**Methods:**
- `tryRetryGeneration(generationId, failureResult)` - Evaluate and retry generation
- `tryRetryEnhancement(enhancementId, failureResult)` - Evaluate and retry enhancement

**Logic:**
1. Check global retry enabled
2. Map legacy code → canonical code
3. Check if canonical code is retryable
4. Count existing attempts
5. If eligible, create new run and schedule retry event

### RetryPolicyConfig

**Responsibility**: Centralized retry policy configuration

**Configuration:**
- `Map<ErrorResult, Integer>` - Max attempts per canonical error code
- Loaded from `application.properties` on startup
- Provides `getMaxAttemptsForError(ErrorResult)` method

### ErrorMapper

**Responsibility**: Translates legacy codes to canonical codes

**Methods:**
- `fromGenerationResult(GenerationResult)` → `Optional<ErrorResult>`
- `fromEnhancementResult(EnhancementResult)` → `Optional<ErrorResult>`
- `fromException(Exception)` → `Optional<ErrorResult>`

**Returns:** `Optional.empty()` for success cases, `Optional.of(ErrorResult)` for errors

### RunManagementService

**Responsibility**: Manages run records and state transitions

**Methods:**
- `retryGeneration(generationId)` - Create new run, update to PENDING_RETRY
- `retryEnhancement(enhancementId)` - Create new run, update to PENDING_RETRY
- `completeGenerationRun(runId, result, reason)` - Mark run as completed
- `completeEnhancementRun(runId, result, reason)` - Mark run as completed

## Monitoring and Observability

### Logging

All retry operations are logged with structured context:

```
INFO  Triggering immediate retry for generation {id}: attempt {n}/{max}, error={canonical_code}
INFO  Successfully triggered retry for generation {id}
ERROR Failed to trigger retry for generation {id}: {reason}
INFO  Max retry attempts reached for generation {id}: {attempts} >= {max}, error={canonical_code}
DEBUG Retry disabled globally, skipping retry for generation {id}
DEBUG Error {legacy_code} (canonical: {canonical_code}) is not retryable, skipping retry
```

### Database Tracking

**Run Records Table:**
- `attempt_number` - Sequential attempt counter (1, 2, 3, ...)
- `state` - Current run state (PENDING, RUNNING, COMPLETED, FAILED)
- `result` - Final result code (SUCCESS, ERR_OOM, etc.)
- `canonical_error` - Mapped canonical error code
- `start_time` - When run started
- `completion_time` - When run finished
- `reason` - Human-readable failure reason

**Benefits:**
- Full audit trail of all attempts
- Ability to analyze failure patterns
- Support for debugging and troubleshooting

## Best Practices

### For Service Developers

1. **Always use canonical codes for logic**: Never make decisions based on legacy codes
2. **Map early**: Convert legacy codes to canonical codes as soon as they're received
3. **Store both codes**: Keep legacy codes for diagnostics, use canonical for logic
4. **Test retryability**: Verify error classification matches expected retry behavior
5. **Monitor retry rates**: Track which errors trigger retries most frequently

### For Configuration

1. **Start conservative**: Begin with lower max attempts, increase if needed
2. **Different limits for different errors**: Transient errors can have higher limits
3. **Monitor exhaustion**: Track how often max attempts are reached
4. **Adjust based on data**: Use metrics to tune retry policies

### For External Worker Developers

1. **Use specific error codes**: Provide detailed legacy codes for better classification
2. **Include error context**: Add reason strings to help with debugging
3. **Be idempotent**: Workers should handle retries gracefully
4. **No retry awareness needed**: Workers don't need to know about retries

## Troubleshooting

### Retry Not Triggered

**Check:**
1. Is `sbomer.retry.enabled=true`?
2. Is the canonical error code retryable? (`ErrorResult.isRetryable()`)
3. Have max attempts been reached? Check run records count
4. Is the entity in `FAILED` state?

**Debug:**
```bash
# Check retry configuration
grep "sbomer.retry" application.properties

# Check run records for entity
curl http://localhost:8080/api/v1/generations/{id}/runs

# Check logs for retry decision
grep "Triggering immediate retry" logs/application.log
```

### Retry Exhausted

**Symptoms:** Max attempts reached, entity still in `FAILED` state

**Resolution:**
1. Investigate root cause of failures
2. Fix underlying issue (infrastructure, configuration, etc.)
3. Use manual retry API to override limits
4. Consider increasing max attempts for this error type

### Wrong Error Classification

**Symptoms:** Error is retryable but shouldn't be (or vice versa)

**Resolution:**
1. Check `ErrorMapper` mapping for the legacy code
2. Verify canonical code's `isRetryable()` property
3. Update mapping if incorrect
4. Consider adding new canonical code if needed

## Future Enhancements

### Exponential Backoff

Currently, retries are immediate. Future enhancement could add delays:

```
Attempt 1: Immediate
Attempt 2: 2 seconds delay
Attempt 3: 4 seconds delay
Attempt 4: 8 seconds delay
```

### Category-Based Strategies

Leverage `ErrorResult.getCategory()` for different retry strategies:

- `EXTERNAL_EXECUTION`: Exponential backoff
- `INTERNAL`: Immediate retry
- `VALIDATION`: No retry (already implemented)

### Ownership-Based Alerting

Use `ErrorResult.getOwnership()` for targeted alerts:

- `CLIENT`: Log warning only
- `SERVICE`: Alert on-call engineer
- `EXTERNAL_SYSTEM`: Escalate to infrastructure team

### Retry Metrics

Add Prometheus metrics:

```
retry_attempts_total{error_type="EXTERNAL_SYSTEM_ERROR"} 42
retry_success_total{error_type="EXTERNAL_SYSTEM_ERROR"} 38
retry_exhausted_total{error_type="EXTERNAL_SYSTEM_ERROR"} 4
retry_duration_seconds{error_type="EXTERNAL_SYSTEM_ERROR"} histogram
```

## References

- **ErrorResult Enum**: `core/domain/enums/ErrorResult.java`
- **ErrorMapper**: `core/utility/ErrorMapper.java`
- **AutomaticRetryService**: `core/service/AutomaticRetryService.java`
- **RetryPolicyConfig**: `core/config/RetryPolicyConfig.java`
- **RunManagementService**: `core/service/RunManagementService.java`
