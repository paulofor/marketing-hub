--changeset repo:2034-02-03-add-targeting-request-to-adset dbms:mysql
ALTER TABLE ad_set
    ADD COLUMN targeting_request_id BINARY(16) AFTER model,
    ADD CONSTRAINT fk_ad_set_targeting_request FOREIGN KEY (targeting_request_id) REFERENCES targeting_request(id);

CREATE INDEX idx_ad_set_targeting_request ON ad_set(targeting_request_id);
