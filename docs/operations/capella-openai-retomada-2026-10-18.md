# Capella — retomada após rotação da chave OpenAI

## Estado conferido e causa

Solicitação: confirmação de atualização da chave após a pendência #3649.
Produto 7 (Capella), cadeia 26, ciclo 12, experimento 105, versão
`capella-private-v3`. Não criar sucessor nem repetir revisões aceitas.

- Backend conferido pelo MCP em `3ff50300d4a3a69a10d27e7661395c7c3c0a9a6c`.
- A primeira etapa da página, job `81ee75b0-17b1-4521-ad6e-5e4ead9aea7e`,
  falhou com HTTP 401. Não há resposta, ID OpenAI, tokens ou custo registrados.
  Ausência de custo não comprova cobrança zero.
- A chave do arquivo protegido foi aceita em `GET /v1/models` (HTTP 200).
  O arquivo está em modo `0600`, montado somente para leitura e com o mesmo
  conteúdo dentro do container. Nenhum valor foi registrado nesta evidência.
- O container iniciou em 17/10, antes da atualização do arquivo em 18/10.
  `OpenAiClientProperties` resolve o token ao construir a configuração; não
  há recarga do campo já carregado apenas porque o arquivo mudou.
- O publicador `ai-worker.yml` não tinha escopo no coordenador nem contrato
  de recuperação vinculado ao SHA. A lacuna foi reproduzida localmente.

## Alternativas e mudança reutilizável

| Alternativa | Benefício | Risco/esforço e decisão |
| --- | --- | --- |
| Reiniciar diretamente por SSH | Rápido | Contorna o fluxo de publicação exigido; descartada |
| Disparar pipeline sem integração ao coordenador | Reutiliza a imagem versionada | Não protege a intervenção nem prova a recuperação; insuficiente |
| Incluir escopo e recuperação no controle existente | Operação auditável, sem infraestrutura paralela | Ajuste pequeno com PR e testes; escolhida |

O escopo `ai-worker` protege somente seu publicador. A retomada automática exige
o SHA integrado e sucesso do job real `deploy`, preserva workflow anteriormente
desativado e não duplica dispatch incerto. O pipeline valida a revisão antes
de testes, imagem e publicação. O contrato de imagem e a lógica Java permanecem
os existentes; nenhuma recarga dinâmica de credencial foi introduzida.

## Matriz local e aceite da operação

| Caso | Verificação |
| --- | --- |
| Falha original | Novo teste rejeita escopo ausente antes da mudança |
| Isolamento | Pausa e retomada próprias sem parar o publicador administrativo |
| Recuperação | Um dispatch, bloqueio sem `deploy`, conclusão com job comprovado |
| Outros contextos | Suítes existentes cobrem Vega, outro ciclo, FEO e suspensão intencional |
| Versões e falhas | Contratos reais de inputs/jobs, SHA divergente, timeout, recibo incerto e não reativação de workflows já parados |
| Imagem | JAR testado preservado e restaurado antes de construir a imagem |

Testes de coordenação e recuperação usam GitHub simulado e estado/lock locais.
O contrato da imagem é validado por Bash e ShellCheck; o workflow, por Actionlint.
Não há frontend ou nova experiência nesta alteração; desktop e celulares ficam
na conferência funcional do percurso retomado, sem misturar simulação com venda.

Após integração: acompanhar o reconciliador e o pipeline do Worker IA, comprovar
container iniciado depois da troca, conferir saúde e retomar pela tela apenas
a geração sem resultado. Aceites humanos, orçamento e revisões permanecem os
originais. A entrega funcional exige saída persistida e próxima passagem aceita;
healthcheck e pipeline verde não comprovam experimento em RUNNING ou vendas.

Limites preservados: US$ 15 cumulativos de preparação/revisão, US$ 10 próprios
de audiovisual e R$ 100 de mídia por cinco dias após homologação. Descoberta
pode ficar abaixo de 30%; continuidade/escala conservam a meta de 30%.
Consumo e saldo devem ser reconferidos antes da retomada; valores desconhecidos
permanecem desconhecidos. O ganho esperado é reduzir a espera após rotação,
medido pela retomada funcional; impacto em vendas depende de medição posterior.
