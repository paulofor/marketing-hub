package com.marketinghub.oprmcoletormei.marketimport.dto;

public record OprmImportFileSeedDto(
        String fileName,
        String fileUrl,
        String datasetType,
        String status
) {}
