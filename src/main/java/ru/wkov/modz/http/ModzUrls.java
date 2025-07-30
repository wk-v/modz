package ru.wkov.modz.http;

import java.net.URI;

import static java.net.URI.create;

/**
 * @author Vadim Kolesnikov (modz@wkov.ru)
 */
public class ModzUrls {

    private final StringBuilder builder;

    private ModzUrls(String api) {
        builder = new StringBuilder(api).append('?');
    }

    public static ModzUrls api(String api) {
        return new ModzUrls(api);
    }

    public ModzUrls add(String key, Object value) {
        builder.append(key.toLowerCase()).append('=').append(value).append('&');
        return this;
    }

    public ModzUrls add(String key, Iterable<?> values) {
        var k = 0;
        for (var value : values) {
            builder
                    .append(key.toLowerCase())
                    .append("%5B")
                    .append(k++)
                    .append("%5D")
                    .append('=')
                    .append(value)
                    .append('&');
        }
        return this;
    }

    public URI uri() {
        builder.setLength(builder.length() - 1);
        return create(builder.toString());
    }
}
