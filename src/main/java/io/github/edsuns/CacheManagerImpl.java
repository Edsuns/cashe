package io.github.edsuns;

import io.github.edsuns.util.SimpleCacheValue;
import io.github.edsuns.util.SingleFlight;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 11:56
 */
@ParametersAreNonnullByDefault
public class CacheManagerImpl<X, ID> implements CacheManager<X, ID> {
    protected final CacheStorage storage;
    protected final Database<X, ID> database;
    protected final SingleFlight<Collection<ID>> single;

    public CacheManagerImpl(CacheStorage storage, Database<X, ID> database) {
        this(storage, database, new SingleFlight<>());
    }

    public CacheManagerImpl(CacheStorage storage, Database<X, ID> database, SingleFlight<Collection<ID>> single) {
        this.storage = storage;
        this.database = database;
        this.single = single;
    }

    @Override
    public List<X> getByIds(Collection<ID> ids) {
        Map<ID, CacheValue> cacheMap = getIdValueMap(ids);
        Map<ID, X> result = new HashMap<>((int) (ids.size() / .75 + 1));
        Map<ID, CacheValue> unCached = null;
        for (ID id : ids) {
            @Nullable
            CacheValue cache = cacheMap.get(id);
            if (cache != null && cache.isHit()) {
                @Nullable
                @SuppressWarnings("unchecked")
                X x = (X) cache.getValue();
                result.put(id, x);
            } else {
                if (unCached == null) {
                    unCached = new HashMap<>();
                }
                unCached.put(id, cache);
            }
        }
        if (unCached != null && !unCached.isEmpty()) {
            final Set<ID> unCachedIds = unCached.keySet();
            Map<ID, X> data = single.call(unCachedIds, () -> database.load(unCachedIds));
            // un-cached id that has no data will be cached by a null value
            storage.put(values(unCached, data));
            result.putAll(data);
        }
        return ids.stream().map(result::get).collect(Collectors.toList());
    }

    @Override
    public void updateByIds(Map<ID, X> idEntities) {
        Map<ID, CacheValue> versionMap = getIdValueMap(idEntities.keySet());
        database.update(idEntities);
        storage.invalidate(storage.put(values(versionMap, idEntities)));
    }

    @Override
    public void scheduledInvalidateUpdated() {
        String millisKey = getInvalidateUpdatedKey();
        long lastInvalidatedAt = storage.getTimestampMillis(millisKey);
        long now = System.currentTimeMillis();
        // query
        List<ID> ids = database.getIdsByUpdatedAtBetween(lastInvalidatedAt, now);
        Map<ID, CacheValue> versionMap = getIdValueMap(ids);
        // query second time
        Map<ID, ?> data = database.getByUpdatedAtBetween(lastInvalidatedAt, now);
        storage.invalidate(storage.put(values(versionMap, data)));
        storage.saveTimestampMillis(millisKey, now);
    }

    @Override
    public void delete(Collection<ID> ids) {
        storage.delete(composeKey(ids));
    }

    private Map<String, CacheValue> values(Map<ID, CacheValue> before, Map<ID, ?> after) {
        return before.entrySet().stream()
                .collect(Collectors.toMap(
                                x -> composeKey(x.getKey()),
                                x -> new SimpleCacheValue(
                                        Optional.ofNullable(x.getValue()).map(CacheValue::getVersion).orElse(null),
                                        after.get(x.getKey()),// nullable
                                        true)
                        )
                );
    }

    private Map<ID, CacheValue> getIdValueMap(Collection<ID> ids) {
        Iterator<CacheValue> it = storage.get(composeKey(ids)).iterator();
        Map<ID, CacheValue> cacheMap = new HashMap<>();
        for (ID id : ids) {
            cacheMap.put(id, it.hasNext() ? it.next() : null);
        }
        return cacheMap;
    }

    protected String getInvalidateUpdatedKey() {
        return getCacheKeyPrefix() + ":" + database.getEntityName() + ":_millis_";
    }

    protected List<String> composeKey(Collection<ID> ids) {
        return ids.stream().map(this::composeKey).collect(Collectors.toList());
    }

    protected String composeKey(ID id) {
        return getCacheKeyPrefix() + ":" + database.getEntityName() + ":" + id;
    }

    protected String getCacheKeyPrefix() {
        return "cashe";
    }

}
