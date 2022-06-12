package io.mamish.therealobama;

import java.util.concurrent.Callable;

public final class Util {

    private Util() {}

    public static <T> T unlikely(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            throw new RuntimeException("Unlikely callable failed", e);
        }
    }

}
