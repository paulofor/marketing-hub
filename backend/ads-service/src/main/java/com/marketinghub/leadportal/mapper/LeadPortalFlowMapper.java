package com.marketinghub.leadportal.mapper;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.dto.LeadPortalSimpleFormStyleDto;
import com.marketinghub.leadportal.dto.LeadPortalFlowDto;
import com.marketinghub.leadportal.dto.LeadPortalFlowQuestionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting lead portal flow entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface LeadPortalFlowMapper {
    @Mapping(target = "questions", source = "questions")
    @Mapping(target = "marketNicheId", source = "marketNiche.id")
    @Mapping(target = "experimentId", source = "experiment.id")
    LeadPortalFlowDto toDto(LeadPortalFlow flow);

    LeadPortalFlowQuestionDto toDto(LeadPortalFlowQuestion question);

    LeadPortalSimpleFormStyleDto toDto(LeadPortalSimpleFormStyle style);
}
