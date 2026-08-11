# Matriz de homologação — projeção de receita de Plutus v1

| Dimensão | Caminho feliz | Validação/falha | Evidência esperada |
|---|---|---|---|
| Plano e mesa | solicitar projeção em plano versionado | plano inexistente é rejeitado | execução e tarefa compartilham plano e versão |
| Paralelismo | solicitar MUSA e Agenda Cheia | uma fila não bloqueia a outra | duas execuções pendentes segregadas |
| Contrato | Plutus retorna três cenários | resposta incompleta falha | premissas, break-even, teto e critérios persistidos |
| Segurança | projeção recomenda investimento | nenhum orçamento ou gasto é alterado | autoridade somente leitura e receita realizada intacta |
| Aprendizado | conclusão gera candidato | agente não promove memória | candidato auditável para comparação futura |
| Observabilidade | fila inicia, conclui ou bloqueia | erro técnico atualiza mesa | status, relatório, erro e horários visíveis no plano |
| Métricas | comparar previsão e realizado | dado ausente não vira zero confirmado | limitações explícitas e cenários não tratados como vendas |
| Navegadores | tela do plano em desktop, iPhone e Pixel | sem overflow ou ação oculta | botão, estados e resumo acessíveis |
