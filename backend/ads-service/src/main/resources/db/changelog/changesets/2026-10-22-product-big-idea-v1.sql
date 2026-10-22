CREATE TABLE product_big_idea_v1 (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 revision INT NOT NULL,
 central_idea VARCHAR(500) NOT NULL,
 content_json LONGTEXT NOT NULL,
 change_reason VARCHAR(2000) NOT NULL,
 created_by VARCHAR(191) NOT NULL,
 source_task_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_big_idea_product_revision (product_id, revision),
 UNIQUE KEY uk_big_idea_product_id (product_id, id),
 CONSTRAINT fk_big_idea_product FOREIGN KEY (product_id) REFERENCES product(id),
 CONSTRAINT fk_big_idea_task FOREIGN KEY (source_task_id) REFERENCES agent_task(id)
) ENGINE=InnoDB;
ALTER TABLE product ADD COLUMN current_big_idea_id BIGINT NULL;
ALTER TABLE product ADD CONSTRAINT fk_product_current_big_idea FOREIGN KEY (id,current_big_idea_id) REFERENCES product_big_idea_v1(product_id,id);
ALTER TABLE agent_task ADD COLUMN product_big_idea_json LONGTEXT NULL;
CREATE TABLE product_big_idea_binding_v1 (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 source_reference VARCHAR(200) NOT NULL,
 big_idea_id BIGINT NOT NULL,
 UNIQUE KEY uk_big_idea_binding (product_id,source_reference),
 CONSTRAINT fk_big_idea_binding_version FOREIGN KEY (product_id,big_idea_id) REFERENCES product_big_idea_v1(product_id,id)
) ENGINE=InnoDB;
