package com.anhui.fabricbaascommon.util;

import com.anhui.fabricbaascommon.function.ThrowableSupplier;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WatcherUtils {
    public static <E extends Exception> void waitFor(ThrowableSupplier<Boolean, E> supplier, int sleepMs, int timeoutMs) throws Exception {
        while (timeoutMs > 0) {
            TimeUnit.MILLISECONDS.sleep(sleepMs);
            timeoutMs -= sleepMs;
            if (supplier.get()) {
                return;
            }
        }
        throw new TimeoutException("等待超时：" + supplier.toString());
    }
}
