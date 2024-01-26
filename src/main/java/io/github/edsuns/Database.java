package io.github.edsuns;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author edsuns@qq.com
 * @since 2024/1/25 14:39
 */
@ParametersAreNonnullByDefault
public interface Database<X, ID> {

    Map<ID, X> load(Collection<ID> ids);

    /**
     * @param idEntities {@code null} value of map to delete the data
     */
    void update(Map<ID, X> idEntities);

    List<ID> getIdsByUpdatedAtBetween(long start, long end);

    Map<ID, X> getByUpdatedAtBetween(long start, long end);

    String getEntityName();
}
