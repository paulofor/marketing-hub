-- liquibase formatted sql
-- Creates chat_session and chat_message tables

-- changeset marketinghub:2025-08-06-create-chat-session
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="chat_session"/>
--    </not>
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255),
    channel VARCHAR(50),
    state VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- changeset marketinghub:2025-08-06-create-chat-message
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="chat_message"/>
--    </not>
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT,
    origin VARCHAR(50),
    content LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
