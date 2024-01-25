package io.github.edsuns;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 14:02
 */
@ParametersAreNonnullByDefault
public interface CacheValue extends Serializable {

    int getVersion();

    @Nullable
    Object getValue();

    boolean isHit();
}
