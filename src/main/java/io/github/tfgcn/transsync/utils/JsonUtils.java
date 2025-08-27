package io.github.tfgcn.transsync.utils;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class JsonUtils {

    private static final Gson GSON;

    static {
        GSON = new GsonBuilder()
                .setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
    }

    private JsonUtils() {}

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> void writeFile(File file, T obj) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(obj, writer);
        }
    }

    public static <T> T readFile(File file, Type type) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }
}
