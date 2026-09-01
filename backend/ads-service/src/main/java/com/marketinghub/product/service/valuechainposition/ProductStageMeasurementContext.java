package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.agenttask.AgentTaskMeasurementSnapshot;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import java.util.List;

/** Responsabilidade: compartilhar as evidências já carregadas durante a resolução de um produto. */
record ProductStageMeasurementContext(
    List<AgentTaskMeasurementSnapshot> tasks,
    List<AgentTaskMeasurementSnapshot> commercialPlanTasks,
    List<StudioCostLedgerEntry> ledger) {}
