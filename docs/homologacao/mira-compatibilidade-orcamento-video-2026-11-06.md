# Compatibilidade do harness compartilhado — 06/11/2026

Escopo: transporte de inferência limitada de um único vídeo autorizado, recuperação
idempotente e anexos dos frames do MP4 final. O recibo é resolvido no backend; tarefas
sem esse recibo preservam o transporte anterior. Não há inferência, tarefa nova,
parecer renovado, aquisição, ativação de candidato ou republicação de Mira.

O manifesto v79 preserva v78 como referência imutável e atualiza os hashes dos arquivos
compartilhados efetivamente alterados. A experiência, o fingerprint publicado e o
`publishedByManifest` v25 permanecem iguais; `automaticDeployOnMerge` continua falso.
A atualização não atesta qualidade premium nem impacto comercial.

Verificações de compatibilidade: transporte nativo com HTTP local (recibo vigente,
recusa e callback perdido, nenhum provedor real), construtor do bundle e resolvedor
de publicação, loader de evidências e suíte de Têmis. Hash ou contexto incompatível
continua sendo erro; não remover arquivo obrigatório para fazer o teste passar.
Os resultados consolidados da entrega pertencem a
`docs/homologacao/vega-retomada-video-2026-11-05.md`.
