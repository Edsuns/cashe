package io.github.edsuns.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * @author edsuns@qq.com
 * @since 2024/1/24 16:17
 */
@ParametersAreNonnullByDefault
public class Some {
    @Nullable
    private Object value;

    public Some() {
    }

    public Some(@Nullable Object value) {
        this.value = value;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Some some = (Some) o;
        return Objects.equals(value, some.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Some{" +
                "value=" + value +
                '}';
    }
}
