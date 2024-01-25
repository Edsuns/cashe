package io.github.edsuns;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 11:53
 */
@ParametersAreNonnullByDefault
public interface CacheManager<X, ID> {
    List<X> getByIds(Collection<ID> ids);

    void updateByIds(Map<ID, X> idEntities);

    default void setNulls(Collection<ID> ids) {
        Map<ID, X> nullValues = new HashMap<>();
        for (ID id : ids) {
            nullValues.put(id, null);
        }
        updateByIds(nullValues);
    }

    void scheduledInvalidateUpdated();

    void delete(Collection<ID> ids);
}
