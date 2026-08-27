package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.util.List;

/** Responsabilidade: agrupar as tarefas reais do produto sob uma atividade estável do processo. */
public record ProductProcessActivityExecutionGroupResponse(
    Long activityDefinitionId,
    String activityId,
    String activityName,
    String activityObjective,
    String activityOwnerName,
    Integer sequenceNumber,
    boolean selectedVersionActivity,
    int taskCount,
    List<BusinessProcessActivityExecutionResponse> tasks) {}
