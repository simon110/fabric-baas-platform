package com.anhui.fabricbaascommon.function;

@FunctionalInterface
public interface ThrowableSupplier<T, E extends Exception> {
    T get() throws E;
}
