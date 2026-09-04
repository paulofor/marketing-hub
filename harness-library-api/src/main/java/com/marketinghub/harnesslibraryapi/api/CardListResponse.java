package com.marketinghub.harnesslibraryapi.api;

import java.util.List;

/** Agrupa as versões retornadas por uma consulta limitada. */
public record CardListResponse(int returnedItems, List<CardVersionResponse> items) {}
