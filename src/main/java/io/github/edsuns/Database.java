package io.github.edsuns;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * @author edsuns@qq.com
 * @since 2024/1/25 14:39
 */
@ParametersAreNonnullByDefault
public interface Database<X, ID> {

    List<X> load(Collection<ID> ids);

    void update(Collection<X> entities);

    List<ID> getIdsByUpdatedBetween(long start, long end);

    String getEntityName();
}
