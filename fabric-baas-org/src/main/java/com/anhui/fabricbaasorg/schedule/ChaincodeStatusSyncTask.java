package com.anhui.fabricbaasorg.schedule;

import com.anhui.fabricbaasorg.service.ChaincodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChaincodeStatusSyncTask {
    @Autowired
    private ChaincodeService chaincodeService;

    @Scheduled(fixedDelay = 3600000)
    public void syncChaincodeStatuses() throws Exception {
        chaincodeService.updateAllApprovedChaincodeStatuses();
    }
}
