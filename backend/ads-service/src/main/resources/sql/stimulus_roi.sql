-- Computes ROI per stimulus with simple multi-touch attribution
SELECT fs.stimulus_type,
       SUM(lr.revenue) / NULLIF(SUM(sms.gross_profit),0) AS roi
FROM funnel_step fs
JOIN step_metric_snapshot sms ON sms.funnel_step_id = fs.id
LEFT JOIN lead_response lr ON lr.funnel_step_id = fs.id AND lr.action = 'PURCHASE'
WHERE fs.funnel_id = :funnelId
GROUP BY fs.stimulus_type;
