package com.marketinghub.oprm.domain;

import java.util.List;

public record OccupationCatalogItem(
        String occupationName,
        List<String> aliases,
        String occupationSummary,
        List<String> taskList,
        List<String> skillsList,
        List<String> toolsList,
        List<String> workContextList,
        String sourceSystem,
        List<String> sourceRecordIds) {
}
