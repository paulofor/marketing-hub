# Homologacao — sessao supervisionada Meta de Argos v1

Data: 2026-08-30

## Objetivo e metrica de liberacao

Permitir que uma execucao B2C orientada a Instagram abra uma busca oficial da Biblioteca de
Anuncios da Meta, receba observacoes humanas auditaveis e reexecute Argos usando exatamente a mesma
investigacao. A liberacao exige que 100% das reanalises consumam a investigacao exibida na tela e
que nenhuma ausencia, falha ou observacao de Facebook seja promovida a cobertura Instagram.

## Matriz ponta a ponta

| Dimensao        | Cenario                                                                        | Evidencia esperada                                                                              |
| --------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| Caminho feliz   | Ciclo concluido aguarda cobertura e pessoa registra anuncio ativo no Instagram | Sessao mostra busca, aceita observacao e habilita reanalise na mesma execucao                   |
| Correlacao      | Novo planejamento usa consulta textual diferente                               | Backend reutiliza o `investigationId` anterior e Argos recebe os anuncios supervisionados       |
| Idempotencia    | Pessoa envia duas vezes o comando de reanalise                                 | Uma unica tentativa nova fica pendente; nenhum ciclo, tarefa ou custo e duplicado               |
| Validacao       | URL fora de `facebook.com/ads/library`                                         | Backend rejeita antes de persistir                                                              |
| Validacao       | Anuncio observado somente no Facebook                                          | Observacao permanece auditavel, mas a reanalise B2C/Instagram continua bloqueada                |
| Validacao       | Pagina ou anuncio nao esta ativo                                               | Sessao informa a lacuna e nao habilita reanalise                                                |
| Falha externa   | Biblioteca publica esta indisponivel                                           | Execucao preserva `AWAITING_SUPERVISED_OBSERVATION`; ausencia nao vira ausencia de mercado      |
| Integracao      | Observacao e registrada                                                        | Payload bruto, ciclo, investigacao, anuncio, texto, plataforma e instante ficam persistidos     |
| Auditoria       | Reanalise termina                                                              | Historico conserva tentativa anterior e nova tentativa com modelo, tokens, evidencias e decisao |
| Observabilidade | Request/response da sessao                                                     | Logs carregam ciclo e investigacao sem token, cookie ou credencial                              |
| Metricas        | Tela atualiza a sessao                                                         | Exibe anuncios aderentes, ativos, anunciantes, data e linguagem observada; nao exibe vendas     |
| Segregacao      | Dados de homologacao                                                           | Nao cria produto, experimento, evento de funil, campanha, gasto, venda ou receita               |
| Desktop         | Chromium desktop                                                               | Link oficial, campos, resumo e comando de reanalise ficam legiveis e acionaveis                 |
| iPhone          | Emulacao iPhone 15 Pro                                                         | Formulario nao corta campos, link ou botao e mantem alvos de toque adequados                    |
| Android         | Emulacao Pixel 7                                                               | Mesmas acoes e mensagens do desktop sem overflow horizontal                                     |

## Criterio de continuar, ajustar ou parar

- **Continuar:** existe pelo menos um anuncio atual, ativo e explicitamente observado no Instagram;
  a reanalise usa a mesma investigacao e permanece conservadora sobre vendas.
- **Ajustar:** a sessao existe, mas a fonte esta indisponivel, a evidencia esta antiga ou falta
  plataforma, atividade ou linguagem visivel.
- **Parar:** aparece scraping, credencial no payload/log, evidencia cruzada entre ciclos, nova
  tentativa duplicada, campanha/gasto ou conclusao comercial sem observacao real.

## Resultado local

Uma verificacao preliminar encontrou o projeto Compose de homologacao fora do identificador exclusivo
da sandbox. A causa foi corrigida no script e a contagem foi reiniciada.

Duas rodadas completas e consecutivas passaram depois da correcao. Cada rodada validou:

- 2.116 testes do backend, sem falhas ou erros;
- 433 testes do frontend, TypeScript, Prettier e build de producao;
- 70 testes do Product Discovery Worker;
- persistencia, idempotencia e segregacao de plataforma em MySQL 5.7 fisico;
- jornada completa em Chromium desktop, iPhone 15 Pro e Pixel 7, sem erro de console, overflow
  horizontal ou alvo de toque menor que 44 px;
- Spotless, sintaxe do script, contrato Compose e diff sem espacos invalidos.

As duas rodadas terminaram sem containers, redes ou volumes temporarios. Nenhum produto,
experimento, evento de funil, campanha, gasto, venda ou receita foi criado.

O contrato de persistencia fisica pode ser reproduzido com
`scripts/validate-argos-meta-supervised-session-mysql57.sh` no modulo `backend/ads-service`.
