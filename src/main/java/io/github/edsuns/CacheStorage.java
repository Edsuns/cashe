package io.github.edsuns;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 11:46
 */
@ParametersAreNonnullByDefault
public interface CacheStorage {
    /**
     * if value does exist, ignore cas and put
     *
     * @param values map key to {@link CacheValue}
     * @return id list that not be put because cas failed
     */
    List<String> put(Map<String, CacheValue> values);

    /**
     * @param keys id list of queried value
     * @return list {@link CacheValue} that contains id property
     */
    List<CacheValue> get(Collection<String> keys);

    void invalidate(Collection<String> keys);

    void delete(Collection<String> keys);

    void saveTimestampMillis(String key, long timeMillis);

    long getTimestampMillis(String key);
}
