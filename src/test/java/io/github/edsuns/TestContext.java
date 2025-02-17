package io.github.edsuns;

import io.github.edsuns.redis.ArrayResultTypeScriptExecutor;
import io.github.edsuns.redis.RedisCacheStorage;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author edsuns@qq.com
 * @since 2024/1/26 17:49
 */
public class TestContext {

    private final RedisConnectionFactory connectionFactory = getConnectionFactory();
    private final CacheStorage storage = new RedisCacheStorage(connectionFactory);
    private final CacheManagerTest.BookDatabase database = new CacheManagerTest.BookDatabase();
    private final CacheManager<CacheManagerTest.Book, Long> bookCacheManager = new CacheManagerImpl<>(storage, database, CacheManagerTest.Book::getId);
    private final RedisTemplate<String, Object> redis = createRedisTemplate(getConnectionFactory());

    public CacheStorage getStorage() {
        return storage;
    }

    public CacheManagerTest.BookDatabase getDatabase() {
        return database;
    }

    public CacheManager<CacheManagerTest.Book, Long> getBookCacheManager() {
        return bookCacheManager;
    }

    public RedisTemplate<String, Object> getRedis() {
        return redis;
    }

    private static LettuceConnectionFactory getConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("127.0.0.1");
        configuration.setPort(6379);
        configuration.setPassword("eMtyKVge8oLd2t81");
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
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
}
