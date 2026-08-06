package com.marketinghub.productai.dto;

import com.marketinghub.productai.PersonalizedSampleFunnelTemplate;

/** Define explicitamente o template reutilizável usado ao criar o funil de microamostra. */
public record CreatePersonalizedSampleFunnelRequest(PersonalizedSampleFunnelTemplate template) {}
