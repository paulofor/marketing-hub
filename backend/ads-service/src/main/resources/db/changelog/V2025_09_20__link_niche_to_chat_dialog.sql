-- liquibase formatted sql
-- changeset marketinghub:2025-09-20-link-niche-to-chat-dialog
-- preconditions onFail:MARK_RAN
--    <not>
--        <columnExists tableName="market_niche" columnName="chat_dialog_id"/>
--    </not>
ALTER TABLE market_niche ADD COLUMN chat_dialog_id BIGINT;
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND CONSTRAINT_NAME = 'fk_market_niche_chat_dialog' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND INDEX_NAME = 'fk_market_niche_chat_dialog';
ALTER TABLE market_niche
    ADD CONSTRAINT fk_market_niche_chat_dialog FOREIGN KEY (chat_dialog_id) REFERENCES chat_dialog(id);
