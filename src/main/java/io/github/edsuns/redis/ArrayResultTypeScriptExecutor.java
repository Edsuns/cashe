package io.github.edsuns.redis;

import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultScriptExecutor;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author edsuns@qq.com
 * @since 2024/1/25 11:47
 */
public class ArrayResultTypeScriptExecutor<K> extends DefaultScriptExecutor<K> {

    private final RedisTemplate<K, ?> template;

    /**
     * @param template The {@link RedisTemplate} to use
     */
    public ArrayResultTypeScriptExecutor(RedisTemplate<K, ?> template) {
        super(template);
        this.template = template;
    }

    @Override
    public <T> T execute(final RedisScript<T> script, final RedisSerializer<?> argsSerializer,
                         final RedisSerializer<T> resultSerializer, final List<K> keys, final Object... args) {
        return template.execute((RedisCallback<T>) connection -> {
            final ReturnType returnType = fromJavaType(script.getResultType());
            final byte[][] keysAndArgs = keysAndArgs(argsSerializer, keys, args);
            final int keySize = keys != null ? keys.size() : 0;
            if (connection.isPipelined() || connection.isQueueing()) {
                // We could script load first and then do evalsha to ensure sha is present,
                // but this adds a sha1 to exec/closePipeline results. Instead, just eval
                connection.eval(scriptBytes(script), returnType, keySize, keysAndArgs);
                return null;
            }
            return eval(connection, script, returnType, keySize, keysAndArgs, resultSerializer);
        });
    }

    @Override
    protected <T> T eval(RedisConnection connection, RedisScript<T> script, ReturnType returnType, int numKeys,
                         byte[][] keysAndArgs, RedisSerializer<T> resultSerializer) {

        Object result;
        try {
            result = connection.evalSha(script.getSha1(), returnType, numKeys, keysAndArgs);
        } catch (Exception e) {

            if (!ScriptUtils.exceptionContainsNoScriptError(e)) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RedisSystemException(e.getMessage(), e);
            }

            result = connection.eval(scriptBytes(script), returnType, numKeys, keysAndArgs);
        }

        if (script.getResultType() == null) {
            return null;
        }

        return deserializeResult(resultSerializer, script.getResultType(), result);
    }

    /**
     * @param javaType can be {@literal null} which translates to {@link ReturnType#STATUS}.
     * @return never {@literal null}.
     */
    public static ReturnType fromJavaType(@Nullable Class<?> javaType) {

        if (javaType == null) {
            return ReturnType.STATUS;
        }

        if (javaType.isArray()) {
            return ReturnType.MULTI;
        }

        return ReturnType.fromJavaType(javaType);
    }

    /**
     * @see org.springframework.data.redis.core.script.ScriptUtils#deserializeResult(RedisSerializer, Object)
     */
    @SuppressWarnings("unchecked")
    protected <T> T deserializeResult(RedisSerializer<T> resultSerializer, Class<?> resultType, Object result) {

        if (result instanceof byte[]) {
            return resultSerializer.deserialize((byte[]) result);
        }

        if (result instanceof List) {
            if (resultType.isArray()) {
                Object[] results = (Object[]) Array.newInstance(resultType.getComponentType(), ((List) result).size());
                for (int i = 0; i < results.length; i++) {
                    results[i] = deserializeResult(resultSerializer, resultType.getComponentType(), ((List) result).get(i));
                }

                return (T) results;
            } else {
                List<Object> results = new ArrayList<>(((List) result).size());

                for (Object obj : (List) result) {
                    results.add(deserializeResult(resultSerializer, resultType, obj));
                }

                return (T) results;
            }
        }

        return (T) result;
    }
}
