-- Calculates drop-off between funnel steps
SELECT fs.order_idx,
       COUNT(lr.id) AS responses
FROM funnel_step fs
LEFT JOIN lead_response lr ON lr.funnel_step_id = fs.id
WHERE fs.funnel_id = :funnelId
GROUP BY fs.order_idx
ORDER BY fs.order_idx;
