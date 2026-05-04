-- Remove legacy 'result' and 'reason' columns from parent generation and enhancement tables
-- (no longer needed as error details are tracked in run entities with canonical codes)
ALTER TABLE generations
    DROP COLUMN IF EXISTS result,
    DROP COLUMN IF EXISTS reason;

ALTER TABLE enhancements
    DROP COLUMN IF EXISTS result,
    DROP COLUMN IF EXISTS reason;

-- Replace legacy 'reason' column with canonical error fields in generation_runs table
ALTER TABLE generation_runs
    DROP COLUMN IF EXISTS reason,
    ADD COLUMN error_result VARCHAR(100),
    ADD COLUMN upstream_reason VARCHAR(500);

-- Replace legacy 'reason' column with canonical error fields in enhancement_runs table
ALTER TABLE enhancement_runs
    DROP COLUMN IF EXISTS reason,
    ADD COLUMN error_result VARCHAR(100),
    ADD COLUMN upstream_reason VARCHAR(500);

-- Add comments for documentation
COMMENT ON COLUMN generation_runs.error_result IS 'Canonical service-owned error code (e.g., GENERATOR_EXECUTION_FAILED)';
COMMENT ON COLUMN generation_runs.upstream_reason IS 'Raw upstream reason from external worker (e.g., TaskRunFailed)';
COMMENT ON COLUMN enhancement_runs.error_result IS 'Canonical service-owned error code (e.g., ENHANCER_EXECUTION_FAILED)';
COMMENT ON COLUMN enhancement_runs.upstream_reason IS 'Raw upstream reason from external worker (e.g., TaskRunFailed)';
