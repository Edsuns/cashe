package io.github.edsuns.util;

import io.github.edsuns.CacheValue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 15:38
 */
@ParametersAreNonnullByDefault
public class SimpleCacheValue implements CacheValue {
    private static final long serialVersionUID = -8366514726030005449L;

    private int version;
    @Nullable
    private Object value;
    private boolean isHit;

    public SimpleCacheValue() {
    }

    public SimpleCacheValue(int version, @Nullable Object value, boolean isHit) {
        this.version = version;
        this.value = value;
        this.isHit = isHit;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Nullable
    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    @Override
    public boolean isHit() {
        return isHit;
    }

    public void setHit(boolean hit) {
        isHit = hit;
    }

}
