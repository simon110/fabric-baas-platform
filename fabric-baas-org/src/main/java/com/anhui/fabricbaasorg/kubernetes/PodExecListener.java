package com.anhui.fabricbaasorg.kubernetes;

import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
@AllArgsConstructor
public class PodExecListener implements ExecListener {
    private final CountDownLatch countDownLatch;

    @Override
    public void onOpen() {
    }

    @Override
    public void onFailure(Throwable t, Response failureResponse) {
        countDownLatch.countDown();
    }

    @Override
    public void onClose(int i, String s) {
        countDownLatch.countDown();
    }
}
