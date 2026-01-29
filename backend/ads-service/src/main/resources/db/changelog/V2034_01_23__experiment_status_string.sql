-- Ensure experiment status is stored as the enum name instead of a numeric ordinal
-- 1) Create a temporary column with the desired type
ALTER TABLE experiment
    ADD COLUMN status_varchar VARCHAR(32) NULL;

-- 2) Copy and normalize the existing values (either names or numeric ordinals)
UPDATE experiment
SET status_varchar = CASE CAST(status AS CHAR)
        WHEN 'PLANNED' THEN 'PLANNED'
        WHEN 'RUNNING' THEN 'RUNNING'
        WHEN 'PAUSED' THEN 'PAUSED'
        WHEN 'VALIDATED' THEN 'VALIDATED'
        WHEN 'INVALIDATED' THEN 'INVALIDATED'
        WHEN 'INCONCLUSIVE' THEN 'INCONCLUSIVE'
        WHEN 'FINISHED' THEN 'FINISHED'
        WHEN 'FAILED' THEN 'FAILED'
        WHEN '0' THEN 'PLANNED'
        WHEN '1' THEN 'RUNNING'
        WHEN '2' THEN 'PAUSED'
        WHEN '3' THEN 'VALIDATED'
        WHEN '4' THEN 'INVALIDATED'
        WHEN '5' THEN 'INCONCLUSIVE'
        WHEN '6' THEN 'FINISHED'
        WHEN '7' THEN 'FAILED'
        ELSE NULL
    END;

-- 3) Drop the legacy column and rename the normalized one
ALTER TABLE experiment DROP COLUMN status;
ALTER TABLE experiment CHANGE COLUMN status_varchar status VARCHAR(32) NULL;
