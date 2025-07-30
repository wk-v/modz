package ru.wkov.modz.data;

import com.ibm.icu.text.CharsetDetector;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzInfo extends Properties {

    private final Map<Object, List<Object>> map;

    public ModzInfo() {
        map = new HashMap<>();
    }

    @SuppressWarnings("all")
    public <T> T one(Object key) {
        var vals = map.get(key);
        return (vals == null || vals.isEmpty()) ? null : (T) vals.get(0);
    }

    @SuppressWarnings("all")
    public <E> List<E> all(Object key) {
        return (List<E>) map.getOrDefault(key, List.of());
    }

    @Override
    public synchronized void load(InputStream stream) throws IOException {
        try (var buffer = new BufferedInputStream(stream)) {
            load(new CharsetDetector().getReader(buffer, UTF_8.name()));
        }
    }

    public synchronized void load(Path path) throws IOException {
        try (var stream = Files.newInputStream(path)) {
            load(stream);
        }
    }

    @Override
    public synchronized Object put(Object key, Object val) {
        if (key instanceof String str) {
            str = StringUtils.normalizeSpace(str);
            if (StringUtils.isEmpty(str)) {
                return null;
            }
            if (str.charAt(0) == '\uFEFF') { // BOM
                str = str.substring(1);
            }
            key = str;
        }

        var vals = map.computeIfAbsent(key, unused -> new ArrayList<>(1));

        if (val instanceof String str) {
            str = StringUtils.strip(str);
            switch (key.toString()) {
                case "name", "description" -> {
                    if (StringUtils.isNotEmpty(str)) {
                        var builder = new StringBuilder();
                        for (int i = 0, k = -1; i < str.length(); i++) {
                            var c = str.charAt(i);
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
                        str = builder.toString();
                    }
                    vals.add(str);
                }
                default -> {
                    Arrays.stream(StringUtils.split(str, ",;="))
                            .filter(part -> StringUtils.isNotBlank(part) && !"require".equalsIgnoreCase(part))
                            .forEachOrdered(vals::add);
                }
            }
        }

        return null;
    }
}
