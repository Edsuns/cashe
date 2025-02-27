package io.github.edsuns;

import io.github.edsuns.util.Some;
import org.junit.jupiter.api.Test;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

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
        private final List<Book> database = Arrays.asList(new Book(1L, "book1"), new Book(2L, "book2"));

        @Override
        public List<Book> load(Collection<Long> ids) {
            return database;
        }

        @Override
        public void update(Collection<Book> entities) {
            for (Book book : database) {
                for (Book target : entities) {
                    if (Objects.equals(book.getId(), target.getId())) {
                        book.setName(target.getName());
                    }
                }
            }
        }

        @Override
        public List<Long> getIdsByUpdatedBetween(long start, long end) {
            return Collections.singletonList(2L);
        }

        @Override
        public String getEntityName() {
            return "book";
        }
    }

    private TestContext testContext = new TestContext();

    @Test
    void test() {
        // prepare
        String keyValue = "cashe:book:2";
        String keyVersion = "cashe:book:2:v";
        testContext.getBookCacheManager().delete(Collections.singletonList(2L));

        List<Book> books = testContext.getBookCacheManager().getByIds(Collections.singletonList(2L));
        assertEquals(1, books.size());
        assertEquals(new Book(2L, "book2"), books.get(0));

        testContext.getDatabase().update(Collections.singletonList(new Book(2L, "book2_2")));
        testContext.getBookCacheManager().scheduledRefreshUpdated();
        List<Book> books1 = testContext.getBookCacheManager().getByIds(Collections.singletonList(2L));
        assertEquals(1, books1.size());
        assertEquals(new Book(2L, "book2_2"), books1.get(0));

        testContext.getBookCacheManager().setNulls(Collections.singletonList(2L));
        List<Book> books2 = testContext.getBookCacheManager().getByIds(Collections.singletonList(2L));
        assertEquals(1, books2.size());
        assertNull(books2.get(0));

        assertEquals(new Some(null), testContext.getRedis().opsForValue().get(keyValue));
        assertEquals(3, testContext.getRedis().opsForValue().get(keyVersion));

        testContext.getBookCacheManager().updateByIds(Collections.singletonList(new Book(2L, "book2_3")));
        assertEquals(new Some(new Book(2L, "book2_3")), testContext.getRedis().opsForValue().get(keyValue));
        testContext.getStorage().delete(Collections.singletonList(keyValue));

        assertNull(testContext.getRedis().opsForValue().get(keyValue));
        assertNull(testContext.getRedis().opsForValue().get(keyVersion));

        // rollback
        testContext.getBookCacheManager().delete(Collections.singletonList(2L));
    }
}