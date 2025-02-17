package io.github.edsuns;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 11:53
 */
@ParametersAreNonnullByDefault
public interface CacheManager<X, ID> {
    List<X> getByIds(Collection<ID> ids);

    void updateByIds(Collection<X> entities);

    /**
     * Sets null cache when delete data.
     */
    void setNulls(Collection<ID> ids);

    void delete(Collection<ID> ids);

    void scheduledRefreshUpdated();

    void scheduledRefreshAll();
}
