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

    // fixedDelay单位为毫秒
    @Scheduled(fixedDelay = 120000)
    public void synchronizeChaincodeStatuses() {
        try {
            chaincodeService.synchronizeApprovedChaincodeStatuses();
        } catch (Exception ex) {
            // TODO: 邮件通知管理员
            log.error("同步链码状态时发生异常：" + ex);
        }
    }
}
