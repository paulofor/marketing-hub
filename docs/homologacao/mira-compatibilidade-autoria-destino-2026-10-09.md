# Compatibilidade de Mira após preservar a autoria de Íris — 09/10/2026

A correção da passagem privada de Capella acrescenta `agentKey` ao artefato de
comunicação projetado por `IrisLearningCycleContext` e verifica autoria/bytes no
seu teste. Essas fontes pertencem também à atestação privada vigente de Mira.
O CI #37938013391 bloqueou corretamente o hash antigo antes de empacotar o backend.

Foram comparadas três alternativas: manter a atestação antiga bloqueia a entrega;
relaxar hashes ou alterar o manifesto histórico destrói integridade; revalidar as
fontes e criar uma sucessora de compatibilidade preserva os limites e permite a
entrega. Foi escolhida a terceira. `mira-private-candidate.v12` conserva a v11 por
referência e SHA-256 e registra somente os hashes das duas fontes revalidadas.

A experiência `mira-private-candidate-v3`, frontend, oferta, preço, ciclo e provas
independentes de Mira permanecem nos seus contratos próprios. Esta atestação tem
status `EVIDENCE_COMPATIBILITY_ONLY`, com deploy automático desabilitado. Não cria
parecer, custo, aceite ou autorização comercial em nome de Mira.

A suíte completa do backend passou localmente: 4.472 testes, zero falhas/erros,
incluindo os testes de Mira e 92 regras de arquitetura. A conferência final do
contrato passou em 27 testes do contexto e 30 da jornada: produtor real até destino
e integração, contratos V3/V4, contexto independente e autoria ausente/divergente.
O replay offline de Capella preserva as provas reais sem acessar runtime ou modelo;
os cinco testes de persistência passaram em MySQL 5.7 descartável.

A compatibilidade é de projeção de metadados e consumo dos mesmos bytes. As provas
funcionais anteriores e as revisões independentes continuam exigidas; a atualização
não transforma testes em venda, utilidade humana, contribuição ou publicação. Os
dados exportados do replay ficam fora do Git. O empacotador canônico deve conferir
a atestação sucessora e o conjunto completo de manifestos antes do novo commit.

A revalidação dos revisores passou: Psique, 173 testes (dez condicionais não
habilitados), e Têmis, 113 testes (um condicional não habilitado), sem falhas/erros.
O empacotador conferiu 94 manifestos. Os 22 testes da seleção de deploy passaram;
a seleção direta da nova atestação retorna `target=none`, preservando as
publicações comerciais próprias e a imutabilidade da v11.
