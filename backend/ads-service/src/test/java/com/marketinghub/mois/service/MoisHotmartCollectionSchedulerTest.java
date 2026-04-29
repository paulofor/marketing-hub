package com.marketinghub.mois.service;

import org.junit.jupiter.api.Test;

class MoisHotmartCollectionSchedulerTest {

    @Test
    void shouldRunSimpleHeartbeatWithoutThrowing() {
        MoisHotmartCollectionScheduler scheduler = new MoisHotmartCollectionScheduler();

        scheduler.scheduleCollection();
    }
}
