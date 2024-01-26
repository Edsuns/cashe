package io.github.edsuns;

import io.github.edsuns.redis.ArrayResultTypeScriptExecutor;
import io.github.edsuns.redis.RedisCacheStorage;
import io.github.edsuns.util.Some;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author edsuns@qq.com
 * @since 2024/1/25 9:41
 */
class CacheManagerTest {
    public static class Book {
        private Long id;
        private String name;

        public Book() {
        }

        public Book(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Book book = (Book) o;
            return Objects.equals(id, book.id) && Objects.equals(name, book.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "Book{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    @ParametersAreNonnullByDefault
    public static class BookDatabase implements Database<Book, Long> {
        private final Map<Long, Book> database = new HashMap<>() {{
            put(1L, new Book(1L, "book1"));
            put(2L, new Book(2L, "book2"));
        }};

        @Override
        public Map<Long, Book> load(Collection<Long> ids) {
            return database.entrySet().stream().filter(x -> ids.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public void update(Map<Long, Book> idEntities) {
            for (Map.Entry<Long, Book> entry : idEntities.entrySet()) {
                if (entry.getValue() == null) {
                    database.remove(entry.getKey());
                } else {
                    database.put(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public List<Long> getIdsByUpdatedAtBetween(long start, long end) {
            return Collections.singletonList(2L);
        }

        @Override
        public Map<Long, Book> getByUpdatedAtBetween(long start, long end) {
            return load(Collections.singletonList(2L));
        }

        @Override
        public String getEntityName() {
            return "book";
        }
    }

    private final RedisConnectionFactory connectionFactory = getConnectionFactory();
    private final CacheStorage storage = new RedisCacheStorage(connectionFactory);
    private final BookDatabase database = new BookDatabase();
    private final CacheManager<Book, Long> bookCacheManager = new CacheManagerImpl<>(storage, database);

    @Test
    void test() {
        // prepare
        String keyValue = "cashe:book:2";
        String keyVersion = "cashe:book:2:v";
        RedisTemplate<String, Object> redis = createRedisTemplate(connectionFactory);
        bookCacheManager.delete(Collections.singletonList(2L));

        List<Book> books = bookCacheManager.getByIds(Collections.singletonList(2L));
        assertEquals(1, books.size());
        assertEquals(new Book(2L, "book2"), books.get(0));

        database.update(new HashMap<>() {{
            put(2L, new Book(2L, "book2_2"));
        }});
        bookCacheManager.scheduledInvalidateUpdated();
        List<Book> books1 = bookCacheManager.getByIds(Collections.singletonList(2L));
        assertEquals(1, books1.size());
        assertEquals(new Book(2L, "book2_2"), books1.get(0));

        bookCacheManager.setNulls(Collections.singletonList(2L));
        List<Book> books2 = bookCacheManager.getByIds(Collections.singletonList(2L));
        assertEquals(1, books2.size());
        assertNull(books2.get(0));

        assertEquals(new Some(null), redis.opsForValue().get(keyValue));
        assertEquals(3, redis.opsForValue().get(keyVersion));

        bookCacheManager.updateByIds(new HashMap<>() {{
            put(2L, new Book(2L, "book2_3"));
        }});
        assertEquals(new Some(new Book(2L, "book2_3")), redis.opsForValue().get(keyValue));
        storage.invalidate(Collections.singletonList(keyValue));

        assertNull(redis.opsForValue().get(keyValue));
        assertEquals(5, redis.opsForValue().get(keyVersion));

        // rollback
        bookCacheManager.delete(Collections.singletonList(2L));
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