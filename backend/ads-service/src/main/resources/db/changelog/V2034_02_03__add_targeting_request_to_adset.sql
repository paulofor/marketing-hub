--liquibase formatted sql
--changeset repo:2034-02-03-add-targeting-request-to-adset dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-dbms type:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ad_set' AND column_name = 'targeting_request_id';
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'targeting_request';
ALTER TABLE ad_set
    ADD COLUMN targeting_request_id BINARY(16) AFTER model,
    ADD CONSTRAINT fk_ad_set_targeting_request FOREIGN KEY (targeting_request_id) REFERENCES targeting_request(id);

CREATE INDEX idx_ad_set_targeting_request ON ad_set(targeting_request_id);
