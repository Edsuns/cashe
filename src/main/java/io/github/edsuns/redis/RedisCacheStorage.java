package io.github.edsuns.redis;

import io.github.edsuns.CacheStorage;
import io.github.edsuns.CacheValue;
import io.github.edsuns.util.SimpleCacheValue;
import io.github.edsuns.util.Some;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.edsuns.util.IOUtil.getResourceAsString;

/**
 * @author edsuns@qq.com
 * @since 2024/1/24 11:28
 */
@ParametersAreNonnullByDefault
public class RedisCacheStorage implements CacheStorage {
    private static final String KEY_VERSION = ":v";

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisScript<Object[][]> luaGetVersionedValues;
    private final RedisScript<String[]> luaPutVersionedValues;
    private final RedisScript<Long> luaClearValuesIncrVersions;

    public RedisCacheStorage(RedisConnectionFactory connectionFactory) {
        this.redisTemplate = createRedisTemplate(connectionFactory);
        this.luaGetVersionedValues = RedisScript.of(getResourceAsString("scripts/get_versioned_values.lua"), Object[][].class);
        this.luaPutVersionedValues = RedisScript.of(getResourceAsString("scripts/put_versioned_values.lua"), String[].class);
        this.luaClearValuesIncrVersions = RedisScript.of(getResourceAsString("scripts/clear_values_incr_versions.lua"), Long.class);
    }

    private static RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setScriptExecutor(new ArrayResultTypeScriptExecutor<>(template));
        template.afterPropertiesSet();
        return template;
    }

    @Override
    public List<String> put(Map<String, CacheValue> values) {
        Set<Map.Entry<String, CacheValue>> entries = values.entrySet();
        List<String> keys = Stream.concat(
                        entries.stream().map(Map.Entry::getKey),
                        entries.stream().map(x -> version(x.getKey()))
                )
                .collect(Collectors.toList());

        Object[] args = new Object[entries.size() * 2];
        Iterator<Map.Entry<String, CacheValue>> iterator = entries.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            CacheValue next = iterator.next().getValue();
            args[i] = new Some(next.getValue());
            args[i + entries.size()] = next.getVersion();
        }

        String[] result = redisTemplate.execute(luaPutVersionedValues, keys, args);
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(result);
    }

    @Override
    public List<CacheValue> get(Collection<String> keys) {
        Object[][] result = redisTemplate.execute(luaGetVersionedValues, keysAndVersions(keys));
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(result).map(x -> {
                    Some some = (Some) x[0];
                    SimpleCacheValue cached = new SimpleCacheValue();
                    if (some != null) {
                        cached.setValue(some.getValue());
                        cached.setHit(true);
                    }
                    Object version = x[1];
                    if (version != null) {
                        cached.setVersion((int) version);
                    }
                    return cached;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void invalidate(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        redisTemplate.execute(luaClearValuesIncrVersions, keysAndVersions(keys));
    }

    @Override
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keysAndVersions(keys));
    }

    private static List<String> keysAndVersions(Collection<String> keys) {
        return Stream.concat(keys.stream(), keys.stream().map(RedisCacheStorage::version))
                .collect(Collectors.toList());
    }

    private static String version(String key) {
        return key + KEY_VERSION;
    }

    @Override
    public void saveTimestampMillis(String key, long timeMillis) {
        redisTemplate.opsForValue().set(key, timeMillis);
    }

    @Override
    public long getTimestampMillis(String key) {
        Long millis = (Long) redisTemplate.opsForValue().get(key);
        if (millis == null) {
            return 0L;
        }
        return millis;
    }
}
