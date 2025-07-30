package ru.wkov.modz;

import com.ibm.icu.text.CharsetDetector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzInfo extends Properties {

    private final Map<Object, List<Object>> info;

    public ModzInfo() {
        info = new ConcurrentHashMap<>();
    }

    public synchronized void load(Path path) throws IOException {
        try (var channel = newInputStream(path)) {
            load(channel);
        }
    }

    @Override
    public synchronized void load(InputStream stream) throws IOException {
        try (var buffer = new BufferedInputStream(stream)) {
            load(new CharsetDetector().getReader(buffer, UTF_8.name()));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T val(Object key) {
        var vals = info.get(key);
        return (vals == null || vals.isEmpty()) ? null : (T) vals.get(0);
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> vals(Object key) {
        return (List<E>) info.getOrDefault(key, List.of());
    }

    @Override
    public synchronized Object put(Object arg1, Object arg2) {
        if (arg1 instanceof String key) {
            key = normalizeSpace(key);

            if (key.isEmpty()) {
                return null;
            }

            if (key.charAt(0) == '\uFEFF') { // BOM
                key = key.substring(1);
            }

            arg1 = key;
        }

        var vals = info.computeIfAbsent(arg1, key -> new ArrayList<>());

        if (arg2 instanceof String val) {
            if ("name".equals(arg1) || "description".equals(arg1)) {
                vals.add(clear(val));
            } else {
                stream(split(strip(val), ",;="))
                        .filter(this::filter)
                        .forEachOrdered(vals::add);
            }
        } else {
            vals.add(arg2);
        }

        return null;
    }

    private String clear(String val) {
        val = strip(val);
        if (val != null) {
            var builder = new StringBuilder();
            for (int i = 0, k = -1; i < val.length(); i++) {
                var c = val.charAt(i);
                builder.append(c);
                if (c == '<') {
                    k = builder.length() - 1;
                } else if (c == '>' && k != -1) {
                    var sub = builder.substring(k);
                    if (sub.equals("<LINE>")) {
                        builder.replace(k, builder.length(), "\n");
                    } else {
                        builder.delete(k, builder.length());
                    }
                    k = -1;
                }
            }
            val = builder.toString();
        }
        return val;
    }

    private boolean filter(String val) {
        return isNotBlank(val) && !"require".equals(val);
    }
}
