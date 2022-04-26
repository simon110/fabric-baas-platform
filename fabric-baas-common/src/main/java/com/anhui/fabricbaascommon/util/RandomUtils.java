package com.anhui.fabricbaascommon.util;

import java.util.List;
import java.util.Random;

public class RandomUtils {
    private static final Random RANDOM = new Random();

    public static int generateInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static <T> T select(List<T> objects) {
        int i = generateInt(objects.size());
        return objects.get(i);
    }

    public static <T> T select(T[] objects) {
        int i = generateInt(objects.length);
        return objects[i];
    }
}
