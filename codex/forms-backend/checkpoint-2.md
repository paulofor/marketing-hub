# Squad Backend Forms — Checkpoint 2

- **Owner:** ChatGPT (AI dev)
- **Status:** ✅ Migração criada e configurada no changelog principal. Coluna `data_key` permanece indexável e demais campos agora suportam textos longos.
- **Riscos atuais:** mínimos — resta apenas validar Liquibase em CI. Caso o arquivo legado volte a ser executado, a nova migração serve como fallback.
- **Custos realizados:** ~2h engenharia (ajuste entidade + YAML + validação).
