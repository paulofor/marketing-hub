-- liquibase formatted sql
-- changeset marketinghub:2025-10-10-move-audience-approval
ALTER TABLE audience ADD COLUMN approved BOOLEAN NOT NULL DEFAULT FALSE;

-- changeset marketinghub:2025-10-10-backfill-audience-approval
UPDATE audience a
SET approved = TRUE
WHERE a.hypothesis_id IS NOT NULL
  AND EXISTS (
        SELECT 1
        FROM experiment e
        WHERE e.hypothesis_id = a.hypothesis_id
          AND e.audience_approved = TRUE
    );

UPDATE audience a
SET approved = TRUE
WHERE a.hypothesis_id IS NULL
  AND a.market_niche_id IS NOT NULL
  AND EXISTS (
        SELECT 1
        FROM experiment e
        WHERE e.niche_id = a.market_niche_id
          AND e.audience_approved = TRUE
    );

-- changeset marketinghub:2025-10-10-drop-experiment-audience-approval
ALTER TABLE experiment DROP COLUMN audience_approved;
