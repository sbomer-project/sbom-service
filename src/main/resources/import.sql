-- ============================================================================
-- SAMPLE DATA FOR LOCAL DEVELOPMENT
-- ============================================================================
-- This file populates the database with sample data for testing the SBOM service
-- including requests, generations, enhancements, and their execution runs.
-- Note: Using explicit db_id values and disabling auto-increment temporarily
-- ============================================================================

-- Temporarily set sequences to allow explicit ID insertion
SELECT setval('requests_db_id_seq', 100, false);
SELECT setval('generations_db_id_seq', 100, false);
SELECT setval('enhancements_db_id_seq', 100, false);
SELECT setval('generation_runs_db_id_seq', 100, false);
SELECT setval('enhancement_runs_db_id_seq', 100, false);
SELECT setval('request_publishers_db_id_seq', 100, false);

-- REQUESTS
INSERT INTO requests (db_id, request_id, status, creationDate) VALUES
  (10, 'dev-req-124', 'FINISHED',    '2020-02-01 09:00:00+00'),
  (11, 'dev-req-125', 'RECEIVED',    '2020-03-05 08:30:00+00'),
  (12, 'dev-req-126', 'FINISHED',    '2020-04-10 11:00:00+00'),
  (13, 'dev-req-127', 'IN_PROGRESS', '2020-05-01 08:00:00+00'),
  (14, 'dev-req-128', 'FAILED',      '2020-06-12 13:15:00+00'),
  (15, 'dev-req-129', 'FINISHED',    '2020-07-20 07:45:00+00'),
  (16, 'dev-req-130', 'IN_PROGRESS', '2020-08-03 10:00:00+00');

-- PUBLISHERS
INSERT INTO request_publishers (db_id, request_db_id, name, version) VALUES
  (10, 10, 'maven-publisher', '1.0.0'),
  (11, 12, 'npm-publisher', '2.1.0'),
  (12, 15, 'maven-publisher', '1.0.0');

INSERT INTO publisher_options (publisher_db_id, opt_key, opt_value) VALUES
  (10, 'repository', 'https://repo.maven.apache.org/maven2'),
  (10, 'format', 'cyclonedx'),
  (11, 'registry', 'https://registry.npmjs.org'),
  (12, 'repository', 'https://repo.maven.apache.org/maven2');

-- GENERATIONS
INSERT INTO generations (db_id, generation_id, request_db_id, status, result, generatorName, generatorVersion, targetIdentifier, targetType, created, updated, finished, child_enhancements_status, latest_result) VALUES
  (10, 'dev-gen-001', 10, 'COMPLETED', 0, 'syft-generator', '1.0.0', 'org.example:app:1.0.0', 'maven', '2020-02-01 09:05:00+00', '2020-02-01 09:15:00+00', '2020-02-01 09:15:00+00', 'ALL_FINISHED', 'SUCCESS'),
  (11, 'dev-gen-002', 12, 'COMPLETED, 0, 'syft-generator', '1.0.0', 'example-app:2.0.0', 'npm', '2020-04-10 11:05:00+00', '2020-04-10 11:20:00+00', '2020-04-10 11:20:00+00', 'ALL_FINISHED', 'SUCCESS'),
  (12, 'dev-gen-003', 13, 'IN_PROGRESS', NULL, 'syft-generator', '1.0.0', 'org.example:service:3.0.0', 'maven', '2020-05-01 08:05:00+00', '2020-05-01 08:10:00+00', NULL, 'SOME_IN_PROGRESS', NULL),
  (13, 'dev-gen-004', 14, 'FAILED', 5, 'syft-generator', '1.0.0', 'org.example:broken:1.0.0', 'maven', '2020-06-12 13:20:00+00', '2020-06-12 13:25:00+00', '2020-06-12 13:25:00+00', 'NONE', 'ERR_GENERATION'),
  (14, 'dev-gen-005', 15, 'COMPLETED', 0, 'syft-generator', '1.1.0', 'org.example:lib:2.5.0', 'maven', '2020-07-20 07:50:00+00', '2020-07-20 08:05:00+00', '2020-07-20 08:05:00+00', 'ALL_FINISHED', 'SUCCESS'),
  (15, 'dev-gen-006', 16, 'INITIALIZING', NULL, 'syft-generator', '1.1.0', 'org.example:new-app:1.0.0', 'maven', '2020-08-03 10:05:00+00', '2020-08-03 10:05:00+00', NULL, 'NONE', NULL);

INSERT INTO generation_options (generation_db_id, opt_key, opt_value) VALUES
  (10, 'buildType', 'maven'),
  (10, 'includeTransitive', 'true'),
  (11, 'buildType', 'npm'),
  (11, 'includeDevDependencies', 'false');

INSERT INTO generation_sbom_urls (generation_db_id, url) VALUES
  (10, 'https://sbom-storage.example.com/sboms/dev-gen-001.json'),
  (11, 'https://sbom-storage.example.com/sboms/dev-gen-002.json'),
  (14, 'https://sbom-storage.example.com/sboms/dev-gen-005.json');

-- GENERATION RUNS
INSERT INTO generation_runs (db_id, run_id, generation_db_id, attempt_number, state, reason, message, start_time, completion_time) VALUES
  (10, 'dev-gen-run-001', 10, 1, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully', '2020-02-01 09:05:00+00', '2020-02-01 09:15:00+00'),
  (11, 'dev-gen-run-002', 11, 1, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully', '2020-04-10 11:05:00+00', '2020-04-10 11:20:00+00'),
  (12, 'dev-gen-run-003', 12, 1, 'RUNNING', NULL, 'Processing dependencies...', '2020-05-01 08:05:00+00', NULL),
  (13, 'dev-gen-run-004a', 13, 1, 'FAILED', 'ERR_GENERATION', 'Failed to resolve dependencies', '2020-06-12 13:20:00+00', '2020-06-12 13:22:00+00'),
  (14, 'dev-gen-run-004b', 13, 2, 'FAILED', 'ERR_GENERATION', 'Failed to resolve dependencies (retry)', '2020-06-12 13:23:00+00', '2020-06-12 13:25:00+00'),
  (15, 'dev-gen-run-005a', 14, 1, 'FAILED', 'ERR_SYSTEM', 'Temporary network issue', '2020-07-20 07:50:00+00', '2020-07-20 07:52:00+00'),
  (16, 'dev-gen-run-005b', 14, 2, 'SUCCEEDED', 'SUCCESS', 'SBOM generated successfully on retry', '2020-07-20 07:55:00+00', '2020-07-20 08:05:00+00'),
  (17, 'dev-gen-run-006', 15, 1, 'PENDING', NULL, 'Queued for processing', NULL, NULL);

-- ENHANCEMENTS
INSERT INTO enhancements (db_id, enhancement_id, generation_db_id, request_db_id, status, result, enhancerName, enhancerVersion, index_value, created, updated, finished, latest_result) VALUES
  (10, 'dev-enh-001', 10, 10, 'FINISHED', 0, 'pnc-enhancer', '1.0.0', 1, '2020-02-01 09:16:00+00', '2020-02-01 09:25:00+00', '2020-02-01 09:25:00+00', 'SUCCESS'),
  (11, 'dev-enh-002', 11, 12, 'FINISHED', 0, 'pnc-enhancer', '1.0.0', 1, '2020-04-10 11:21:00+00', '2020-04-10 11:30:00+00', '2020-04-10 11:30:00+00', 'SUCCESS'),
  (12, 'dev-enh-003', 12, 13, 'IN_PROGRESS', NULL, 'pnc-enhancer', '1.0.0', 1, '2020-05-01 08:11:00+00', '2020-05-01 08:15:00+00', NULL, NULL),
  (13, 'dev-enh-004', 14, 15, 'FINISHED', 0, 'pnc-enhancer', '1.1.0', 1, '2020-07-20 08:06:00+00', '2020-07-20 08:15:00+00', '2020-07-20 08:15:00+00', 'SUCCESS'),
  (14, 'dev-enh-005', 14, 15, 'FINISHED', 0, 'license-enhancer', '1.0.0', 2, '2020-07-20 08:16:00+00', '2020-07-20 08:20:00+00', '2020-07-20 08:20:00+00', 'SUCCESS');

INSERT INTO enhancement_options (enhancement_db_id, opt_key, opt_value) VALUES
  (10, 'pncUrl', 'https://pnc.example.com'),
  (10, 'includeBuilds', 'true'),
  (13, 'pncUrl', 'https://pnc.example.com'),
  (14, 'licenseDb', 'https://licenses.example.com');

INSERT INTO enhancement_sbom_urls (enhancement_db_id, url) VALUES
  (10, 'https://sbom-storage.example.com/sboms/dev-enh-001.json'),
  (11, 'https://sbom-storage.example.com/sboms/dev-enh-002.json'),
  (13, 'https://sbom-storage.example.com/sboms/dev-enh-004.json'),
  (14, 'https://sbom-storage.example.com/sboms/dev-enh-005.json');

-- ENHANCEMENT RUNS
INSERT INTO enhancement_runs (db_id, run_id, enhancement_db_id, attempt_number, state, reason, message, start_time, completion_time) VALUES
  (10, 'dev-enh-run-001', 10, 1, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully', '2020-02-01 09:16:00+00', '2020-02-01 09:25:00+00'),
  (11, 'dev-enh-run-002', 11, 1, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully', '2020-04-10 11:21:00+00', '2020-04-10 11:30:00+00'),
  (12, 'dev-enh-run-003', 12, 1, 'RUNNING', NULL, 'Fetching PNC build data...', '2020-05-01 08:11:00+00', NULL),
  (13, 'dev-enh-run-004a', 13, 1, 'FAILED', 'ERR_GENERAL', 'PNC API timeout', '2020-07-20 08:06:00+00', '2020-07-20 08:08:00+00'),
  (14, 'dev-enh-run-004b', 13, 2, 'SUCCEEDED', 'SUCCESS', 'PNC data enriched successfully on retry', '2020-07-20 08:10:00+00', '2020-07-20 08:15:00+00'),
  (15, 'dev-enh-run-005', 14, 1, 'SUCCEEDED', 'SUCCESS', 'License information added', '2020-07-20 08:16:00+00', '2020-07-20 08:20:00+00');

-- Reset sequences to continue from where we left off
SELECT setval('requests_db_id_seq', 20, false);
SELECT setval('generations_db_id_seq', 20, false);
SELECT setval('enhancements_db_id_seq', 20, false);
SELECT setval('generation_runs_db_id_seq', 20, false);
SELECT setval('enhancement_runs_db_id_seq', 20, false);
SELECT setval('request_publishers_db_id_seq', 20, false);

-- ============================================================================
-- SUMMARY: 7 requests, 6 generations, 8 generation runs, 5 enhancements, 6 enhancement runs
-- ============================================================================