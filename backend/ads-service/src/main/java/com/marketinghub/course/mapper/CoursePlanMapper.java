package com.marketinghub.course.mapper;

import com.marketinghub.course.CoursePlan;
import com.marketinghub.course.dto.CoursePlanDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link CoursePlan}.
 */
@Mapper(componentModel = "spring")
public interface CoursePlanMapper {
    CoursePlanDto toDto(CoursePlan plan);
}
