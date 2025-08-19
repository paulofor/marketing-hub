-- liquibase formatted sql
-- changeset marketinghub:2025-09-20-add-chat-dialog-fk-to-market-niche
ALTER TABLE market_niche ADD COLUMN chat_dialog_id BIGINT;
ALTER TABLE market_niche
    ADD CONSTRAINT fk_market_niche_chat_dialog FOREIGN KEY (chat_dialog_id) REFERENCES chat_dialog(id);
