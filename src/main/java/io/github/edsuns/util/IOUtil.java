package io.github.edsuns.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author edsuns@qq.com
 * @since 2024/1/24 15:58
 */
public class IOUtil {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static String getResourceAsString(String classpath) {
        try (InputStream in = IOUtil.class.getClassLoader().getResourceAsStream(classpath)) {
            return readString(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readString(InputStream in) throws IOException {
        return readString(in, StandardCharsets.UTF_8);
    }

    public static String readString(InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transferTo(in, out);
        return out.toString(charset);
    }

    public static long transferTo(InputStream in, OutputStream out) throws IOException {
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }
}
