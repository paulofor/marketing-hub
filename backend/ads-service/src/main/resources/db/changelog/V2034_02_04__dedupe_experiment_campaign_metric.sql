--liquibase formatted sql
--changeset repo:2034-02-04-dedupe-experiment-campaign-metric dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM (SELECT experiment_id FROM experiment_campaign_metric GROUP BY experiment_id HAVING COUNT(*) > 1) AS dup;
DELETE FROM experiment_campaign_metric
WHERE id NOT IN (
    SELECT max_id FROM (
        SELECT MAX(id) AS max_id
        FROM experiment_campaign_metric
        GROUP BY experiment_id
    ) AS dedup
);

--changeset repo:2034-02-04-experiment-campaign-metric-unique dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'experiment_campaign_metric' AND index_name = 'uk_experiment_campaign_metric_experiment_id';
ALTER TABLE experiment_campaign_metric
    ADD CONSTRAINT uk_experiment_campaign_metric_experiment_id UNIQUE (experiment_id);
