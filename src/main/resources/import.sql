INSERT INTO requests (id, status, creationDate) VALUES
  ('dev-req-124', 'FINISHED',    '2020-02-01 09:00:00'),
  ('dev-req-125', 'NEW', '2020-03-05 08:30:00'),
  ('dev-req-126', 'FINISHED',    '2020-04-10 11:00:00'),
  ('dev-req-127', 'NEW',     '2020-05-01 08:00:00'),
  ('dev-req-128', 'FAILED',      '2020-06-12 13:15:00'),
  ('dev-req-129', 'FINISHED',    '2020-07-20 07:45:00'),
  ('dev-req-130', 'FINISHED',    '2020-08-03 10:00:00');


INSERT INTO generations (id, request_id, status) VALUES
  ('dev-gen-001', 'dev-req-124', 'FINISHED'),
  ('dev-gen-002', 'dev-req-126', 'FINISHED');


INSERT INTO enhancements (id, generation_id, status, sequence_index) VALUES
  ('dev-enh-001', 'dev-gen-001', 'FINISHED', 1),
  ('dev-enh-002', 'dev-gen-002', 'FINISHED', 1);
