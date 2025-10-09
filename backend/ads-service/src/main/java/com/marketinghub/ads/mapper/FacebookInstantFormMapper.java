package com.marketinghub.ads.mapper;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.dto.FacebookInstantFormDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FacebookInstantFormMapper {
    @Mapping(target = "hypothesisId", source = "hypothesis.id")
    @Mapping(target = "facebookPageId", source = "page.id")
    @Mapping(target = "facebookPageExternalId", source = "page.pageId")
    @Mapping(target = "facebookPageName", source = "page.name")
    @Mapping(target = "facebookFormId", source = "formId")
    FacebookInstantFormDto toDto(FacebookInstantForm form);
}
