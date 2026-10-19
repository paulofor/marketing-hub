# Compatibilidade de Mira com a política de modelos estratégicos

A política híbrida de Astra altera somente as descrições de runtime no catálogo
compartilhado de agentes. A atestação privada v29 de Mira fixava o hash anterior
desse catálogo; o empacotamento recusou corretamente a divergência.

Foram revalidados 21 testes de catálogo/detalhe/versão do backend, os 14 testes de
serviço e controller privados de Mira e 132 testes de tarefas/recuperação. Os
373 testes iniciais dos agentes e contratos, a matriz local MySQL e as 12
conferências do painel estão descritos em `agentes-modelo-estrategico-v1.md`.
Os 16 testes do empacotador preservam divergência bloqueante, histórico e
segregação. A alteração de formatação de AgentTaskServiceTest é isolada e não muda
comportamento; corrigiu uma violação anterior da baseline detectada no CI.

A atestação v30 preserva a v29 imutável, a experiência privada v3, sua publicação,
pareceres e autorizações. Atualiza somente a referência do catálogo validado e
vincula esta prova. Não é revisão comercial, aprovação de artefato ou autorização
de tráfego. Nenhuma tarefa de Mira é criada ou repetida. O seletor deve indicar
`frontend-version=none`: a atestação declara `automaticDeployOnMerge=false`.

Limite: os testes comprovam compatibilidade e seleção do modelo. Não medem a
qualidade de uma nova inferência, conversão ou contribuição comercial.
