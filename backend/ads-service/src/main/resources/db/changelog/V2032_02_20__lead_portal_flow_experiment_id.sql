--liquibase formatted sql
--changeset repo:2032-02-20-lead-portal-flow-experiment-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'experiment_id';
ALTER TABLE lead_portal_flow
    ADD COLUMN experiment_id BIGINT NULL,
    ADD CONSTRAINT fk_lead_portal_flow_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);

--changeset repo:2032-02-20-lead-portal-flow-experiment-backfill dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'experiment_id';
UPDATE lead_portal_flow flow
    JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
SET flow.experiment_id = exp.id
WHERE flow.experiment_id IS NULL;
