-- Add PENDING_RETRY status to Generation and Enhancement enums
-- This allows tracking retry operations separately from initial attempts
-- PENDING_RETRY is internal only and maps to PENDING for external systems

-- Update generations table constraint
ALTER TABLE generations 
  DROP CONSTRAINT IF EXISTS generations_status_check;
  
ALTER TABLE generations 
  ADD CONSTRAINT generations_status_check 
  CHECK (status IN ('PENDING', 'PENDING_RETRY', 'GENERATING', 'COMPLETED', 'FAILED'));

-- Update enhancements table constraint
ALTER TABLE enhancements 
  DROP CONSTRAINT IF EXISTS enhancements_status_check;
  
ALTER TABLE enhancements 
  ADD CONSTRAINT enhancements_status_check 
  CHECK (status IN ('PENDING', 'PENDING_RETRY', 'ENHANCING', 'COMPLETED', 'FAILED'));

-- Add indexes for efficient retry queries
-- Note: Using simple indexes instead of partial indexes for H2 compatibility
-- PostgreSQL will still benefit from these indexes for retry status queries
CREATE INDEX idx_generations_retry_status ON generations(status);
CREATE INDEX idx_enhancements_retry_status ON enhancements(status);

-- Add comments for documentation (PostgreSQL only, ignored by H2)
COMMENT ON COLUMN generations.status IS 'Generation lifecycle status. PENDING_RETRY indicates a retry is queued.';
COMMENT ON COLUMN enhancements.status IS 'Enhancement lifecycle status. PENDING_RETRY indicates a retry is queued.';
