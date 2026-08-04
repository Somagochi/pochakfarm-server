ALTER TABLE users
  ADD COLUMN required_terms_agreed_at DATETIME(6) NULL,
  ADD COLUMN service_quality_agreed_at DATETIME(6) NULL,
  ADD COLUMN marketing_agreed_at DATETIME(6) NULL;

UPDATE users
SET required_terms_agreed_at = created_at
WHERE required_terms_agreed_at IS NULL;

ALTER TABLE pre_registrations
  ADD COLUMN message_sent_at DATETIME(6) NULL;
