--changeset repo:2034-02-02-create-targeting-option dbms:mysql
CREATE TABLE targeting_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    candidate_id BIGINT NOT NULL,
    facebook_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    audience_size BIGINT,
    match_score DECIMAL(5,4),
    search_locale VARCHAR(10),
    search_country VARCHAR(5),
    search_term VARCHAR(255),
    created_at datetime(6),
    updated_at datetime(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_targeting_option_candidate FOREIGN KEY (candidate_id) REFERENCES targeting_candidate(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE targeting_option_path (
    option_id BIGINT NOT NULL,
    path_entry VARCHAR(255) NOT NULL,
    CONSTRAINT fk_targeting_option_path_option FOREIGN KEY (option_id) REFERENCES targeting_option(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE targeting_candidate ADD COLUMN country VARCHAR(5) AFTER idioma;

CREATE INDEX idx_targeting_option_candidate ON targeting_option(candidate_id);
CREATE INDEX idx_targeting_candidate_country ON targeting_candidate(country);
