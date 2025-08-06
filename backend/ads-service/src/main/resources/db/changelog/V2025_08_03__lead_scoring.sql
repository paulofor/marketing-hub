-- Adds score column to lead and creates funnel_event table
--changeset marketinghub:2025-08-03-lead-score
ALTER TABLE `lead` ADD COLUMN score INT NOT NULL DEFAULT 0;

--changeset marketinghub:2025-08-03-funnel-event
CREATE TABLE `funnel_event` (
    id BINARY(16) PRIMARY KEY,
    lead_id BINARY(16),
    stimulus VARCHAR(50),
    created_at DATETIME(6),
    CONSTRAINT fk_funnel_event_lead FOREIGN KEY (lead_id) REFERENCES `lead`(id)
);
CREATE INDEX idx_funnel_event_lead ON `funnel_event` (lead_id, created_at);
