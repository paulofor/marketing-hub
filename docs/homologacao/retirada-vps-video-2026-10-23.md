# Retirada do VPS 177.153.62.107

Solicitação de 23/10/2026: liberar este VPS e verificar o uso dos coletores Meta e Hotmart.
Destino escolhido: `163.245.200.7`, com 6 vCPU, 3.724 MiB disponíveis e 46 GiB livres na
consulta inicial. São amostras, não medições de pico.

## Evidência e decisão

- Apolo: vídeo saudável, 177 MiB numa amostra, sem job de vídeo ou análise em execução.
- Meta Library: 47 investigações `ACTIVE_SUPERVISED`, nenhuma `PENDING`/`PROCESSING`.
  São acompanhamentos supervisionados, preservados no backend; não são a fila da API.
  Última observação: 24/09. O executor consulta autorização a cada 15 minutos e recebe
  credencial vencida, sem produzir coleta. Esse token não é o da operação de anúncios.
- Hotmart: agenda explicitamente desativada, 3.940 referências no backend, última coleta
  em 07/09; dois registros históricos `RUNNING` de maio/agosto não representam threads
  em execução no container. Não serão apagados nem encerrados artificialmente.
- MOIS, OPRM e MDS já estavam parados, com reinício automático desativado. ClickBank ausente.
- O host só expõe SSH e as três portas dos containers ativos; não há banco, proxy público
  ou outro serviço de negócio ativo observado.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Transferir os três e manter todos ativos | Pouca mudança funcional | Mantém consultas sem entrega e consumo desnecessário | Não adotada |
| Apolo ativo e coletores preservados suspensos no destino | Libera o host, conserva retomada e capacidade | Exige preservar estado e proteger publicação | Adotada |
| Distribuir entre vários hosts ou excluir coletores | Pode reduzir carga individual | Mais dependências ou perda de recuperação; capacidade já suficiente | Não adotada |

## Matriz local definida antes dos testes

| Critério | Validação |
| --- | --- |
| Destino completo | Nenhum workflow continua acessando o host retirado; monitor/MCP usam novas rotas |
| Recursos e isolamento | Apolo limitado a 2 CPUs/1,5 GiB; Hotmart em 18096 sem conflitar com PDE 8096 |
| Continuidade | Runtime real de vídeo com backend simulado, health, fila vazia e render sintético |
| Suspensão | `prepare` cria parado; recusa trocar executor ativo; `activate` exige escolha e health |
| Recuperação | Estado/segredos transferidos sem exposição, checksums, cópia preservada e ausência de execução duplicada |
| Publicação | Fila compartilhada, sem prune global, estado anterior dos workflows preservado |
| Dados e métricas | Histórico permanece no backend; smoke local identificado, sem compra/inferência/mídia |
| Interface | Cadastro e inventário conferidos no painel; navegador desktop para operação administrativa |

## Lacuna do harness e prevenção

Workflows podiam reativar coletores parados por um push; OPRM MEI ainda executava limpeza
no host antigo, inclusive `compose down` num diretório compartilhado. Publicadores e
observabilidade continham destinos divergentes. A correção remove essa dependência,
preserva reativação somente explícita e testa destino, portas, recursos e ciclo de vida.
Hipótese operacional: reduzir consultas sem entrega e intervenções na migração, preservando
produção audiovisual. Não há evidência de aumento de vendas atribuível à mudança.

## Execução e encerramento

Evidências de testes, PR, SHA, publicações, estado transferido e saúde serão acrescentadas
ao registro do PR após a validação. O contrato comercial do VPS só pode ser cancelado
após comprovar ausência de dependências e recuperação no destino. Não apagar backups,
imagens ou histórico para apenas reduzir o indicador de disco.

## Validação local executada

- 12 regressões de retirada, transferência protegida e ciclo de vida, além dos
  testes existentes de coordenação (53) e recuperação (61; correção de contrato
  confirmada pelos 14 testes estruturais).
- Backend: 9 testes de inventário. MCP: 91 testes. Meta e Hotmart: testes unitários
  dos módulos aprovados. Apolo: 279 testes aprovados; 9 testes preexistentes com
  integração externa condicionada permanecem desabilitados, sem chamadas pagas.
- Imagem real do Dockerfile: inicialização, consulta de pendências/heartbeat contra
  backend local simulado e render de 10 s em 720x1280 com áudio aprovados sob limites
  de 2 CPUs, 1,5 GiB e 512 processos. Topologia e imagem temporárias removidas.
- Actionlint pela ferramenta versionada, bash -n, ShellCheck, fila compartilhada e
  contrato de deploy transacional aprovados. A engine dedicada não vê bind mounts
  locais; o smoke usou volumes efêmeros e docker cp, preservando isolamento.
- Nenhum teste enviou dados comerciais, criou mídia paga ou alterou filas produtivas.
