package io.github.edsuns.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author edsuns@qq.com
 * @since 2022/7/22
 */
public class SingleFlight<K> {

    private final ConcurrentMap<K, Call<?>> calls;

    public SingleFlight() {
        this(new ConcurrentHashMap<>());
    }

    public SingleFlight(ConcurrentMap<K, Call<?>> calls) {
        this.calls = calls;
    }

    @SuppressWarnings("unchecked")
    public <T> T call(K key, Supplier<T> fn) {
        if (key == null || fn == null) throw new NullPointerException();
        Call<T> call = (Call<T>) calls.computeIfAbsent(key, k -> new Call<>(fn));
        return call.get(v -> calls.remove(key));
    }

    private static class Call<T> {
        private final Supplier<T> fn;
        private volatile T val;

        Call(Supplier<T> fn) {
            this.fn = fn;
        }

        T get(Consumer<T> onLoaded) {
            T v = val;
            if (v == null) {
                synchronized (this) {
                    v = val;
                    if (v == null) {
                        v = val = fn.get();
                        onLoaded.accept(v);
                    }
                }
            }
            return v;
        }
    }
}
