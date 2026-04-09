--liquibase formatted sql

--changeset repo:2037-04-09-lead-portal-flow-prompt-email dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM prompt WHERE domain = 'LEAD_PORTAL_FLOW' AND active = 1;
UPDATE prompt
SET template = REPLACE(
    template,
    'As perguntas devem ajudar o lead a refletir sobre dores e objetivos, oferecendo opções de resposta realistas quando houver múltipla escolha.\n',
    'As perguntas devem ajudar o lead a refletir sobre dores e objetivos, oferecendo opções de resposta realistas quando houver múltipla escolha.\nInclua SEMPRE uma pergunta obrigatória do tipo EMAIL com dataKey "email", pois toda a comunicação de follow-up será feita por e-mail.\nConsidere que o frontend envia o formulário via POST multipart (FormData) para o endpoint {{url}}, com envio assíncrono e feedback de sucesso/erro ao usuário.\n'
)
WHERE domain = 'LEAD_PORTAL_FLOW'
  AND active = 1
  AND template LIKE '%As perguntas devem ajudar o lead a refletir sobre dores e objetivos, oferecendo opções de resposta realistas quando houver múltipla escolha.%';
