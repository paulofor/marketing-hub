package com.marketinghub.product.bigidea.v1.service.research;

import jakarta.validation.constraints.Size;

/** Recebe fontes e orientação opcionais sem conceder autoridade comercial à pesquisa. */
public record StartBigIdeaResearchRequest(@Size(max = 12000) String brief) {}
