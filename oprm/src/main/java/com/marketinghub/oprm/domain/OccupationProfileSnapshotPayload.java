package com.marketinghub.oprm.domain;

import java.util.List;

public record OccupationProfileSnapshotPayload(
        String occupationName,
        String occupationSummary,
        List<String> taskList,
        List<String> skillsList,
        List<String> toolsList,
        List<String> workContextList,
        String sourceSystem,
        List<String> sourceRecordIds,
        OccupationAliasResolution aliasResolution,
        String nicheName,
        String locale) {
}
