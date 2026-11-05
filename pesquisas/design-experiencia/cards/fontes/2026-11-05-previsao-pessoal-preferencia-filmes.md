# Fonte revisada — Expectativa pessoal na previsão de preferência por filmes

**Referência científica:** Zihan Chen, Lei Nico Zheng e Di Zhu, *You Know What You Will Like: People Predict Their Own Ratings Better Than Recommender Systems*. arXiv:2611.06696v1, submetido em 04/11/2026. https://arxiv.org/html/2611.06696v1

## Evidência encontrada
Análise observacional de expectativas sobre filmes no MovieLens, coletadas em 2023–2024. Dos **26.020 palpites** emitidos, apenas **551 pares** de previsão e nota posterior (de **277 usuários**) atenderam ao critério de nota registrada ao menos um dia depois. Nas observações analisadas, a correlação entre nota prevista pela própria pessoa e nota posterior foi **0,69**, contra **0,34** do MovieLens e **0,54–0,59** dos recomendadores comparadores treinados com avaliações atualizadas. Incluir a expectativa atual nos modelos reduziu o erro quadrático médio fora da amostra em **26%**. Aproximadamente **98,8% das solicitações de palpites** da plataforma ficaram sem resposta. Para ordenação de filmes conhecidos e bem avaliados, os recomendadores puderam empatar ou superar pessoas.

## Hipótese interpretativa
A previsão pontual do usuário pode conter conhecimento contextual sobre gosto e o item específico que registros passados não representam inteiramente. O estudo não isolou conhecimento privado, exposição prévia ao conteúdo, influência da pergunta nem auto-seleção.

## Aplicação possível
Em recomendações de produtos e conteúdos digitais do Marketing Hub, experimentar solicitação **opcional e contextual** da expectativa do próprio usuário para os itens em comparação, junto com sinais comportamentais consentidos. Evitar questionários repetitivos ou perfis invasivos.

## Resultados reais observados
Foram analisadas previsões de avaliações e avaliações posteriores de filmes na amostra selecionada. Não foram medidas compras, conversão comercial, satisfação de produtos pagos nem aumento causal de precisão provocado pela pergunta.

## Riscos e limites
Working paper/preprint, uma única plataforma de cinéfilos, amostra selecionada (551 pares equivalem a cerca de 2,1% dos palpites), muitos convites sem resposta, e observação condicionada a o usuário assistir e avaliar. Não generalizar para compras B2C ou concluir que perguntas obrigatórias melhoram recomendação.
