-- ============================================================================
-- SAMPLE DATA FOR LOCAL DEVELOPMENT
-- ============================================================================
-- This file populates the database with sample data for testing the SBOM service
-- including requests, generations, enhancements, and their execution runs.
-- ============================================================================

-- REQUESTS
-- Various request states to test different scenarios
INSERT INTO requests (request_id, status, creationDate, child_generations_status) VALUES
  ('dev-req-124', 'FINISHED',    '2020-02-01 09:00:00+00', 'ALL_FINISHED'),
  ('dev-req-125', 'RECEIVED',    '2020-03-05 08:30:00+00', 'NONE'),
  ('dev-req-126', 'FINISHED',    '2020-04-10 11:00:00+00', 'ALL_FINISHED'),
  ('dev-req-127', 'IN_PROGRESS', '2020-05-01 08:00:00+00', 'SOME_IN_PROGRESS'),
  ('dev-req-128', 'FAILED',      '2020-06-12 13:15:00+00', 'SOME_FAILED'),
  ('dev-req-129', 'FINISHED',    '2020-07-20 07:45:00+00', 'ALL_FINISHED'),
  ('dev-req-130', 'IN_PROGRESS', '2020-08-03 10:00:00+00', 'SOME_IN_PROGRESS');


-- PUBLISHERS
-- Sample publishers with various configurations
INSERT INTO request_publishers (request_db_id, name, version) VALUES
  ((SELECT db_id FROM requests WHERE request_id = 'dev-req-124'), 'maven-publisher', '1.0.0'),
  ((SELECT db_id FROM requests WHERE request_id = 'dev-req-126'), 'npm-publisher', '2.1.0'),
  ((SELECT db_id FROM requests WHERE request_id = 'dev-req-129'), 'maven-publisher', '1.0.0');

INSERT INTO publisher_options (publisher_db_id, opt_key, opt_value) VALUES
  ((SELECT db_id FROM request_publishers WHERE request_db_id = (SELECT db_id FROM requests WHERE request_id = 'dev-req-124')), 'repository', 'https://repo.maven.apache.org/maven2'),
  ((SELECT db_id FROM request_publishers WHERE request_db_id = (SELECT db_id FROM requests WHERE request_id = 'dev-req-124')), 'format', 'cyclonedx'),
  ((SELECT db_id FROM request_publishers WHERE request_db_id = (SELECT db_id FROM requests WHERE request_id = 'dev-req-126')), 'registry', 'https://registry.npmjs.org'),
  ((SELECT db_id FROM request_publishers WHERE request_db_id = (SELECT db_id FROM requests WHERE request_id = 'dev-req-129')), 'repository', 'https://repo.maven.apache.org/maven2');


-- GENERATIONS
-- Various generation states including successful, failed, and in-progress
INSERT INTO generations (generation_id, request_db_id, status, result, generatorName, generatorVersion, targetIdentifier, targetType, created, updated, finished, child_enhancements_status, latest_result) VALUES
  ('dev-gen-001', (SELECT db_id FROM requests WHERE request_id = 'dev-req-124'), 'FINISHED', 0, 'syft-generator', '1.0.0', 'org.example:app:1.0.0', 'maven', '2020-02-01 09:05:00+00', '2020-02-01 09:15:00+00', '2020-02-01 09:15:00+00', 'ALL_FINISHED', 'SUCCESS'),
  ('dev-gen-002', (SELECT db_id FROM requests WHERE request_id = 'dev-req-126'), 'FINISHED', 0, 'syft-generator', '1.0.0', 'example-app:2.0.0', 'npm', '2020-04-10 11:05:00+00', '2020-04-10 11:20:00+00', '2020-04-10 11:20:00+00', 'ALL_FINISHED', 'SUCCESS'),
  ('dev-gen-003', (SELECT db_id FROM requests WHERE request_id = 'dev-req-127'), 'IN_PROGRESS', NULL, 'syft-generator', '1.0.0', 'org.example:service:3.0.0', 'maven', '2020-05-01 08:05:00+00', '2020-05-01 08:10:00+00', NULL, 'SOME_IN_PROGRESS', NULL),
  ('dev-gen-004', (SELECT db_id FROM requests WHERE request_id = 'dev-req-128'), 'FAILED', 5, 'syft-generator', '1.0.0', 'org.example:broken:1.0.0', 'maven', '2020-06-12 13:20:00+00', '2020-06-12 13:25:00+00', '2020-06-12 13:25:00+00', 'NONE', 'ERR_GENERATION'),
  ('dev-gen-005', (SELECT db_id FROM requests WHERE request_id = 'dev-req-129'), 'FINISHED', 0, 'syft-generator', '1.1.0', 'org.example:lib:2.5.0', 'maven', '2020-07-20 07:50:00+00', '2020-07-20 08:05:00+00', '2020-07-20 08:05:00+00', 'ALL_FINISHED', 'SUCCESS'),
  ('dev-gen-006', (SELECT db_id FROM requests WHERE request_id = 'dev-req-130'), 'INITIALIZING', NULL, 'syft-generator', '1.1.0', 'org.example:new-app:1.0.0', 'maven', '2020-08-03 10:05:00+00', '2020-08-03 10:05:00+00', NULL, 'NONE', NULL);

INSERT INTO generation_options (generation_db_id, opt_key, opt_value) VALUES
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-001'), 'buildType', 'maven'),
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-001'), 'includeTransitive', 'true'),
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-002'), 'buildType', 'npm'),
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-002'), 'includeDevDependencies', 'false');

INSERT INTO generation_sbom_urls (generation_db_id, url) VALUES
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-001'), 'https://sbom-storage.example.com/sboms/dev-gen-001.json'),
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-002'), 'https://sbom-storage.example.com/sboms/dev-gen-002.json'),
  ((SELECT db_id FROM generations WHERE generation_id = 'dev-gen-005'), 'https://sbom-storage.example.com/sboms/dev-gen-005.json');


-- GENERATION RUNS
-- Execution history showing retries and different outcomes
INSERT INTO generation_runs (run_id, generation_db_id, attempt_number, state, reason, message, start_time, completion_time) VALUES
  -- Successful generation on first attempt
  ('dev-gen-run-001', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-001'), 1, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully', '2020-02-01 09:05:00+00', '2020-02-01 09:15:00+00'),
  
  -- Successful generation on first attempt
  ('dev-gen-run-002', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-002'), 1, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully', '2020-04-10 11:05:00+00', '2020-04-10 11:20:00+00'),
  
  -- Currently running generation
  ('dev-gen-run-003', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-003'), 1, 'RUNNING', NULL, 'Processing dependencies...', '2020-05-01 08:05:00+00', NULL),
  
  -- Failed generation with retry history
  ('dev-gen-run-004a', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-004'), 1, 'FAILED', 'ERR_GENERATION', 'Failed to resolve dependencies', '2020-06-12 13:20:00+00', '2020-06-12 13:22:00+00'),
  ('dev-gen-run-004b', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-004'), 2, 'FAILED', 'ERR_GENERATION', 'Failed to resolve dependencies (retry)', '2020-06-12 13:23:00+00', '2020-06-12 13:25:00+00'),
  
  -- Successful generation after one retry
  ('dev-gen-run-005a', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-005'), 1, 'FAILED', 'ERR_SYSTEM', 'Temporary network issue', '2020-07-20 07:50:00+00', '2020-07-20 07:52:00+00'),
  ('dev-gen-run-005b', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-005'), 2, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully on retry', '2020-07-20 07:55:00+00', '2020-07-20 08:05:00+00'),
  
  -- Pending generation
  ('dev-gen-run-006', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-006'), 1, 'PENDING', NULL, 'Queued for processing', NULL, NULL);


-- ENHANCEMENTS
-- Various enhancement states
INSERT INTO enhancements (enhancement_id, generation_db_id, request_db_id, status, result, enhancerName, enhancerVersion, index_value, created, updated, finished, latest_result) VALUES
  ('dev-enh-001', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-001'), (SELECT db_id FROM requests WHERE request_id = 'dev-req-124'), 'FINISHED', 0, 'pnc-enhancer', '1.0.0', 1, '2020-02-01 09:16:00+00', '2020-02-01 09:25:00+00', '2020-02-01 09:25:00+00', 'SUCCESS'),
  ('dev-enh-002', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-002'), (SELECT db_id FROM requests WHERE request_id = 'dev-req-126'), 'FINISHED', 0, 'pnc-enhancer', '1.0.0', 1, '2020-04-10 11:21:00+00', '2020-04-10 11:30:00+00', '2020-04-10 11:30:00+00', 'SUCCESS'),
  ('dev-enh-003', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-003'), (SELECT db_id FROM requests WHERE request_id = 'dev-req-127'), 'IN_PROGRESS', NULL, 'pnc-enhancer', '1.0.0', 1, '2020-05-01 08:11:00+00', '2020-05-01 08:15:00+00', NULL, NULL),
  ('dev-enh-004', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-005'), (SELECT db_id FROM requests WHERE request_id = 'dev-req-129'), 'FINISHED', 0, 'pnc-enhancer', '1.1.0', 1, '2020-07-20 08:06:00+00', '2020-07-20 08:15:00+00', '2020-07-20 08:15:00+00', 'SUCCESS'),
  ('dev-enh-005', (SELECT db_id FROM generations WHERE generation_id = 'dev-gen-005'), (SELECT db_id FROM requests WHERE request_id = 'dev-req-129'), 'FINISHED', 0, 'license-enhancer', '1.0.0', 2, '2020-07-20 08:16:00+00', '2020-07-20 08:20:00+00', '2020-07-20 08:20:00+00', 'SUCCESS');

INSERT INTO enhancement_options (enhancement_db_id, opt_key, opt_value) VALUES
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-001'), 'pncUrl', 'https://pnc.example.com'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-001'), 'includeBuilds', 'true'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-004'), 'pncUrl', 'https://pnc.example.com'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-005'), 'licenseDb', 'https://licenses.example.com');

INSERT INTO enhancement_sbom_urls (enhancement_db_id, url) VALUES
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-001'), 'https://sbom-storage.example.com/sboms/dev-enh-001.json'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-002'), 'https://sbom-storage.example.com/sboms/dev-enh-002.json'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-004'), 'https://sbom-storage.example.com/sboms/dev-enh-004.json'),
  ((SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-005'), 'https://sbom-storage.example.com/sboms/dev-enh-005.json');


-- ENHANCEMENT RUNS
-- Execution history for enhancements
INSERT INTO enhancement_runs (run_id, enhancement_db_id, attempt_number, state, reason, message, start_time, completion_time) VALUES
  -- Successful enhancement on first attempt
  ('dev-enh-run-001', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-001'), 1, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully', '2020-02-01 09:16:00+00', '2020-02-01 09:25:00+00'),
  
  -- Successful enhancement on first attempt
  ('dev-enh-run-002', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-002'), 1, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully', '2020-04-10 11:21:00+00', '2020-04-10 11:30:00+00'),
  
  -- Currently running enhancement
  ('dev-enh-run-003', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-003'), 1, 'RUNNING', NULL, 'Fetching PNC build data...', '2020-05-01 08:11:00+00', NULL),
  
  -- Successful enhancement after retry
  ('dev-enh-run-004a', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-004'), 1, 'FAILED', 'ERR_GENERAL', 'PNC API timeout', '2020-07-20 08:06:00+00', '2020-07-20 08:08:00+00'),
  ('dev-enh-run-004b', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-004'), 2, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully on retry', '2020-07-20 08:10:00+00', '2020-07-20 08:15:00+00'),
  
  -- Successful license enhancement
  ('dev-enh-run-005', (SELECT db_id FROM enhancements WHERE enhancement_id = 'dev-enh-005'), 1, 'SUCCEEDED', 'SUCCESS', 'License information added', '2020-07-20 08:16:00+00', '2020-07-20 08:20:00+00');


-- ============================================================================
-- SUMMARY OF SAMPLE DATA
-- ============================================================================
-- Requests: 7 (various states: FINISHED, RECEIVED, IN_PROGRESS, FAILED)
-- Publishers: 3 (with options)
-- Generations: 6 (various states with retry scenarios)
-- Generation Runs: 8 (showing successful, failed, running, and pending states)
-- Enhancements: 5 (various states)
-- Enhancement Runs: 6 (showing successful, failed, and running states)
-- ============================================================================

