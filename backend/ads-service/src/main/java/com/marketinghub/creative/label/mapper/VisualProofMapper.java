package com.marketinghub.creative.label.mapper;

import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.creative.label.dto.VisualProofDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VisualProofMapper {
    VisualProofDto toDto(VisualProof proof);
}
