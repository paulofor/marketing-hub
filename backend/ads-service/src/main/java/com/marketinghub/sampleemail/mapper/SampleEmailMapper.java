package com.marketinghub.sampleemail.mapper;

import com.marketinghub.sampleemail.SampleEmail;
import com.marketinghub.sampleemail.dto.SampleEmailDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para converter {@link SampleEmail} em DTOs.
 */
@Mapper(componentModel = "spring")
public interface SampleEmailMapper {

    @Mapping(target = "experimentId", source = "experiment.id")
    @Mapping(target = "selected", expression = "java(email.getExperiment() != null && email.getExperiment().getSelectedSampleEmail() != null && email.getExperiment().getSelectedSampleEmail().getId() != null && email.getExperiment().getSelectedSampleEmail().getId().equals(email.getId()))")
    SampleEmailDto toDto(SampleEmail email);
}
