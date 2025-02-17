package io.github.edsuns;

import io.github.edsuns.util.SimpleCacheValue;
import io.github.edsuns.util.SingleFlight;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author edsuns@qq.com
 * @since 2024/1/23 11:56
 */
@ParametersAreNonnullByDefault
public class CacheManagerImpl<X, ID> implements CacheManager<X, ID> {
    protected final CacheStorage cacheStorage;
    protected final Database<X, ID> database;
    protected final Function<X, ID> idGetter;
    protected final SingleFlight<Collection<ID>> single;

    public CacheManagerImpl(CacheStorage cacheStorage, Database<X, ID> database, Function<X, ID> idGetter) {
        this(cacheStorage, database, idGetter, new SingleFlight<>());
    }

    public CacheManagerImpl(CacheStorage cacheStorage, Database<X, ID> database,
                            Function<X, ID> idGetter, SingleFlight<Collection<ID>> single) {
        this.cacheStorage = cacheStorage;
        this.database = database;
        this.idGetter = idGetter;
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
            if (cache != null && !cache.isNullCache()) {
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
            List<X> data = single.call(unCachedIds, () -> database.load(unCachedIds));
            // un-cached id that has no data will be cached by a null value
            cacheStorage.put(values(unCached, data));
            data.forEach(x -> result.put(idGetter.apply(x), x));
        }
        return ids.stream().map(result::get).collect(Collectors.toList());
    }

    @Override
    public void updateByIds(Collection<X> entities) {
        Map<ID, CacheValue> versionMap = getIdValueMap(entities.stream().map(idGetter).collect(Collectors.toList()));
        database.update(entities);
        cacheStorage.delete(cacheStorage.put(values(versionMap, entities)));
    }

    @Override
    public void setNulls(Collection<ID> ids) {
        Map<ID, CacheValue> versionMap = getIdValueMap(ids);
        cacheStorage.delete(cacheStorage.put(values(versionMap, Collections.emptyList())));
    }

    @Override
    public void delete(Collection<ID> ids) {
        cacheStorage.delete(composeKey(ids));
    }

    @Override
    public void scheduledRefreshUpdated() {
        String millisKey = getInvalidateUpdatedKey();
        long lastInvalidatedAt = cacheStorage.getTimestampMillis(millisKey);
        long now = System.currentTimeMillis();
        List<ID> ids = database.getIdsByUpdatedBetween(lastInvalidatedAt, now);
        // begin CAS scope
        Map<ID, CacheValue> versionMap = getIdValueMap(ids);
        List<X> data = database.load(ids);
        cacheStorage.delete(cacheStorage.put(values(versionMap, data)));
        cacheStorage.saveTimestampMillis(millisKey, now);
    }

    @Override
    public void scheduledRefreshAll() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    private Map<String, CacheValue> values(Map<ID, CacheValue> before, Collection<X> after) {
        Map<ID, X> afterMap = after.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> b));
        Map<String, CacheValue> result = new HashMap<>((int) (after.size() / .75 + 1));
        for (Map.Entry<ID, CacheValue> entry : before.entrySet()) {
            ID id = entry.getKey();
            String key = composeKey(id);
            X value = afterMap.get(id);// nullable
            result.put(key, new SimpleCacheValue(
                    Optional.ofNullable(entry.getValue()).map(CacheValue::getVersion).orElse(null),// initial null
                    value));
        }
        return result;
    }

    private Map<ID, CacheValue> getIdValueMap(Collection<ID> ids) {
        Iterator<CacheValue> it = cacheStorage.get(composeKey(ids)).iterator();
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
