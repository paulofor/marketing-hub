-- Retention of leads by nurture stage
SELECT nurture_stage, COUNT(*) AS leads
FROM lead
GROUP BY nurture_stage;
