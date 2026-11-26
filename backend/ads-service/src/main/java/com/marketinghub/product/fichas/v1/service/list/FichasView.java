package com.marketinghub.product.fichas.v1.service.list;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Responsabilidade: transportar retratos históricos sem conceder autorização operacional. */
public record FichasView(String ambiente, boolean despachoPermitido, List<JsonNode> fichas) {}
