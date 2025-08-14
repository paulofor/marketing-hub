-- liquibase formatted sql
-- Creates chat_dialog table

-- changeset marketinghub:2025-09-15-create-chat-dialog
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="chat_dialog"/>
--    </not>
CREATE TABLE IF NOT EXISTS chat_dialog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500),
    description LONGTEXT,
    theme VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
